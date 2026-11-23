// Bwriter web app service worker — registration stub.
//
// Offline caching is deliberately not implemented in this first step: the web
// build is online-only for now, and every request goes straight to the
// network. The handlers exist so the registration in index.html succeeds and
// the installability requirements are met; a real cache strategy lands with
// the offline work.
self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
  event.respondWith(fetch(event.request));
});
