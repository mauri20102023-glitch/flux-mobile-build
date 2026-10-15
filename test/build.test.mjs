import {test} from 'node:test';
import assert from 'node:assert/strict';
import {validateBuildFiles,checksReady,REQUIRED_CHECKS} from '../src/build-guard.ts';
import {FluxBuild} from '../src/build.ts';
function fixture(env={}){const values=new Map();const store={get:async k=>values.get(k),put:async(k,v)=>values.set(k,v),transaction:async f=>f(store)};return {values,build:new FluxBuild(store,env,async()=>{throw new Error('Unexpected network');})};}
test('Build cannot edit identity, approval rules, authentication, tests, workflows or traverse paths',()=>{
  for(const path of ['FLUX_IDENTITY.md','src/index.ts','src/build-guard.ts','src/capabilities.ts','.github/workflows/build-flux-mobile.yml','web/flux-ath/app.js','test/build.test.mjs','web/flux-ath/components/../../app.js','web/flux-ath/components//new.js','/tmp/new.js'])assert.throws(()=>validateBuildFiles([{path,content:'change'}]),/protegido/);
  assert.equal(validateBuildFiles([{path:'web/flux-ath/components/weather.js',content:'export const ready=false;'}]).length,1);
});
test('Build rejects oversized patches, duplicate paths and credential material',()=>{
  const path='src/adapters/weather.ts';assert.throws(()=>validateBuildFiles([{path,content:'a'.repeat(48001)}]),/48 KB/);assert.throws(()=>validateBuildFiles([{path,content:'a'},{path,content:'b'}]),/protegido/);assert.throws(()=>validateBuildFiles([{path,content:'-----BEGIN PRIVATE KEY-----\nsecret'}]),/credencial/);
});
test('preparing patch persists without running code; missing GitHub App cannot publish',async()=>{
  const f=fixture();const response=await f.build.create({objective:'Adicionar um card',files:[{path:'web/flux-ath/components/card.js',content:'export const title="Clima";'}]},'owner');assert.equal(response.status,201);const {task}=await response.json();assert.equal(task.state,'draft');await assert.rejects(()=>f.build.publish(task.id,'owner'),/não configurado/);assert.equal(f.values.get('build:task:'+task.id).state,'draft');
});
test('Build task scope cannot grant itself permissions or target another repository',async()=>{
  const f=fixture();await assert.rejects(()=>f.build.create({objective:'change',files:[{path:'src/adapters/x.ts',content:'export {}'}],repository:'attacker/repo'},'owner'),/não são controlados/);
  const req=new Request('https://flux.test/v1/build/deploy',{method:'POST',headers:{'content-type':'application/json'},body:'{}'});await assert.rejects(()=>f.build.route(req,'owner'),/não permitida/);
});
test('CI must succeed on the exact task head and all mandatory checks must exist',()=>{
  const head='a'.repeat(40),checks=REQUIRED_CHECKS.map(name=>({name,status:'completed',conclusion:'success',head_sha:head}));assert.equal(checksReady(checks,head),true);assert.equal(checksReady([],head),false);assert.equal(checksReady(checks,'b'.repeat(40)),false);checks[0].conclusion='failure';assert.equal(checksReady(checks,head),false);
});
