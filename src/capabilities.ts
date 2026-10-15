import { EvolutionError, readBoundedObject } from './evolution.ts';

type Store = {
  get<T>(key:string):Promise<T|undefined>;
  put<T>(key:string,value:T):Promise<void>;
  transaction<T>(callback:(store:Store)=>Promise<T>):Promise<T>;
  delete(key:string):Promise<boolean>;
};
type Provider = { id:string; connected:boolean; missing:string[]; evidence?:unknown };
type Catalog = { providers:Provider[] };
type Dependencies = {
  connect:(request:Request)=>Promise<Response|null>;
  calendar:()=>Promise<Response>;
  memories:()=>Promise<unknown[]>;
};
export type ToolResult = {
  id:string; state:'available'|'empty'|'unavailable'|'error'|'approval_required'|'accepted'|'verified';
  content:string; data?:unknown; accepted:boolean; verified:boolean; at:string;
  approvalId?:string;
};
type Tool = {id:string;name:string;when_to_use:string;provider?:string;operation?:'read'|'action';requires:string[];inputs:Record<string,unknown>;confirmation:'none'|'explicit';executor:string;verifier:string;detail:string};
const TOOLS:Tool[] = [
  {id:'calendar.read',name:'Consultar agenda',when_to_use:'Pedido para consultar compromissos',provider:'calendar',operation:'read',requires:['calendar_oauth_or_android_snapshot'],inputs:{},confirmation:'none',executor:'core.calendar',verifier:'authenticated_snapshot_or_provider_response',detail:'Leitura do Google Calendar autorizado ou snapshot enviado pelo Android.'},
  {id:'memory.list',name:'Consultar memórias autorizadas',when_to_use:'Listar memórias armazenadas com consentimento',requires:['paired_owner'],inputs:{},confirmation:'none',executor:'core.memories',verifier:'durable_storage',detail:'Memórias do proprietário; não inclui cofre nem notas privadas locais.'},
  {id:'gmail.messages.read',name:'Ler e-mails',when_to_use:'Consultar mensagens recentes ou pesquisar e-mails',provider:'gmail',operation:'read',requires:['gmail_oauth'],inputs:{query:{type:'string',maxLength:200}},confirmation:'none',executor:'connect.gmail.read',verifier:'official_api_response',detail:'Até cinco mensagens em texto; anexos não são carregados.'},
  {id:'drive.files.list',name:'Listar arquivos do Drive',when_to_use:'Localizar arquivos no Drive',provider:'drive',operation:'read',requires:['drive_oauth'],inputs:{query:{type:'string',maxLength:200}},confirmation:'none',executor:'connect.drive.read',verifier:'official_api_response',detail:'Listagem autorizada; não implica download ou edição.'},
  {id:'samsung_tv.devices.list',name:'Consultar dispositivos SmartThings',when_to_use:'Ver dispositivos Samsung autorizados',provider:'smartthings',operation:'read',requires:['smartthings_oauth'],inputs:{},confirmation:'none',executor:'connect.smartthings.read',verifier:'official_api_response',detail:'Somente aparelhos retornados pela conta; não presume modelo ou estado físico.'},
  {id:'samsung_tv.volume.set',name:'Ajustar volume da TV',when_to_use:'Definir volume de aparelho selecionado e compatível',provider:'smartthings',operation:'action',requires:['smartthings_oauth','device_audioVolume'],inputs:{deviceId:{type:'string',format:'uuid'},volume:{type:'integer',minimum:0,maximum:100}},confirmation:'explicit',executor:'connect.smartthings.action',verifier:'status.audioVolume.volume_after_command',detail:'Exige seleção do aparelho, capability audioVolume e confirmação; aceitação não comprova efeito físico.'},
  {id:'spotify.playback.read',name:'Consultar reprodução',when_to_use:'Ver o que está tocando no Spotify',provider:'spotify',operation:'read',requires:['spotify_oauth'],inputs:{},confirmation:'none',executor:'connect.spotify.read',verifier:'official_api_response',detail:'Conta e dispositivo de reprodução precisam estar disponíveis.'},
  {id:'spotify.playback.pause',name:'Pausar Spotify',when_to_use:'Pedido explícito para pausar reprodução',provider:'spotify',operation:'action',requires:['spotify_oauth','active_player','eligible_account'],inputs:{},confirmation:'explicit',executor:'connect.spotify.action',verifier:'playback.is_playing_after_command',detail:'Pode exigir Premium e aparelho ativo; confirme o resultado retornado.'},
  {id:'youtube.search',name:'Buscar vídeos no YouTube',when_to_use:'Procurar vídeo ou videoaula',provider:'youtube',operation:'read',requires:['youtube_oauth'],inputs:{query:{type:'string',maxLength:200}},confirmation:'none',executor:'connect.youtube.read',verifier:'official_api_response',detail:'Busca na API; abrir o vídeo ou controlar a TV são ações diferentes.'},
  {id:'canva.designs.list',name:'Consultar designs Canva',when_to_use:'Listar designs da conta autorizada',provider:'canva',operation:'read',requires:['canva_oauth'],inputs:{},confirmation:'none',executor:'connect.canva.read',verifier:'official_api_response',detail:'Não oferece edição nem criação nesta operação.'},
  {id:'mercadolivre.account.read',name:'Consultar conta Mercado Livre',when_to_use:'Consultar perfil autorizado do vendedor',provider:'mercadolivre',operation:'read',requires:['mercadolivre_oauth'],inputs:{},confirmation:'none',executor:'connect.mercadolivre.read',verifier:'official_api_response',detail:'Não representa consulta de vendas, pedidos ou campanhas.'},
  {id:'github.repositories.list',name:'Consultar repositórios GitHub',when_to_use:'Listar repositórios autorizados',provider:'github',operation:'read',requires:['github_oauth'],inputs:{},confirmation:'none',executor:'connect.github.read',verifier:'official_api_response',detail:'Não concede escrita ao Build.'},
];
type Approval = {toolId:string;inputs:Record<string,unknown>;actor:string;expires:number};
const result=(id:string,state:ToolResult['state'],content:string,data?:unknown):ToolResult=>({id,state,content,data,accepted:false,verified:false,at:new Date().toISOString()});
const isObject=(value:unknown):value is Record<string,unknown>=>Boolean(value)&&typeof value==='object'&&!Array.isArray(value);

