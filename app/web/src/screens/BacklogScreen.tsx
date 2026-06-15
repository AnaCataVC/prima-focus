import { useEffect, useState } from 'react';
import type { Task } from '../db/models';
import { getAllTasks, deleteTask } from '../db/repository';
import { Card, IconButton, Chip } from '../components/BaseComponents';

export const BacklogScreen = () => {
  const [tasks, setTasks] = useState<Task[]>([]);

  const loadTasks = async () => {
    const data = await getAllTasks();
    setTasks(data);
  };

  useEffect(() => {
    loadTasks();
  }, []);

  const handleDelete = async (id: string) => {
    if (window.confirm('¿Seguro que quieres eliminar esta tarea?')) {
      await deleteTask(id);
      loadTasks();
    }
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', width: '100%', display: 'flex', flexDirection: 'column', gap: '16px' }}>
      <h2>Backlog (Todas las tareas)</h2>
      {tasks.length === 0 ? (
        <p style={{ color: 'var(--color-muted)' }}>No hay tareas aún.</p>
      ) : (
        tasks.map(task => (
          <Card key={task.taskId}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <strong style={{ textDecoration: task.status === 'completed' ? 'line-through' : 'none' }}>{task.title}</strong>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginTop: '4px' }}>
                  <Chip label={task.status} />
                  <span className="micro">Score: {task.priorityScore.toFixed(1)}</span>
                  <span className="micro">Categoría: {task.category}</span>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <IconButton icon="✏️" onClick={() => alert('Edición próximamente')} />
                <IconButton icon="🗑️" onClick={() => handleDelete(task.taskId)} />
              </div>
            </div>
          </Card>
        ))
      )}
    </div>
  );
};
