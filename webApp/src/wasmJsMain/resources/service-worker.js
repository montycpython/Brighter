// Bwriter web app service worker.
//
// The app shell (page, script, wasm, icons, plates) is served
// stale-while-revalidate from a versioned cache: the app opens at once,
// even offline, and a fresh deploy is picked up in the background for
// the next open. Everything else — above all the Firestore and Google
// API traffic that carries the books themselves — is never cached and
// always goes to the network.
const CACHE = 'bwriter-shell-v2';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(
        keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))
      ))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.origin !== self.location.origin) {
    return;
  }
  event.respondWith(
    caches.open(CACHE).then(async (cache) => {
      const cached = await cache.match(event.request);
      const network = fetch(event.request)
        .then((response) => {
          if (response && response.status === 200) {
            cache.put(event.request, response.clone());
          }
          return response;
        })
        .catch(() => cached);
      return cached || network;
    })
  );
});
