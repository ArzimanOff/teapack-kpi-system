import client from './client'

export const sendOperatorEvent = (data) =>
  client.post('/api/collect/operator-event', data)

export const startEmulator = (shiftId, lineId) => {
  const params = new URLSearchParams({ shiftId })
  if (lineId) params.append('lineId', lineId)
  return client.post(`/api/emulator/start?${params.toString()}`)
}

export const stopEmulator = () =>
  client.post('/api/emulator/stop')

export const getEmulatorState = () =>
  client.get('/api/emulator/state')