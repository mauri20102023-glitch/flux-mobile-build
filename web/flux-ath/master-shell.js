import {state, save, sendChat, renderMessages, setVoiceState, stopVoice, core, headers, readResponse, requireConnection, friendlyError, go, toast, showSceneCard} from './app.js';
const $ = s => document.querySelector(s);
const ADVANCED = 'flux-advanced-v1', MUTE = 'flux-microphone-muted-v1';
// Reset obsolete dashboard layout once; preserve data and accessibility preferences.
if(!localStorage.getItem('flux-master-layout-v1')){
  try{const old=JSON.parse(localStorage.getItem('flux-command-preferences-v1')||'{}');localStorage.setItem('flux-command-preferences-v1',JSON.stringify({...old,mode:'normal',panel:false}));}catch{}
  localStorage.setItem('flux-master-layout-v1','1');
}
const drawer = $('#fluxDrawer');
let previousFocus = null;
function menu(open) {
  if(open) previousFocus=document.activeElement;
  document.body.classList.toggle('drawer-open',open);
  drawer.inert=!open;
  $('#drawerBackdrop').hidden=!open;
  $('#menuToggle').setAttribute('aria-expanded',String(open));
  if(open)drawer.querySelector('button').focus();else previousFocus?.focus();
}
drawer.inert=true;
$('#menuToggle').onclick=()=>menu(!document.body.classList.contains('drawer-open'));
$('#drawerBackdrop').onclick=()=>menu(false);
drawer.addEventListener('click',e=>{if(e.target.closest('[data-go]'))menu(false);});
document.addEventListener('keydown',e=>{
  if(!document.body.classList.contains('drawer-open'))return;
  if(e.key==='Escape'){e.preventDefault();menu(false);return;}
  if(e.key==='Tab'){
    const nodes=[...drawer.querySelectorAll('button:not([disabled])')];
    const first=nodes[0],last=nodes.at(-1);
    if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus();}
    else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus();}
  }
});
$('#minimalOrbMount').append($('.view[data-view=home] .core-wrap'));
$('#contextualCards').append($('#sceneFeed'));
const settings=$('.view[data-view=settings] .settings-grid');
const layout=document.createElement('article');layout.className='panel setting-card';
layout.innerHTML='<h3>Espaço e acessibilidade</h3>';
layout.append($('.command-toolbar'));settings.append(layout);
let interacted=false, sending=false;
$('#homeForm').onsubmit=async e=>{
  e.preventDefault();if(sending)return;
  const input=$('#homeInput'),text=input.value.trim();if(!text)return;
  sending=true;interacted=true;input.value='';
  $('#homeForm button[type=submit]').disabled=true;
  setVoiceState('thinking','PROCESSANDO');
  try{await sendChat(text,{stayHome:true});}
  finally{sending=false;$('#homeForm button[type=submit]').disabled=false;idle();}
};
$('#homeInput').onkeydown=e=>{if(e.key==='Enter'&&!e.shiftKey){e.preventDefault();$('#homeForm').requestSubmit();}};
window.addEventListener('flux:messages',()=>{
  if(!interacted)return;
  const last=state.messages.at(-1);
  if(last?.role==='flux'){$('#homeResponse').textContent=last.text;$('#homeResponse').hidden=false;}else if(!last){$('#homeResponse').textContent='';$('#homeResponse').hidden=true;}
});
function idle(){
  if(localStorage.getItem(MUTE)==='true')setVoiceState('muted','MICROFONE SILENCIADO');
  else if(!navigator.onLine)setVoiceState('offline','SEM CONEXÃO');
  else setVoiceState('idle','TOQUE PARA FALAR');
}
function mute(value){
  localStorage.setItem(MUTE,String(value));
  if(value)stopVoice();
  $('#microphoneMute').setAttribute('aria-pressed',String(value));
  $('#microphoneMute').textContent=value?'Reativar microfone do FLUX':'Silenciar microfone do FLUX';
  idle();
}
$('#microphoneMute').onclick=()=>mute(localStorage.getItem(MUTE)!=='true');
mute(localStorage.getItem(MUTE)==='true');
window.addEventListener('offline',()=>{stopVoice();idle();});window.addEventListener('online',idle);
document.addEventListener('visibilitychange',()=>document.body.classList.toggle('page-hidden',document.hidden));
let transitionTimer;
function advanced(value, animate=false){
  clearTimeout(transitionTimer);document.body.classList.remove('advanced-transition');
  localStorage.setItem(ADVANCED,JSON.stringify({schema:1,enabled:value}));
  document.body.dataset.advanced=String(value);
  $('#advancedToggle').setAttribute('aria-pressed',String(value));
  $('#advancedToggle').textContent=value?'Sair do Advanced':'Iniciar Advanced';
  $('#advancedStatus').textContent=value?'Advanced ativo · mesma sessão do FLUX':'Modo padrão';
  if(animate){go('home');if(document.documentElement.dataset.motion!=='reduced'&&!matchMedia('(prefers-reduced-motion:reduce)').matches){document.body.classList.add('advanced-transition');transitionTimer=setTimeout(()=>document.body.classList.remove('advanced-transition'),1700);}}
}
let enabled=false;try{enabled=JSON.parse(localStorage.getItem(ADVANCED))?.enabled===true;}catch{}
advanced(enabled);
$('#advancedToggle').onclick=()=>advanced(document.body.dataset.advanced!=='true',true);
$('#capabilitiesRefresh').onclick=async()=>{
  const target=$('#capabilitiesList');target.textContent='Consultando o Core…';
  try{requireConnection();const catalog=await readResponse(await fetch(core('/v1/capabilities'),{headers:headers(),cache:'no-store'}));target.replaceChildren();for(const cap of catalog.capabilities||[]){const row=document.createElement('div');row.className='capability-row';const name=document.createElement('b');name.textContent=cap.name;const status=document.createElement('small');status.textContent=cap.state+' · '+cap.detail;row.append(name,status);capabilityControls(row,cap);target.append(row);}}
  catch(error){target.textContent=friendlyError(error);}
};
// Explicit simulator: content is copied locally. No physical display or transport ACK is fabricated.
const touch=document.createElement('dialog');touch.id='touchDialog';
touch.innerHTML='<p class="eyebrow">FLUX ADVANCED · PRÉVIA LOCAL</p><h2>Painel inferior</h2><p>Prévia neste navegador. Nenhum monitor físico foi detectado ou controlado.</p><div class="touch-grid"><button data-touch="voice">Conversar</button><button data-touch="chat">Conversa escrita</button><button data-touch="devices">Dispositivos</button><button data-touch="study">Estudar</button><button data-touch="cards">Receber cartões</button><button data-touch="base">Voltar ao painel</button></div><div class="touch-cards" id="touchCards" hidden></div><p id="touchStatus" role="status">Painel fixo</p><button class="secondary-button" id="touchClose">Fechar prévia</button>';
document.body.append(touch);$('#touchPreview').onclick=()=>touch.showModal();$('#touchClose').onclick=()=>touch.close();
touch.onclick=e=>{
  const action=e.target.closest('[data-touch]')?.dataset.touch;if(!action)return;
  if(action==='voice'){touch.close();$('#voiceCore').click();return;}
  if(['chat','devices','study'].includes(action)){touch.close();go(action);return;}
  const cards=$('#touchCards');
  if(action==='base'){cards.replaceChildren();cards.hidden=true;$('#touchStatus').textContent='Painel fixo';return;}
  cards.replaceChildren();for(const original of $('#sceneFeed').children){const card=document.createElement('article');card.textContent=original.querySelector('strong')?.textContent||'';const detail=document.createElement('small');detail.textContent=original.querySelector('p')?.textContent||'';card.append(detail);cards.append(card);}
  cards.hidden=false;$('#touchStatus').textContent=cards.children.length?'Cartões exibidos nesta prévia local.':'Nenhum cartão solicitado na conversa.';
};

