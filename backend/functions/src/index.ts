import {initializeApp} from 'firebase-admin/app';
import {getFirestore, Timestamp} from 'firebase-admin/firestore';
import {onCall, onRequest, HttpsError} from 'firebase-functions/v2/https';
import {defineSecret} from 'firebase-functions/params';
import {replyToSathi, SathiError} from './sathi';
initializeApp();
const geminiKey = defineSecret('GEMINI_API_KEY');
const region = 'asia-south1';

export const apiHealth = onRequest({region, maxInstances: 1, minInstances: 0, timeoutSeconds: 10, memory: '128MiB'}, (request, response) => {
  if (request.method !== 'GET') { response.status(405).set('Allow', 'GET').end(); return; }
  response.set('Cache-Control', 'public, max-age=60').json({service: 'life-mate', apiVersion: 1, status: 'ok'});
});

// Provider credentials are read only by this function from Secret Manager.
export const sathi = onCall({region, secrets: [geminiKey], maxInstances: 2, minInstances: 0, concurrency: 8, timeoutSeconds: 40, memory: '256MiB'}, async request => {
  try {
    return await replyToSathi(request.auth?.uid, request.data, {
      model: process.env.GEMINI_MODEL || 'gemini-2.5-flash', key: () => geminiKey.value(), fetch,
      consumeQuota: async uid => {
        const now = Date.now(), day = new Date(now).toISOString().slice(0, 10);
        const db = getFirestore();
        const user = db.doc(`internalLimits/${uid}_${day}`), global = db.doc(`internalLimits/global_${day}`);
        await db.runTransaction(async tx => {
          const [u, g] = await Promise.all([tx.get(user), tx.get(global)]);
          if ((u.data()?.count ?? 0) >= 20 || (g.data()?.count ?? 0) >= 200) throw new SathiError('resource-exhausted', 'Daily support limit reached');
          const expiresAt = Timestamp.fromMillis(now + 172800000);
          tx.set(user, {count: (u.data()?.count ?? 0) + 1, expiresAt});
          tx.set(global, {count: (g.data()?.count ?? 0) + 1, expiresAt});
        });
      }
    });
  } catch (error) {
    if (error instanceof SathiError) throw new HttpsError(error.code, error.message);
    // Do not echo Firebase/provider errors, user text or secrets into replies/logs.
    throw new HttpsError('unavailable', 'Sathi is unavailable. Try again later.');
  }
});
