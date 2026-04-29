import { useState, useEffect, useRef } from 'react';
import { Phone, Plus, Upload, Search, Trash2, Edit, X, Check } from 'lucide-react';
import { getPhoneNumbers, getPhoneGroups, addPhoneNumber, updatePhoneNumber, deletePhoneNumber, deletePhoneNumbers, uploadPhoneNumbers } from '../api/api';
import toast from 'react-hot-toast';

export default function PhoneNumbers() {
  const [numbers, setNumbers] = useState([]);
  const [groups, setGroups] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [groupFilter, setGroupFilter] = useState('');
  const [selected, setSelected] = useState([]);
  const [showAdd, setShowAdd] = useState(false);
  const [showUpload, setShowUpload] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState({ number: '', name: '', group: '' });
  const fileRef = useRef();

  const loadNumbers = () => {
    getPhoneNumbers({ page, size: 20, search: search || null, group: groupFilter || null })
      .then(res => { setNumbers(res.data.content); setTotalPages(res.data.totalPages); })
      .catch(() => toast.error('Failed to load numbers'));
  };

  useEffect(() => { loadNumbers(); }, [page, search, groupFilter]);
  useEffect(() => { getPhoneGroups().then(res => setGroups(res.data)).catch(() => {}); }, []);

  const handleAdd = async () => {
    try {
      await addPhoneNumber(form);
      toast.success('Number added'); setShowAdd(false); setForm({ number: '', name: '', group: '' }); loadNumbers();
    } catch (e) { toast.error(e.response?.data?.message || 'Failed to add'); }
  };

  const handleUpdate = async (id) => {
    try {
      await updatePhoneNumber(id, form);
      toast.success('Updated'); setEditId(null); loadNumbers();
    } catch (e) { toast.error('Failed to update'); }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this number?')) return;
    try { await deletePhoneNumber(id); toast.success('Deleted'); loadNumbers(); }
    catch { toast.error('Failed to delete'); }
  };

  const handleBulkDelete = async () => {
    if (!confirm(`Delete ${selected.length} numbers?`)) return;
    try { await deletePhoneNumbers(selected); toast.success('Deleted'); setSelected([]); loadNumbers(); }
    catch { toast.error('Failed'); }
  };

  const handleUpload = async (file) => {
    try {
      const res = await uploadPhoneNumbers(file, form.group || null);
      toast.success(`Added ${res.data.added} numbers`);
      setShowUpload(false); loadNumbers();
    } catch { toast.error('Upload failed'); }
  };

  const toggleAll = () => {
    setSelected(selected.length === numbers.length ? [] : numbers.map(n => n.id));
  };

  return (
    <div>
      <div className="page-header">
        <div><h2>Phone Numbers</h2><p>Manage phone numbers for voice broadcast</p></div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn btn-outline" onClick={() => setShowUpload(true)}><Upload size={16} /> Upload CSV</button>
          <button className="btn btn-primary" onClick={() => setShowAdd(true)}><Plus size={16} /> Add Number</button>
        </div>
      </div>

      <div className="toolbar">
        <div className="search-wrapper">
          <Search size={16} />
          <input className="search-input" placeholder="Search numbers or names..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} style={{ paddingLeft: '40px' }} />
        </div>
        <select className="form-select" style={{ width: 'auto', minWidth: '150px' }} value={groupFilter} onChange={e => { setGroupFilter(e.target.value); setPage(0); }}>
          <option value="">All Groups</option>
          {groups.map(g => <option key={g} value={g}>{g}</option>)}
        </select>
        {selected.length > 0 && (
          <button className="btn btn-danger btn-sm" onClick={handleBulkDelete}><Trash2 size={14} /> Delete ({selected.length})</button>
        )}
      </div>

      <div className="table-wrapper">
        <table>
          <thead>
            <tr>
              <th><input type="checkbox" className="checkbox" checked={selected.length === numbers.length && numbers.length > 0} onChange={toggleAll} /></th>
              <th>Phone Number</th><th>Name</th><th>Group</th><th>Status</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {numbers.map(n => (
              <tr key={n.id}>
                <td><input type="checkbox" className="checkbox" checked={selected.includes(n.id)} onChange={() => setSelected(prev => prev.includes(n.id) ? prev.filter(x => x !== n.id) : [...prev, n.id])} /></td>
                <td style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--text-primary)' }}>{n.number}</td>
                <td>{editId === n.id ? <input className="form-input" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} style={{ padding: '4px 8px', width: '120px' }} /> : (n.name || '—')}</td>
                <td>{editId === n.id ? <input className="form-input" value={form.group} onChange={e => setForm({ ...form, group: e.target.value })} style={{ padding: '4px 8px', width: '100px' }} /> : (n.group || '—')}</td>
                <td><span className={`badge ${n.active ? 'badge-active' : 'badge-failed'}`}><span className="badge-dot" />{n.active ? 'Active' : 'Inactive'}</span></td>
                <td>
                  <div style={{ display: 'flex', gap: '6px' }}>
                    {editId === n.id ? (
                      <>
                        <button className="btn btn-icon btn-success btn-sm" onClick={() => handleUpdate(n.id)}><Check size={14} /></button>
                        <button className="btn btn-icon btn-outline btn-sm" onClick={() => setEditId(null)}><X size={14} /></button>
                      </>
                    ) : (
                      <>
                        <button className="btn btn-icon btn-outline btn-sm" onClick={() => { setEditId(n.id); setForm({ name: n.name || '', group: n.group || '' }); }}><Edit size={14} /></button>
                        <button className="btn btn-icon btn-outline btn-sm" onClick={() => handleDelete(n.id)}><Trash2 size={14} /></button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {numbers.length === 0 && (
              <tr><td colSpan={6}>
                <div className="empty-state"><Phone size={40} /><h3>No phone numbers yet</h3><p>Add numbers manually or upload a CSV file to get started.</p></div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</button>
          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Page {page + 1} of {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
        </div>
      )}

      {/* Add Modal */}
      {showAdd && (
        <div className="modal-overlay" onClick={() => setShowAdd(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header"><h3>Add Phone Number</h3><button className="btn btn-icon btn-outline btn-sm" onClick={() => setShowAdd(false)}><X size={16} /></button></div>
            <div className="form-group"><label>Phone Number *</label><input className="form-input" placeholder="+880 1XXXXXXXXX" value={form.number} onChange={e => setForm({ ...form, number: e.target.value })} /></div>
            <div className="form-group"><label>Contact Name</label><input className="form-input" placeholder="Optional" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></div>
            <div className="form-group"><label>Group</label><input className="form-input" placeholder="e.g. Team A" value={form.group} onChange={e => setForm({ ...form, group: e.target.value })} /></div>
            <div className="modal-actions"><button className="btn btn-outline" onClick={() => setShowAdd(false)}>Cancel</button><button className="btn btn-primary" onClick={handleAdd}>Add Number</button></div>
          </div>
        </div>
      )}

      {/* Upload Modal */}
      {showUpload && (
        <div className="modal-overlay" onClick={() => setShowUpload(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header"><h3>Upload CSV</h3><button className="btn btn-icon btn-outline btn-sm" onClick={() => setShowUpload(false)}><X size={16} /></button></div>
            <div className="upload-area" onClick={() => fileRef.current?.click()} onDragOver={e => { e.preventDefault(); e.currentTarget.classList.add('drag-over'); }} onDragLeave={e => e.currentTarget.classList.remove('drag-over')} onDrop={e => { e.preventDefault(); e.currentTarget.classList.remove('drag-over'); handleUpload(e.dataTransfer.files[0]); }}>
              <Upload size={32} style={{ color: 'var(--accent-blue)' }} />
              <p>Drag & drop CSV file or click to browse</p>
              <p style={{ fontSize: '12px' }}>Format: number, name (optional), group (optional)</p>
            </div>
            <input ref={fileRef} type="file" accept=".csv" hidden onChange={e => e.target.files[0] && handleUpload(e.target.files[0])} />
            <div className="form-group" style={{ marginTop: '16px' }}><label>Default Group</label><input className="form-input" placeholder="Assign group to all" value={form.group} onChange={e => setForm({ ...form, group: e.target.value })} /></div>
          </div>
        </div>
      )}
    </div>
  );
}
