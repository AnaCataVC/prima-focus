import React, { useState } from 'react';
import { Card, StartButton, IconButton, Chip } from '../components/BaseComponents';
import type { Task } from '../db/models';

function deduceCategoryInfo(title: string) {
  const lower = title.toLowerCase();
  if (/(informe|proyecto|trabajo|reunión|presentación)/.test(lower)) {
    return { category: 'trabajo', categoryWeight: 4.0 };
  }
  if (/(cita|médico|pastilla|medicación|salud)/.test(lower)) {
    return { category: 'salud', subcategory: 'medicación', categoryWeight: 5.0, nonPostponable: true };
  }
  if (/(pagar|factura|trámite|banco|impuesto)/.test(lower)) {
    return { category: 'urgente', subcategory: 'trámites', categoryWeight: 5.0, nonPostponable: true };
  }
  return { category: 'general', categoryWeight: 1.0 };
}

export const InboxModal: React.FC<{ onAdd: (task: Partial<Task>) => void }> = ({ onAdd }) => {
  const [title, setTitle] = useState('');

  const handleAdd = () => {
    if (!title.trim()) return;
    
    const { category, subcategory, categoryWeight, nonPostponable } = deduceCategoryInfo(title);

    onAdd({
      title: title.trim(),
      category,
      subcategory,
      categoryWeight,
      nonPostponable,
      status: 'inbox'
    });
    setTitle('');
  };

  return (
    <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px', background: 'var(--color-surface)', borderRadius: '16px', width: '640px', maxWidth: '100%', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <div style={{
          width: '48px', height: '48px', borderRadius: '50%', background: 'var(--color-background)', 
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', border: '1px solid var(--color-muted)'
        }}>🎤</div>
        <input 
          type="text" 
          placeholder="Anotar en 2s" 
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleAdd()}
          style={{ flex: 1, border: 'none', background: 'transparent', outline: 'none', fontSize: '16px' }} 
        />
        <div style={{ display: 'flex', gap: '8px' }}>
          <Chip label="Hoy" />
          <Chip label="Sin fecha" />
        </div>
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
        <button className="btn-primary" onClick={handleAdd}>Añadir</button>
      </div>
    </div>
  );
};

export const HomeTodayCard: React.FC<{ task?: Task | null }> = ({ task }) => {
  if (!task) {
    return (
      <Card>
        <div style={{ padding: '24px', textAlign: 'center', color: 'var(--color-muted)' }}>
          <p>Tu Tarea Hoy aparecerá aquí.</p>
          <p className="micro">Añade tareas en el Inbox para comenzar.</p>
        </div>
      </Card>
    );
  }

  const priorityColor = task.priorityScore >= 70 ? 'var(--color-danger)' : (task.priorityScore >= 40 ? 'var(--color-primary)' : 'var(--color-muted)');

  return (
    <Card>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <h1 style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{task.title}</h1>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <span className="micro" style={{ display: 'flex', alignItems: 'center', gap: '4px', textTransform: 'capitalize' }}>
            {task.category === 'trabajo' ? '💼' : (task.category === 'salud' ? '💊' : '📌')} {task.category}
          </span>
          {task.subcategory && (
            <>
              <span className="micro">•</span>
              <span className="micro" style={{ textTransform: 'capitalize' }}>{task.subcategory}</span>
            </>
          )}
          <span className="micro">•</span>
          <Chip label={`${task.estimatedMinutes} min`} />
          <div style={{ width: '12px', height: '12px', borderRadius: '50%', background: priorityColor, marginLeft: 'auto' }} title={`Score: ${task.priorityScore.toFixed(1)}`} />
        </div>
        
        {task.isProject && (
          <div style={{ background: '#FFF3CD', color: '#856404', padding: '8px', borderRadius: '4px', fontSize: '12px', fontWeight: 'bold' }}>
            Proyecto grande — dividir en subtareas
          </div>
        )}

        {task.subtasksCount > 0 && (
          <div>
            <p className="micro" style={{ marginTop: '8px' }}>{task.subtasksCount} pasos</p>
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'center', margin: '24px 0' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <StartButton onClick={() => console.log('start', task.taskId)} />
            <span style={{ fontWeight: 'bold' }}>Empezar</span>
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-around', borderTop: '1px solid var(--shadow)', paddingTop: '16px' }}>
          <IconButton icon="✏️" onClick={() => {}} />
          <IconButton icon="➕" onClick={() => {}} />
          <IconButton icon="🕒" onClick={() => {}} />
        </div>
      </div>
    </Card>
  );
};

export const TimerPanel: React.FC<{ task?: Task | null, onComplete?: () => void }> = ({ task, onComplete }) => {
  const [timeLeft, setTimeLeft] = useState(25 * 60);
  const [isRunning, setIsRunning] = useState(false);

  React.useEffect(() => {
    if (task) {
      setTimeLeft((task.estimatedMinutes || 25) * 60);
      setIsRunning(false);
    }
  }, [task]);

  React.useEffect(() => {
    let interval: any;
    if (isRunning && timeLeft > 0) {
      interval = setInterval(() => {
        setTimeLeft(t => t - 1);
      }, 1000);
    } else if (timeLeft === 0 && isRunning) {
      setIsRunning(false);
    }
    return () => clearInterval(interval);
  }, [isRunning, timeLeft]);

  const mins = Math.floor(timeLeft / 60).toString().padStart(2, '0');
  const secs = (timeLeft % 60).toString().padStart(2, '0');
  if (!task) return null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '24px', padding: '24px' }}>
      <h2 style={{ fontSize: '14px', color: 'var(--color-muted)' }}>{task.title}</h2>
      <div style={{ 
        width: '220px', height: '220px', borderRadius: '50%', 
        border: '8px solid var(--color-primary)', 
        display: 'flex', alignItems: 'center', justifyContent: 'center' 
      }}>
        <div style={{ fontSize: '48px', fontWeight: 'bold', color: 'var(--color-text)' }}>
          {mins}:{secs}
        </div>
      </div>
      
      <div style={{ display: 'flex', gap: '16px', width: '100%' }}>
        <button className="btn-icon" style={{ flex: 1, borderRadius: 'var(--radius-pill)' }} onClick={() => setIsRunning(!isRunning)}>{isRunning ? 'Pausa' : 'Iniciar'}</button>
        <button className="btn-primary" style={{ flex: 1, backgroundColor: 'var(--color-accent)', color: 'var(--color-text)' }} onClick={onComplete}>Terminé</button>
      </div>
    </div>
  );
};
