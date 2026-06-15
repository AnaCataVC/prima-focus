import { openDB } from 'idb';
import type { DBSchema, IDBPDatabase } from 'idb';
import type { Task, Session, Event } from './models';

// Interfaz para TypeScript y auto-completado
interface PrimaFocusDB extends DBSchema {
  tasks: {
    key: string;
    value: Task;
    indexes: {
      'by_status': string;
      'by_date': string;
      'by_priority': number;
      'by_updatedAt': number;
    };
  };
  sessions: {
    key: string;
    value: Session;
    indexes: {
      'by_taskId': string;
    };
  };
  events: {
    key: string;
    value: Event;
    indexes: {
      'by_taskId': string;
      'by_scheduledAt': number;
    };
  };
  analytics: {
    key: string;
    value: any; // O define Analytics model si lo requieres
    indexes: {
      'by_createdAt': number;
    };
  };
}

const DB_NAME = 'focusapp-db';
const DB_VERSION = 1;

export async function initDB(): Promise<IDBPDatabase<PrimaFocusDB>> {
  const db = await openDB<PrimaFocusDB>(DB_NAME, DB_VERSION, {
    upgrade(db) {
      // Store Tasks
      if (!db.objectStoreNames.contains('tasks')) {
        const tasksStore = db.createObjectStore('tasks', { keyPath: 'taskId' });
        tasksStore.createIndex('by_status', 'status');
        tasksStore.createIndex('by_date', 'date');
        tasksStore.createIndex('by_priority', 'priorityScore');
        tasksStore.createIndex('by_updatedAt', 'updatedAt');
      }

      // Store Sessions
      if (!db.objectStoreNames.contains('sessions')) {
        const sessionsStore = db.createObjectStore('sessions', { keyPath: 'sessionId' });
        sessionsStore.createIndex('by_taskId', 'taskId');
      }

      // Store Events
      if (!db.objectStoreNames.contains('events')) {
        const eventsStore = db.createObjectStore('events', { keyPath: 'eventId' });
        eventsStore.createIndex('by_taskId', 'taskId');
        eventsStore.createIndex('by_scheduledAt', 'scheduledAt');
      }

      // Store Analytics
      if (!db.objectStoreNames.contains('analytics')) {
        const analyticsStore = db.createObjectStore('analytics', { keyPath: 'eventId' });
        analyticsStore.createIndex('by_createdAt', 'createdAt');
      }
    }
  });

  return db;
}
