/** Official OAuth clients; credentials are encrypted and never returned to clients/models. */
import { EvolutionError, readBoundedObject } from './evolution.ts';
type Store = { get<T>(key:string):Promise<T|undefined>; put<T>(key:string,value:T):Promise<void>; delete(key:string):Promise<boolean>; transaction<T>(fn:(s:Store)=>Promise<T>):Promise<T> };
export type ConnectEnv = { FLUX_CONNECT_KEY?:string; FLUX_PUBLIC_URL?:string; GOOGLE_CLIENT_ID?:string; GOOGLE_CLIENT_SECRET?:string; SPOTIFY_CLIENT_ID?:string; CANVA_CLIENT_ID?:string; CANVA_CLIENT_SECRET?:string; GITHUB_CLIENT_ID?:string; GITHUB_CLIENT_SECRET?:string; BRAVE_API_KEY?:string; MERCADOLIVRE_CLIENT_ID?:string; MERCADOLIVRE_CLIENT_SECRET?:string; SMARTTHINGS_CLIENT_ID?:string; SMARTTHINGS_CLIENT_SECRET?:string };
type Provider={id:string;name:string;authorize:string;token:string;scope:string;client:string;secret?:string;basic?:boolean;pkce?:boolean;test:string;revoke?:string};
const google=(id:string,name:string,scope:string,test:string):Provider=>({id,name,authorize:'https://accounts.google.com/o/oauth2/v2/auth',token:'https://oauth2.googleapis.com/token',scope,client:'GOOGLE_CLIENT_ID',secret:'GOOGLE_CLIENT_SECRET',test,revoke:'https://oauth2.googleapis.com/revoke'});
export const CONNECTORS:Provider[]=[
 google('gmail','Gmail','https://www.googleapis.com/auth/gmail.readonly','https://gmail.googleapis.com/gmail/v1/users/me/profile'),
 google('calendar','Google Calendar','https://www.googleapis.com/auth/calendar.events','https://www.googleapis.com/calendar/v3/calendars/primary/events?maxResults=1'),
 google('drive','Google Drive','https://www.googleapis.com/auth/drive.readonly','https://www.googleapis.com/drive/v3/files?pageSize=5&fields=files(id,name,mimeType,webViewLink)'),
 google('tasks','Google Tasks','https://www.googleapis.com/auth/tasks','https://tasks.googleapis.com/tasks/v1/users/@me/lists?maxResults=5'),
 google('youtube','YouTube','https://www.googleapis.com/auth/youtube.readonly','https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true'),
 {id:'spotify',name:'Spotify',authorize:'https://accounts.spotify.com/authorize',token:'https://accounts.spotify.com/api/token',scope:'user-read-private user-read-playback-state user-modify-playback-state playlist-read-private',client:'SPOTIFY_CLIENT_ID',test:'https://api.spotify.com/v1/me'},
 {id:'canva',name:'Canva',authorize:'https://www.canva.com/api/oauth/authorize',token:'https://api.canva.com/rest/v1/oauth/token',scope:'profile:read design:meta:read design:content:read',client:'CANVA_CLIENT_ID',secret:'CANVA_CLIENT_SECRET',basic:true,test:'https://api.canva.com/rest/v1/users/me'},
 {id:'mercadolivre',name:'Mercado Livre',authorize:'https://auth.mercadolivre.com.br/authorization',token:'https://api.mercadolibre.com/oauth/token',scope:'read offline_access',client:'MERCADOLIVRE_CLIENT_ID',secret:'MERCADOLIVRE_CLIENT_SECRET',test:'https://api.mercadolibre.com/users/me'},
 {id:'smartthings',name:'Samsung / SmartThings',authorize:'https://api.smartthings.com/oauth/authorize',token:'https://api.smartthings.com/oauth/token',scope:'r:devices:* x:devices:*',client:'SMARTTHINGS_CLIENT_ID',secret:'SMARTTHINGS_CLIENT_SECRET',basic:true,pkce:false,test:'https://api.smartthings.com/v1/devices'},
 {id:'github',name:'GitHub',authorize:'https://github.com/login/oauth/authorize',token:'https://github.com/login/oauth/access_token',scope:'read:user',client:'GITHUB_CLIENT_ID',secret:'GITHUB_CLIENT_SECRET',test:'https://api.github.com/user'},
];
const out=(body:unknown,status=200)=>Response.json(body,{status,headers:{'cache-control':'no-store'}});
const b64=(bytes:Uint8Array)=>btoa(String.fromCharCode(...bytes)).replaceAll('+','-').replaceAll('/','_').replaceAll('=','');
const unb64=(text:string)=>Uint8Array.from(atob(text.replaceAll('-','+').replaceAll('_','/')),c=>c.charCodeAt(0));
const random=()=>b64(crypto.getRandomValues(new Uint8Array(32)));
const digest=async(text:string)=>b64(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(text))));
type Grant={access:string;refresh?:string;expires:number;scope:string};
type Pending={provider:string;verifier:string;expires:number};
export class FluxConnect {
 constructor(private store:Store,private env:ConnectEnv,private network:typeof fetch=fetch){}
 private provider(id:string){const p=CONNECTORS.find(p=>p.id===id);if(!p)throw new EvolutionError(404,'Integração desconhecida.');return p;}
 private value(name:string){return (this.env as Record<string,string|undefined>)[name]?.trim()||'';}
 private async key(){const hex=this.env.FLUX_CONNECT_KEY||'';if(!/^[0-9a-f]{64}$/i.test(hex))throw new EvolutionError(503,'Criptografia das conexões ainda não configurada no servidor.');return crypto.subtle.importKey('raw',Uint8Array.from(hex.match(/../g)!,x=>parseInt(x,16)),{name:'AES-GCM'},false,['encrypt','decrypt']);}
 private async encrypt(id:string,value:unknown){const iv=crypto.getRandomValues(new Uint8Array(12));const encrypted=await crypto.subtle.encrypt({name:'AES-GCM',iv,additionalData:new TextEncoder().encode(id)},await this.key(),new TextEncoder().encode(JSON.stringify(value)));return {iv:b64(iv),data:b64(new Uint8Array(encrypted))};}
 private async decrypt<T>(id:string,value:{iv:string;data:string}):Promise<T>{const result=await crypto.subtle.decrypt({name:'AES-GCM',iv:unb64(value.iv),additionalData:new TextEncoder().encode(id)},await this.key(),unb64(value.data));return JSON.parse(new TextDecoder().decode(result));}
 private async load(id:string){const saved=await this.store.get<{iv:string;data:string}>('connect:grant:'+id);return saved?this.decrypt<Grant>(id,saved):null;}
 private async save(id:string,grant:Grant){await this.store.put('connect:grant:'+id,await this.encrypt(id,grant));}
 private callback(id:string){return (this.env.FLUX_PUBLIC_URL||'https://flux-evolution-preview.mauri20102023.workers.dev').replace(/\/$/,'')+'/v1/connect/callback/'+id;}
 async route(request:Request):Promise<Response|null>{
  const u=new URL(request.url),base='/v1/connect';
  if(u.pathname===base && request.method==='GET'){
   const providers=[];
   for(const p of CONNECTORS){const grant=await this.store.get('connect:grant:'+p.id);const evidence=await this.store.get('connect:evidence:'+p.id);const missing=[p.client,...(p.secret?[p.secret]:[]),'FLUX_CONNECT_KEY'].filter(k=>!this.value(k));providers.push({id:p.id,name:p.name,scope:p.scope,redirectUri:this.callback(p.id),state:grant?(evidence?'FUNCIONANDO':'PREPARADO'):'PREPARADO',connected:Boolean(grant),missing,evidence:evidence||null});}
   return out({providers,alternatives:[{id:'whatsapp',state:'PREPARADO',method:'Android ACTION_SEND/wa.me; usuário confirma envio. API Business exige conta empresarial e configuração própria.'},{id:'instagram',state:'PREPARADO',method:'Compartilhamento oficial Android. Publicação automatizada exige conta profissional e revisão Meta; cliente API ainda em desenvolvimento.'},{id:'facebook',state:'EM DESENVOLVIMENTO',method:'Publicação via API exige página, OAuth e aprovação Meta.'},{id:'cloudflare',state:'EM DESENVOLVIMENTO',method:'Builder deve usar executor com escopo e revisão; plugin do Codex não autoriza o FLUX.'},{id:'health',state:'EM DESENVOLVIMENTO',method:'Health Connect ainda não implementado; relógio deve sincronizar dados autorizados.'}]});
  }
  const match=u.pathname.match(/^\/v1\/connect\/([a-z]+)\/(start|test|read|action|revoke)$/);
  if(!match || request.method!=='POST')return null;
  const p=this.provider(match[1]),op=match[2];
  if(op==='start'){
   const missing=[p.client,...(p.secret?[p.secret]:[])].filter(k=>!this.value(k));if(missing.length)throw new EvolutionError(503,'Cadastre o aplicativo OAuth e configure no servidor: '+missing.join(', ')+'. Não envie segredos pelo chat.');
   await this.key();
   const state=random(),verifier=random();await this.store.put('connect:state:'+await digest(state),await this.encrypt('state',{provider:p.id,verifier,expires:Date.now()+600000}));
   const authorize=new URL(p.authorize);authorize.search=new URLSearchParams({client_id:this.value(p.client),response_type:'code',redirect_uri:this.callback(p.id),scope:p.scope,state}).toString();
   if(p.pkce!==false){authorize.searchParams.set('code_challenge',await digest(verifier));authorize.searchParams.set('code_challenge_method','S256');}
   if(p.client==='GOOGLE_CLIENT_ID'){authorize.searchParams.set('access_type','offline');authorize.searchParams.set('prompt','consent');}
   return out({url:authorize.toString(),expiresIn:600});
  }
  if(op==='revoke'){
   const grant=await this.load(p.id);let providerRevoked=false;
   try { if(grant&&p.revoke){const r=await this.network(p.revoke,{method:'POST',body:new URLSearchParams({token:grant.refresh||grant.access}),signal:AbortSignal.timeout(15000)});providerRevoked=r.ok;} }
   catch { /* Local revocation must succeed even when the provider is unavailable. */ }
   finally {await this.store.delete('connect:grant:'+p.id);await this.store.delete('connect:evidence:'+p.id);}
   return out({localRevoked:true,providerRevoked,message:providerRevoked?'Acesso revogado no provedor.':'Credencial excluída do FLUX. Revogue também na página de aplicativos conectados do provedor.'});
  }
  const token=await this.access(p);
  if(op==='test'){await this.get(p.test,token);const evidence={at:new Date().toISOString(),operation:'official API connection test',httpStatus:200};await this.store.put('connect:evidence:'+p.id,evidence);return out({connected:true,evidence});}
  const body=await readBoundedObject(request);
  if(op==='action')return out(await this.action(p,token,body));
  return out(await this.read(p,token,String(body.query||'').slice(0,200)));
 }
 async callbackRequest(request:Request):Promise<Response>{
  const u=new URL(request.url),p=this.provider(u.pathname.split('/').pop()||''),state=u.searchParams.get('state')||'';
  if(!/^[A-Za-z0-9_-]{43}$/.test(state))throw new EvolutionError(400,'Estado OAuth inválido. Inicie a conexão novamente.');
  const key='connect:state:'+await digest(state);
  const encrypted=await this.store.transaction(async s=>{const saved=await s.get<{iv:string;data:string}>(key);await s.delete(key);return saved;});
  if(!encrypted)throw new EvolutionError(400,'Autorização expirada ou já utilizada.');
  const pending=await this.decrypt<Pending>('state',encrypted);
  if(pending.provider!==p.id||pending.expires<Date.now())throw new EvolutionError(400,'Autorização expirada ou de outro serviço.');
  if(u.searchParams.has('error'))throw new EvolutionError(400,'A autorização foi recusada no provedor. Nenhuma conta foi conectada.');
  const code=u.searchParams.get('code');if(!code||code.length>4096)throw new EvolutionError(400,'Código OAuth ausente.');
  const grant=await this.exchange(p,{grant_type:'authorization_code',code,redirect_uri:this.callback(p.id),...(p.pkce!==false?{code_verifier:pending.verifier}:{})});await this.save(p.id,grant);
  return new Response('<!doctype html><meta name="viewport" content="width=device-width"><title>FLUX Connect</title><body style="background:#080d14;color:#e9f3fa;font:18px system-ui;padding:32px"><h1>Autorização recebida</h1><p>Volte ao FLUX e toque em Testar conexão. A conta só será marcada como validada depois da resposta real da API.</p></body>',{headers:{'content-type':'text/html;charset=utf-8','cache-control':'no-store','referrer-policy':'no-referrer','content-security-policy':"default-src 'none'; style-src 'unsafe-inline'"}});
 }
 private async exchange(p:Provider,fields:Record<string,string>):Promise<Grant>{
  const form=new URLSearchParams({...fields,client_id:this.value(p.client)}),headers:Record<string,string>={'accept':'application/json','content-type':'application/x-www-form-urlencoded','user-agent':'FLUX-AI'};
  if(p.secret){if(p.basic)headers.authorization='Basic '+btoa(this.value(p.client)+':'+this.value(p.secret));else form.set('client_secret',this.value(p.secret));}
  const r=await this.network(p.token,{method:'POST',headers,body:form,signal:AbortSignal.timeout(20000)});if(!r.ok)throw new EvolutionError(r.status===429?429:502,'O provedor recusou a autorização. Confira cadastro OAuth, callback e permissões.');
  const data=await this.boundedJson(r) as Record<string,unknown>;if(typeof data.access_token!=='string'||data.error)throw new EvolutionError(502,'O provedor não retornou uma credencial válida.');
  const seconds=p.id==='github'&&!data.expires_in?31536000:Math.min(86400,Math.max(60,Number(data.expires_in)||3600));return {access:data.access_token,refresh:typeof data.refresh_token==='string'?data.refresh_token:undefined,expires:Date.now()+seconds*1000,scope:String(data.scope||p.scope)};
 }
 private async access(p:Provider){const old=await this.load(p.id);if(!old)throw new EvolutionError(409,'Autorize esta conta antes de executar a operação.');if(old.expires>Date.now()+30000)return old.access;if(!old.refresh)throw new EvolutionError(409,'A sessão do provedor expirou. Reconecte a conta.');const next=await this.exchange(p,{grant_type:'refresh_token',refresh_token:old.refresh});next.refresh ||= old.refresh;await this.save(p.id,next);return next.access;}
 private async boundedJson(response:Response){
  const reader=response.body?.getReader();if(!reader)throw new EvolutionError(502,'Resposta vazia do provedor.');
  let length=0,text='';const decoder=new TextDecoder();
  try{while(true){const part=await reader.read();if(part.done)break;length+=part.value.byteLength;if(length>1000000){await reader.cancel();throw new EvolutionError(502,'Resposta do provedor acima do limite seguro.');}text+=decoder.decode(part.value,{stream:true});}text+=decoder.decode();}
  finally{reader.releaseLock();}
  try{return JSON.parse(text);}catch{throw new EvolutionError(502,'Formato de resposta inválido do provedor.');}
 }
 private async get(url:string,token:string){const r=await this.network(url,{headers:{authorization:'Bearer '+token,accept:'application/json','user-agent':'FLUX-AI'},signal:AbortSignal.timeout(20000)});if(!r.ok)throw new EvolutionError(r.status===429?429:502,`A API respondeu ${r.status}; operação não confirmada. Confira escopo, conta e limites.`);return r.status===204?{playback:null}:this.boundedJson(r);}
 private async read(p:Provider,token:string,query:string){
  switch(p.id){
   case 'gmail':{
    const listing=await this.get('https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=5&q='+encodeURIComponent(query||'in:inbox'),token);
    const rows=Array.isArray(listing.messages)?listing.messages.slice(0,5):[];
    const messages=await Promise.all(rows.map(async(row:{id:string})=>{
     if(!/^[A-Za-z0-9_-]{1,120}$/.test(row.id))throw new EvolutionError(502,'Identificador de mensagem inválido.');
     const mail=await this.get('https://gmail.googleapis.com/gmail/v1/users/me/messages/'+row.id+'?format=full',token);
     const headers:Array<{name:string;value:string}>=mail.payload?.headers||[];
     const header=(name:string)=>headers.find(h=>h.name?.toLowerCase()===name)?.value?.slice(0,300)||'';
     const texts:string[]=[];
     const walk=(part:{mimeType?:string;body?:{data?:string};parts?:unknown[]},depth=0)=>{if(depth>8||texts.join('').length>12000)return;if(part.mimeType==='text/plain'&&part.body?.data){try{texts.push(new TextDecoder().decode(unb64(part.body.data)).slice(0,12000));}catch{/* Unsupported part encoding is not fabricated. */}}for(const child of (part.parts||[]).slice(0,20))walk(child as typeof part,depth+1);};
     if(mail.payload)walk(mail.payload);
     return {id:row.id,subject:header('subject'),from:header('from'),date:header('date'),snippet:String(mail.snippet||'').slice(0,1000),text:texts.join('\n').slice(0,12000),attachmentsLoaded:false};
    }));
    return {messages,scope:'up to five messages; plaintext body only; attachments not fetched'};
   }
   case 'calendar':return this.get('https://www.googleapis.com/calendar/v3/calendars/primary/events?singleEvents=true&orderBy=startTime&maxResults=20&timeMin='+encodeURIComponent(new Date().toISOString()),token);
   case 'drive':return this.get('https://www.googleapis.com/drive/v3/files?pageSize=20&fields=files(id,name,mimeType,webViewLink)&q='+encodeURIComponent("trashed = false"+(query?" and name contains '"+query.replaceAll("'", "\\'")+"'":'')),token);
   case 'spotify':return this.get(query?'https://api.spotify.com/v1/search?type=track&limit=5&q='+encodeURIComponent(query):'https://api.spotify.com/v1/me/player',token);
   case 'youtube':return this.get('https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=5&q='+encodeURIComponent(query),token);
   case 'tasks':return this.get('https://tasks.googleapis.com/tasks/v1/lists/@default/tasks?maxResults=20',token);
   case 'github':return this.get('https://api.github.com/user/repos?per_page=10',token);
   case 'smartthings':return this.get('https://api.smartthings.com/v1/devices',token);
   case 'mercadolivre':return this.get('https://api.mercadolibre.com/users/me',token);
   case 'canva':return this.get('https://api.canva.com/rest/v1/designs',token);
   default:throw new EvolutionError(400,'Leitura não disponível.');
  }
 }
 private async action(p:Provider,token:string,body:Record<string,unknown>){
  if(body.confirm!==true)throw new EvolutionError(403,'Confirme explicitamente a ação antes de executá-la.');
  if(p.id==='smartthings'){
   const deviceId=String(body.deviceId||'');if(!/^[0-9a-f-]{36}$/i.test(deviceId))throw new EvolutionError(400,'Selecione um dispositivo válido retornado pelo SmartThings.');
   const commands:Record<string,{capability:string;command:string}>={on:{capability:'switch',command:'on'},off:{capability:'switch',command:'off'},play:{capability:'mediaPlayback',command:'play'},pause:{capability:'mediaPlayback',command:'pause'}};
   const selected=commands[String(body.action)];if(!selected)throw new EvolutionError(400,'Comando de dispositivo inválido.');
   const device=await this.get('https://api.smartthings.com/v1/devices/'+deviceId,token);
   if(!device.components?.some((c:{id:string;capabilities?:Array<{id:string}>})=>c.id==='main'&&c.capabilities?.some(cap=>cap.id===selected.capability)))throw new EvolutionError(409,'O dispositivo não anunciou esta capacidade; comando não enviado.');
   const response=await this.network('https://api.smartthings.com/v1/devices/'+deviceId+'/commands',{method:'POST',headers:{authorization:'Bearer '+token,'content-type':'application/json'},body:JSON.stringify({commands:[{component:'main',capability:selected.capability,command:selected.command,arguments:[]}]}),signal:AbortSignal.timeout(15000)});
   if(!response.ok)throw new EvolutionError(502,`Comando não confirmado pelo SmartThings (${response.status}).`);
   return {accepted:true,providerHttpStatus:response.status,deviceId,verification:await this.get('https://api.smartthings.com/v1/devices/'+deviceId+'/status',token).catch(()=>null),message:'API aceitou o comando. O estado retornado deve confirmar o resultado; não comprova exibição física de vídeo.'};
  }
  if(p.id!=='spotify')throw new EvolutionError(400,'Ações externas deste serviço ainda não implementadas.');
  const op=String(body.action),allowed:Record<string,string>={pause:'pause',play:'play',next:'next',previous:'previous'};
  if(!allowed[op])throw new EvolutionError(400,'Comando de reprodução inválido.');
  const r=await this.network('https://api.spotify.com/v1/me/player/'+allowed[op],{method:op==='next'||op==='previous'?'POST':'PUT',headers:{authorization:'Bearer '+token},signal:AbortSignal.timeout(15000)});
  if(!r.ok)throw new EvolutionError(502,`Reprodução não confirmada (${r.status}). Spotify pode exigir Premium e dispositivo ativo.`);
  return {accepted:true,providerHttpStatus:r.status,verification:await this.get('https://api.spotify.com/v1/me/player',token).catch(()=>null),message:'Comando aceito; confira o estado de reprodução retornado.'};
 }
}
