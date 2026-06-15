import { useEffect, useState } from 'react';
import { InboxModal, HomeTodayCard, TimerPanel } from './screens/Wireframes';
import { getTodayTask, addTask, updateTask } from './db/repository';
import type { Task } from './db/models';
import './index.css';

function App() {
  const [todayTask, setTodayTask] = useState<Task | null>(null);

  const fetchTodayTask = async () => {
    try {
      const task = await getTodayTask();
      setTodayTask(task);
    } catch (error) {
      console.error('Failed to fetch today task:', error);
    }
  };

  useEffect(() => {
    fetchTodayTask();
  }, []);

  const handleAddTask = async (partialTask: Partial<Task>) => {
    try {
      await addTask(partialTask);
      await fetchTodayTask();
    } catch (error) {
      console.error('Failed to add task:', error);
    }
  };

  const handleCompleteTask = async (task: Task) => {
    try {
      task.status = 'completed';
      await updateTask(task);
      await fetchTodayTask();
    } catch (error) {
      console.error('Failed to complete task:', error);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', backgroundColor: 'var(--color-surface)' }}>
      <header style={{ padding: '16px 24px', borderBottom: '1px solid var(--shadow)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--color-background)' }}>
        <h1 style={{ color: 'var(--color-primary)', fontSize: '20px' }}>Prima-Focus PWA</h1>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <span className="micro" title="Offline Mode">📶 Offline (Local DB)</span>
          <span className="micro">1/3 completadas</span>
        </div>
      </header>
      
      <main style={{ flex: 1, padding: '24px', display: 'flex', gap: '32px', justifyContent: 'center', maxWidth: '1440px', margin: '0 auto', width: '100%' }}>
        
        <section style={{ flex: '0 0 320px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <h2>Captura Rápida</h2>
          <InboxModal onAdd={handleAddTask} />
        </section>
        
        <section style={{ flex: '1 1 640px', maxWidth: '640px', display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center' }}>
          <h2 style={{ alignSelf: 'flex-start' }}>Tarea Hoy</h2>
          <div style={{ width: '100%' }}>
            <HomeTodayCard task={todayTask} />
          </div>
        </section>

        <section style={{ flex: '0 0 320px', display: 'flex', flexDirection: 'column', gap: '16px', background: 'var(--color-background)', borderRadius: '16px', border: '1px solid var(--shadow)' }}>
          {todayTask ? <TimerPanel task={todayTask} onComplete={() => handleCompleteTask(todayTask)} /> : (
            <div style={{ padding: '24px', textAlign: 'center', color: 'var(--color-muted)' }}>
              Selecciona o añade una tarea para iniciar el timer.
            </div>
          )}
        </section>

      </main>
    </div>
  );
}

export default App;
