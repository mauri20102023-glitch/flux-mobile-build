import {EvolutionError,readBoundedObject} from './evolution.ts';
import {BUILD_BASE,BUILD_REPOSITORY,REQUIRED_CHECKS,validateBuildFiles,checksReady,type BuildFile} from './build-guard.ts';
export interface BuildEnv{FLUX_BUILD_APP_ID?:string;FLUX_BUILD_INSTALLATION_ID?:string;FLUX_BUILD_PRIVATE_KEY?:string}
type Store={get<T>(key:string):Promise<T|undefined>;put<T>(key:string,value:T):Promise<void>;transaction<T>(fn:(s:Store)=>Promise<T>):Promise<T>};
type Task={id:string;actor:string;objective:string;files:BuildFile[];state:'draft'|'publishing'|'pr_open'|'failed';at:string;branch?:string;commit?:string;pr?:number;url?:string;error?:string};
const out=(data:unknown,status=200)=>Response.json(data,{status,headers:{'cache-control':'no-store'}});
const b64=(bytes:Uint8Array)=>btoa(String.fromCharCode(...bytes)).replaceAll('+','-').replaceAll('/','_').replaceAll('=','');
export class FluxBuild{
  constructor(private store:Store,private env:BuildEnv,private network:typeof fetch=fetch){}
  private configured(){return Boolean(this.env.FLUX_BUILD_APP_ID&&this.env.FLUX_BUILD_INSTALLATION_ID&&this.env.FLUX_BUILD_PRIVATE_KEY);}
  private async token():Promise<string>{
    if(!this.configured())throw new EvolutionError(503,'Configure um GitHub App restrito para o FLUX Build. A conexão do ChatGPT não concede acesso ao FLUX.');
    if(!/^\d+$/.test(this.env.FLUX_BUILD_INSTALLATION_ID!))throw new EvolutionError(503,'Instalação GitHub App inválida.');
    const pem=this.env.FLUX_BUILD_PRIVATE_KEY!.replace(/-----[^-]+-----/g,'').replace(/\s/g,'');
    let key:CryptoKey;try{key=await crypto.subtle.importKey('pkcs8',Uint8Array.from(atob(pem),c=>c.charCodeAt(0)),{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['sign']);}catch{throw new EvolutionError(503,'Chave GitHub App inválida. Use PKCS8 no servidor.');}
    const now=Math.floor(Date.now()/1000),encode=(v:unknown)=>b64(new TextEncoder().encode(JSON.stringify(v)));
    const unsigned=encode({alg:'RS256',typ:'JWT'})+'.'+encode({iat:now-60,exp:now+540,iss:this.env.FLUX_BUILD_APP_ID});
    const jwt=unsigned+'.'+b64(new Uint8Array(await crypto.subtle.sign('RSASSA-PKCS1-v1_5',key,new TextEncoder().encode(unsigned))));
    const grant=await this.api<{token?:string}>(`/app/installations/${this.env.FLUX_BUILD_INSTALLATION_ID}/access_tokens`,jwt,'POST',{repositories:[BUILD_REPOSITORY.split('/')[1]],permissions:{contents:'write',pull_requests:'write',checks:'read',administration:'read'}});
    if(!grant.token)throw new EvolutionError(502,'O GitHub não forneceu token de instalação.');return grant.token;
  }
  private async api<T>(path:string,token:string,method='GET',body?:unknown):Promise<T>{
    const response=await this.network('https://api.github.com'+path,{method,headers:{authorization:'Bearer '+token,accept:'application/vnd.github+json','content-type':'application/json','user-agent':'FLUX-Build','x-github-api-version':'2026-03-10'},...(body?{body:JSON.stringify(body)}:{}),signal:AbortSignal.timeout(20000),redirect:'error'});
    if(!response.ok)throw new EvolutionError(502,`GitHub respondeu ${response.status}; operação não confirmada.`);
    const reader=response.body?.getReader();if(!reader)throw new EvolutionError(502,'Resposta GitHub vazia.');
    const decoder=new TextDecoder();let text='',size=0;
    try{while(true){const part=await reader.read();if(part.done)break;size+=part.value.byteLength;if(size>1000000){await reader.cancel();throw new EvolutionError(502,'Resposta GitHub acima do limite.');}text+=decoder.decode(part.value,{stream:true});}text+=decoder.decode();}finally{reader.releaseLock();}
    try{return JSON.parse(text) as T;}catch{throw new EvolutionError(502,'Resposta GitHub inválida.');}
  }
  private async task(id:string,actor:string):Promise<Task>{const task=await this.store.get<Task>('build:task:'+id);if(!task||task.actor!==actor)throw new EvolutionError(404,'Tarefa não encontrada nesta sessão.');return task;}
  private summary(task:Task){const {actor,...view}=task;return view;}
  private async audit(task:Task){await this.store.transaction(async tx=>{const rows=await tx.get<unknown[]>('build:audit')??[];await tx.put('build:audit',[...rows.slice(-99),{id:task.id,state:task.state,at:new Date().toISOString(),commit:task.commit,pr:task.pr}]);});}
  async create(body:Record<string,unknown>,actor:string){
    if(typeof body.objective!=='string'||!body.objective.trim()||body.objective.length>2000)throw new EvolutionError(400,'Descreva o objetivo em até 2.000 caracteres.');
    if(Object.keys(body).some(k=>!['objective','files'].includes(k)))throw new EvolutionError(403,'Repositório, branch base e política não são controlados pelo pedido.');
    const files=validateBuildFiles(body.files),id=crypto.randomUUID();
    const task:Task={id,actor,objective:body.objective.trim(),files,state:'draft',at:new Date().toISOString()};
    await this.store.put('build:task:'+id,task);await this.audit(task);
    return out({task:this.summary(task),message:'Patch preparado para revisão. Nenhum código foi executado ou publicado.'},201);
  }
  async publish(id:string,actor:string){
    let task=await this.task(id,actor);if(task.state==='pr_open')return out({task:this.summary(task)});
    if(!this.configured())throw new EvolutionError(503,'GitHub App do FLUX Build não configurado; patch permanece em revisão.');
    validateBuildFiles(task.files);
    await this.store.transaction(async tx=>{const current=await tx.get<Task>('build:task:'+id);if(current?.state!=='draft')throw new EvolutionError(409,'Tarefa já iniciada; revise o resultado antes de tentar novamente.');await tx.put('build:task:'+id,{...current,state:'publishing'});});
    task={...task,state:'publishing',branch:'flux-build/'+id};
    try{
      const token=await this.token(),base=`/repos/${BUILD_REPOSITORY}`;
      const branch=await this.api<{protected?:boolean;commit:{sha:string}}>(`${base}/branches/${BUILD_BASE}`,token);
      if(branch.protected!==true)throw new EvolutionError(409,'Proteja a branch main com revisão e verificações obrigatórias antes de habilitar escrita do Build.');
      const protection=await this.api<{required_status_checks?:{strict?:boolean;contexts?:string[]};required_pull_request_reviews?:{required_approving_review_count?:number};enforce_admins?:{enabled?:boolean};allow_force_pushes?:{enabled?:boolean};allow_deletions?:{enabled?:boolean}}>(`${base}/branches/${BUILD_BASE}/protection`,token);
      if(!protection.required_status_checks?.strict||!REQUIRED_CHECKS.every(name=>protection.required_status_checks?.contexts?.includes(name))||(protection.required_pull_request_reviews?.required_approving_review_count??0)<1||protection.enforce_admins?.enabled!==true||protection.allow_force_pushes?.enabled===true||protection.allow_deletions?.enabled===true)throw new EvolutionError(409,'Proteção insuficiente: exija revisão, CI atualizado, proteção de administradores e bloqueio de force push/exclusão.');
      const parent=await this.api<{tree:{sha:string}}>(`${base}/git/commits/${branch.commit.sha}`,token);
      const tree=await this.api<{sha:string}>(`${base}/git/trees`,token,'POST',{base_tree:parent.tree.sha,tree:task.files.map(f=>({...f,type:'blob',mode:'100644'}))});
      const commit=await this.api<{sha:string}>(`${base}/git/commits`,token,'POST',{message:'FLUX Build: '+task.objective.slice(0,100),tree:tree.sha,parents:[branch.commit.sha]});
      task.commit=commit.sha;
      await this.api(`${base}/git/refs`,token,'POST',{ref:'refs/heads/'+task.branch,sha:commit.sha});
      const pr=await this.api<{number:number;html_url:string}>(`${base}/pulls`,token,'POST',{title:'FLUX Build: '+task.objective.slice(0,100),base:BUILD_BASE,head:task.branch,draft:true,body:'Patch preparado pelo FLUX Build para revisão humana. Objetivo: '+task.objective+'\n\nCI ainda precisa executar no commit. Não houve merge, deploy ou execução local de código. Caminhos críticos bloqueados pelo servidor.'});
      task={...task,state:'pr_open',pr:pr.number,url:pr.html_url};
    }catch(error){task={...task,state:'failed',error:error instanceof EvolutionError?error.message:'Build não concluído. Confira a branch de tarefa antes de repetir.'};}
    await this.store.put('build:task:'+id,task);await this.audit(task);
    return out({task:this.summary(task),merged:false,deployed:false},task.state==='failed'?409:201);
  }
  async route(request:Request,actor:string):Promise<Response|null>{
    const path=new URL(request.url).pathname;
    if(!path.startsWith('/v1/build'))return null;
    if(path==='/v1/build'&&request.method==='GET')return out({configured:this.configured(),repository:BUILD_REPOSITORY,base:BUILD_BASE,state:this.configured()?'configurado_sem_validacao':'conectar',requiredChecks:REQUIRED_CHECKS,scope:'Preparar patch, abrir branch e PR em rascunho após revisão explícita. Sem executor de terminal, merge ou deploy.'});
    if(path==='/v1/build/tasks'&&request.method==='POST')return this.create(await readBoundedObject(request),actor);
    const match=path.match(/^\/v1\/build\/tasks\/([0-9a-f-]{36})(?:\/(publish|checks))?$/);
    if(match){const task=await this.task(match[1],actor);
      if(!match[2]&&request.method==='GET')return out({task:this.summary(task)});
      if(match[2]==='publish'&&request.method==='POST'){const body=await readBoundedObject(request);if(body.approve!==true)throw new EvolutionError(403,'Revise e autorize explicitamente a abertura de branch e PR.');return this.publish(task.id,actor);}
      if(match[2]==='checks'&&request.method==='GET'){
        if(!task.commit||!task.pr)throw new EvolutionError(409,'Nenhum PR criado para esta tarefa.');
        const token=await this.token();
        const pr=await this.api<{head:{sha:string}}>(`/repos/${BUILD_REPOSITORY}/pulls/${task.pr}`,token);
        const checked=await this.api<{check_runs:Array<{name:string;status:string;conclusion:string|null;head_sha:string}>}>(`/repos/${BUILD_REPOSITORY}/commits/${pr.head.sha}/check-runs?per_page=100`,token);
        return out({taskId:task.id,head:pr.head.sha,checks:checked.check_runs.map(c=>({name:c.name,status:c.status,conclusion:c.conclusion,head_sha:c.head_sha})),checksPassed:pr.head.sha===task.commit&&checksReady(checked.check_runs,task.commit),reviewRequired:true,merged:false,deployed:false});
      }
    }
    throw new EvolutionError(405,'Operação não permitida pelo FLUX Build. Merge, deploy e mudanças de política exigem fluxo manual separado.');
  }
}
