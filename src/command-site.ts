/// <reference path="./command-env.d.ts" />
import { EMBEDDED_ASSETS } from './embedded-assets.ts';
/** Only the interface is deployed here. All authenticated operations use the existing Core. */
export default {
  async fetch(request: Request, env: CommandEnv): Promise<Response> {
    const url=new URL(request.url);
    if(url.pathname==='/health'||url.pathname.startsWith('/v1/'))return env.FLUX_CORE.fetch(request);
    if(request.method!=='GET'&&request.method!=='HEAD')return new Response('Método inválido.',{status:405});
    const path=url.pathname==='/'?'/index.html':url.pathname;
    const asset=EMBEDDED_ASSETS[path];
    if(!asset)return new Response('Recurso não encontrado.',{status:404});
    const bytes=Uint8Array.from(atob(asset.base64),c=>c.charCodeAt(0));
    return new Response(request.method==='HEAD'?null:bytes,{headers:{'content-type':asset.mimeType,'cache-control':'no-store','x-content-type-options':'nosniff','referrer-policy':'strict-origin-when-cross-origin','permissions-policy':'camera=(self), microphone=(self), display-capture=(self)'}});
  }
};
