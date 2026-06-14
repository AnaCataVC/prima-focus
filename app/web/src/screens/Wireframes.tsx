import React, { useState } from 'react';
import { Card, StartButton, IconButton, Chip } from '../components/BaseComponents';

export const InboxModal: React.FC = () => {
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
          style={{ flex: 1, border: 'none', background: 'transparent', outline: 'none', fontSize: '16px' }} 
        />
        <div style={{ display: 'flex', gap: '8px' }}>
          <Chip label="Hoy" />
          <Chip label="Sin fecha" />
        </div>
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
        <button className="btn-primary">Añadir</button>
      </div>
    </div>
  );
};

export const HomeTodayCard: React.FC = () => {
  return (
    <Card>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <h1 style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>Enviar informe trimestral</h1>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <span className="micro" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>💼 Trabajo</span>
          <span className="micro">•</span>
          <span className="micro">entrega</span>
          <span className="micro">•</span>
          <Chip label="45 min" />
          <div style={{ width: '12px', height: '12px', borderRadius: '50%', background: 'var(--color-danger)', marginLeft: 'auto' }} title="Priority Score ≥ 70" />
        </div>
        
        <div>
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px' }}>
            <input type="checkbox" /> Reunir datos
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', marginTop: '8px' }}>
            <input type="checkbox" /> Formatear slides
          </label>
          <p className="micro" style={{ marginTop: '8px' }}>3 pasos</p>
        </div>

        <div style={{ display: 'flex', justifyContent: 'center', margin: '24px 0' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <StartButton onClick={() => console.log('start')} />
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

export const TimerPanel: React.FC = () => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '24px', padding: '24px' }}>
      <h2 style={{ fontSize: '14px', color: 'var(--color-muted)' }}>Enviar informe trimestral</h2>
      <div style={{ 
        width: '220px', height: '220px', borderRadius: '50%', 
        border: '8px solid var(--color-primary)', 
        display: 'flex', alignItems: 'center', justifyContent: 'center' 
      }}>
        <div style={{ fontSize: '48px', fontWeight: 'bold', color: 'var(--color-text)' }}>
          25:00
        </div>
      </div>
      <p className="micro">Paso 1: Reunir datos</p>
      
      <div style={{ display: 'flex', gap: '16px', width: '100%' }}>
        <button className="btn-icon" style={{ flex: 1, borderRadius: 'var(--radius-pill)' }}>Pausa</button>
        <button className="btn-primary" style={{ flex: 1, backgroundColor: 'var(--color-accent)', color: 'var(--color-text)' }}>Terminé</button>
      </div>
    </div>
  );
};
