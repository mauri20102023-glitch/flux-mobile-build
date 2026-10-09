import { EvolutionError, readBoundedObject } from './evolution.ts';
export interface SearchResult { title: string; url: string; excerpt: string; }
/** Search results are citations, never execution instructions; no arbitrary result URL is fetched. */
export class FluxResearch {
  constructor(private key: string | undefined, private reserve: () => Promise<void>, private network: typeof fetch = fetch) {}
  async route(request: Request): Promise<Response | null> {
    if (new URL(request.url).pathname !== '/v1/research') return null;
    if (request.method === 'GET') return Response.json({ state: 'PREPARADO', configured: Boolean(this.key), provider: 'Brave Search', required: this.key ? [] : ['BRAVE_API_KEY'], liveTestRequired: true }, { headers: { 'cache-control': 'no-store' } });
    if (request.method !== 'POST') throw new EvolutionError(405, 'Método inválido.');
    const body = await readBoundedObject(request);
    const query = typeof body.query === 'string' ? body.query.trim() : '';
    if (!query || query.length > 300) throw new EvolutionError(400, 'Informe uma pesquisa de até 300 caracteres.');
    if (!this.key) throw new EvolutionError(503, 'Pesquisa preparada: falta BRAVE_API_KEY no servidor. Cadastre a conta Brave Search e configure o segredo no painel seguro; não envie a chave pelo chat.');
    await this.reserve();
    const url = new URL('https://api.search.brave.com/res/v1/web/search');
    url.search = new URLSearchParams({ q: query, count: '5', country: 'BR', search_lang: 'pt-br', safesearch: 'strict' }).toString();
    const response = await this.network(url.toString(), { headers: { 'X-Subscription-Token': this.key, accept: 'application/json' }, signal: AbortSignal.timeout(20000) });
    if (!response.ok) throw new EvolutionError(response.status === 429 ? 429 : 502, `Pesquisa não confirmada (${response.status}). Confira chave, plano e limites do Brave.`);
    const reader = response.body?.getReader();
    if (!reader) throw new EvolutionError(502, 'Pesquisa retornou resposta vazia.');
    let text = '', bytes = 0; const decoder = new TextDecoder();
    try { while (true) { const part = await reader.read(); if (part.done) break; bytes += part.value.byteLength; if (bytes > 500000) { await reader.cancel(); throw new EvolutionError(502, 'Pesquisa ultrapassou o limite de resposta.'); } text += decoder.decode(part.value, { stream: true }); } text += decoder.decode(); }
    finally { reader.releaseLock(); }
    let data: { web?: { results?: Array<{ title?: unknown; url?: unknown; description?: unknown }> } };
    try { data = JSON.parse(text); } catch { throw new EvolutionError(502, 'Resposta de pesquisa inválida.'); }
    const results: SearchResult[] = (data.web?.results ?? []).slice(0, 5).flatMap(row => {
      if (typeof row.url !== 'string' || typeof row.title !== 'string') return [];
      try { const u = new URL(row.url); if (u.protocol !== 'https:') return []; } catch { return []; }
      return [{ title: row.title.slice(0, 200), url: row.url.slice(0, 2000), excerpt: typeof row.description === 'string' ? row.description.replace(/<[^>]*>/g, '').slice(0, 1200) : '' }];
    });
    return Response.json({ results, source: 'Brave Search', searchedAt: new Date().toISOString(), pagesRead: false, message: 'Trechos fornecidos pelo índice de pesquisa. Conteúdo integral das páginas não foi aberto nem verificado.' }, { headers: { 'cache-control': 'no-store' } });
  }
}
