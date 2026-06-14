self.addEventListener('install', (event) => {
  console.log('[Service Worker] Installed');
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  console.log('[Service Worker] Activated');
  event.waitUntil(self.clients.claim());
});

self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SCHEDULE_NOTIFICATION') {
    const { taskTitle, score, isNonPostponable } = event.data.payload;
    
    let title = taskTitle;
    let body = "";
    let requireInteraction = false;
    let actions = [{ action: 'start', title: 'Empezar' }];

    if (score >= 70) {
      body = "¡Alta prioridad! Tienes que enfocarte ahora.";
      requireInteraction = true;
    } else if (score >= 40) {
      body = "Es un buen momento para avanzar en tu Tarea de Hoy.";
    } else {
      body = "Recordatorio suave para tu tarea.";
    }

    if (!isNonPostponable) {
      actions.push({ action: 'snooze', title: 'Posponer 1h' });
    }

    const options = {
      body,
      icon: '/favicon.svg',
      requireInteraction,
      actions,
      data: { taskTitle }
    };

    self.registration.showNotification(title, options);
  }
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  
  if (event.action === 'start') {
    // Focus or open app and start timer
    event.waitUntil(
      self.clients.matchAll({ type: 'window' }).then((clientList) => {
        for (const client of clientList) {
          if (client.url === '/' && 'focus' in client) return client.focus();
        }
        if (self.clients.openWindow) return self.clients.openWindow('/');
      })
    );
  } else if (event.action === 'snooze') {
    console.log('Posponer 1h ejecutado desde notificación web');
    // Here we would sync back to the app to update the next scheduled time
  } else {
    // Default click
    event.waitUntil(
      self.clients.matchAll({ type: 'window' }).then((clientList) => {
        for (const client of clientList) {
          if (client.url === '/' && 'focus' in client) return client.focus();
        }
        if (self.clients.openWindow) return self.clients.openWindow('/');
      })
    );
  }
});
