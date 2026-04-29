import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: `${API_BASE}/api`,
  headers: { 'Content-Type': 'application/json' },
});

// ── Dashboard ──
export const getDashboardStats = () => api.get('/dashboard/stats');

// ── Phone Numbers ──
export const getPhoneNumbers = (params) => api.get('/phone-numbers', { params });
export const getPhoneGroups = () => api.get('/phone-numbers/groups');
export const addPhoneNumber = (data) => api.post('/phone-numbers', data);
export const updatePhoneNumber = (id, data) => api.put(`/phone-numbers/${id}`, data);
export const deletePhoneNumber = (id) => api.delete(`/phone-numbers/${id}`);
export const deletePhoneNumbers = (ids) => api.delete('/phone-numbers/bulk', { data: { ids } });
export const uploadPhoneNumbers = (file, group) => {
  const formData = new FormData();
  formData.append('file', file);
  if (group) formData.append('group', group);
  return api.post('/phone-numbers/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

// ── Sessions ──
export const getSessions = () => api.get('/sessions');
export const getSession = (id) => api.get(`/sessions/${id}`);
export const createSession = (data) => api.post('/sessions', data);
export const startSession = (id) => api.post(`/sessions/${id}/start`);
export const stopSession = (id) => api.post(`/sessions/${id}/stop`);
export const getSessionStats = (id) => api.get(`/sessions/${id}/stats`);
export const getSessionLogs = (id) => api.get(`/sessions/${id}/logs`);
export const muteParticipant = (sessionId, callSid) => api.post(`/sessions/${sessionId}/mute/${callSid}`);
export const unmuteParticipant = (sessionId, callSid) => api.post(`/sessions/${sessionId}/unmute/${callSid}`);

export const WS_URL = `${API_BASE}/ws`;
export default api;
