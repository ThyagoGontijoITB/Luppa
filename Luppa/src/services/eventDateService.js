export const parseEventDateTime = (date, time) => {
  if (!date || !time) return null
  const parsed = new Date(`${date}T${time}`)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

export const getEventStart = (event) => parseEventDateTime(event.startDate, event.startTime)

export const getEventEnd = (event) => parseEventDateTime(event.endDate, event.endTime)

export const isEventDateRangeValid = (event) => {
  const start = getEventStart(event)
  const end = getEventEnd(event)
  return Boolean(start && end && end >= start)
}

export const canRateEvent = (event, now = new Date()) => {
  const ended = getEventEnd(event)
  return Boolean(ended && ended <= now)
}

export const formatEventDate = (date) => {
  if (!date) return 'Data inválida'
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(date)
}

export const formatEventTime = (time) => time || 'Horário inválido'

export const formatEventDateTime = (date, time) => {
  const parsed = parseEventDateTime(date, time)
  if (!parsed) return 'Data inválida'

  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsed)
}
