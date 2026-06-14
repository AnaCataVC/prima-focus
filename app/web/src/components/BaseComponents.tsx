import React from 'react';

export const Card: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div className="card">{children}</div>
);

export const StartButton: React.FC<{ onClick: () => void }> = ({ onClick }) => (
  <button className="btn-start" onClick={onClick}>
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
  </button>
);

export const IconButton: React.FC<{ icon: React.ReactNode, onClick: () => void }> = ({ icon, onClick }) => (
  <button className="btn-icon" onClick={onClick}>
    {icon}
  </button>
);

export const Chip: React.FC<{ label: string }> = ({ label }) => (
  <span className="chip">{label}</span>
);
