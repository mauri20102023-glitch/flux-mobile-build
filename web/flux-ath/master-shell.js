import {state, sendChat, setVoiceState, stopVoice, core, headers, readResponse, requireConnection, friendlyError, go, toast} from './app.js';
const $ = s => document.querySelector(s);
const ADVANCED = 'flux-advanced-v1', MUTE = 'flux-microphone-muted-v1';
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
  try{requireConnection();const catalog=await readResponse(await fetch(core('/v1/capabilities'),{headers:headers(),cache:'no-store'}));target.replaceChildren();for(const cap of catalog.capabilities||[]){const row=document.createElement('div');row.className='capability-row';const name=document.createElement('b');name.textContent=cap.name;const status=document.createElement('small');status.textContent=cap.state+' · '+cap.detail;row.append(name,status);target.append(row);}}
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
