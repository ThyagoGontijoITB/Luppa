import { apiRequest, toApiAssetUrl } from './apiClient'
import { userRoles } from '../constants/users'

export const normalizeBackendUser = (user) => ({
  id: user.id?.toString(),
  name: user.name || '',
  email: user.email || '',
  phone: user.phone || '',
  birthDate: user.dateOfBirth || '',
  photoUrl: toApiAssetUrl(user.profileImageUrl || user.profileImagePath, '/uploads/profile'),
  profileImagePath: user.profileImagePath || '',
  role: user.role || 'USER',
  status: user.status || 'ACTIVE',
  emailVerified: Boolean(user.emailVerified),
  createdAt: user.createdAt || '',
  updatedAt: user.updatedAt || '',
})

export const authenticate = async ({ email, password }) => {
  const result = await apiRequest('/api/auth/login', {
    method: 'POST',
    auth: false,
    body: { email, password },
  })

  return {
    user: normalizeBackendUser(result.user),
    token: result.token,
    requiresEmailVerification: Boolean(result.requiresEmailVerification),
    source: 'api',
  }
}

export const registerAccount = async (payload) => {
  const role = payload.accountType === 'creator' ? userRoles.CREATOR : userRoles.USER
  const result = await apiRequest('/api/auth/register', {
    method: 'POST',
    auth: false,
    body: {
      name: payload.fullName,
      email: payload.email,
      dateOfBirth: payload.birthDate,
      phone: payload.phone,
      password: payload.password,
      role,
      termsAccepted: Boolean(payload.termsAccepted),
      marketingEmails: Boolean(payload.marketingEmails),
    },
  })

  return {
    email: result.email,
    codeExpiresInSeconds: result.codeExpiresInSeconds,
    resendCooldownSeconds: result.resendCooldownSeconds,
    requiresEmailVerification: true,
    source: 'api',
  }
}

export const verifyRegistrationCode = ({ email, code }) => apiRequest('/api/auth/register/verify', {
  method: 'POST',
  auth: false,
  body: { email, code },
})

export const resendRegistrationCode = (email) => apiRequest('/api/auth/register/resend', {
  method: 'POST',
  auth: false,
  body: { email },
})

export const fetchCurrentUser = async () => normalizeBackendUser(await apiRequest('/api/auth/me'))

export const updateCurrentUser = async (payload) => {
  const result = await apiRequest('/api/auth/me', {
    method: 'PUT',
    body: {
      name: payload.name.trim(),
      email: payload.email.trim(),
      dateOfBirth: payload.birthDate,
      phone: payload.phone.trim(),
    },
  })
  return {
    user: normalizeBackendUser(result.user),
    token: result.token,
  }
}

export const uploadCurrentUserImage = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return normalizeBackendUser(await apiRequest('/api/auth/me/image', {
    method: 'POST',
    body: formData,
  }))
}
