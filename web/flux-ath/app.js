const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const VERSION = "1.7.0";
const STORE = "flux-workspace-v1";
const DEVICE_ID = localStorage.getItem("flux-device-id") || `web-${crypto.randomUUID()}`;
localStorage.setItem("flux-device-id", DEVICE_ID);

const initial = {
  theme: "ember",
  coreUrl: location.origin,
  token: "",
  pinHash: "",
  tasks: [],
  projects: [{ id: crypto.randomUUID(), name: "FLUX Mobile", note: "Assistente pessoal integrado", progress: .82 }],
  memories: [{ id: crypto.randomUUID(), title: "Como me chamar", content: "Olá, Maurício", category: "PREFERÊNCIA", private: false }],
  messages: [{ id: crypto.randomUUID(), role: "flux", text: "Olá, Maurício. Estou pronto quando você estiver.", mode: "FLUX" }],
  connected: [],
  lastImage: "",
  wakeEnabled: false,
  voiceEnabled: true,
};

function loadState() {
  try { return { ...initial, ...JSON.parse(localStorage.getItem(STORE) || "{}") }; }
  catch { return { ...initial }; }
}
let state = loadState();
let sessionPin = "";
let installEvent = null;
let screenStream = null;
let screenTimer = null;
let selectedStyle = "cinematográfica";

