import { useState, useEffect } from 'react';
import { Radio, Plus, Play, Square, X, Volume2, VolumeX, Users, Phone } from 'lucide-react';
import { getSessions, createSession, startSession, stopSession, getSessionStats, getSessionLogs, muteParticipant, unmuteParticipant, getPhoneGroups } from '../api/api';
import toast from 'react-hot-toast';

export default function Sessions({ callUpdates, wsConnected }) {
  const [sessions, setSessions] = useState([]);
  const [groups, setGroups] = useState([]);
  const [showCreate, setShowCreate] = useState(false);
  const [activeSession, setActiveSession] = useState(null);
  const [stats, setStats] = useState(null);
  const [logs, setLogs] = useState([]);
  const [form, setForm] = useState({ title: '', mode: 'BROADCAST', group: '' });

  const loadSessions = () => { getSessions().then(r => setSessions(r.data)).catch(() => {}); };
  useEffect(() => { loadSessions(); getPhoneGroups().then(r => setGroups(r.data)).catch(() => {}); }, []);

  useEffect(() => {
    if (activeSession) {
      getSessionStats(activeSession.id).then(r => setStats(r.data)).catch(() => {});
      getSessionLogs(activeSession.id).then(r => setLogs(r.data)).catch(() => {});
    }
  }, [activeSession, callUpdates]);

  const handleCreate = async () => {
    try {
      const res = await createSession(form);
      toast.success('Session created'); setShowCreate(false); setForm({ title: '', mode: 'BROADCAST', group: '' });
      loadSessions(); setActiveSession(res.data);
    } catch (e) { toast.error(e.response?.data?.message || 'Failed'); }
  };

  const handleStart = async (id) => {
    try { await startSession(id); toast.success('Calling started!'); loadSessions(); if (activeSession?.id === id) setActiveSession(prev => ({ ...prev, status: 'ACTIVE' })); }
    catch (e) { toast.error(e.response?.data?.message || 'Failed to start'); }
  };

  const handleStop = async (id) => {
    if (!confirm('Stop this session and end all calls?')) return;
    try { await stopSession(id); toast.success('Session stopped'); loadSessions(); if (activeSession?.id === id) setActiveSession(prev => ({ ...prev, status: 'COMPLETED' })); }
    catch (e) { toast.error('Failed to stop'); }
  };

  const handleMute = async (callSid, muted) => {
    if (!activeSession) return;
    try {
      if (muted) await muteParticipant(activeSession.id, callSid);
      else await unmuteParticipant(activeSession.id, callSid);
      toast.success(muted ? 'Muted' : 'Unmuted');
    } catch { toast.error('Failed'); }
  };

  const statusColors = { QUEUED: '#94a3b8', RINGING: '#f59e0b', IN_PROGRESS: '#10b981', COMPLETED: '#3b82f6', FAILED: '#ef4444', BUSY: '#f59e0b', NO_ANSWER: '#8b5cf6', CANCELLED: '#ef4444' };

  return (
    <div>
      <div className="page-header">
        <div><h2>Call Sessions</h2><p>Create and manage voice broadcast sessions</p></div>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}><Plus size={16} /> New Session</button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: activeSession ? '1fr 1.5fr' : '1fr', gap: '20px' }}>
        {/* Session List */}
        <div className="card">
          <h3 className="section-title"><Radio size={16} /> Sessions</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '600px', overflowY: 'auto' }}>
            {sessions.map(s => (
              <div key={s.id} onClick={() => setActiveSession(s)} style={{
                padding: '14px', borderRadius: 'var(--radius-md)', cursor: 'pointer',
                background: activeSession?.id === s.id ? 'var(--accent-blue-glow)' : 'var(--bg-glass)',
                border: `1px solid ${activeSession?.id === s.id ? 'rgba(59,130,246,0.3)' : 'var(--border-color)'}`,
                transition: 'var(--transition)'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: 600, fontSize: '14px' }}>{s.title}</span>
                  <span className={`badge badge-${s.status?.toLowerCase()}`}><span className="badge-dot" />{s.status}</span>
                </div>
                <div style={{ display: 'flex', gap: '16px', marginTop: '8px', fontSize: '12px', color: 'var(--text-muted)' }}>
                  <span className={`badge badge-${s.mode?.toLowerCase()}`}>{s.mode}</span>
                  <span><Users size={12} /> {s.totalNumbers} numbers</span>
                </div>
                <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                  {s.status === 'CREATED' && <button className="btn btn-success btn-sm" onClick={e => { e.stopPropagation(); handleStart(s.id); }}><Play size={12} /> Start</button>}
                  {s.status === 'ACTIVE' && <button className="btn btn-danger btn-sm" onClick={e => { e.stopPropagation(); handleStop(s.id); }}><Square size={12} /> Stop</button>}
                </div>
              </div>
            ))}
            {sessions.length === 0 && <div className="empty-state" style={{ padding: '30px' }}><Radio size={32} /><h3>No sessions</h3><p>Create a session to start broadcasting</p></div>}
          </div>
        </div>

        {/* Active Session Detail */}
        {activeSession && (
          <div className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <div>
                <h3 style={{ fontSize: '18px', fontWeight: 700 }}>{activeSession.title}</h3>
                <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                  <span className={`badge badge-${activeSession.status?.toLowerCase()}`}><span className="badge-dot" />{activeSession.status}</span>
                  <span className={`badge badge-${activeSession.mode?.toLowerCase()}`}>{activeSession.mode}</span>
                </div>
              </div>
              <button className="btn btn-icon btn-outline btn-sm" onClick={() => setActiveSession(null)}><X size={16} /></button>
            </div>

            {stats && (
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', marginBottom: '8px' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Progress</span>
                  <span style={{ fontWeight: 600 }}>{stats.connected + stats.completed} / {stats.totalNumbers}</span>
                </div>
                <div className="progress-bar"><div className="progress-fill" style={{ width: `${stats.totalNumbers > 0 ? ((stats.connected + stats.completed) / stats.totalNumbers) * 100 : 0}%` }} /></div>

                <div className="stats-grid" style={{ marginTop: '16px', marginBottom: '16px' }}>
                  {[
                    { label: 'Connected', value: stats.connected, color: 'emerald' },
                    { label: 'Ringing', value: stats.ringing, color: 'amber' },
                    { label: 'Failed', value: stats.failed, color: 'red' },
                    { label: 'Completed', value: stats.completed, color: 'blue' },
                  ].map(s => (
                    <div key={s.label} style={{ textAlign: 'center', padding: '12px', background: 'var(--bg-glass)', borderRadius: 'var(--radius-sm)' }}>
                      <p style={{ fontSize: '22px', fontWeight: 800 }}>{s.value}</p>
                      <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{s.label}</p>
                    </div>
                  ))}
                </div>
              </>
            )}

            <h4 className="section-title" style={{ fontSize: '14px' }}>Participants</h4>
            <div className="participant-grid">
              {logs.map(l => (
                <div key={l.id} className={`participant-card status-${l.status?.toLowerCase()}`}>
                  <div className="participant-avatar" style={{ background: `${statusColors[l.status]}20`, color: statusColors[l.status] }}>
                    {l.status === 'IN_PROGRESS' ? <Volume2 size={16} /> : <Phone size={16} />}
                  </div>
                  <div className="participant-info">
                    <div className="phone">{l.phoneNumber?.slice(-4)}</div>
                    <div className="status-text">{l.status}{l.retryCount > 0 ? ` (retry ${l.retryCount})` : ''}</div>
                  </div>
                  {l.status === 'IN_PROGRESS' && activeSession.mode === 'INTERACTIVE' && (
                    <button className="btn btn-icon btn-sm btn-outline" onClick={() => handleMute(l.callSid, !l.muted)}>
                      {l.muted ? <VolumeX size={14} /> : <Volume2 size={14} />}
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Create Modal */}
      {showCreate && (
        <div className="modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header"><h3>New Call Session</h3><button className="btn btn-icon btn-outline btn-sm" onClick={() => setShowCreate(false)}><X size={16} /></button></div>
            <div className="form-group"><label>Session Title *</label><input className="form-input" placeholder="e.g. Morning Broadcast" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} /></div>
            <div className="form-group">
              <label>Mode *</label>
              <select className="form-select" value={form.mode} onChange={e => setForm({ ...form, mode: e.target.value })}>
                <option value="BROADCAST">Broadcast (One-way)</option>
                <option value="INTERACTIVE">Interactive (Moderated)</option>
              </select>
            </div>
            <div className="form-group"><label>Phone Group (optional)</label>
              <select className="form-select" value={form.group} onChange={e => setForm({ ...form, group: e.target.value })}>
                <option value="">All Active Numbers</option>
                {groups.map(g => <option key={g} value={g}>{g}</option>)}
              </select>
            </div>
            <div className="modal-actions"><button className="btn btn-outline" onClick={() => setShowCreate(false)}>Cancel</button><button className="btn btn-primary" onClick={handleCreate} disabled={!form.title}>Create Session</button></div>
          </div>
        </div>
      )}
    </div>
  );
}

