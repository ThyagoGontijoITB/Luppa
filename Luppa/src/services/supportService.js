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

export const supportErrorMessage = (error) => {
  if (error.status === 429) return 'Muitas tentativas. Aguarde antes de enviar novamente.'
  if (error.status === 400) return 'Confira os dados informados e tente novamente.'
  return 'Não foi possível enviar sua mensagem. Tente novamente mais tarde.'
}
