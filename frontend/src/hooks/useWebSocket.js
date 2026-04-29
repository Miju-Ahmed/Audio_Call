import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs';
import { WS_URL } from '../api/api';

export default function useWebSocket() {
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const [callUpdates, setCallUpdates] = useState([]);
  const [sessionStats, setSessionStats] = useState(null);
  const listenersRef = useRef([]);

  const addListener = useCallback((callback) => {
    listenersRef.current.push(callback);
    return () => {
      listenersRef.current = listenersRef.current.filter(l => l !== callback);
    };
  }, []);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/call-updates', (message) => {
          const update = JSON.parse(message.body);
          setCallUpdates(prev => {
            const idx = prev.findIndex(u => u.callLogId === update.callLogId);
            if (idx >= 0) {
              const next = [...prev];
              next[idx] = update;
              return next;
            }
            return [...prev, update];
          });
          listenersRef.current.forEach(cb => cb(update));
        });
        client.subscribe('/topic/session-stats', (message) => {
          setSessionStats(JSON.parse(message.body));
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers?.message);
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (client.active) client.deactivate();
    };
  }, []);

  const clearUpdates = useCallback(() => setCallUpdates([]), []);

  return { connected, callUpdates, sessionStats, clearUpdates, addListener };
}
