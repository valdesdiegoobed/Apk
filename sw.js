const CACHE='expedientes-local-v10';
const APP_SHELL=['/Apk/','/Apk/manifest.webmanifest','/Apk/icon.svg'];
self.addEventListener('install',event=>{event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(APP_SHELL)));self.skipWaiting()});
self.addEventListener('activate',event=>{event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key!==CACHE).map(key=>caches.delete(key)))));self.clients.claim()});
self.addEventListener('fetch',event=>{if(event.request.method!=='GET')return;event.respondWith(fetch(event.request).then(response=>{if(event.request.mode==='navigate'){const copy=response.clone();caches.open(CACHE).then(cache=>cache.put('/Apk/',copy))}return response}).catch(()=>caches.match(event.request).then(cached=>cached||caches.match('/Apk/'))))});