function save() { localStorage.setItem(STORE, JSON.stringify(state)); }
function escapeHtml(value = "") { return String(value).replace(/[&<>'"]/g, char => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", "'":"&#39;", '"':"&quot;" })[char]); }
function core(path) { return `${(state.coreUrl || location.origin).replace(/\/$/, "")}${path}`; }
function headers() { return { "content-type":"application/json", authorization:`Bearer ${state.token}`, "x-flux-device-id":DEVICE_ID }; }
function setBusy(button, busy, label = "PROCESSANDO") { if (!button) return; if (busy) { button.dataset.label = button.textContent; button.textContent = label; button.disabled = true; } else { button.textContent = button.dataset.label || button.textContent; button.disabled = false; } }

function toast(message, type = "") {
  const node = document.createElement("div"); node.className = `toast ${type}`; node.textContent = message;
  $("#toasts").append(node); setTimeout(() => node.remove(), 4200);
}

const viewMeta = {
  home:["SISTEMA PESSOAL","Olá, Maurício"], chat:["CONVERSA","FLUX Chat"], planner:["ORGANIZAÇÃO","Planos"],
  studio:["CRIAÇÃO","FLUX Studio"], memory:["CONTEXTO","Memória"], integrations:["SERVIÇOS","FLUX Link"],
  devices:["ECOSSISTEMA","Dispositivos"], lab:["EVOLUÇÃO SEGURA","FLUX Lab"], settings:["CONTROLE","Configurações"],
};
function go(view, updateUrl = true) {
  if (!viewMeta[view]) view = "home";
  $$(".view").forEach(node => node.classList.toggle("active", node.dataset.view === view));
  $$('[data-go]').forEach(node => node.classList.toggle("active", node.dataset.go === view));
  $("#sectionEyebrow").textContent = viewMeta[view][0]; $("#sectionTitle").textContent = viewMeta[view][1];
  if (updateUrl) history.replaceState(null, "", view === "home" ? location.pathname : `?view=${view}`);
  window.scrollTo({ top:0, behavior:"smooth" });
  if (view === "chat") setTimeout(() => $("#chatInput").focus(), 150);
}

function updateClock() {
  const now = new Date(); const hour = now.getHours();
  $("#clock").textContent = now.toLocaleTimeString("pt-BR", { hour:"2-digit", minute:"2-digit" });
  $("#todayLabel").textContent = now.toLocaleDateString("pt-BR", { weekday:"long", day:"numeric", month:"long" });
  $("#greeting").textContent = `${hour < 12 ? "Bom dia" : hour < 18 ? "Boa tarde" : "Boa noite"}, Maurício.`;
}

function renderMessages() {
  const box = $("#messages"); box.innerHTML = "";
  state.messages.forEach(item => {
    const node = document.createElement("div"); node.className = `message ${item.role}${item.pending ? " pending" : ""}`;
    node.innerHTML = `${escapeHtml(item.text)}<small>${item.role === "flux" ? escapeHtml(item.mode || "FLUX") : "VOCÊ"}</small>`; box.append(node);
  });
  box.scrollTop = box.scrollHeight;
  const last = [...state.messages].reverse().find(item => item.role === "flux" && !item.pending);
  if (last) $("#lastExchange").textContent = `“${last.text.slice(0,180)}${last.text.length > 180 ? "…" : ""}”`;
}

async function sendChat(message) {
  const clean = message.trim(); if (!clean) return;
  const user = { id:crypto.randomUUID(), role:"user", text:clean }; const pending = { id:crypto.randomUUID(), role:"flux", text:"Pensando…", mode:"FLUX", pending:true };
  state.messages.push(user, pending); save(); renderMessages(); go("chat");
  try {
    requireConnection();
    const response = await fetch(core("/v1/chat"), { method:"POST", headers:headers(), body:JSON.stringify({ requestId:crypto.randomUUID(), conversationId:getConversationId(), message:clean, deviceId:DEVICE_ID, modality:"TEXT" }) });
    const payload = await readResponse(response);
    pending.text = payload.content; pending.mode = payload.mode || "FLUX"; pending.pending = false;
  } catch (error) { pending.text = friendlyError(error); pending.mode = "AVISO"; pending.pending = false; }
  save(); renderMessages();
}
function getConversationId() { let id = localStorage.getItem("flux-conversation-id"); if (!id) { id = crypto.randomUUID(); localStorage.setItem("flux-conversation-id", id); } return id; }
function requireConnection() { if (!state.token || state.token.length < 20) { const error = new Error("Conecte o token protegido em Configurações."); error.code = "NO_TOKEN"; throw error; } }
async function readResponse(response) { let body = {}; try { body = await response.json(); } catch {} if (!response.ok) throw new Error(body.message || body.error || `Erro ${response.status}`); return body; }
function friendlyError(error) { const text = error?.message || "Falha inesperada."; if (/unauthorized/i.test(text)) return "O token do FLUX não foi aceito. Abra Configurações e reconecte o núcleo."; return text; }

function renderTasks() {
  const list = $("#taskList"); list.innerHTML = ""; $("#taskCount").textContent = state.tasks.filter(item => !item.done).length;
  $("#taskSummary").textContent = state.tasks.some(item => !item.done) ? `${state.tasks.filter(item => !item.done).length} tarefa(s) em aberto` : "Nenhuma pendência urgente";
  state.tasks.forEach(task => {
    const row = document.createElement("div"); row.className = `task-row${task.done ? " done" : ""}`;
    row.innerHTML = `<button class="task-check" data-task-toggle="${task.id}">${task.done ? "✓" : ""}</button><b>${escapeHtml(task.title)}</b><button class="delete-button" data-task-delete="${task.id}" aria-label="Excluir">×</button>`; list.append(row);
  });
  if (!state.tasks.length) list.innerHTML = '<p class="helper">Nada pendente. Aproveite o espaço livre.</p>';
}
function addTask(title) { const clean = title.trim(); if (!clean) return; state.tasks.unshift({ id:crypto.randomUUID(), title:clean, done:false }); save(); renderTasks(); toast("Tarefa adicionada.", "ok"); }
function renderProjects() {
  const list = $("#projectList"); list.innerHTML = ""; state.projects.forEach(project => { const node = document.createElement("article"); node.className = "project-card"; node.style.setProperty("--progress", Math.max(0,Math.min(1,project.progress || 0))); node.innerHTML = `<b>${escapeHtml(project.name)}</b><p>${escapeHtml(project.note || "Projeto FLUX")}</p><small>${Math.round((project.progress || 0)*100)}% CONCLUÍDO</small>`; list.append(node); });
}

async function hashText(value) { const bytes = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)); return btoa(String.fromCharCode(...new Uint8Array(bytes))); }
async function deriveKey(pin, salt) { const base = await crypto.subtle.importKey("raw", new TextEncoder().encode(pin), "PBKDF2", false, ["deriveKey"]); return crypto.subtle.deriveKey({ name:"PBKDF2", salt, iterations:150000, hash:"SHA-256" }, base, { name:"AES-GCM", length:256 }, false, ["encrypt","decrypt"]); }
async function encryptPrivate(text, pin) { const salt = crypto.getRandomValues(new Uint8Array(16)); const iv = crypto.getRandomValues(new Uint8Array(12)); const key = await deriveKey(pin, salt); const encrypted = new Uint8Array(await crypto.subtle.encrypt({ name:"AES-GCM", iv }, key, new TextEncoder().encode(text))); return { data:btoa(String.fromCharCode(...encrypted)), salt:btoa(String.fromCharCode(...salt)), iv:btoa(String.fromCharCode(...iv)) }; }
async function decryptPrivate(bundle, pin) { const bytes = value => Uint8Array.from(atob(value), char => char.charCodeAt(0)); const key = await deriveKey(pin, bytes(bundle.salt)); const clear = await crypto.subtle.decrypt({ name:"AES-GCM", iv:bytes(bundle.iv) }, key, bytes(bundle.data)); return new TextDecoder().decode(clear); }
function renderMemories() {
  const grid = $("#memoryList"); grid.innerHTML = "";
  state.memories.forEach(memory => { const node = document.createElement("article"); node.className = "memory-card"; const visible = !memory.private || memory.revealed; node.innerHTML = `<header><span>${memory.private ? "⌾" : "∞"}</span><button class="delete-button" data-memory-delete="${memory.id}" aria-label="Esquecer">×</button></header><h3>${escapeHtml(memory.title)}</h3><p>${visible ? escapeHtml(memory.content || "Memória criptografada") : "••••••••••••"}</p><footer><span class="tag">${escapeHtml(memory.category || "GERAL")}</span>${memory.private && !visible ? `<button class="card-action" data-memory-reveal="${memory.id}">REVELAR</button>` : ""}</footer>`; grid.append(node); });
  if (!state.memories.length) grid.innerHTML = '<article class="memory-card"><h3>Nenhuma memória</h3><p>Adicione apenas o que você quer que o FLUX guarde.</p></article>';
}

const integrations = [
  ["whatsapp","WhatsApp","WA","Mensagens e conversas","https://wa.me/"], ["instagram","Instagram","IG","Conteúdo e perfil","https://www.instagram.com/"],
  ["email","E-mail","@","Escrever uma mensagem","mailto:"], ["calendar","Google Agenda","31","Compromissos e eventos","https://calendar.google.com/"],
  ["canva","Canva","CA","Criar designs","https://www.canva.com/"], ["mercadolivre","Mercado Livre","ML","Compras e pedidos","https://www.mercadolivre.com.br/"],
  ["spotify","Spotify","♫","Música e playlists","https://open.spotify.com/"], ["smartthings","SmartThings","ST","TV e casa Samsung","https://my.smartthings.com/"],
  ["drive","Google Drive","DR","Arquivos e documentos","https://drive.google.com/"], ["youtube","YouTube","▶","Vídeos e canais","https://www.youtube.com/"],
];
function renderIntegrations() {
  const grid = $("#integrationGrid"); grid.innerHTML = "";
  integrations.forEach(([id,name,icon,desc]) => { const connected = state.connected.includes(id); const node = document.createElement("article"); node.className="integration-card"; node.innerHTML=`<header><span class="integration-icon">${icon}</span><span class="tag state ${connected ? "ready" : ""}">${connected ? "ATALHO ATIVO" : "DISPONÍVEL"}</span></header><h3>${name}</h3><p>${desc}</p><footer><small>${connected ? "Acesso rápido autorizado" : "Abre o serviço oficial"}</small><button class="card-action" data-integration="${id}">${connected ? "ABRIR" : "CONECTAR"}</button></footer>`; grid.append(node); });
}
function openIntegration(id) { const item = integrations.find(row => row[0] === id); if (!item) return; if (!state.connected.includes(id)) { state.connected.push(id); save(); renderIntegrations(); toast(`${item[1]} adicionado aos atalhos.`, "ok"); } window.open(item[4], "_blank", "noopener,noreferrer"); }

function renderDevices() {
  const devices = [
    ["mobile","Este dispositivo","▣","ONLINE","Microfone, chat, visão e notificações"], ["tv","Samsung Crystal","▱","PRONTO PARA PAREAR","Painel FLUX pela TV e SmartThings"],
    ["watch","FLUX Watch","◷","PLANEJADO","Voz rápida, alertas e saúde"], ["edith","FLUX EDITH","◉","PLANEJADO","Óculos, câmera e áudio contextual"],
    ["home","FLUX Home Hub","⌂","PLANEJADO","HDMI, microfones e automação da casa"],
  ];
  const grid=$("#deviceGrid"); grid.innerHTML=""; devices.forEach(([id,name,icon,status,desc])=>{ const node=document.createElement("article"); node.className="device-card"; node.innerHTML=`<header><span class="device-icon">${icon}</span><span class="tag">${status}</span></header><h3>${name}</h3><p>${desc}</p><footer><small>${id === "mobile" ? "Conectado agora" : "Integração segura"}</small><button class="card-action" data-device="${id}">${id === "mobile" ? "DETALHES" : "CONFIGURAR"}</button></footer>`; grid.append(node); });
}

async function healthCheck(show = false) {
  const badge=$("#coreStatus"); badge.className="status-pill"; badge.querySelector("b").textContent="VERIFICANDO";
  try { const response=await fetch(core("/health"),{cache:"no-store"}); const body=await readResponse(response); if(body.features?.live===false){badge.querySelector("b").textContent="ATIVAR VOZ";}else{badge.classList.add("online");badge.querySelector("b").textContent="ONLINE";} if(show) toast(`FLUX Core ${body.version || "online"}${body.features?.live===false ? " — Gemini Live precisa da chave do servidor" : ""}.`,body.features?.live===false?"":"ok"); return body; }
  catch(error){ badge.classList.add("offline"); badge.querySelector("b").textContent="OFFLINE"; if(show) toast(`Núcleo indisponível: ${friendlyError(error)}`,"error"); throw error; }
}
async function diagnostics() { requireConnection(); const response=await fetch(core("/v1/diagnostics"),{headers:headers()}); return readResponse(response); }

class FluxLive {
  constructor() { this.socket=null; this.context=null; this.mic=null; this.processor=null; this.source=null; this.nextPlay=0; this.userText=""; this.fluxText=""; this.session=null; }
  get active(){ return Boolean(this.socket); }
  async start(){
    if(this.active) return; requireConnection(); setVoiceState("connecting","CONECTANDO AO FLUX");
    const sessionResponse=await fetch(core("/v1/live/session"),{method:"POST",headers:headers(),body:"{}"}); this.session=await readResponse(sessionResponse);
    this.context=new (window.AudioContext||window.webkitAudioContext)({latencyHint:"interactive"}); await this.context.resume();
    this.mic=await navigator.mediaDevices.getUserMedia({audio:{channelCount:1,echoCancellation:true,noiseSuppression:true,autoGainControl:true}});
    const ws=new WebSocket(`${this.session.endpoint}?access_token=${encodeURIComponent(this.session.token)}`); this.socket=ws;
    ws.onopen=()=>ws.send(JSON.stringify({setup:{model:`models/${this.session.model}`,generationConfig:{responseModalities:["AUDIO"],temperature:.7,speechConfig:{voiceConfig:{prebuiltVoiceConfig:{voiceName:this.session.voice}}}},systemInstruction:{parts:[{text:this.session.systemInstruction}]},inputAudioTranscription:{},outputAudioTranscription:{},realtimeInputConfig:{automaticActivityDetection:{disabled:false,silenceDurationMs:700,prefixPaddingMs:300,startOfSpeechSensitivity:"START_SENSITIVITY_HIGH",endOfSpeechSensitivity:"END_SENSITIVITY_HIGH"},activityHandling:"START_OF_ACTIVITY_INTERRUPTS",turnCoverage:"TURN_INCLUDES_ONLY_ACTIVITY"}}}));
    ws.onmessage=event=>this.handle(JSON.parse(event.data)); ws.onerror=()=>this.fail("A conexão de voz foi interrompida."); ws.onclose=()=>{ if(this.socket===ws) this.stop(false); };
  }
  setupAudio(){
    if(this.processor) return; this.source=this.context.createMediaStreamSource(this.mic); this.processor=this.context.createScriptProcessor(2048,1,1); const sink=this.context.createGain(); sink.gain.value=0; this.source.connect(this.processor); this.processor.connect(sink); sink.connect(this.context.destination);
    this.processor.onaudioprocess=event=>{ if(!this.socket||this.socket.readyState!==WebSocket.OPEN)return; const pcm=downsample(event.inputBuffer.getChannelData(0),this.context.sampleRate,16000); this.socket.send(JSON.stringify({realtimeInput:{audio:{data:bytesToBase64(pcm.buffer),mimeType:"audio/pcm;rate=16000"}}})); };
    setVoiceState("listening","OUVINDO — PODE FALAR");
  }
  handle(message){
    if(message.setupComplete){ this.setupAudio(); return; }
    const server=message.serverContent||{}; const parts=server.modelTurn?.parts||[];
    parts.forEach(part=>{ if(part.inlineData?.data){ setVoiceState("speaking","FLUX RESPONDENDO"); this.play(part.inlineData.data); }});
    if(server.inputTranscription?.text) this.userText=mergeTranscript(this.userText,server.inputTranscription.text);
    if(server.outputTranscription?.text) this.fluxText=mergeTranscript(this.fluxText,server.outputTranscription.text);
    if(server.interrupted){ this.nextPlay=this.context.currentTime; setVoiceState("listening","OUVINDO — PODE FALAR"); }
    if(server.turnComplete){ if(this.userText.trim()) state.messages.push({id:crypto.randomUUID(),role:"user",text:this.userText.trim()}); if(this.fluxText.trim()) state.messages.push({id:crypto.randomUUID(),role:"flux",text:this.fluxText.trim(),mode:"FLUX LIVE"}); this.userText=""; this.fluxText=""; save(); renderMessages(); setVoiceState("listening","OUVINDO — PODE FALAR"); }
  }
  play(encoded){ const bytes=Uint8Array.from(atob(encoded),char=>char.charCodeAt(0)); const samples=new Float32Array(Math.floor(bytes.length/2)); const view=new DataView(bytes.buffer); for(let i=0;i<samples.length;i++) samples[i]=view.getInt16(i*2,true)/32768; const buffer=this.context.createBuffer(1,samples.length,24000); buffer.copyToChannel(samples,0); const source=this.context.createBufferSource(); source.buffer=buffer; source.connect(this.context.destination); const at=Math.max(this.context.currentTime+.015,this.nextPlay); source.start(at); this.nextPlay=at+buffer.duration; }
  sendText(text, complete=true){ if(this.socket?.readyState!==WebSocket.OPEN) return false; this.socket.send(JSON.stringify({clientContent:{turns:[{role:"user",parts:[{text}]}],turnComplete:complete}})); return true; }
  sendFrame(data){ if(this.socket?.readyState!==WebSocket.OPEN)return; this.socket.send(JSON.stringify({realtimeInput:{video:{data,mimeType:"image/jpeg"}}})); }
  fail(message){ toast(message,"error"); setVoiceState("error","FALHA NA CONEXÃO"); this.stop(false); }
  stop(close=true){ const ws=this.socket; this.socket=null; if(close) try{ws?.close(1000,"user-finished");}catch{} this.processor?.disconnect(); this.source?.disconnect(); this.mic?.getTracks().forEach(track=>track.stop()); try{this.context?.close();}catch{} this.processor=null; this.source=null; this.mic=null; this.context=null; this.nextPlay=0; setVoiceState("idle","TOQUE PARA FALAR"); }
}
const live=new FluxLive();
function mergeTranscript(current,incoming){ if(!current)return incoming; if(incoming.startsWith(current))return incoming; if(current.endsWith(incoming))return current; return current+incoming; }
function bytesToBase64(buffer){ const bytes=new Uint8Array(buffer); let binary=""; const step=0x8000; for(let i=0;i<bytes.length;i+=step) binary+=String.fromCharCode(...bytes.subarray(i,i+step)); return btoa(binary); }
function downsample(float32,inputRate,outputRate){ const ratio=inputRate/outputRate; const length=Math.round(float32.length/ratio); const result=new Int16Array(length); for(let i=0;i<length;i++){ const start=Math.floor(i*ratio),end=Math.min(float32.length,Math.floor((i+1)*ratio)); let sum=0,count=0; for(let j=start;j<end;j++){sum+=float32[j];count++;} const sample=Math.max(-1,Math.min(1,sum/Math.max(1,count))); result[i]=sample<0?sample*32768:sample*32767; } return result; }
function setVoiceState(mode,label){ $("#voiceCore").dataset.state=mode; $("#voiceStatus").textContent=label; $("#stopVoice").hidden=mode==="idle"; $("#homeHint").textContent=mode==="listening"?"Estou ouvindo. Pode falar naturalmente.":mode==="speaking"?"Pode me interromper a qualquer momento.":"Toque no núcleo — no Android, basta dizer “Flux”."; }
async function toggleVoice(){ try{ if(live.active)live.stop(); else await live.start(); }catch(error){ live.fail(friendlyError(error)); if(error.code==="NO_TOKEN")go("settings"); } }

async function startScreen(){
  if(!navigator.mediaDevices?.getDisplayMedia){ toast("O compartilhamento de tela exige um navegador compatível ou o app Android.","error"); return; }
  try { if(!live.active) await live.start(); screenStream=await navigator.mediaDevices.getDisplayMedia({video:{frameRate:2},audio:false}); const video=document.createElement("video"); video.srcObject=screenStream; video.muted=true; await video.play(); const canvas=document.createElement("canvas"),ctx=canvas.getContext("2d"); canvas.width=640; canvas.height=360; const send=()=>{ if(!screenStream)return; const scale=Math.min(canvas.width/video.videoWidth,canvas.height/video.videoHeight); const w=video.videoWidth*scale,h=video.videoHeight*scale; ctx.fillStyle="#000";ctx.fillRect(0,0,canvas.width,canvas.height);ctx.drawImage(video,(canvas.width-w)/2,(canvas.height-h)/2,w,h); live.sendFrame(canvas.toDataURL("image/jpeg",.68).split(",")[1]); }; screenTimer=setInterval(send,1000); send(); screenStream.getVideoTracks()[0].addEventListener("ended",stopScreen); $("#screenShare").hidden=false; live.sendText("Analise a tela que eu compartilhar. Só descreva quando eu perguntar; preserve dados pessoais e nunca repita senhas.",false); toast("FLUX Vision ativada.","ok"); }
  catch(error){ toast(error.name==="NotAllowedError"?"Compartilhamento cancelado.":friendlyError(error),"error"); }
}
function stopScreen(){ clearInterval(screenTimer); screenTimer=null; screenStream?.getTracks().forEach(track=>track.stop()); screenStream=null; $("#screenShare").hidden=true; toast("FLUX Vision encerrada."); }

function modal({eyebrow="FLUX",title,body,confirm="SALVAR",onConfirm}){
  const dialog=$("#modal"); $("#modalEyebrow").textContent=eyebrow; $("#modalTitle").textContent=title; $("#modalBody").innerHTML=body; $("#modalActions").innerHTML=`<button value="cancel">CANCELAR</button><button type="button" class="confirm">${confirm}</button>`;
  $("#modalActions .confirm").onclick=async()=>{ try{ const result=await onConfirm?.(dialog); if(result!==false)dialog.close(); }catch(error){toast(friendlyError(error),"error");} }; dialog.showModal(); return dialog;
}
function newTaskModal(){ modal({eyebrow:"FLUX PLANNER",title:"Nova tarefa",body:'<label class="modal-field">TAREFA<input id="modalTask" maxlength="180" autofocus placeholder="O que precisa ser feito?"></label>',onConfirm:()=>{addTask($("#modalTask").value);go("planner");}}); setTimeout(()=>$("#modalTask")?.focus(),100); }
function newProjectModal(){ modal({eyebrow:"FLUX PLANNER",title:"Novo projeto",body:'<label class="modal-field">NOME<input id="modalProject" maxlength="80"></label><label class="modal-field">OBJETIVO<textarea id="modalProjectNote" rows="3" maxlength="240"></textarea></label>',onConfirm:()=>{const name=$("#modalProject").value.trim();if(!name)return false;state.projects.unshift({id:crypto.randomUUID(),name,note:$("#modalProjectNote").value.trim(),progress:0});save();renderProjects();toast("Projeto criado.","ok");}}); }
function newMemoryModal(){ modal({eyebrow:"FLUX MEMORY",title:"Guardar memória",body:'<label class="modal-field">TÍTULO<input id="memoryTitle" maxlength="80"></label><label class="modal-field">CONTEÚDO<textarea id="memoryContent" rows="4" maxlength="600"></textarea></label><label class="modal-field">CATEGORIA<select id="memoryCategory"><option>PREFERÊNCIA</option><option>PESSOAL</option><option>PROJETO</option><option>ROTINA</option></select></label><label class="toggle-row"><span><b>Memória privada</b><small>Criptografada com seu PIN</small></span><input id="memoryPrivate" type="checkbox"></label>',onConfirm:async()=>{const title=$("#memoryTitle").value.trim(),content=$("#memoryContent").value.trim(),isPrivate=$("#memoryPrivate").checked;if(!title||!content)return false;let memory={id:crypto.randomUUID(),title,category:$("#memoryCategory").value,private:isPrivate};if(isPrivate){if(!state.pinHash||!sessionPin){toast("Defina e desbloqueie seu PIN primeiro.","error");go("settings");return false;}memory.encrypted=await encryptPrivate(content,sessionPin);}else memory.content=content;state.memories.unshift(memory);save();renderMemories();toast("Memória guardada.","ok");}}); }

async function createImage(event){ event.preventDefault(); const button=$("#generateImage"),prompt=$("#imagePrompt").value.trim(); if(!prompt)return toast("Descreva a imagem primeiro.","error"); try{requireConnection();setBusy(button,true,"CRIANDO…");const response=await fetch(core("/v1/images"),{method:"POST",headers:headers(),body:JSON.stringify({prompt:`${prompt}. Estilo ${selectedStyle}.`})});const body=await readResponse(response);state.lastImage=`data:${body.mediaType||"image/jpeg"};base64,${body.image}`;save();renderImage();toast("Imagem criada pelo FLUX Studio.","ok");}catch(error){toast(friendlyError(error),"error");if(error.code==="NO_TOKEN")go("settings");}finally{setBusy(button,false);} }
function renderImage(){if(state.lastImage)$("#imageCanvas").innerHTML=`<img src="${state.lastImage}" alt="Imagem criada pelo FLUX Studio">`;}

async function runLab(name){
  if(name==="builder")return modal({eyebrow:"FLUX BUILDER",title:"Projetar uma função",body:'<label class="modal-field">O QUE O FLUX DEVE FAZER?<textarea id="builderIdea" rows="5" maxlength="800" placeholder="Descreva a função, quando deve ser ativada e o que não pode fazer."></textarea></label>',confirm:"CRIAR PROPOSTA",onConfirm:()=>{const idea=$("#builderIdea").value.trim();if(!idea)return false;state.projects.unshift({id:crypto.randomUUID(),name:"Função em avaliação",note:idea,progress:.08});save();renderProjects();toast("Proposta criada. Nada foi instalado sem testes.","ok");}});
  if(name==="test") { const results=await systemTests(); showReport("FLUX TEST LAB","Resultado dos testes",results); return; }
  if(name==="security") { const checks=[statusLine("Conexão HTTPS",location.protocol==="https:"),statusLine("Token protegido configurado",state.token.length>=20),statusLine("PIN local configurado",Boolean(state.pinHash)),statusLine("Microfone só sob comando",!live.active),statusLine("Chave Gemini fora do navegador",true)]; showReport("FLUX SECURITY","Auditoria local",checks.join("")); return; }
  if(name==="update") { try{const health=await healthCheck();showReport("FLUX UPDATE","Versão do sistema",`<p>Interface <b>${VERSION}</b></p><p>Núcleo <b>${escapeHtml(health.version||"online")}</b></p><p class="helper">Atualizações são publicadas pelo repositório confiável e podem ser revertidas.</p>`);}catch(error){toast(friendlyError(error),"error");} return; }
  if(name==="recovery") { localStorage.setItem("flux-recovery-backup",JSON.stringify(state)); toast("Ponto de recuperação local criado.","ok"); showReport("FLUX RECOVERY","Backup concluído",'<p>Configurações, tarefas, projetos e memórias foram copiadas neste aparelho.</p><button class="secondary-button wide" id="restoreBackup">RESTAURAR ÚLTIMO BACKUP</button>'); setTimeout(()=>$("#restoreBackup").onclick=restoreBackup,0); return; }
  if(name==="developer") { const console=$("#developerConsole");console.hidden=!console.hidden;console.textContent=`FLUX Developer Mode\nversion=${VERSION}\ndevice=${DEVICE_ID}\ncore=${state.coreUrl}\nvoice=${live.active?"active":"idle"}\nscreen=${screenStream?"shared":"private"}\ntasks=${state.tasks.length}\nmemories=${state.memories.length}\nserviceWorker=${"serviceWorker" in navigator}`; }
}
function statusLine(name,ok){return `<p><b style="color:${ok?"var(--ok)":"var(--danger)"}">${ok?"✓":"×"}</b> ${escapeHtml(name)}</p>`;}
function showReport(eyebrow,title,content){modal({eyebrow,title,body:content,confirm:"FECHAR",onConfirm:()=>true});}
async function systemTests(){const lines=[statusLine("Interface carregada",true),statusLine("Armazenamento local",storageTest()),statusLine("Web Audio disponível",Boolean(window.AudioContext||window.webkitAudioContext)),statusLine("Microfone suportado",Boolean(navigator.mediaDevices?.getUserMedia))];try{await healthCheck();lines.push(statusLine("FLUX Core online",true));}catch{lines.push(statusLine("FLUX Core online",false));}if(state.token){try{const report=await diagnostics();lines.push(statusLine("Gemini texto",report.diagnostics?.ai==="OK"),statusLine("Gemini Live",report.diagnostics?.voice==="OK"));}catch{lines.push(statusLine("Autenticação do núcleo",false));}}else lines.push(statusLine("Token configurado",false));return lines.join("");}
function storageTest(){try{localStorage.setItem("flux-test","ok");localStorage.removeItem("flux-test");return true;}catch{return false;}}
function restoreBackup(){try{const backup=localStorage.getItem("flux-recovery-backup");if(!backup)throw new Error("Nenhum backup encontrado.");state={...initial,...JSON.parse(backup)};save();location.reload();}catch(error){toast(friendlyError(error),"error");}}

async function testAudio(){ try{ if(live.active){live.sendText("Diga apenas: Olá Maurício, o áudio do FLUX está funcionando.");toast("Teste enviado para a voz FLUX.","ok");}else{await live.start();setTimeout(()=>live.sendText("Diga apenas: Olá Maurício, o áudio do FLUX está funcionando."),1400);} }catch(error){toast(friendlyError(error),"error");} }

function bindEvents(){
  document.addEventListener("click",async event=>{const goButton=event.target.closest("[data-go]");if(goButton){go(goButton.dataset.go);return;}const action=event.target.closest("[data-action]")?.dataset.action;if(action==="toggle-voice")return toggleVoice();if(action==="start-screen")return startScreen();if(action==="stop-screen")return stopScreen();if(action==="new-task")return newTaskModal();if(action==="new-project")return newProjectModal();if(action==="new-memory")return newMemoryModal();if(action==="run-check")return runLab("test");if(action==="scan-devices")return toast("Busca concluída. Use SmartThings para autorizar a TV.","ok");if(action==="audio-test")return testAudio();const prompt=event.target.closest("[data-prompt]")?.dataset.prompt;if(prompt)return sendChat(prompt);const integration=event.target.closest("[data-integration]")?.dataset.integration;if(integration)return openIntegration(integration);const lab=event.target.closest("[data-lab]")?.dataset.lab;if(lab)return runLab(lab);const theme=event.target.closest("[data-theme-pick]")?.dataset.themePick;if(theme){state.theme=theme;save();applyTheme();return;}const toggle=event.target.closest("[data-task-toggle]")?.dataset.taskToggle;if(toggle){const task=state.tasks.find(item=>item.id===toggle);if(task)task.done=!task.done;save();renderTasks();return;}const del=event.target.closest("[data-task-delete]")?.dataset.taskDelete;if(del){state.tasks=state.tasks.filter(item=>item.id!==del);save();renderTasks();return;}const memoryDelete=event.target.closest("[data-memory-delete]")?.dataset.memoryDelete;if(memoryDelete){state.memories=state.memories.filter(item=>item.id!==memoryDelete);save();renderMemories();toast("Memória esquecida.");return;}const reveal=event.target.closest("[data-memory-reveal]")?.dataset.memoryReveal;if(reveal)return revealMemory(reveal);const device=event.target.closest("[data-device]")?.dataset.device;if(device)return deviceAction(device);});
  $("#voiceCore").onclick=toggleVoice; $("#stopVoice").onclick=()=>live.stop();
  $("#chatForm").onsubmit=event=>{event.preventDefault();const input=$("#chatInput");sendChat(input.value);input.value="";input.style.height="auto";};
  $("#chatInput").oninput=event=>{event.target.style.height="auto";event.target.style.height=`${Math.min(150,event.target.scrollHeight)}px`;};
  $("#chatInput").onkeydown=event=>{if(event.key==="Enter"&&!event.shiftKey){event.preventDefault();$("#chatForm").requestSubmit();}};
  $("#clearChat").onclick=()=>{state.messages=[];localStorage.removeItem("flux-conversation-id");save();renderMessages();toast("Conversa limpa.");};
  $("#taskForm").onsubmit=event=>{event.preventDefault();addTask($("#taskInput").value);$("#taskInput").value="";};
  $("#imageForm").onsubmit=createImage; $$('[data-style]').forEach(button=>button.onclick=()=>{selectedStyle=button.dataset.style;$$('[data-style]').forEach(item=>item.classList.toggle("selected",item===button));});
  $("#saveConnection").onclick=async()=>{state.coreUrl=$("#coreUrl").value.trim().replace(/\/$/,"")||location.origin;const token=$("#authToken").value.trim();if(token)state.token=token;save();setBusy($("#saveConnection"),true,"TESTANDO…");try{await healthCheck();if(state.token){await diagnostics();toast("FLUX Core autenticado e pronto.","ok");}else toast("Núcleo encontrado. Falta o token protegido.");}catch(error){toast(friendlyError(error),"error");}finally{setBusy($("#saveConnection"),false);}};
  $("#savePin").onclick=async()=>{const pin=$("#privatePin").value.trim();if(!/^\d{4,8}$/.test(pin))return toast("Use um PIN de 4 a 8 números.","error");state.pinHash=await hashText(pin);sessionPin=pin;save();$("#privatePin").value="";toast("PIN local salvo.","ok");};
  $("#voiceEnabled").onchange=event=>{state.voiceEnabled=event.target.checked;save();if(!state.voiceEnabled)live.stop();};
  $("#wakeEnabled").onchange=event=>{event.target.checked=false;state.wakeEnabled=false;save();toast("A ativação contínua por “Flux” fica no aplicativo Android; navegadores bloqueiam microfone permanente.");};
  $("#installButton").onclick=async()=>{if(installEvent){installEvent.prompt();await installEvent.userChoice;installEvent=null;$("#installButton").hidden=true;}};
  window.addEventListener("beforeinstallprompt",event=>{event.preventDefault();installEvent=event;$("#installButton").hidden=false;});
}

async function revealMemory(id){const memory=state.memories.find(item=>item.id===id);if(!memory?.encrypted)return;modal({eyebrow:"FLUX SECURITY",title:"Desbloquear memória",body:'<label class="modal-field">PIN<input id="unlockPin" type="password" inputmode="numeric" maxlength="8"></label>',confirm:"DESBLOQUEAR",onConfirm:async()=>{const pin=$("#unlockPin").value;if(await hashText(pin)!==state.pinHash)throw new Error("PIN incorreto.");memory.content=await decryptPrivate(memory.encrypted,pin);memory.revealed=true;sessionPin=pin;renderMemories();setTimeout(()=>{memory.revealed=false;delete memory.content;renderMemories();},30000);}});}
function deviceAction(id){if(id==="mobile")showReport("FLUX NEXUS","Este dispositivo",`<p>ID protegido: <b>${escapeHtml(DEVICE_ID.slice(0,18))}…</b></p><p>Chat, voz Gemini Live, visão e PWA disponíveis.</p>`);else if(id==="tv")openIntegration("smartthings");else toast("Módulo preparado para a próxima etapa de hardware.");}
function applyTheme(){document.documentElement.dataset.theme=state.theme;$$('[data-theme-pick]').forEach(node=>node.classList.toggle("selected",node.dataset.themePick===state.theme));}

async function init(){
  applyTheme(); bindEvents(); renderMessages(); renderTasks(); renderProjects(); renderMemories(); renderIntegrations(); renderDevices(); renderImage();
  $("#coreUrl").value=state.coreUrl; $("#authToken").value=state.token; $("#voiceEnabled").checked=state.voiceEnabled; $("#wakeEnabled").checked=false;
  updateClock();setInterval(updateClock,30000);const requested=new URLSearchParams(location.search).get("view");go(requested||"home",false);
  if("serviceWorker" in navigator && location.protocol==="https:")navigator.serviceWorker.register("/sw.js").catch(()=>{});
  setTimeout(()=>$("#boot").classList.add("done"),520);healthCheck().catch(()=>{});
}
init();
