import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { getToken } from '../utils/auth'
import { WS_GATEWAY_URL } from '../api/config'

// STOMP-over-нативный-WebSocket (без SockJS): api-gateway прозрачно
// проксирует ws-upgrade. JWT валидируется на STOMP CONNECT-фрейме.
export const useWebSocket = (lineId, onMessage) => {
  const [connected, setConnected] = useState(false)
  const handlerRef = useRef(onMessage)

  useEffect(() => { handlerRef.current = onMessage }, [onMessage])

  useEffect(() => {
    if (!lineId) {
      setConnected(false)
      return
    }

    const token = getToken()
    if (!token) {
      setConnected(false)
      return
    }

    const stompClient = new Client({
      brokerURL: `${WS_GATEWAY_URL}/ws-kpi`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        setConnected(true)
        stompClient.subscribe(`/topic/kpi/${lineId}`, message => {
          try {
            const data = JSON.parse(message.body)
            handlerRef.current && handlerRef.current(data)
          } catch (e) {}
        })
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: (frame) => {
        setConnected(false)
        console.warn('STOMP error:', frame.headers?.message || frame.body)
      },
      reconnectDelay: 5000,
    })

    stompClient.activate()
    return () => {
      setConnected(false)
      stompClient.deactivate()
    }
  }, [lineId])

  return { connected }
}
