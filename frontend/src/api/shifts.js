import client from './client'

export const createShift = (data) =>
  client.post('/api/shifts', data)

export const startShift = (shiftId) =>
  client.post(`/api/shifts/${shiftId}/start`)

export const closeShift = (shiftId) =>
  client.post(`/api/shifts/${shiftId}/close`)

export const cancelShift = (shiftId) =>
  client.delete(`/api/shifts/${shiftId}`)

export const getShift = (shiftId) =>
  client.get(`/api/shifts/${shiftId}`)

export const getShiftAggregate = (shiftId) =>
  client.get(`/api/shifts/${shiftId}/aggregate`)

export const getShiftsByLine = (lineId) =>
  client.get(`/api/shifts/line/${lineId}`)

// params: { status, lineId, operatorId, dateFrom, dateTo }
export const findShifts = (params) =>
  client.get('/api/shifts', { params })
