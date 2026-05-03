import client from './client'

export const login = (username, password) =>
  client.post('/api/auth/login', { username, password })

export const register = (data) =>
  client.post('/api/auth/register', data)