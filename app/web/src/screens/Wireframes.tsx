import React, { useState } from 'react';
import { Card, Chip } from '../components/BaseComponents';
import type { Task, Session } from '../db/models';

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

export const HomeTodayCard: React.FC<{ task?: Task | null, onSplit?: (task: Task) => void }> = ({ task, onSplit }) => {
  if (!task) {
    return (
      <Card>
        <p style={{ color: 'var(--color-muted)', textAlign: 'center' }}>No hay tareas para hoy. ¡Disfruta tu día!</p>
      </Card>
    );
  }

  const priorityColor = task.priorityScore >= 70 ? 'var(--color-danger)' : (task.priorityScore >= 40 ? 'var(--color-primary)' : 'var(--color-muted)');

  return (
    <Card>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <h1 style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', margin: 0 }}>{task.title}</h1>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
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
          <div style={{ background: '#FFF3CD', color: '#856404', padding: '12px', borderRadius: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '12px', fontWeight: 'bold' }}>
            <span>⚠️ Proyecto grande — dividir en subtareas</span>
            {onSplit && (
              <button className="btn-icon" style={{ padding: '4px 12px', fontSize: '12px' }} onClick={() => onSplit(task)}>Dividir</button>
            )}
          </div>
        )}

        {task.subtasksCount > 0 && (
          <div>
            <p className="micro" style={{ marginTop: '8px' }}>{task.subtasksCount} pasos</p>
          </div>
        )}
      </div>
    </Card>
  );
};

export const TimerPanel: React.FC<{ task?: Task | null, onComplete?: (sessionData: Partial<Session>) => void }> = ({ task, onComplete }) => {
  const [timeLeft, setTimeLeft] = useState(25 * 60);
  const [isRunning, setIsRunning] = useState(false);
  const [sessionStart, setSessionStart] = useState<number | null>(null);

  React.useEffect(() => {
    if (task) {
      setTimeLeft((task.estimatedMinutes || 25) * 60);
      setIsRunning(false);
      setSessionStart(null);
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

  const handleStartPause = () => {
    if (!isRunning && !sessionStart) {
      setSessionStart(Date.now());
    }
    setIsRunning(!isRunning);
  };

  const handleComplete = () => {
    if (onComplete) {
      const end = Date.now();
      const start = sessionStart || end;
      const durationMinutes = Math.round((end - start) / 60000);
      
      onComplete({
        taskId: task?.taskId,
        startAt: start,
        endAt: end,
        durationMinutes: durationMinutes,
        mode: 'focus',
        result: 'completed',
        feeling: 'neutral'
      });
    }
  };

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
        <button className="btn-icon" style={{ flex: 1, borderRadius: 'var(--radius-pill)' }} onClick={handleStartPause}>{isRunning ? 'Pausa' : 'Iniciar'}</button>
        <button className="btn-primary" style={{ flex: 1, backgroundColor: 'var(--color-accent)', color: 'var(--color-text)' }} onClick={handleComplete}>Terminé</button>
      </div>
    </div>
  );
};

export const SplitTaskModal: React.FC<{ task: Task, onClose: () => void, onSave: (subtasks: string[]) => void }> = ({ task, onClose, onSave }) => {
  const [subtasks, setSubtasks] = useState<string[]>(['', '']);

  const handleAdd = () => setSubtasks([...subtasks, '']);
  const handleChange = (index: number, val: string) => {
    const newSubtasks = [...subtasks];
    newSubtasks[index] = val;
    setSubtasks(newSubtasks);
  };

  const handleSave = () => {
    const valid = subtasks.filter(s => s.trim() !== '');
    if (valid.length > 0) {
      onSave(valid);
    }
  };

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
      <div style={{ background: 'var(--color-surface)', padding: '24px', borderRadius: '16px', width: '400px', maxWidth: '90%' }}>
        <h2 style={{ marginTop: 0 }}>Dividir "{task.title}"</h2>
        <p className="micro">Ingresa el título de las subtareas. La tarea original se marcará como completada.</p>
        
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', margin: '16px 0' }}>
          {subtasks.map((st, i) => (
            <input key={i} className="input-field" placeholder={`Subtarea ${i+1}`} value={st} onChange={e => handleChange(i, e.target.value)} />
          ))}
          <button className="btn-icon" onClick={handleAdd}>+ Añadir otra</button>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
          <button className="btn-icon" onClick={onClose}>Cancelar</button>
          <button className="btn-primary" onClick={handleSave}>Guardar</button>
        </div>
      </div>
    </div>
  );
};

