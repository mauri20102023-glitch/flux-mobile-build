import {test} from 'node:test';
import assert from 'node:assert/strict';
import {modelText} from '../src/model-response.ts';
test('numeric JSON response uses the actual text completion without crashing',()=>{assert.equal(modelText({response:391,choices:[{message:{content:'391'}}]}),'391');assert.equal(modelText({response:391}),'391');});
test('structured response cannot mask a valid completion or become object text',()=>{assert.equal(modelText({response:{answer:42},choices:[{message:{content:'Resposta: 42'}}]}),'Resposta: 42');assert.equal(modelText({response:{answer:42}}),'');assert.equal(modelText({output:[{content:[{text:'391'}]}]}),'391');});
