import { userRoles, userRoleLabels, userStatuses } from '../constants/users'
import { apiPageContent, apiRequest } from './apiClient'
import { normalizeBackendUser } from './authService'

const normalizeText = (value) => String(value || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')

export const fetchUsers = async (filters = {}) => apiPageContent(await apiRequest('/api/users', {
  query: {
    search: filters.query,
    role: filters.role,
    status: filters.status,
    page: 0,
    size: 200,
  },
})).map(normalizeBackendUser)

export const searchUsers = (users, { query = '', role = 'all', status = 'all', sort = 'name' }) => {
  const term = normalizeText(query.trim())
  const filtered = (Array.isArray(users) ? users : []).filter((user) => {
    if (!user) return false
    const matchesText = !term || [user.name, user.email].some((value) => normalizeText(value).includes(term))
    const matchesRole = role === 'all' || user.role === role
    const matchesStatus = status === 'all' || user.status === status

    return matchesText && matchesRole && matchesStatus
  })

  return [...filtered].sort((first, second) => {
    if (sort === 'newest') return new Date(second.createdAt) - new Date(first.createdAt)
    if (sort === 'oldest') return new Date(first.createdAt) - new Date(second.createdAt)
    return normalizeText(first.name).localeCompare(normalizeText(second.name), 'pt-BR')
  })
}

export const createUser = async (_users, payload) => normalizeBackendUser(await apiRequest('/api/users', {
  method: 'POST',
  body: {
    name: payload.name.trim(),
    email: payload.email.trim(),
    phone: payload.phone.trim(),
    dateOfBirth: payload.birthDate,
    password: payload.password,
    role: payload.role,
    status: payload.status,
  },
}))

export const updateUser = async (_users, userId, payload) => normalizeBackendUser(await apiRequest(`/api/users/${userId}`, {
  method: 'PUT',
  body: {
    name: payload.name.trim(),
    email: payload.email.trim(),
    phone: payload.phone.trim(),
    dateOfBirth: payload.birthDate,
    role: payload.role,
    status: payload.status,
  },
}))

export const deleteUser = async (_users, userId) => {
  await apiRequest(`/api/users/${userId}`, { method: 'DELETE' })
  return userId
}

export const sendAdminUserMessage = async (userId, payload) => apiRequest(`/api/admin/users/${userId}/message`, {
  method: 'POST',
  body: {
    subject: payload.subject.trim(),
    message: payload.message.trim(),
  },
})

export const getUserRoleOptions = (currentUserRole) => Object.values(userRoles)
  .filter((role) => role !== userRoles.LUPPA_ADMIN)
  .filter((role) => currentUserRole === userRoles.LUPPA_ADMIN || role !== userRoles.ADMIN)
  .map((role) => ({ value: role, label: userRoleLabels[role] || role }))

export const getUserRoleLabel = (role) => userRoleLabels[role] || role

export const getUserStatusOptions = () => Object.values(userStatuses)
