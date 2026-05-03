import client from './client'

export const sendOperatorEvent = (data) =>
  client.post('/api/collect/operator-event', data)

export const startEmulator = (shiftId) =>
  client.post(`/api/emulator/start?shiftId=${shiftId}`)

export const stopEmulator = () =>
  client.post('/api/emulator/stop')

export const getEmulatorState = () =>
  client.get('/api/emulator/state')