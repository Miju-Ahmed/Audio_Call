import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Phone, Radio, PhoneCall, TrendingUp, Plus, Activity } from 'lucide-react';
import { getDashboardStats } from '../api/api';

export default function Dashboard({ callUpdates, wsConnected }) {
  const [stats, setStats] = useState(null);
  const [recentEvents, setRecentEvents] = useState([]);

  useEffect(() => {
    getDashboardStats().then(res => setStats(res.data)).catch(() => {});
  }, []);

  useEffect(() => {
    if (callUpdates.length > 0) {
      const latest = callUpdates.slice(-20).reverse();
      setRecentEvents(latest);
    }
  }, [callUpdates]);

  const statusEmoji = {
    QUEUED: '⏳', RINGING: '🔔', IN_PROGRESS: '🟢', COMPLETED: '✅',
    FAILED: '❌', BUSY: '📵', NO_ANSWER: '📞', CANCELLED: '🚫'
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>Dashboard</h2>
          <p>Real-time overview of your voice broadcast system</p>
        </div>
        <Link to="/sessions" className="btn btn-primary">
          <Plus size={18} /> New Session
        </Link>
      </div>

      <div className="stats-grid">
        <div className="stat-card" style={{ animationDelay: '0ms' }}>
          <div className="stat-icon blue"><Phone size={24} /></div>
          <div className="stat-info">
            <h3>{stats?.activePhoneNumbers ?? '—'}</h3>
            <p>Active Numbers</p>
          </div>
        </div>
        <div className="stat-card" style={{ animationDelay: '50ms' }}>
          <div className="stat-icon emerald"><Radio size={24} /></div>
          <div className="stat-info">
            <h3>{stats?.activeSessions ?? '—'}</h3>
            <p>Active Sessions</p>
          </div>
        </div>
        <div className="stat-card" style={{ animationDelay: '100ms' }}>
          <div className="stat-icon amber"><PhoneCall size={24} /></div>
          <div className="stat-info">
            <h3>{stats?.callsToday ?? '—'}</h3>
            <p>Calls Today</p>
          </div>
        </div>
        <div className="stat-card" style={{ animationDelay: '150ms' }}>
          <div className="stat-icon purple"><TrendingUp size={24} /></div>
          <div className="stat-info">
            <h3>{stats?.successRate != null ? `${stats.successRate}%` : '—'}</h3>
            <p>Success Rate</p>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
        <div className="card">
          <h3 className="section-title"><Activity size={18} /> Live Activity</h3>
          {!wsConnected && (
            <div className="empty-state" style={{ padding: '30px' }}>
              <p>WebSocket disconnected. Live updates will appear when connected.</p>
            </div>
          )}
          {wsConnected && recentEvents.length === 0 && (
            <div className="empty-state" style={{ padding: '30px' }}>
              <p>No active calls. Start a session to see live updates.</p>
            </div>
          )}
          {recentEvents.length > 0 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '400px', overflowY: 'auto' }}>
              {recentEvents.map((evt, i) => (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: '12px',
                  padding: '10px 14px', background: 'var(--bg-glass)',
                  borderRadius: 'var(--radius-sm)', fontSize: '13px'
                }}>
                  <span>{statusEmoji[evt.status] || '📞'}</span>
                  <span style={{ color: 'var(--text-primary)', fontWeight: 600, fontFamily: 'monospace' }}>
                    {evt.phoneNumber}
                  </span>
                  <span className={`badge badge-${evt.status?.toLowerCase()}`}>
                    <span className="badge-dot" />
                    {evt.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card">
          <h3 className="section-title"><Radio size={18} /> Quick Actions</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '16px' }}>
            <Link to="/sessions" className="btn btn-primary" style={{ justifyContent: 'center' }}>
              <Radio size={18} /> Create Broadcast Session
            </Link>
            <Link to="/phone-numbers" className="btn btn-outline" style={{ justifyContent: 'center' }}>
              <Phone size={18} /> Manage Phone Numbers
            </Link>
            <Link to="/logs" className="btn btn-outline" style={{ justifyContent: 'center' }}>
              <Activity size={18} /> View Call Logs
            </Link>
          </div>

          {stats && (
            <div style={{ marginTop: '24px', padding: '16px', background: 'var(--bg-glass)', borderRadius: 'var(--radius-md)' }}>
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '8px' }}>System Summary</p>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <p style={{ fontSize: '20px', fontWeight: 700 }}>{stats.totalPhoneNumbers}</p>
                  <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Total Numbers</p>
                </div>
                <div>
                  <p style={{ fontSize: '20px', fontWeight: 700 }}>{stats.totalSessions}</p>
                  <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Total Sessions</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
