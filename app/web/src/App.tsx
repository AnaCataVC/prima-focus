import { useEffect, useState } from 'react';
import { InboxModal, HomeTodayCard, TimerPanel, SplitTaskModal } from './screens/Wireframes';
import { BacklogScreen } from './screens/BacklogScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { getTodayTask, addTask, updateTask, addSession } from './db/repository';
import type { Task, Session } from './db/models';
import { loginAnonymously, onAuthChange } from './sync/firebase';
import { syncQueue } from './sync/SyncQueue';
import type { User } from 'firebase/auth';
import './index.css';

function App() {
  const [currentView, setCurrentView] = useState<'focus' | 'backlog' | 'settings'>('focus');
  const [todayTask, setTodayTask] = useState<Task | null>(null);
  const [taskToSplit, setTaskToSplit] = useState<Task | null>(null);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [authInitialized, setAuthInitialized] = useState(false);

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
    
    const unsubscribe = onAuthChange(async (user) => {
      if (user) {
        setCurrentUser(user);
        syncQueue.startListening();
        syncQueue.processQueue();
        setAuthInitialized(true);
      } else {
        try {
          await loginAnonymously();
        } catch (err) {
          console.error('Offline / Failed anonymous login:', err);
          setAuthInitialized(true);
        }
      }
    });
    
    return () => unsubscribe();
  }, []);

  const handleAddTask = async (partialTask: Partial<Task>) => {
    try {
      await addTask(partialTask);
      await fetchTodayTask();
    } catch (error) {
      console.error('Failed to add task:', error);
    }
  };

  const handleCompleteTask = async (task: Task, sessionData: Partial<Session>) => {
    try {
      await addSession(sessionData);
      task.status = 'completed';
      await updateTask(task);
      await fetchTodayTask();
    } catch (error) {
      console.error('Failed to complete task:', error);
    }
  };

  const handleSplitTask = async (subtasksTitles: string[]) => {
    if (!taskToSplit) return;
    try {
      // Create subtasks
      for (const title of subtasksTitles) {
        await addTask({
          title,
          parentId: taskToSplit.taskId,
          category: taskToSplit.category,
          estimatedMinutes: Math.floor(taskToSplit.estimatedMinutes / subtasksTitles.length) || 25,
        });
      }
      // Mark parent as completed
      taskToSplit.status = 'completed';
      await updateTask(taskToSplit);
      setTaskToSplit(null);
      await fetchTodayTask();
    } catch (error) {
      console.error('Failed to split task:', error);
    }
  };

  if (!authInitialized) {
    return <div style={{ display: 'flex', height: '100vh', alignItems: 'center', justifyContent: 'center' }}>Cargando...</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', backgroundColor: 'var(--color-surface)' }}>
      <header style={{ padding: '16px 24px', borderBottom: '1px solid var(--shadow)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--color-background)' }}>
        <h1 style={{ color: 'var(--color-primary)', fontSize: '20px', margin: 0 }}>Prima-Focus</h1>
        
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          <button 
            style={{ padding: '8px 16px', borderRadius: '16px', border: 'none', cursor: 'pointer', background: currentView === 'focus' ? 'var(--color-primary)' : 'transparent', color: currentView === 'focus' ? '#fff' : 'inherit', fontWeight: currentView === 'focus' ? 'bold' : 'normal' }}
            onClick={() => setCurrentView('focus')}
          >
            Foco
          </button>
          <button 
            style={{ padding: '8px 16px', borderRadius: '16px', border: 'none', cursor: 'pointer', background: currentView === 'backlog' ? 'var(--color-primary)' : 'transparent', color: currentView === 'backlog' ? '#fff' : 'inherit', fontWeight: currentView === 'backlog' ? 'bold' : 'normal' }}
            onClick={() => setCurrentView('backlog')}
          >
            Backlog
          </button>
          <button 
            style={{ padding: '8px 16px', borderRadius: '16px', border: 'none', cursor: 'pointer', background: currentView === 'settings' ? 'var(--color-primary)' : 'transparent', color: currentView === 'settings' ? '#fff' : 'inherit', fontWeight: currentView === 'settings' ? 'bold' : 'normal' }}
            onClick={() => setCurrentView('settings')}
          >
            Configuración
          </button>
        </div>

        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          {currentUser ? (
            <span className="micro" title="Conectado a Firebase" style={{ color: 'var(--color-primary)' }}>☁️ Sincronizado</span>
          ) : (
            <span className="micro" title="Offline Mode">📶 Local</span>
          )}
        </div>
      </header>
      
      <main style={{ flex: 1, padding: '24px', display: 'flex', gap: '32px', justifyContent: 'center', maxWidth: '1440px', margin: '0 auto', width: '100%' }}>
        {currentView === 'focus' ? (
          <>
            <section style={{ flex: '0 0 320px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <h2>Captura Rápida</h2>
              <InboxModal onAdd={handleAddTask} />
            </section>
            
            <section style={{ flex: '1 1 640px', maxWidth: '640px', display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center' }}>
              <h2 style={{ alignSelf: 'flex-start' }}>Tarea Hoy</h2>
              <div style={{ width: '100%' }}>
                <HomeTodayCard task={todayTask} onSplit={setTaskToSplit} />
              </div>
            </section>

            <section style={{ flex: '0 0 320px', display: 'flex', flexDirection: 'column', gap: '16px', background: 'var(--color-background)', borderRadius: '16px', border: '1px solid var(--shadow)' }}>
              {todayTask ? <TimerPanel task={todayTask} onComplete={(sessionData) => handleCompleteTask(todayTask, sessionData)} /> : (
                <div style={{ padding: '24px', textAlign: 'center', color: 'var(--color-muted)' }}>
                  Selecciona o añade una tarea para iniciar el timer.
                </div>
              )}
            </section>
          </>
        ) : currentView === 'backlog' ? (
          <BacklogScreen />
        ) : (
          <SettingsScreen />
        )}
      </main>

      {taskToSplit && (
        <SplitTaskModal 
          task={taskToSplit} 
          onClose={() => setTaskToSplit(null)} 
          onSave={handleSplitTask} 
        />
      )}
    </div>
  );
}

export default App;
