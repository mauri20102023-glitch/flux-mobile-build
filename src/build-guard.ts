import {EvolutionError} from './evolution.ts';
export const BUILD_REPOSITORY='mauri20102023-glitch/flux-mobile-build';
export const BUILD_BASE='main';
export const REQUIRED_CHECKS=['web','web-ui','android','android-ui'];
/** All omitted paths fail closed. Identity, auth, workflows, tests and policy cannot be rewritten. */
const ALLOWED=[/^web\/flux-ath\/components\/[A-Za-z0-9_/-]+\.(?:js|css)$/, /^src\/adapters\/[A-Za-z0-9_/-]+\.ts$/, /^apps\/android\/app\/src\/main\/java\/ai\/flux\/mobile\/components\/[A-Za-z0-9_/-]+\.kt$/, /^docs\/features\/[A-Za-z0-9_/-]+\.md$/];
export interface BuildFile{path:string;content:string}
export function validateBuildFiles(raw:unknown):BuildFile[]{
  if(!Array.isArray(raw)||!raw.length||raw.length>8)throw new EvolutionError(400,'Inclua de um a oito arquivos.');
  let total=0;const names=new Set<string>();
  return raw.map(item=>{
    if(!item||typeof item!=='object'||typeof item.path!=='string'||typeof item.content!=='string')throw new EvolutionError(400,'Arquivo de código inválido.');
    if(item.path.includes('..')||item.path.includes('//')||!ALLOWED.some(rule=>rule.test(item.path))||names.has(item.path))throw new EvolutionError(403,'Caminho protegido ou não permitido pelo BUILD_GUARD. Revisão manual necessária.');
    names.add(item.path);total+=new TextEncoder().encode(item.content).length;
    if(total>48000)throw new EvolutionError(413,'O patch ultrapassa 48 KB.');
    if(/(?:-----BEGIN .*PRIVATE KEY-----|\b(?:sk-proj-|gh[pousr]_|AIza)[A-Za-z0-9_-]{15,}|(?:api[_-]?key|password|authorization)\s*[:=]\s*["'](?:Bearer\s+)?[A-Za-z0-9_-]{24,})/i.test(item.content))throw new EvolutionError(400,'O patch parece conter uma credencial. Use configuração segura no servidor.');
    return {path:item.path,content:item.content};
  });
}
export function checksReady(checks:Array<{name:string;status:string;conclusion:string|null;head_sha?:string}>,head:string):boolean{
  return REQUIRED_CHECKS.every(name=>{const matching=checks.filter(c=>c.name===name&&c.head_sha===head);return matching.length>0&&matching.every(c=>c.status==='completed'&&c.conclusion==='success');});
}
