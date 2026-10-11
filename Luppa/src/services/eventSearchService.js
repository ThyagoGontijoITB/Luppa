import { getEventStart } from './eventDateService'

const normalizeText = (value) => value.toString().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')

export const searchAndSortEvents = (events, { query = '', category = 'all', sort = 'nearest' }) => {
  const term = normalizeText(query.trim())
  const filtered = events.filter((event) => {
    const matchesText = !term || [
      event.title,
      event.locationName,
      event.address,
      event.categoryName,
      event.categorySlug,
    ].some((value) => normalizeText(value || '').includes(term))
    const matchesCategory = category === 'all' || event.categorySlug === category

    return matchesText && matchesCategory
  })

  return [...filtered].sort((first, second) => {
    if (sort === 'az') return first.title.localeCompare(second.title, 'pt-BR')
    if (sort === 'za') return second.title.localeCompare(first.title, 'pt-BR')
    if (sort === 'furthest') return getEventStart(second) - getEventStart(first)
    return getEventStart(first) - getEventStart(second)
  })
}
