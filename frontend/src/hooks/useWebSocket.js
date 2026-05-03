import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export const useWebSocket = (lineId, onMessage) => {
  const clientRef = useRef(null)
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    if (!lineId) return

    const stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8084/ws'),
      onConnect: () => {
        setConnected(true)
        stompClient.subscribe(`/topic/kpi/${lineId}`, message => {
          const data = JSON.parse(message.body)
          onMessage(data)
        })
      },
      onDisconnect: () => setConnected(false),
      reconnectDelay: 5000,
    })

    stompClient.activate()
    clientRef.current = stompClient

    return () => stompClient.deactivate()
  }, [lineId])

  return { connected }
}