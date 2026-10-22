import { resetRequestCache } from '../services/requestCache'
import { useEffect, useState } from 'react'
import { authenticate, fetchCurrentUser, registerAccount } from '../services/authService'
import { hasPermission, hasRole } from '../services/permissionService'
import { AuthContext } from './authContext'

const getInitialToken = () => {
  try {
    return window.localStorage.getItem('luppa.auth.token') || null
  } catch {
    return null
  }
}

const clearStoredAuth = () => {
  resetRequestCache()
  window.localStorage.removeItem('luppa.auth.user')
  window.localStorage.removeItem('luppa.auth.token')
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(getInitialToken)
  const [loading, setLoading] = useState(Boolean(token))

  useEffect(() => {
    if (!token) {
      setUser(null)
      setLoading(false)
      clearStoredAuth()
      return undefined
    }
    let active = true
    setLoading(true)
    fetchCurrentUser()
      .then((currentUser) => {
        if (!active) return
        setUser(currentUser)
        window.localStorage.setItem('luppa.auth.user', JSON.stringify(currentUser))
      })
      .catch(() => {
        if (!active) return
        setUser(null)
        setToken(null)
        clearStoredAuth()
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [token])

  useEffect(() => {
    const onStorage = (event) => {
      if (event.key !== 'luppa.auth.token' && event.key !== null) return
      resetRequestCache()
      setUser(null)
      setToken(getInitialToken())
      setLoading(Boolean(getInitialToken()))
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  const login = async (credentials) => {
    setLoading(true)
    try {
      const result = await authenticate(credentials)
      if (!result.token) throw new Error('missing_auth_token')
      resetRequestCache()
      setUser(result.user)
      setToken(result.token)
      window.localStorage.setItem('luppa.auth.user', JSON.stringify(result.user))
      window.localStorage.setItem('luppa.auth.token', result.token)
      return result.user
    } finally {
      setLoading(false)
    }
  }

  const register = async (payload) => {
    setLoading(true)
    try {
      const result = await registerAccount(payload)
      if (result.requiresEmailVerification) return result
      if (!result.token) throw new Error('missing_auth_token')
      resetRequestCache()
      setUser(result.user)
      setToken(result.token)
      window.localStorage.setItem('luppa.auth.user', JSON.stringify(result.user))
      window.localStorage.setItem('luppa.auth.token', result.token)
      return result.user
    } finally {
      setLoading(false)
    }
  }

  const logout = () => {
    setUser(null)
    setToken(null)
    clearStoredAuth()
  }

  const setAuthenticatedUser = (currentUser) => {
    resetRequestCache()
    setUser(currentUser)
    window.localStorage.setItem('luppa.auth.user', JSON.stringify(currentUser))
  }

  const setAuthenticatedSession = (currentUser, currentToken) => {
    resetRequestCache()
    setUser(currentUser)
    setToken(currentToken)
    window.localStorage.setItem('luppa.auth.user', JSON.stringify(currentUser))
    window.localStorage.setItem('luppa.auth.token', currentToken)
  }

  const value = {
    isAuthenticated: Boolean(user),
    user,
    token,
    userId: user?.id || null,
    name: user?.name || '',
    email: user?.email || '',
    role: user?.role || null,
    loading,
    login,
    register,
    logout,
    setAuthenticatedUser,
    setAuthenticatedSession,
    hasPermission: (permission) => hasPermission(user, permission),
    hasRole: (roles) => hasRole(user, roles),
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
