import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getToken } from '../utils/auth'

// Подписка на push-уведомления от notification-service.
// onCreated(list)  — приходит после создания (массив Notification)
// onAllRead()      — приходит после mark-all-read
export const useNotificationSocket = (onCreated, onAllRead) => {
  const [connected, setConnected] = useState(false)
  const createdRef = useRef(onCreated)
  const allReadRef = useRef(onAllRead)

  useEffect(() => { createdRef.current = onCreated }, [onCreated])
  useEffect(() => { allReadRef.current = onAllRead }, [onAllRead])

  useEffect(() => {
    const token = getToken()
    if (!token) {
      setConnected(false)
      return
    }
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8086/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        setConnected(true)
        client.subscribe('/topic/notifications', message => {
          try {
            const data = JSON.parse(message.body)
            createdRef.current && createdRef.current(Array.isArray(data) ? data : [data])
          } catch (e) {}
        })
        client.subscribe('/topic/notifications/read-all', () => {
          allReadRef.current && allReadRef.current()
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
  }, [])

  return { connected }
}
