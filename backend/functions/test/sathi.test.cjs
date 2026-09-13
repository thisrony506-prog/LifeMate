const {test} = require('node:test');
const assert = require('node:assert/strict');
const {replyToSathi, SathiError} = require('../lib/sathi');
const success = () => new Response(JSON.stringify({candidates:[{content:{parts:[{text:'I hear you. Take one gentle breath.'}]}}]}));
function deps(overrides={}) { return {consumeQuota:async()=>{}, key:()=> 'inert-provider-fixture', model:'test-model', fetch:async()=>success(), ...overrides}; }
test('unauthenticated/invalid requests never consume quota or contact provider',async()=>{
 let calls=0;const d=deps({consumeQuota:async()=>{calls++},fetch:async()=>{calls++;return success()}});
 await assert.rejects(replyToSathi(undefined,{message:'hi',language:'en'},d),e=>e.code==='unauthenticated');
 for(const data of [null,[],{}, {message:' ',language:'en'}, {message:'x'.repeat(2001),language:'bn'}, {message:'hi',language:'xx'}]) await assert.rejects(replyToSathi('u',data,d),e=>e.code==='invalid-argument');
 assert.equal(calls,0);
});
test('quota exhaustion makes no provider call',async()=>{
 let calls=0;
 await assert.rejects(replyToSathi('u',{message:'Hi',language:'en'},deps({consumeQuota:async()=>{throw new SathiError('resource-exhausted','limit')},fetch:async()=>{calls++;return success()}})), e=>e.code==='resource-exhausted');
 assert.equal(calls,0);
});
test('only approved message is sent, key is a header, and language is explicit',async()=>{
 let captured;
 const answer=await replyToSathi('u',{message:'I feel worried',language:'bn',journal:'DO NOT SEND'},deps({fetch:async(url,options)=>{captured={url,options};return success()}}));
 assert.match(answer.reply,/gentle/);
 assert.equal(captured.options.headers['x-goog-api-key'],'inert-provider-fixture');
 assert.equal(captured.url.includes('inert-provider-fixture'),false);
 const body=JSON.parse(captured.options.body);
 assert.deepEqual(body.contents,[{role:'user',parts:[{text:'I feel worried'}]}]);
 assert.match(body.systemInstruction.parts[0].text,/Bangla/);
 assert.equal(captured.options.body.includes('DO NOT SEND'),false);
});
test('crisis fallback does not call provider',async()=>{
 for(const [message,language] of [['kill myself','en'],['আত্মহত্যা','bn']]) {
  const result=await replyToSathi('u',{message,language},deps({fetch:async()=>{throw new Error('must not be called')}}));
  assert.match(result.reply,/999|৯৯৯/);
 }
});
test('provider failures, malformed, empty and oversized bodies fail safely',async()=>{
 const responses=[()=>new Response('private error',{status:500}),()=>new Response('not json'),()=>new Response('{}'),()=>new Response('x'.repeat(65537)),()=>new Response(JSON.stringify({candidates:[{content:{parts:[{text:'x'.repeat(6001)}]}}]}))];
 for(const response of responses) await assert.rejects(replyToSathi('u',{message:'Hi',language:'en'},deps({fetch:async()=>response()})),e=>e.code==='unavailable'&&!e.message.includes('private error'));
});
test('missing server credential is unavailable, never a fake AI reply',async()=>{
 await assert.rejects(replyToSathi('u',{message:'Hi',language:'en'},deps({key:()=>''})),e=>e.code==='unavailable');
});
