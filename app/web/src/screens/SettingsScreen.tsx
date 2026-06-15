import React, { useState, useEffect } from 'react';
import { Card } from '../components/BaseComponents';
import { requestNotificationPermission } from '../sync/messaging';

export const SettingsScreen: React.FC = () => {
  const [manualBoost, setManualBoost] = useState<number>(5);
  const [autoSplit, setAutoSplit] = useState<boolean>(false);
  const [rrule, setRrule] = useState<string>('Diario');

  useEffect(() => {
    const savedBoost = localStorage.getItem('prima_manualBoost');
    if (savedBoost) setManualBoost(Number(savedBoost));
    const savedAutoSplit = localStorage.getItem('prima_autoSplit');
    if (savedAutoSplit) setAutoSplit(savedAutoSplit === 'true');
    const savedRrule = localStorage.getItem('prima_rrule');
    if (savedRrule) setRrule(savedRrule);
  }, []);

  const handleSave = () => {
    localStorage.setItem('prima_manualBoost', manualBoost.toString());
    localStorage.setItem('prima_autoSplit', autoSplit.toString());
    localStorage.setItem('prima_rrule', rrule);
    alert('Ajustes guardados localmente.');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', padding: '24px', maxWidth: '640px', margin: '0 auto', width: '100%' }}>
      <h2>Ajustes de Recurrencia y Prioridad</h2>
      
      <Card>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <h3>Recurrencia (RRULE)</h3>
          <select 
            value={rrule} 
            onChange={(e) => setRrule(e.target.value)}
            style={{ padding: '8px', borderRadius: '8px', border: '1px solid var(--color-muted)' }}
          >
            <option value="Diario">Diario</option>
            <option value="Semanal">Semanal</option>
            <option value="Cada mes">Cada mes</option>
            <option value="Custom">Custom...</option>
          </select>
          <p className="micro">Resumen: Se repetirá cada {rrule.toLowerCase()}</p>
        </div>
      </Card>

      <Card>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <h3>Ajustes de Prioridad</h3>
          
          <label style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <input 
              type="checkbox" 
              checked={autoSplit} 
              onChange={(e) => setAutoSplit(e.target.checked)} 
            />
            Auto-split en tareas largas (&gt;120min)
          </label>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '16px' }}>
            <label style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>Manual Boost (0 - 30)</span>
              <span style={{ fontWeight: 'bold' }}>{manualBoost}</span>
            </label>
            <input 
              type="range" 
              min="0" max="30" 
              value={manualBoost} 
              onChange={(e) => setManualBoost(Number(e.target.value))} 
              style={{ width: '100%' }}
            />
            <p className="micro">Impacto estimado: Suma directa al Score final.</p>
          </div>
        </div>
      </Card>

      <Card>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <h3>Reglas Non-Postponable (Categorías)</h3>
          
          <label style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <input type="checkbox" defaultChecked />
            Salud → Medicación
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <input type="checkbox" defaultChecked />
            Trámites → Urgente
          </label>
        </div>
      </Card>

      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3>Notificaciones (FCM)</h3>
            <p className="micro">Recibe recordatorios de tus tareas</p>
          </div>
          <button className="btn-icon" onClick={requestNotificationPermission}>Habilitar</button>
        </div>
      </Card>

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
        <button className="btn-primary" onClick={handleSave}>Guardar Cambios</button>
      </div>
    </div>
  );
};
