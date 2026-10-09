/** Workers AI may parse `response` as JSON. Prefer the textual completion. */
interface Completion {
  response?: unknown;
  output_text?: unknown;
  result?: { response?: unknown };
  choices?: Array<{ message?: { content?: unknown } }>;
  output?: Array<{ content?: Array<{ text?: unknown; refusal?: unknown }> }>;
}
export function modelText(raw: unknown): string {
  if (!raw || typeof raw !== 'object') return '';
  const r = raw as Completion;
  const choices = r.choices?.[0]?.message?.content;
  const parts = r.output?.flatMap(item => item.content ?? [])
    .map(part => typeof part.text === 'string' ? part.text : typeof part.refusal === 'string' ? part.refusal : '').join('');
  for (const candidate of [choices, r.output_text, parts, r.response, r.result?.response]) {
    if (typeof candidate === 'string' && candidate.trim()) return candidate.trim();
    if (typeof candidate === 'number' && Number.isFinite(candidate)) return String(candidate);
  }
  return '';
}
