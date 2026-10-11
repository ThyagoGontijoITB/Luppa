import { apiRequest } from './apiClient'

export const requestPasswordReset = (email) => apiRequest('/api/auth/forgot-password', {
  method: 'POST',
  auth: false,
  body: { email },
})

export const resetPassword = ({ token, password }) => apiRequest('/api/auth/reset-password', {
  method: 'POST',
  auth: false,
  body: { token, password },
})

export const verifyEmail = (token) => apiRequest('/api/auth/verify-email', {
  auth: false,
  query: { token },
})

export const resendVerification = (email) => apiRequest('/api/auth/resend-verification', {
  method: 'POST',
  auth: false,
  body: { email },
})

export const getCommunicationPreferences = () => apiRequest('/api/me/communication-preferences')

export const updateCommunicationPreferences = (preferences) => apiRequest('/api/me/communication-preferences', {
  method: 'PUT',
  body: preferences,
})
