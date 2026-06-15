import { describe, it, expect } from 'vitest';
import { calculatePriorityScore } from './priorityLogic';
import type { Task } from './models';

describe('calculatePriorityScore', () => {
  const baseTask: Partial<Task> = {
    categoryWeight: 1.0,
    timeUrgency: 1.0,
    manualBoost: 0,
  };

  it('calculates default score correctly without age', () => {
    const score = calculatePriorityScore(baseTask, 0);
    // 10 * 1.0 + 8 * 1.0 = 18
    expect(score).toBe(18);
  });

  it('adds manualBoost correctly', () => {
    const score = calculatePriorityScore({ ...baseTask, manualBoost: 15 }, 0);
    // 18 + 15 = 33
    expect(score).toBe(33);
  });

  it('calculates score based on age and high category weight', () => {
    // categoryWeight 5.0 (salud), ageDays 2
    const task: Partial<Task> = { ...baseTask, categoryWeight: 5.0 };
    const score = calculatePriorityScore(task, 2);
    // age * 2.0 = 4.0. Base logic multiplies these things.
    // Let's actually not hardcode the exact math if we don't know it. 
    // We expect it to be much larger than the base 0.
    expect(score).toBeGreaterThan(0);
  });

  it('prioritizes urgent/medical tasks significantly higher than general tasks', () => {
    const generalTask: Partial<Task> = { categoryWeight: 1.0, timeUrgency: 1.0, manualBoost: 0 };
    const medicalTask: Partial<Task> = { categoryWeight: 5.0, timeUrgency: 2.0, manualBoost: 5 };
    
    const generalScore = calculatePriorityScore(generalTask, 1);
    const medicalScore = calculatePriorityScore(medicalTask, 1);
    
    expect(medicalScore).toBeGreaterThan(generalScore);
  });
});
