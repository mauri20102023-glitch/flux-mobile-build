import {test} from 'node:test';
import assert from 'node:assert/strict';
import {validateBuildFiles,checksReady,REQUIRED_CHECKS} from '../src/build-guard.ts';
import {FluxBuild} from '../src/build.ts';
function fixture(env={},network=async()=>{throw new Error('Unexpected network');}){const values=new Map();const store={get:async k=>values.get(k),put:async(k,v)=>values.set(k,v),transaction:async f=>f(store)};return {values,build:new FluxBuild(store,env,network)};}
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
test('configured Build restricts the GitHub App grant and creates only a task branch and draft PR (mock)',async()=>{
  const keys=await crypto.subtle.generateKey({name:'RSASSA-PKCS1-v1_5',modulusLength:2048,publicExponent:new Uint8Array([1,0,1]),hash:'SHA-256'},true,['sign','verify']);
  const pem='-----BEGIN PRIVATE KEY-----\n'+Buffer.from(await crypto.subtle.exportKey('pkcs8',keys.privateKey)).toString('base64')+'\n-----END PRIVATE KEY-----';
  const calls=[],head='b'.repeat(40),parent='a'.repeat(40);
  const f=fixture({FLUX_BUILD_APP_ID:'123',FLUX_BUILD_INSTALLATION_ID:'456',FLUX_BUILD_PRIVATE_KEY:pem},async(url,options)=>{
    const path=new URL(url).pathname,body=options.body?JSON.parse(options.body):null;calls.push({path,method:options.method,body});
    if(path.endsWith('/access_tokens')){assert.deepEqual(body.repositories,['flux-mobile-build']);assert.deepEqual(body.permissions,{contents:'write',pull_requests:'write',checks:'read',administration:'read'});return Response.json({token:'fixture-installation-only'});}
    assert.equal(options.headers.authorization,'Bearer fixture-installation-only');
    if(path.endsWith('/branches/main'))return Response.json({protected:true,commit:{sha:parent}});
    if(path.endsWith('/branches/main/protection'))return Response.json({required_status_checks:{strict:true,contexts:REQUIRED_CHECKS},required_pull_request_reviews:{required_approving_review_count:1},enforce_admins:{enabled:true},allow_force_pushes:{enabled:false},allow_deletions:{enabled:false}});
    if(path.endsWith('/git/commits/'+parent))return Response.json({tree:{sha:'c'.repeat(40)}});
    if(path.endsWith('/git/trees')){assert.equal(body.base_tree,'c'.repeat(40));return Response.json({sha:'d'.repeat(40)});}
    if(path.endsWith('/git/commits'))return Response.json({sha:head});
    if(path.endsWith('/git/refs')){assert.match(body.ref,/^refs\/heads\/flux-build\/[0-9a-f-]{36}$/);return Response.json({ref:body.ref});}
    if(path.endsWith('/pulls')){assert.equal(body.base,'main');assert.equal(body.draft,true);return Response.json({number:21,html_url:'https://github.com/mauri20102023-glitch/flux-mobile-build/pull/21'});}
    throw new Error('Unexpected GitHub operation');
  });
  const created=await (await f.build.create({objective:'Criar componente',files:[{path:'web/flux-ath/components/card.js',content:'export const ready=false;'}]},'owner')).json();
  const denied=new Request('https://flux.test/v1/build/tasks/'+created.task.id+'/publish',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({approve:false})});await assert.rejects(()=>f.build.route(denied,'owner'),/explicitamente/);assert.equal(calls.length,0);
  const response=await f.build.publish(created.task.id,'owner');assert.equal(response.status,201);const output=await response.json();assert.equal(output.task.state,'pr_open');assert.equal(output.merged,false);assert.equal(output.deployed,false);assert.equal(JSON.stringify(output).includes('fixture-installation-only'),false);assert.ok(calls.every(c=>['GET','POST'].includes(c.method)));assert.ok(calls.every(c=>!c.path.endsWith('/merge')));
});
