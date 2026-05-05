import client from './client'

export const getKpiByShift = (shiftId) =>
  client.get(`/api/kpi/shift/${shiftId}`)

export const getKpiByLine = (lineId) =>
  client.get(`/api/kpi/line/${lineId}`)

// params: { dateFrom, dateTo, limit }
export const getLineSummary = (lineId, params = {}) =>
  client.get(`/api/kpi/line/${lineId}/summary`, { params })

// params: { lineId, dateFrom, dateTo, oeeMin, availabilityMin, performanceMin, qualityMin }
export const getKpiHistory = (params) =>
  client.get('/api/kpi/history', { params })
