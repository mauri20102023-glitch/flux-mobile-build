import {test} from 'node:test';
import assert from 'node:assert/strict';
import {FluxCapabilities} from '../src/capabilities.ts';
import {FluxState} from '../src/index.ts';
function fixture(connected=false,action={accepted:true,verified:false}){
  const values=new Map(),calls=[];const store={get:async k=>values.get(k),put:async(k,v)=>values.set(k,v),delete:async k=>values.delete(k),transaction:async f=>f(store),setAlarm:async()=>{}};
  const deps={calendar:async()=>Response.json({connected:false,events:[]}),memories:async()=>[],connect:async req=>{
    const url=new URL(req.url);if(url.pathname==='/v1/connect')return Response.json({providers:['gmail','smartthings','spotify','calendar'].map(id=>({id,connected,missing:connected?[]:['OAUTH_CONFIG']}))});
    calls.push({path:url.pathname,body:await req.json()});return Response.json(url.pathname.endsWith('/action')?action:{messages:[{subject:'Fixture'}]});
  }};
  return {registry:new FluxCapabilities(store,deps),values,calls,store,deps};
}
test('new capability, tool and Build routes require existing Core authentication',async()=>{
  const f=fixture(),core=new FluxState({storage:f.store},{FLUX_AUTH_TOKEN:'a'.repeat(32)});
  for(const path of ['/v1/capabilities','/v1/tools/invoke','/v1/build','/v1/build/tasks']){
    const r=await core.fetch(new Request('https://flux.test'+path));assert.equal(r.status,401);
  }
});
test('runtime registry describes readiness and scopes without claiming external execution',async()=>{
  const f=fixture();const c=await f.registry.catalog();assert.equal(c.capabilities.find(c=>c.id==='gmail.messages.read').state,'conectar');assert.equal(c.capabilities.find(c=>c.id==='memory.list').state,'pronto');assert.equal(f.calls.length,0);assert.ok((await f.registry.instructions()).includes('não possui executor remoto'));
});
test('unconnected natural reads never become fabricated provider results',async()=>{
  const f=fixture();const r=await f.registry.natural('FLUX, leia meus e-mails','actor');assert.equal(r.state,'unavailable');assert.equal(r.verified,false);assert.equal(f.calls.length,0);assert.equal(await f.registry.natural('Explique como consultar e-mails','actor'),null);
});
test('explicit natural read reaches the actual authorized adapter with bounded parameters',async()=>{
  const f=fixture(true);const r=await f.registry.natural('Mostre meus emails','actor');assert.equal(r.verified,true);assert.equal(f.calls[0].path,'/v1/connect/gmail/read');assert.deepEqual(f.calls[0].body,{});
});
test('calendar fallback uses actual Android snapshot and treats empty events as empty',async()=>{
  const f=fixture();f.deps.calendar=async()=>Response.json({connected:true,events:[],updatedAt:new Date().toISOString()});const r=await f.registry.invoke('calendar.read',{},'a');assert.equal(r.state,'empty');assert.equal(r.verified,true);
});
test('write requires separate actor-bound single-use confirmation; accept is not verify',async()=>{
  const f=fixture(true);const pending=await f.registry.invoke('samsung_tv.volume.set',{deviceId:'123e4567-e89b-42d3-a456-426614174000',volume:25},'owner-session');assert.equal(pending.state,'approval_required');assert.equal(f.calls.length,0);
  await assert.rejects(()=>f.registry.confirm(pending.approvalId,'other-session',true),/outra sessão/);
  const done=await f.registry.confirm(pending.approvalId,'owner-session',true);assert.equal(done.accepted,true);assert.equal(done.verified,false);assert.equal(done.state,'accepted');assert.equal(f.calls[0].body.volume,25);
  await assert.rejects(()=>f.registry.confirm(pending.approvalId,'owner-session',true),/utilizada/);
  const audit=f.values.get('tools:audit');assert.equal(audit.length,1);assert.equal(JSON.stringify(audit).includes('volume'),true);assert.equal(JSON.stringify(audit).includes('123e4567'),false);
});
test('expired or rejected confirmations cannot execute; model cannot inject confirm parameters',async()=>{
  const f=fixture(true);await assert.rejects(()=>f.registry.invoke('spotify.playback.pause',{confirm:true},'a'),/não permitido/);
  const pending=await f.registry.invoke('spotify.playback.pause',{},'a');assert.equal((await f.registry.confirm(pending.approvalId,'a',false)).accepted,false);assert.equal(f.calls.length,0);
  const expired=await f.registry.invoke('spotify.playback.pause',{},'a');f.values.get('tools:approval:'+expired.approvalId).expires=0;await assert.rejects(()=>f.registry.confirm(expired.approvalId,'a',true),/expirada/);assert.equal(f.calls.length,0);
});
test('schema rejects out-of-range volume, unknown tools and remote URL parameters',async()=>{
  const f=fixture(true);for(const volume of [-1,101,1.5,'25'])await assert.rejects(()=>f.registry.invoke('samsung_tv.volume.set',{deviceId:'123e4567-e89b-42d3-a456-426614174000',volume},'a'),/inteiro/);
  await assert.rejects(()=>f.registry.invoke('deploy.production',{},'a'),/não implementada/);await assert.rejects(()=>f.registry.invoke('gmail.messages.read',{url:'https://evil.test'},'a'),/não permitido/);
});
