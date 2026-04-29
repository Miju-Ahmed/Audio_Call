import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import PhoneNumbers from './pages/PhoneNumbers';
import Sessions from './pages/Sessions';
import CallLogs from './pages/CallLogs';
import useWebSocket from './hooks/useWebSocket';

export default function App() {
  const { connected, callUpdates, sessionStats, clearUpdates } = useWebSocket();

  return (
    <BrowserRouter>
      <Toaster position="top-right" toastOptions={{
        style: { background: '#1e293b', color: '#f1f5f9', border: '1px solid rgba(255,255,255,0.06)', fontSize: '14px' },
        success: { iconTheme: { primary: '#10b981', secondary: '#fff' } },
        error: { iconTheme: { primary: '#ef4444', secondary: '#fff' } },
      }} />
      <div className="app-layout">
        <Sidebar wsConnected={connected} />
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Dashboard callUpdates={callUpdates} wsConnected={connected} />} />
            <Route path="/phone-numbers" element={<PhoneNumbers />} />
            <Route path="/sessions" element={<Sessions callUpdates={callUpdates} wsConnected={connected} />} />
            <Route path="/logs" element={<CallLogs />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
