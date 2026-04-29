import { useState, useEffect } from 'react';
import { FileText, Download, Search } from 'lucide-react';
import { getSessions, getSessionLogs } from '../api/api';
import toast from 'react-hot-toast';

export default function CallLogs() {
  const [sessions, setSessions] = useState([]);
  const [selectedSession, setSelectedSession] = useState('');
  const [logs, setLogs] = useState([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  useEffect(() => { getSessions().then(r => setSessions(r.data)).catch(() => {}); }, []);

  useEffect(() => {
    if (selectedSession) {
      getSessionLogs(selectedSession).then(r => setLogs(r.data)).catch(() => toast.error('Failed to load logs'));
    } else { setLogs([]); }
  }, [selectedSession]);

  const filtered = logs.filter(l => {
    if (statusFilter && l.status !== statusFilter) return false;
    if (search && !l.phoneNumber?.includes(search)) return false;
    return true;
  });

  const statusCounts = logs.reduce((acc, l) => { acc[l.status] = (acc[l.status] || 0) + 1; return acc; }, {});

  const exportCsv = () => {
    const headers = 'Phone Number,Status,Retry Count,Duration (s),Initiated At,Answered At,Ended At,Failure Reason\n';
    const rows = filtered.map(l =>
      `${l.phoneNumber},${l.status},${l.retryCount},${l.durationSeconds || ''},${l.initiatedAt || ''},${l.answeredAt || ''},${l.endedAt || ''},"${l.failureReason || ''}"`
    ).join('\n');
    const blob = new Blob([headers + rows], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = `call-logs-${selectedSession}.csv`; a.click();
    URL.revokeObjectURL(url);
  };

  const formatTime = (ts) => ts ? new Date(ts).toLocaleTimeString() : '—';

  return (
    <div>
      <div className="page-header">
        <div><h2>Call Logs</h2><p>Detailed logs and reports for all call sessions</p></div>
        {filtered.length > 0 && (
          <button className="btn btn-outline" onClick={exportCsv}><Download size={16} /> Export CSV</button>
        )}
      </div>

      <div className="toolbar">
        <select className="form-select" style={{ width: 'auto', minWidth: '200px' }} value={selectedSession} onChange={e => setSelectedSession(e.target.value)}>
          <option value="">Select a session...</option>
          {sessions.map(s => <option key={s.id} value={s.id}>{s.title} ({s.status})</option>)}
        </select>
        <div className="search-wrapper">
          <Search size={16} />
          <input className="search-input" placeholder="Search phone number..." value={search} onChange={e => setSearch(e.target.value)} style={{ paddingLeft: '40px' }} />
        </div>
        <select className="form-select" style={{ width: 'auto', minWidth: '130px' }} value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
          <option value="">All Status</option>
          {['QUEUED','RINGING','IN_PROGRESS','COMPLETED','FAILED','BUSY','NO_ANSWER','CANCELLED'].map(s => (
            <option key={s} value={s}>{s} {statusCounts[s] ? `(${statusCounts[s]})` : ''}</option>
          ))}
        </select>
      </div>

      {selectedSession && logs.length > 0 && (
        <div className="stats-grid" style={{ marginBottom: '20px' }}>
          {Object.entries(statusCounts).map(([status, count]) => (
            <div key={status} style={{ textAlign: 'center', padding: '14px', background: 'var(--bg-glass)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
              <p style={{ fontSize: '24px', fontWeight: 800 }}>{count}</p>
              <span className={`badge badge-${status.toLowerCase()}`}><span className="badge-dot" />{status}</span>
            </div>
          ))}
        </div>
      )}

      <div className="table-wrapper">
        <table>
          <thead><tr><th>Phone Number</th><th>Status</th><th>Retries</th><th>Duration</th><th>Initiated</th><th>Answered</th><th>Ended</th><th>Failure Reason</th></tr></thead>
          <tbody>
            {filtered.map(l => (
              <tr key={l.id}>
                <td style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--text-primary)' }}>{l.phoneNumber}</td>
                <td><span className={`badge badge-${l.status?.toLowerCase()}`}><span className="badge-dot" />{l.status}</span></td>
                <td>{l.retryCount}</td>
                <td>{l.durationSeconds ? `${l.durationSeconds}s` : '—'}</td>
                <td>{formatTime(l.initiatedAt)}</td>
                <td>{formatTime(l.answeredAt)}</td>
                <td>{formatTime(l.endedAt)}</td>
                <td style={{ fontSize: '12px', color: 'var(--accent-red)', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }}>{l.failureReason || '—'}</td>
              </tr>
            ))}
            {!selectedSession && (
              <tr><td colSpan={8}><div className="empty-state"><FileText size={40} /><h3>Select a session</h3><p>Choose a session from the dropdown to view its call logs.</p></div></td></tr>
            )}
            {selectedSession && filtered.length === 0 && (
              <tr><td colSpan={8}><div className="empty-state"><FileText size={40} /><h3>No logs found</h3><p>No call logs match your current filters.</p></div></td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
