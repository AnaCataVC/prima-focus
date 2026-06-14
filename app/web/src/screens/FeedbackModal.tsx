import React from 'react';
import { Card, Chip } from '../components/BaseComponents';

export const FeedbackModal: React.FC = () => {
  return (
    <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '24px', background: 'var(--color-surface)', borderRadius: '16px', width: '360px', maxWidth: '100%', margin: '0 auto', boxShadow: '0 8px 24px var(--shadow)' }}>
      <h2 style={{ textAlign: 'center', fontSize: '18px' }}>Registro Breve</h2>
      
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <p style={{ fontWeight: 500 }}>¿Completaste el paso?</p>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button style={{ flex: 1, background: 'var(--color-accent)', color: '#fff', border: 'none', padding: '12px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold' }}>Sí</button>
          <button style={{ flex: 1, background: 'var(--color-warn)', color: '#fff', border: 'none', padding: '12px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold' }}>Parcial</button>
          <button style={{ flex: 1, background: 'var(--color-muted)', color: '#fff', border: 'none', padding: '12px', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold' }}>No</button>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <p style={{ fontWeight: 500 }}>¿Cómo te sentiste?</p>
        <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0 16px' }}>
          <button className="btn-icon" style={{ fontSize: '24px' }}>😐</button>
          <button className="btn-icon" style={{ fontSize: '24px' }}>🙂</button>
          <button className="btn-icon" style={{ fontSize: '24px' }}>😃</button>
        </div>
      </div>

      <div style={{ padding: '12px', background: 'var(--color-warn)', borderRadius: '8px', opacity: 0.8 }}>
        <p className="micro" style={{ color: '#fff', fontWeight: 'bold', marginBottom: '8px' }}>¿Quieres posponer o dividir en subtareas?</p>
        <div style={{ display: 'flex', gap: '8px' }}>
          <Chip label="Posponer" />
          <Chip label="Dividir" />
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'center' }}>
        <button className="btn-primary" style={{ width: '100%' }}>Guardar</button>
        <p className="micro">Se guardará en Historial</p>
      </div>
    </div>
  );
};
