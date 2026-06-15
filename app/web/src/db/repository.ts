import { initDB } from './database';
import type { Task, Session } from './models';
import { createTask } from './taskFactory';
import { calculatePriorityScore } from './priorityLogic';
import { syncQueue } from '../sync/SyncQueue';

export async function addTask(partialTask: Partial<Task>): Promise<string> {
  const db = await initDB();
  
  // Create task with defaults and calculate priority score
  const task = createTask(partialTask);
  task.priorityScore = calculatePriorityScore(task);
  
  await db.put('tasks', task);
  syncQueue.enqueueMutation();
  return task.taskId;
}

export async function getTodayTask(): Promise<Task | null> {
  const db = await initDB();
  
  // Optimize by iterating over the priority index from highest to lowest
  let cursor = await db.transaction('tasks').store.index('by_priority').openCursor(null, 'prev');
  
  while (cursor) {
    if (cursor.value.status === 'inbox' || cursor.value.status === 'today') {
      return cursor.value;
    }
    cursor = await cursor.continue();
  }
  
  return null;
}

export async function updateTask(task: Task): Promise<void> {
  const db = await initDB();
  task.updatedAt = Date.now();
  task.version += 1;
  task.dirty = true;
  await db.put('tasks', task);
  syncQueue.enqueueMutation();
}

export async function addSession(partialSession: Partial<Session>): Promise<string> {
  const db = await initDB();
  const now = Date.now();
  const session: Session = {
    sessionId: partialSession.sessionId || crypto.randomUUID(),
    taskId: partialSession.taskId || '',
    userId: partialSession.userId || 'local-user',
    startAt: partialSession.startAt || now,
    endAt: partialSession.endAt || now,
    mode: partialSession.mode || 'focus',
    durationMinutes: partialSession.durationMinutes || 0,
    result: partialSession.result || 'completed',
    feeling: partialSession.feeling || 'neutral',
    createdAt: partialSession.createdAt || now,
    updatedAt: partialSession.updatedAt || now,
    dirty: partialSession.dirty ?? true,
  };
  await db.put('sessions', session);
  syncQueue.enqueueMutation();
  return session.sessionId;
}

export async function getAllTasks(): Promise<Task[]> {
  const db = await initDB();
  const tasks = await db.getAll('tasks');
  // Sort by priorityScore descending
  tasks.sort((a, b) => b.priorityScore - a.priorityScore);
  return tasks;
}

export async function deleteTask(taskId: string): Promise<void> {
  const db = await initDB();
  // Technically we should mark as deleted and sync, but for MVP we delete directly.
  await db.delete('tasks', taskId);
}
