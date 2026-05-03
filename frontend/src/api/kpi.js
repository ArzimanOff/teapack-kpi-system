import client from './client'

export const getKpiByShift = (shiftId) =>
  client.get(`/api/kpi/shift/${shiftId}`)

export const getKpiByLine = (lineId) =>
  client.get(`/api/kpi/line/${lineId}`)