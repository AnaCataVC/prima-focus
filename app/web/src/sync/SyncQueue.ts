import { db as firestore, auth } from './firebase';
import { collection, doc, setDoc, onSnapshot } from 'firebase/firestore';
import { initDB } from '../db/database';

export class SyncQueue {
  private isOnline: boolean = navigator.onLine;
  private unsubscribes: (() => void)[] = [];

  constructor() {
    window.addEventListener('online', () => {
      this.isOnline = true;
      this.processQueue();
    });
    window.addEventListener('offline', () => {
      this.isOnline = false;
    });
  }

  private setupCollectionListener(userId: string, db: any, collectionName: 'tasks' | 'sessions', idField: string, versionField: string) {
    const unsub = onSnapshot(collection(firestore, `users/${userId}/${collectionName}`), async (snapshot) => {
      for (const change of snapshot.docChanges()) {
        const docId = change.doc.id;
        if (change.type === 'added' || change.type === 'modified') {
          const remoteDoc = change.doc.data() as any;
          const localDoc = await db.get(collectionName, remoteDoc[idField]);
          if (!localDoc || remoteDoc[versionField] > localDoc[versionField]) {
            remoteDoc.dirty = false;
            await db.put(collectionName, remoteDoc);
          }
        } else if (change.type === 'removed') {
          await db.delete(collectionName, docId);
        }
      }
    });
    this.unsubscribes.push(unsub);
  }

  public async startListening() {
    const userId = auth.currentUser?.uid;
    if (!userId) return;

    const db = await initDB();

    // Clear old listeners
    this.unsubscribes.forEach(unsub => unsub());
    this.unsubscribes = [];

    this.setupCollectionListener(userId, db, 'tasks', 'taskId', 'version');
    this.setupCollectionListener(userId, db, 'sessions', 'sessionId', 'updatedAt');
  }

  public enqueueMutation() {
    if (this.isOnline) {
      this.processQueue();
    }
  }

  private async pushCollection(userId: string, db: any, collectionName: 'tasks' | 'sessions', idField: string) {
    const items = await db.getAll(collectionName);
    const dirtyItems = items.filter((item: any) => item.dirty);
    
    for (const item of dirtyItems) {
      const docRef = doc(firestore, `users/${userId}/${collectionName}`, item[idField]);
      const { dirty, ...cloudData } = item;
      await setDoc(docRef, cloudData, { merge: true });
      
      item.dirty = false;
      await db.put(collectionName, item);
    }
  }

  public async processQueue() {
    if (!this.isOnline) return;
    const userId = auth.currentUser?.uid;
    if (!userId) return;

    try {
      const db = await initDB();
      await this.pushCollection(userId, db, 'tasks', 'taskId');
      await this.pushCollection(userId, db, 'sessions', 'sessionId');
    } catch (error) {
      console.error('Error processing sync queue:', error);
    }
  }
}

export const syncQueue = new SyncQueue();
