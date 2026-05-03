export const getToken = () => localStorage.getItem('token')
export const getUser = () => JSON.parse(localStorage.getItem('user') || '{}')
export const getRole = () => localStorage.getItem('role')

export const setAuth = (token, username, role) => {
  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify({ username }))
  localStorage.setItem('role', role)
}

export const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('role')
}

export const isAuthenticated = () => !!getToken()