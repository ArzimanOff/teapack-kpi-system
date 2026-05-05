import client from './client'

export const getThresholds = () => client.get('/api/thresholds')
export const updateThresholds = (data) => client.put('/api/thresholds', data)
