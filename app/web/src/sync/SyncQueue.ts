

export interface SyncOperation {
  collection: 'tasks' | 'events' | 'sessions';
  operationType: 'CREATE' | 'UPDATE' | 'DELETE';
  payload: any;
  version: number;
}

export class SyncQueue {
  private isOnline: boolean = navigator.onLine;

  constructor() {
    window.addEventListener('online', () => {
      this.isOnline = true;
      this.processQueue();
    });
    window.addEventListener('offline', () => {
      this.isOnline = false;
    });
  }

  public enqueueMutation(collection: string, payload: any) {
    console.log('Enqueue', collection, payload);
    // 1. Mark item as dirty locally and increment version
    // 2. Save to IndexedDB
    // 3. Try to process queue
    if (this.isOnline) {
      this.processQueue();
    }
  }

  public async processQueue() {
    if (!this.isOnline) return;
    
    // Fetch all records where dirty = true from IndexedDB
    // For each record, push to Firestore
    // On success, set dirty = false and update locally
    console.log('Processing sync queue...');
  }
}
