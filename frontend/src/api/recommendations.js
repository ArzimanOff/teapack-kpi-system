import client from './client'

// params: { role, lineId, days }
export const getRecommendations = (params = {}) =>
  client.get('/api/recommendations', { params })
