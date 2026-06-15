import { describe, it, expect } from 'vitest';
import { createTask } from './taskFactory';

describe('createTask', () => {
  it('initializes a task with correct default values', () => {
    const task = createTask({ title: 'Test task' });
    
    expect(task.title).toBe('Test task');
    expect(task.status).toBe('inbox');
    expect(task.taskId).toBeDefined();
    expect(task.createdAt).toBeDefined();
    expect(task.version).toBe(1);
    expect(task.categoryWeight).toBe(1.0);
    expect(task.isProject).toBe(false);
  });

  it('detects urgency based on subcategory and overrides flags', () => {
    const urgentTask = createTask({ title: 'Urgent payment', subcategory: 'urgente' });
    
    expect(urgentTask.subcategory).toBe('urgente');
    expect(urgentTask.nonPostponable).toBe(true);
  });

  it('respects partially provided data', () => {
    const customTask = createTask({ 
      title: 'Custom', 
      estimatedMinutes: 60,
      isProject: true,
      manualBoost: 10
    });
    
    expect(customTask.title).toBe('Custom');
    expect(customTask.estimatedMinutes).toBe(60);
    expect(customTask.isProject).toBe(true);
    expect(customTask.manualBoost).toBe(10);
    // Still sets status and id
    expect(customTask.status).toBe('inbox');
    expect(customTask.taskId).toBeDefined();
  });
});
