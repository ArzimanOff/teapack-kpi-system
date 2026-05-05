import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getToken } from '../utils/auth'

// Подписка на live-агрегаты смены: data-processing-service публикует
// ShiftDataDto в /topic/aggregate/{shiftId} при каждом обновлении.
export const useAggregateSocket = (shiftId, onMessage) => {
  const [connected, setConnected] = useState(false)
  const handlerRef = useRef(onMessage)

  useEffect(() => { handlerRef.current = onMessage }, [onMessage])

  useEffect(() => {
    if (!shiftId) {
      setConnected(false)
      return
    }
    const token = getToken()
    if (!token) {
      setConnected(false)
      return
    }

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8083/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        setConnected(true)
        client.subscribe(`/topic/aggregate/${shiftId}`, message => {
          try {
            const data = JSON.parse(message.body)
            handlerRef.current && handlerRef.current(data)
          } catch (e) {}
        })
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
      reconnectDelay: 5000,
    })

    client.activate()
    return () => {
      setConnected(false)
      client.deactivate()
    }
  }, [shiftId])

  return { connected }
}
