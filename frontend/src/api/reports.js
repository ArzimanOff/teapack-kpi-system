import client from './client'

export const getReportByShift = (shiftId) =>
  client.get(`/api/reports/shift/${shiftId}`)

export const getReportsByLine = (lineId) =>
  client.get(`/api/reports/line/${lineId}`)

export const generateReport = (shiftId) =>
  client.post(`/api/reports/generate/${shiftId}`)

export const exportShiftCsv = (shiftId) =>
  client.get(`/api/reports/${shiftId}/csv`, { responseType: 'blob' })