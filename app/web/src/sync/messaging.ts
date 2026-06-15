import { getMessaging, getToken, onMessage } from "firebase/messaging";
import { app, auth, db } from "./firebase";
import { doc, setDoc } from "firebase/firestore";

export const requestNotificationPermission = async () => {
  try {
    const permission = await Notification.requestPermission();
    if (permission === 'granted') {
      const messaging = getMessaging(app);
      const token = await getToken(messaging);
      if (token) {
        console.log('FCM Token generated');
        const userId = auth.currentUser?.uid;
        if (userId) {
          await setDoc(doc(db, `users/${userId}`), { fcmToken: token, updatedAt: Date.now() }, { merge: true });
          alert("Notificaciones habilitadas correctamente.");
        }
      }
    } else {
      alert("Permiso de notificaciones denegado.");
    }
  } catch (error) {
    console.error('Error getting FCM token: ', error);
  }
};

export const setupMessageListener = () => {
  try {
    const messaging = getMessaging(app);
    onMessage(messaging, (payload) => {
      console.log('Message received: ', payload);
      if (payload.notification) {
        new Notification(payload.notification.title || 'Prima-Focus', {
          body: payload.notification.body,
        });
      }
    });
  } catch (error) {
    console.error('Messaging not supported or not setup', error);
  }
};
