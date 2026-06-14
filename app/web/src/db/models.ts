export interface Task {
  taskId: string;
  title: string;
  description: string;
  category: string;
  subcategory: string;
  categoryWeight: number;
  date?: string; // YYYY-MM-DD
  time?: string; // HH:mm
  hasTime: boolean;
  timeUrgency: number;
  estimatedMinutes: number;
  subtasksCount: number;
  isProject: boolean;
  recurrence: string;
  manualBoost: number; // default 5
  nonPostponable: boolean;
  priorityScore: number;
  status: 'inbox' | 'today' | 'completed' | 'postponed';
  posponedReason?: string;
  encrypted: boolean;
  createdAt: number;
  updatedAt: number;
  version: number;
  dirty: boolean;
  meta: Record<string, any>;
}

export interface Session {
  sessionId: string;
  taskId: string;
  userId: string;
  startAt: number;
  endAt: number;
  mode: string;
  durationMinutes: number;
  result: string;
  feeling: string;
  createdAt: number;
  updatedAt: number;
  dirty: boolean;
}

export interface Event {
  eventId: string;
  taskId: string;
  type: string;
  scheduledAt: number;
  attempt: number;
  action: string;
  status: string;
  createdAt: number;
  updatedAt: number;
  dirty: boolean;
}
