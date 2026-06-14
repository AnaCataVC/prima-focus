import { Task } from './models';

export function createTask(partial: Partial<Task>): Task {
  const isMedicalOrUrgent = 
    partial.subcategory === 'medicación' || 
    partial.subcategory === 'urgente' || 
    partial.subcategory === 'trámites';

  const nonPostponable = partial.nonPostponable ?? isMedicalOrUrgent;
  
  // As requested in the checklist, prompt for auto-split if > 120 (UI level),
  // but we also keep track of it here.
  const estimatedMinutes = partial.estimatedMinutes ?? 0;
  
  // Manual boost defaults to 5
  const manualBoost = partial.manualBoost ?? 5;

  const now = Date.now();

  return {
    taskId: partial.taskId || crypto.randomUUID(),
    title: partial.title || '',
    description: partial.description || '',
    category: partial.category || 'general',
    subcategory: partial.subcategory || '',
    categoryWeight: partial.categoryWeight ?? 1.0,
    date: partial.date,
    time: partial.time,
    hasTime: !!partial.time,
    timeUrgency: partial.timeUrgency ?? 0,
    estimatedMinutes,
    subtasksCount: partial.subtasksCount ?? 0,
    isProject: partial.isProject ?? false,
    recurrence: partial.recurrence || '',
    manualBoost,
    nonPostponable,
    priorityScore: partial.priorityScore ?? 0,
    status: partial.status || 'inbox',
    posponedReason: partial.posponedReason,
    encrypted: partial.encrypted ?? false,
    createdAt: partial.createdAt || now,
    updatedAt: partial.updatedAt || now,
    version: partial.version || 1,
    dirty: partial.dirty ?? true,
    meta: partial.meta || {},
  };
}
