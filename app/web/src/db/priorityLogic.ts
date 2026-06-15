import type { Task } from './models';

export function calculatePriorityScore(task: Partial<Task>, ageDays: number = 0): number {
  const categoryWeight = task.categoryWeight ?? 1.0;
  const hasDate = task.date ? 1 : 0;
  const timeUrgency = task.timeUrgency ?? 0.0;
  const subtasksCount = task.subtasksCount ?? 0;
  const estimatedMinutes = task.estimatedMinutes ?? 0;
  const manualBoost = task.manualBoost ?? 5.0;

  let score = 
    10 * categoryWeight + 
    6 * hasDate + 
    8 * timeUrgency - 
    2 * Math.log(1 + subtasksCount) - 
    0.02 * estimatedMinutes - 
    0.5 * ageDays + 
    manualBoost;

  if (categoryWeight >= 4.0) {
    score = Math.max(score, 70);
  }

  return score;
}

export function evaluateIsProject(task: Partial<Task>): boolean {
  const estimatedMinutes = task.estimatedMinutes ?? 0;
  const subtasksCount = task.subtasksCount ?? 0;
  return estimatedMinutes > 180 || subtasksCount > 10;
}
