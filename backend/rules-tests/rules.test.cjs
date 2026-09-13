const {before,after,test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {initializeTestEnvironment,assertSucceeds,assertFails}=require('@firebase/rules-unit-testing');
const {doc,setDoc,getDoc,getDocs,collection,updateDoc,serverTimestamp,Timestamp}=require('firebase/firestore');
const {ref,uploadBytes,getBytes,deleteObject}=require('firebase/storage');
let env;
before(async()=>{
 env=await initializeTestEnvironment({projectId:'demo-life-mate',firestore:{rules:fs.readFileSync(path.join(__dirname,'../firestore.rules'),'utf8')},storage:{rules:fs.readFileSync(path.join(__dirname,'../storage.rules'),'utf8')}});
});
after(async()=>{await env?.cleanup()});
test('vault ownership, field whitelist and monotonic revision',async()=>{
 const alice=env.authenticatedContext('alice').firestore(), bob=env.authenticatedContext('bob').firestore();
 const path='users/alice/vault/r1';
 await assertSucceeds(setDoc(doc(alice,path),{ciphertext:'encrypted-fixture',revision:1,updatedAt:serverTimestamp()}));
 await assertSucceeds(getDoc(doc(alice,path)));
 await assertFails(getDoc(doc(bob,path)));
 await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),path)));
 await assertFails(updateDoc(doc(alice,path),{revision:1,updatedAt:serverTimestamp()}));
 await assertSucceeds(updateDoc(doc(alice,path),{revision:2,updatedAt:serverTimestamp()}));
 await assertFails(updateDoc(doc(alice,path),{revision:3,plainJournal:'private',updatedAt:serverTimestamp()}));
 await assertFails(setDoc(doc(alice,'users/alice/vault/bad'),{ciphertext:'x',revision:99,updatedAt:serverTimestamp()}));
});
test('vault recovery marker cannot be replaced',async()=>{
 const db=env.authenticatedContext('marker-owner').firestore();const target=doc(db,'users/marker-owner/meta/vault');
 await assertSucceeds(setDoc(target,{ciphertext:'encrypted-marker'}));
 await assertFails(setDoc(target,{ciphertext:'replaced'}));
});
test('location is invite-only, expires and cannot be extended by owner',async()=>{
 const owner=env.authenticatedContext('sender').firestore(), guest=env.authenticatedContext('viewer').firestore();
 const path='live/unpredictable-invite-fixture';const expiry=Timestamp.fromMillis(Date.now()+600000);
 await assertSucceeds(setDoc(doc(owner,path),{owner:'sender',ciphertext:'encrypted-location',expiresAt:expiry,updatedAt:serverTimestamp()}));
 await assertSucceeds(getDoc(doc(guest,path)));
 await assertFails(getDocs(collection(guest,'live')));
 await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),path)));
 await assertFails(updateDoc(doc(guest,path),{ciphertext:'tamper',updatedAt:serverTimestamp()}));
 await assertFails(updateDoc(doc(owner,path),{expiresAt:Timestamp.fromMillis(Date.now()+700000),updatedAt:serverTimestamp()}));
 await assertSucceeds(updateDoc(doc(owner,path),{ciphertext:'new-position',updatedAt:serverTimestamp()}));
 await env.withSecurityRulesDisabled(async ctx=>setDoc(doc(ctx.firestore(),'live/expired'),{owner:'sender',ciphertext:'x',expiresAt:Timestamp.fromMillis(Date.now()-60000),updatedAt:Timestamp.now()}));
 await assertFails(getDoc(doc(guest,'live/expired')));
});
test('internal quota counters are never client-readable or writable',async()=>{
 const db=env.authenticatedContext('alice').firestore();
 await assertFails(setDoc(doc(db,'internalLimits/global_today'),{count:0}));
 await assertFails(getDoc(doc(db,'internalLimits/global_today')));
});
test('storage is owner-private and accepts only bounded binary objects',async()=>{
 const a=env.authenticatedContext('photo-owner').storage(), b=env.authenticatedContext('intruder').storage();
 const path='users/photo-owner/encrypted/p1';
 await assertSucceeds(uploadBytes(ref(a,path),new Uint8Array([1,2,3]),{contentType:'application/octet-stream'}));
 assert.equal((await assertSucceeds(getBytes(ref(a,path)))).byteLength,3);
 await assertFails(getBytes(ref(b,path)));
 await assertFails(uploadBytes(ref(a,'users/photo-owner/encrypted/plain'),new Uint8Array([1]),{contentType:'image/jpeg'}));
 await assertFails(uploadBytes(ref(b,path),new Uint8Array([1]),{contentType:'application/octet-stream'}));
 await assertFails(uploadBytes(ref(a,'users/photo-owner/encrypted/large'),new Uint8Array(21*1024*1024),{contentType:'application/octet-stream'}));
 await assertSucceeds(deleteObject(ref(a,path)));
});
