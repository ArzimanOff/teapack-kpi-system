// Базовый URL api-gateway. Все REST/WebSocket-запросы идут через :8080.
// Прямые подключения к доменным сервисам (:8083/:8084/:8086) убраны —
// gateway проксирует REST на /api/**/, и нативный WebSocket на /ws-*/**.
// JWT валидируется на STOMP-уровне (через connectHeaders в STOMP CONNECT-фрейме),
// поэтому не нужен SockJS-fallback и сложный CORS.
export const GATEWAY_URL = 'http://localhost:8080'

// Базовый URL для WebSocket-подключений (через тот же gateway). Берём origin
// от GATEWAY_URL и переключаем схему http→ws/https→wss.
export const WS_GATEWAY_URL = GATEWAY_URL.replace(/^http/i, 'ws')
