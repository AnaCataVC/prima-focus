import { initDB } from './database';
import type { Task } from './models';
import { createTask } from './taskFactory';
import { calculatePriorityScore } from './priorityLogic';

export async function addTask(partialTask: Partial<Task>): Promise<string> {
  const db = await initDB();
  
  // Create task with defaults and calculate priority score
  const task = createTask(partialTask);
  task.priorityScore = calculatePriorityScore(task);
  
  await db.put('tasks', task);
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
}
