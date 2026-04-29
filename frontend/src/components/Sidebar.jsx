import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Phone, Radio, FileText } from 'lucide-react';

export default function Sidebar({ wsConnected }) {
  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <div className="logo-icon">
          <Radio size={22} color="white" />
        </div>
        <div>
          <h1>VoiceCast</h1>
          <span>Broadcast System</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        <NavLink to="/" end className={({ isActive }) => isActive ? 'active' : ''}>
          <LayoutDashboard size={20} />
          <span>Dashboard</span>
        </NavLink>
        <NavLink to="/phone-numbers" className={({ isActive }) => isActive ? 'active' : ''}>
          <Phone size={20} />
          <span>Phone Numbers</span>
        </NavLink>
        <NavLink to="/sessions" className={({ isActive }) => isActive ? 'active' : ''}>
          <Radio size={20} />
          <span>Call Sessions</span>
        </NavLink>
        <NavLink to="/logs" className={({ isActive }) => isActive ? 'active' : ''}>
          <FileText size={20} />
          <span>Call Logs</span>
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <div className="ws-status">
          <div className={`ws-dot ${wsConnected ? 'connected' : ''}`} />
          <span>{wsConnected ? 'Live Connected' : 'Disconnected'}</span>
        </div>
      </div>
    </aside>
  );
}
