/** Workers AI may return a parsed JSON scalar/object in `response`. Prefer textual completion. */
export function modelText(raw: unknown): string {
  if (!raw || typeof raw !== 'object') return '';
  const r = raw as Record<string, any>;
  const choices = r.choices?.[0]?.message?.content;
  const parts = r.output?.flatMap((item: any) => item.content ?? [])
    .map((part: any) => typeof part.text === 'string' ? part.text : typeof part.refusal === 'string' ? part.refusal : '').join('');
  for (const candidate of [choices, r.output_text, parts, r.response, r.result?.response]) {
    if (typeof candidate === 'string' && candidate.trim()) return candidate.trim();
    if (typeof candidate === 'number' && Number.isFinite(candidate)) return String(candidate);
  }
  return '';
}