async function request(path,body){
  requireConnection();
  return readResponse(await fetch(core(path),{method:body?'POST':'GET',headers:headers(),...(body?{body:JSON.stringify(body)}:{}),cache:'no-store'}));
}
const approvalDialog=document.createElement('dialog');approvalDialog.className='approval-dialog';
approvalDialog.innerHTML='<h2>Confirmar ação</h2><p id="approvalDescription"></p><p id="approvalState" role="status">Nada foi executado.</p><button id="approvalCancel" class="secondary-button">Cancelar ação</button><button id="approvalConfirm" class="primary-button">Confirmar ação</button>';
document.body.append(approvalDialog);let pendingAction=null;
window.addEventListener('flux:tool-result',e=>{
  // Never overwrite an unresolved consent with a later model response.
  if(pendingAction){toast('Conclua a confirmação atual antes de preparar outra ação.');return;}
  pendingAction=e.detail;
  const detail=e.detail.data||{};
  $('#approvalDescription').textContent=e.detail.id==='samsung_tv.volume.set'?`Definir volume do aparelho ${detail.deviceId} para ${detail.volume}.`:'Pausar a reprodução na conta Spotify autorizada.';
  $('#approvalState').textContent='Nada foi executado. A confirmação vale por cinco minutos.';
  approvalDialog.showModal();
});
async function resolveApproval(approve){
  if(!pendingAction)return;$('#approvalConfirm').disabled=true;$('#approvalCancel').disabled=true;
  try{const result=await request('/v1/tools/approvals/'+pendingAction.approvalId,{approve});state.messages.push({id:crypto.randomUUID(),role:'flux',text:result.content,mode:'FERRAMENTA'});save();renderMessages();pendingAction=null;approvalDialog.close();showSceneCard({kind:result.id,label:result.id,icon:'◇',title:result.verified?'Estado confirmado na API':result.accepted?'Comando aceito':'Ação encerrada',detail:result.content,source:'FLUX CORE · '+result.at,badge:result.verified?'VERIFICADO':'NÃO VERIFICADO'});}
  catch(error){$('#approvalState').textContent=friendlyError(error);if(!approve){pendingAction=null;approvalDialog.close();}}
  finally{$('#approvalConfirm').disabled=false;$('#approvalCancel').disabled=false;}
}
$('#approvalConfirm').onclick=()=>resolveApproval(true);$('#approvalCancel').onclick=()=>resolveApproval(false);
approvalDialog.addEventListener('cancel',e=>{e.preventDefault();resolveApproval(false);});
function capabilityControls(row,cap){
  const output=document.createElement('p');output.className='helper';output.setAttribute('role','status');
  const button=document.createElement('button');button.className='secondary-button';button.disabled=!cap.serverExecutable;
  let query=null,devices=null,volume=null;
  if(cap.inputs?.query){query=document.createElement('input');query.placeholder='O que procurar?';query.maxLength=200;query.setAttribute('aria-label','Busca em '+cap.name);row.append(query);}
  if(cap.id==='samsung_tv.volume.set'){
    devices=document.createElement('select');devices.setAttribute('aria-label','Aparelho SmartThings');devices.disabled=true;
    volume=document.createElement('input');volume.type='number';volume.min='0';volume.max='100';volume.step='1';volume.value='20';volume.setAttribute('aria-label','Volume da TV');
    const load=document.createElement('button');load.className='secondary-button';load.textContent='Carregar aparelhos';load.disabled=!cap.serverExecutable;
    load.onclick=async()=>{load.disabled=true;try{const result=await request('/v1/tools/invoke',{id:'samsung_tv.devices.list',inputs:{}});devices.replaceChildren();for(const item of result.data?.items||[]){if(!item.components?.some(c=>c.id==='main'&&c.capabilities?.some(cap=>cap.id==='audioVolume')))continue;const option=document.createElement('option');option.value=item.deviceId;option.textContent=item.label||item.name||item.deviceId;devices.append(option);}devices.disabled=!devices.options.length;button.disabled=devices.disabled;output.textContent=devices.disabled?'Nenhum aparelho com controle de volume retornado pela conta.':'Escolha o aparelho e o volume.';}catch(e){output.textContent=friendlyError(e);}finally{load.disabled=false;}};
    row.append(load,devices,volume);button.disabled=true;
  }
  button.textContent=cap.confirmation==='explicit'?'Preparar ação':'Consultar';
  button.onclick=async()=>{
    button.disabled=true;
    try{
      const inputs=devices?{deviceId:devices.value,volume:Number(volume.value)}:query?{query:query.value}:{};
      let result=await request('/v1/tools/invoke',{id:cap.id,inputs});
      if(result.approvalId){const details=devices?`${devices.selectedOptions[0].textContent} · volume ${inputs.volume}`:cap.name;const approve=confirm('Confirmar: '+details+'?');result=await request('/v1/tools/approvals/'+result.approvalId,{approve});}
      output.textContent=result.content;
      if(result.data&&cap.confirmation!=='explicit'){const details=document.createElement('details');const summary=document.createElement('summary');summary.textContent='Ver dados consultados';const text=document.createElement('pre');text.textContent=JSON.stringify(result.data,null,2);details.append(summary,text);output.append(details);}
      showSceneCard({id:crypto.randomUUID(),kind:cap.id,label:cap.name,title:result.verified?'Estado confirmado na API':result.accepted?'Comando aceito':'Resultado da consulta',detail:result.content,source:'FLUX CORE · '+result.at,badge:result.verified?'VERIFICADO':'NÃO VERIFICADO',icon:'◇'});
    }catch(error){output.textContent=friendlyError(error);}finally{button.disabled=!cap.serverExecutable;}
  };
  row.append(button,output);
}
// Build is a concrete patch review flow. Opening a PR is separate from release approval.
const build=document.createElement('article');build.className='panel setting-card';build.id='buildPanel';
build.innerHTML='<p class="eyebrow">FLUX BUILD</p><h3>Preparar uma alteração</h3><p>Revise o código antes de autorizar a abertura de uma branch e um PR. Integração e publicação exigem revisão posterior.</p><button class="secondary-button" id="buildStatusRefresh">Verificar acesso do Build</button><p id="buildState" role="status">Acesso do Build ainda não consultado.</p><form id="buildForm"><label>Objetivo<input id="buildObjective" required maxlength="2000" placeholder="Ex.: adicionar um cartão de clima"></label><label>Arquivo de componente<input id="buildPath" required maxlength="220" placeholder="web/flux-ath/components/weather.js"></label><label>Código para revisão<textarea id="buildCode" required maxlength="48000" rows="7" spellcheck="false"></textarea></label><button class="secondary-button" id="buildPrepare">Preparar patch</button></form><p class="helper">O executor aceita componentes, adaptadores e documentação de funcionalidades. Identidade, permissões, testes, CI e código central são protegidos.</p><pre id="buildReview" hidden></pre><button class="primary-button" id="buildPublish" hidden>Autorizar branch e PR em rascunho</button><button class="secondary-button" id="buildChecks" hidden>Consultar testes do PR</button><a id="buildLink" target="_blank" rel="noopener noreferrer" hidden>Abrir PR para revisão</a>';
$('.view[data-view=lab]').append(build);
let buildTask=null;
$('#buildStatusRefresh').onclick=async()=>{try{const data=await request('/v1/build');$('#buildState').textContent=data.configured?'GitHub App configurado. Proteção da branch será verificada antes da escrita.':'Configure o GitHub App próprio do FLUX no servidor para abrir branches e PRs.';}catch(e){$('#buildState').textContent=friendlyError(e);}};
$('#buildForm').onsubmit=async e=>{
  e.preventDefault();$('#buildPrepare').disabled=true;$('#buildPublish').hidden=true;$('#buildChecks').hidden=true;$('#buildLink').hidden=true;buildTask=null;
  try{const data=await request('/v1/build/tasks',{objective:$('#buildObjective').value,files:[{path:$('#buildPath').value,content:$('#buildCode').value}]});buildTask=data.task;$('#buildReview').textContent=JSON.stringify({objective:buildTask.objective,files:buildTask.files},null,2);$('#buildReview').hidden=false;$('#buildPublish').hidden=false;$('#buildState').textContent='Patch preparado para sua revisão. Nenhum código foi executado.';}catch(error){$('#buildState').textContent=friendlyError(error);}finally{$('#buildPrepare').disabled=false;}
};
$('#buildPublish').onclick=async()=>{
  if(!buildTask||!confirm('Autorizar o FLUX a abrir uma branch de trabalho e um PR em rascunho com o patch exibido? Isso não aprova merge nem publicação.'))return;
  $('#buildPublish').disabled=true;
  try{const data=await request('/v1/build/tasks/'+buildTask.id+'/publish',{approve:true});buildTask=data.task;if(!buildTask.url)throw new Error(buildTask.error||'PR não confirmado.');const url=new URL(buildTask.url);if(url.origin!=='https://github.com'||!url.pathname.startsWith('/mauri20102023-glitch/flux-mobile-build/pull/'))throw new Error('Endereço de PR não reconhecido.');$('#buildLink').href=url.href;$('#buildLink').hidden=false;$('#buildChecks').hidden=false;$('#buildPublish').hidden=true;$('#buildState').textContent='PR em rascunho criado. Aguarde os testes e a revisão.';}catch(e){$('#buildState').textContent=friendlyError(e);}finally{$('#buildPublish').disabled=false;}
};
$('#buildChecks').onclick=async()=>{if(!buildTask)return;try{const data=await request('/v1/build/tasks/'+buildTask.id+'/checks');$('#buildState').textContent=data.checksPassed?'Testes obrigatórios aprovados neste commit. Revisão humana e publicação continuam pendentes.':'Testes incompletos ou com falha. Confira o PR.';}catch(e){$('#buildState').textContent=friendlyError(e);}};
