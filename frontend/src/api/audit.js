import client from './client'

export const findAuditEvents = (params) =>
  client.get('/api/audit', { params })
