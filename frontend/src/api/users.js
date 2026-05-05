import client from './client'

export const getUsers = () => client.get('/api/users')
export const getUser = (id) => client.get(`/api/users/${id}`)
export const createUser = (data) => client.post('/api/users', data)
export const updateUser = (id, data) => client.put(`/api/users/${id}`, data)
export const deactivateUser = (id) => client.delete(`/api/users/${id}`)
export const getRoles = () => client.get('/api/users/roles')
