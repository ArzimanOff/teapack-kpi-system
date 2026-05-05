export const getToken = () => localStorage.getItem('token')
export const getRefreshToken = () => localStorage.getItem('refreshToken')
export const getUser = () => JSON.parse(localStorage.getItem('user') || '{}')
export const getRole = () => localStorage.getItem('role')

export const setAuth = (token, username, role, refreshToken) => {
  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify({ username }))
  localStorage.setItem('role', role)
  if (refreshToken) localStorage.setItem('refreshToken', refreshToken)
}

export const setAccessToken = (token) => {
  localStorage.setItem('token', token)
}

export const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  localStorage.removeItem('role')
}

export const isAuthenticated = () => !!getToken()

// ROLE_ADMIN всегда имеет доступ ко всему
export const hasRole = (allowed) => {
  const role = getRole()
  if (!role) return false
  if (role === 'ROLE_ADMIN') return true
  if (!allowed || allowed.length === 0) return true
  return allowed.includes(role)
}
