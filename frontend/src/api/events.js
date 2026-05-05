import client from './client'

export const sendOperatorEvent = (data) =>
  client.post('/api/collect/operator-event', data)

export const startEmulator = (shiftId, lineId) => {
  const params = new URLSearchParams({ shiftId })
  if (lineId) params.append('lineId', lineId)
  return client.post(`/api/emulator/start?${params.toString()}`)
}

export const stopEmulator = (shiftId) => {
  const url = shiftId
    ? `/api/emulator/stop?shiftId=${shiftId}`
    : '/api/emulator/stop'
  return client.post(url)
}

export const removeEmulatorRun = (shiftId) =>
  client.delete(`/api/emulator/${shiftId}`)

export const applyEmulatorScenario = (shiftId, scenario, ticks = 5) =>
  client.post(`/api/emulator/${shiftId}/scenario`,
    null, { params: { scenario, ticks } })

export const getEmulatorRuns = () =>
  client.get('/api/emulator/runs')

export const getEmulatorState = () =>
  client.get('/api/emulator/state')