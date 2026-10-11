export const isEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)

export const onlyDigits = (value) => String(value || '').replace(/\D/g, '')

export const formatBrazilianCep = (value) => {
  const digits = onlyDigits(value).slice(0, 8)
  if (digits.length <= 5) return digits
  return `${digits.slice(0, 5)}-${digits.slice(5)}`
}

export const cepDigits = (value) => onlyDigits(value).slice(0, 8)

export const isBrazilianCep = (value) => cepDigits(value).length === 8

export const formatBrazilianPhone = (value) => {
  const digits = onlyDigits(value).slice(0, 11)
  if (digits.length <= 2) return digits ? `(${digits}` : ''
  if (digits.length <= 6) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`
  if (digits.length <= 10) return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`
}

export const phoneDigits = (value) => onlyDigits(value).slice(0, 11)

export const isPhone = (value) => [10, 11].includes(phoneDigits(value).length)

export const isValidPastDate = (value) => {
  if (!value) return false
  const date = new Date(`${value}T00:00:00`)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return !Number.isNaN(date.getTime()) && date < today
}

export const isValidDate = (value) => {
  if (!value) return false
  const date = new Date(`${value}T00:00:00`)
  return !Number.isNaN(date.getTime())
}

const toDateInputValue = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const todayDateInputValue = () => toDateInputValue(new Date())

export const maxEventStartDateInputValue = () => {
  const date = new Date()
  date.setFullYear(date.getFullYear() + 1)
  return toDateInputValue(date)
}

export const combineDateTime = (date, time) => {
  if (!date || !time) return null
  const value = new Date(`${date}T${time}`)
  return Number.isNaN(value.getTime()) ? null : value
}