/** Server-owned registry. A model never receives tokens or a confirmation executor. */
export class FluxCapabilities {
  constructor(private store:Store, private deps:Dependencies){}
  private async providers():Promise<Catalog>{
    const response=await this.deps.connect(new Request('https://flux.internal/v1/connect'));
    if(!response?.ok)throw new EvolutionError(503,'Catálogo de conexões indisponível.');
    return response.json() as Promise<Catalog>;
  }
  async catalog(){
    const [{providers},calendar]=await Promise.all([this.providers(),this.deps.calendar().then(async r=>await r.json() as {connected?:boolean;events?:unknown[]})]);
    return {schema:'flux-capability-registry-v1',checkedAt:new Date().toISOString(),capabilities:TOOLS.map(tool=>{
      const provider=providers.find(p=>p.id===tool.provider);
      const ready=!tool.provider||provider?.connected||tool.id==='calendar.read'&&calendar.connected===true;
      return {...tool,state:ready?'pronto':'conectar',serverExecutable:ready,detail:ready?tool.detail:'Autorize a conta e conclua a configuração no FLUX. '+tool.detail,connectionVerified:Boolean(provider?.evidence)};
    }),planned:[{id:'whatsapp.business.send',state:'planejado',detail:'Requer conta Business e adaptador de envio; WhatsApp pessoal usa compartilhamento assistido no Android.'},{id:'devices.remote.handoff',state:'planejado',detail:'A prévia do painel inferior não comprova transporte a um monitor.'},{id:'build.release',state:'planejado',detail:'O Build não tem executor de merge ou deploy de produção.'}]};
  }
  async instructions():Promise<string>{
    const catalog=await this.catalog();
    return '\nCapacidades reais deste Core (catálogo do servidor; pronto significa elegível para tentativa, não ação já executada):\n'
      +JSON.stringify(catalog.capabilities.map(c=>({id:c.id,name:c.name,state:c.state,confirmation:c.confirmation})))
      +'\nNão declare envios, controle de TV, execução de código, compras ou deploy sem resultado verificado da ferramenta. O canal Live atual conhece o catálogo, mas não possui executor remoto de ferramentas; ofereça a conversa escrita para executar. WhatsApp pessoal requer finalização pelo usuário; vendas/ads do Mercado Livre e publicação Canva não estão implementadas. Não assuma que os plugins do ChatGPT autorizam contas no FLUX.';
  }
  private tool(id:unknown):Tool{const found=TOOLS.find(t=>t.id===id);if(!found)throw new EvolutionError(404,'Capacidade não implementada.');return found;}
  private inputs(tool:Tool,raw:unknown):Record<string,unknown>{
    if(!isObject(raw))throw new EvolutionError(400,'Parâmetros devem ser um objeto.');
    if(Object.keys(raw).some(k=>!Object.hasOwn(tool.inputs,k)))throw new EvolutionError(400,'Parâmetro não permitido.');
    if('query' in raw&&(typeof raw.query!=='string'||raw.query.length>200))throw new EvolutionError(400,'Busca inválida.');
    if(tool.id==='samsung_tv.volume.set'){
      if(typeof raw.deviceId!=='string'||!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(raw.deviceId))throw new EvolutionError(400,'Escolha um aparelho válido.');
      if(!Number.isInteger(raw.volume)||Number(raw.volume)<0||Number(raw.volume)>100)throw new EvolutionError(400,'Volume deve ser inteiro entre 0 e 100.');
    }
    return raw;
  }
  private async call(tool:Tool,inputs:Record<string,unknown>):Promise<ToolResult>{
    if(tool.id==='memory.list'){const rows=await this.deps.memories();return {...result(tool.id,rows.length?'available':'empty','Memórias autorizadas consultadas.',{memories:rows}),verified:true};}
    const providers=(await this.providers()).providers;
    if(tool.id==='calendar.read'&&!providers.find(p=>p.id==='calendar')?.connected){
      const data=await (await this.deps.calendar()).json() as {connected?:boolean;events?:unknown[]};
      return data.connected?{...result(tool.id,data.events?.length?'available':'empty','Agenda enviada pelo Android consultada.',data),verified:true}:result(tool.id,'unavailable','Agenda não autorizada ou snapshot expirado. Autorize a agenda no Android ou conecte Google Calendar.');
    }
    if(!providers.find(p=>p.id===tool.provider)?.connected)return result(tool.id,'unavailable','Conta não autorizada no FLUX. Nenhuma ação foi executada.');
    const body=tool.operation==='action'?{...inputs,confirm:true,action:tool.id==='samsung_tv.volume.set'?'setVolume':'pause'}:inputs;
    try{
      const response=await this.deps.connect(new Request(`https://flux.internal/v1/connect/${tool.provider}/${tool.operation}`,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(body)}));
      if(!response?.ok)throw new Error('Operação não confirmada.');
      const data=await response.json() as {verified?:boolean;accepted?:boolean};
      if(tool.operation==='action')return {...result(tool.id,data.verified?'verified':'accepted',data.verified?'Estado confirmado pela API; efeito físico ainda exige observação.':'Comando aceito pela API; resultado não confirmado.',data),accepted:data.accepted===true,verified:data.verified===true};
      return {...result(tool.id,'available','Consulta concluída na API autorizada.',data),verified:true};
    }catch(error){return result(tool.id,'error',error instanceof EvolutionError?error.message:'A consulta ou ação falhou; resultado não confirmado.');}
  }
  async invoke(id:unknown,raw:unknown,actor:string):Promise<ToolResult>{
    const tool=this.tool(id),inputs=this.inputs(tool,raw);
    if(tool.confirmation==='explicit'){
      const capability=(await this.catalog()).capabilities.find(c=>c.id===tool.id);
      if(!capability?.serverExecutable)return result(tool.id,'unavailable','Conta não autorizada no FLUX. Nenhuma ação foi preparada ou executada.');
      const approvalId=crypto.randomUUID();
      await this.store.transaction(async tx=>{
        const now=Date.now(),rows=await tx.get<Array<{id:string;actor:string;expires:number}>>('tools:pending')??[];
        const active=rows.filter(r=>r.expires>now);
        for(const row of rows.filter(r=>r.expires<=now))await tx.delete('tools:approval:'+row.id);
        if(active.length>=20)throw new EvolutionError(429,'Conclua ou cancele as confirmações pendentes antes de preparar outra ação.');
        await tx.put('tools:approval:'+approvalId,{toolId:tool.id,inputs,actor,expires:now+300000} satisfies Approval);
        await tx.put('tools:pending',[...active,{id:approvalId,actor,expires:now+300000}]);
      });
      return {...result(tool.id,'approval_required','Revise o aparelho, os parâmetros e confirme a ação. Nada foi executado.',inputs),approvalId};
    }
    return this.call(tool,inputs);
  }
  async confirm(id:string,actor:string,approve:boolean):Promise<ToolResult>{
    const approval=await this.store.transaction(async tx=>{
      const saved=await tx.get<Approval>('tools:approval:'+id);
      if(!saved||saved.expires<=Date.now()||saved.actor!==actor)throw new EvolutionError(409,'Confirmação expirada, utilizada ou de outra sessão.');
      await tx.delete('tools:approval:'+id);
      const pending=await tx.get<Array<{id:string;actor:string;expires:number}>>('tools:pending')??[];
      await tx.put('tools:pending',pending.filter(r=>r.id!==id));return saved;
    });
    if(!approve)return result(approval.toolId,'unavailable','Ação cancelada pelo usuário.');
    const started=Date.now(),output=await this.call(this.tool(approval.toolId),approval.inputs);
    // Bounded log intentionally excludes prompt, content, recipients, parameters and credentials.
    await this.store.transaction(async tx=>{
      const rows=await tx.get<unknown[]>('tools:audit')??[];
      await tx.put('tools:audit',[...rows.slice(-99),{id,toolId:approval.toolId,state:output.state,verified:output.verified,at:output.at,durationMs:Date.now()-started}]);
    });
    return output;
  }
  async natural(message:string,actor:string):Promise<ToolResult|null>{
    const text=message.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
    if(/(?:do que|o que|quais).*(?:capaz|pode fazer|capacidades)/.test(text)){
      const catalog=await this.catalog();return {...result('capabilities.list','available',catalog.capabilities.map(c=>`${c.name}: ${c.state}`).join('\n'),catalog),verified:true};
    }
    if(/^(?:flux[, ]+)?(?:pause|pausar) (?:o )?spotify[.!]?$/.test(text))return this.invoke('spotify.playback.pause',{},actor);
    const volume=text.match(/^(?:flux[, ]+)?(?:defina|ajuste|coloque) (?:o )?volume (?:da |do )?(?:tv|televisao)(?: para| em)? (\d{1,3})[.!]?$/);
    if(volume){
      const devices=await this.call(this.tool('samsung_tv.devices.list'),{});
      if(!devices.verified)return devices;
      const data=devices.data as {items?:Array<{deviceId:string;label?:string;deviceTypeName?:string;ocf?:{deviceType?:string};components?:Array<{id:string;categories?:Array<{name:string}>;capabilities?:Array<{id:string}>}>}>};
      const eligible=(data.items??[]).filter(d=>(d.ocf?.deviceType==='oic.d.tv'||/television|samsung.*tv/i.test(d.deviceTypeName??'')||d.components?.some(c=>c.categories?.some(cat=>cat.name==='Television')))&&d.components?.some(c=>c.id==='main'&&c.capabilities?.some(cap=>cap.id==='audioVolume')));
      if(eligible.length!==1)return result('samsung_tv.volume.set','unavailable',eligible.length?'Há mais de um aparelho compatível. Selecione o aparelho em Configurações → Capacidades reais.':'Nenhum aparelho com controle de volume foi retornado pela conta.');
      return this.invoke('samsung_tv.volume.set',{deviceId:eligible[0].deviceId,volume:Number(volume[1])},actor);
    }
    // Only explicit read intents are routed; quoted instructions and ambiguous side effects go to conversation.
    if(!/^(?:(?:flux)[, ]+)?(?:mostre|mostrar|consulte|consultar|leia|ler|veja|ver|quais|o que esta tocando)\b/.test(text))return null;
    const id= /(?:e-mails?|emails?|gmail)/.test(text)?'gmail.messages.read'
      :/(?:agenda|compromissos|eventos)/.test(text)?'calendar.read'
      :/(?:dispositivos|minha tv)/.test(text)?'samsung_tv.devices.list'
      :/(?:spotify|tocando)/.test(text)?'spotify.playback.read'
      :/(?:memorias)/.test(text)?'memory.list':null;
    return id?this.invoke(id,{},actor):null;
  }
  async route(request:Request,actor:string):Promise<Response|null>{
    const path=new URL(request.url).pathname;
    if(path==='/v1/capabilities'&&request.method==='GET')return Response.json(await this.catalog(),{headers:{'cache-control':'no-store'}});
    if(path==='/v1/tools/invoke'&&request.method==='POST'){const body=await readBoundedObject(request);return Response.json(await this.invoke(body.id,body.inputs??{},actor),{headers:{'cache-control':'no-store'}});}
    const match=path.match(/^\/v1\/tools\/approvals\/([0-9a-f-]{36})$/);
    if(match&&request.method==='POST'){const body=await readBoundedObject(request);if(typeof body.approve!=='boolean')throw new EvolutionError(400,'Confirme ou cancele explicitamente.');return Response.json(await this.confirm(match[1],actor,body.approve),{headers:{'cache-control':'no-store'}});}
    return null;
  }
}
