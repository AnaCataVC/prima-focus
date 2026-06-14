import { InboxModal, HomeTodayCard, TimerPanel } from './screens/Wireframes';
import './index.css';

function App() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', backgroundColor: 'var(--color-surface)' }}>
      <header style={{ padding: '16px 24px', borderBottom: '1px solid var(--shadow)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--color-background)' }}>
        <h1 style={{ color: 'var(--color-primary)', fontSize: '20px' }}>Prima-Focus PWA</h1>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <span className="micro" title="Sync State: Synced">☁️ (Synced)</span>
          <span className="micro">Viernes 12 jun • 1/3 completadas</span>
        </div>
      </header>
      
      <main style={{ flex: 1, padding: '24px', display: 'flex', gap: '32px', justifyContent: 'center', maxWidth: '1440px', margin: '0 auto', width: '100%' }}>
        
        <section style={{ flex: '0 0 320px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <h2>Captura Rápida</h2>
          <InboxModal />
        </section>
        
        <section style={{ flex: '1 1 640px', maxWidth: '640px', display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center' }}>
          <h2 style={{ alignSelf: 'flex-start' }}>Tarea Hoy</h2>
          <div style={{ width: '100%' }}>
            <HomeTodayCard />
          </div>
        </section>

        <section style={{ flex: '0 0 320px', display: 'flex', flexDirection: 'column', gap: '16px', background: 'var(--color-background)', borderRadius: '16px', border: '1px solid var(--shadow)' }}>
          <TimerPanel />
        </section>

      </main>
    </div>
  );
}

export default App;
