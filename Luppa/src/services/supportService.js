import { apiRequest } from './apiClient'

export const sendSupportMessage = (payload, authenticated = false) => apiRequest('/api/support/contact', {
  method: 'POST',
  auth: authenticated,
  body: {
    name: payload.name.trim(),
    email: payload.email.trim(),
    category: payload.category.trim(),
    subject: payload.subject.trim(),
    message: payload.message.trim(),
  },
})
