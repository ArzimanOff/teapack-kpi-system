import client from './client'

export const getUnreadNotifications = () =>
  client.get('/api/notifications/unread')

export const getNotificationsByShift = (shiftId) =>
  client.get(`/api/notifications/shift/${shiftId}`)

export const markAsRead = (id) =>
  client.patch(`/api/notifications/${id}/read`)

export const markAllRead = () =>
  client.patch('/api/notifications/read-all')

export const getUnreadCount = () =>
  client.get('/api/notifications/unread/count')