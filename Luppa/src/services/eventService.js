import { eventStatusLabels } from '../constants/events'
import { formatEventDateTime } from './eventDateService'
import { searchAndSortEvents } from './eventSearchService'
import { apiPageContent, apiRequest, hasApiSession, toApiAssetUrl } from './apiClient'
import { normalizeBackendUser } from './authService'
import { buildStructuredAddress, formatStructuredAddress } from './eventLocationService'

const publicStatuses = ['published']
const pageAll = { page: 0, size: 200 }

const statusFromApi = (status) => {
  const value = status?.toLowerCase?.() || 'draft'
  if (value === 'review') return 'pending_review'
  return value
}

const statusToApi = (status) => {
  if (status === 'pending_review') return 'REVIEW'
  return status?.toUpperCase?.() || 'DRAFT'
}

const splitDateTime = (value) => {
  if (!value) return { date: '', time: '' }
  const [date, time = ''] = value.split('T')
  return { date, time: time.slice(0, 5) }
}

const joinDateTime = (date, time) => `${date}T${time || '00:00'}:00`
const limit = (value, maxLength) => (value || '').slice(0, maxLength)

const normalizeEventAddress = (event) => buildStructuredAddress(event.address || {})

const normalizeCategory = (category, count = 0) => ({
  id: category.slug || category.id?.toString(),
  slug: category.slug,
  title: category.name,
  description: category.description,
  icon: category.slug || 'all',
  count,
})

const normalizeSocials = (links = []) => links.reduce((socials, link) => ({
  ...socials,
  [link.platform?.toLowerCase?.() || 'website']: link.url,
}), {})

export const normalizeEvent = (event, extras = {}) => {
  const start = splitDateTime(event.startDateTime)
  const end = splitDateTime(event.endDateTime)
  const status = statusFromApi(event.status)
  const imageUrl = toApiAssetUrl(event.coverImage?.url || event.coverImage?.storageKey || event.coverImage?.path, '/uploads/events')
  const addressFields = normalizeEventAddress(event)
  const addressText = formatStructuredAddress(addressFields) || event.location

  return {
    id: event.id?.toString(),
    title: event.title,
    description: event.description,
    categoryId: event.category?.slug || event.category?.id?.toString(),
    categorySlug: event.category?.slug,
    categoryName: event.category?.name,
    address: addressText,
    addressFields,
    locationName: addressText,
    latitude: event.latitude === null || event.latitude === undefined ? null : Number(event.latitude),
    longitude: event.longitude === null || event.longitude === undefined ? null : Number(event.longitude),
    startDateTime: event.startDateTime,
    endDateTime: event.endDateTime,
    startDate: start.date,
    startTime: start.time,
    endDate: end.date,
    endTime: end.time,
    coverImageUrl: imageUrl,
    creatorUserId: event.creator?.id?.toString(),
    creatorName: event.creator?.name,
    creator: event.creator ? normalizeBackendUser(event.creator) : null,
    status,
    createdAt: event.createdAt,
    updatedAt: event.updatedAt,
    interestCount: Number(event.interestCount ?? 0),
    participants: Number(event.interestCount ?? 0),
    destaquePrincipal: Boolean(event.destaquePrincipal),
    featured: Boolean(event.featured ?? event.destaquePrincipal),
    organizerSocials: normalizeSocials(extras.socialLinks),
    eventRating: extras.eventRating || null,
    reviews: extras.reviews || [],
    userReview: extras.userReview || null,
    saved: extras.saved || false,
    savedAt: extras.savedAt,
  }
}

const averageRating = (ratings) => {
  if (!ratings.length) return null
  const total = ratings.reduce((sum, rating) => sum + rating.score, 0)
  return { rating: Math.round((total / ratings.length) * 10) / 10, reviewCount: ratings.length }
}

const normalizeRating = (rating) => ({
  id: rating.id?.toString(),
  eventId: rating.eventId?.toString(),
  score: rating.score,
  description: rating.description || '',
  ratedAt: rating.ratedAt,
  author: {
    name: rating.user?.name || 'Usuário',
    photoUrl: toApiAssetUrl(rating.user?.profileImageUrl, '/uploads/profile'),
  },
})

export const loadEventDetails = async (eventOrId) => {
  const id = typeof eventOrId === 'object' ? eventOrId.id : eventOrId
  const [event, ratings, socialLinks] = await Promise.all([
    apiRequest(`/api/events/${id}`, { auth: false }),
    apiRequest(`/api/events/${id}/ratings`, { auth: false }),
    apiRequest(`/api/events/${id}/social-links`, { auth: false }),
  ])

  let saved = false
  let userReview = null
  try {
    if (!hasApiSession()) return normalizeEvent(event, { socialLinks, eventRating: averageRating(ratings), reviews: ratings.map(normalizeRating) })
    const [savedStatus, myRatings] = await Promise.all([
      apiRequest(`/api/events/${id}/save`),
      apiRequest('/api/me/ratings'),
    ])
    saved = Boolean(savedStatus.saved)
    const ownRating = myRatings.find((rating) => rating.eventId?.toString() === id?.toString())
    if (ownRating) userReview = { rating: ownRating.score, comment: ownRating.description }
  } catch (error) {
    if (error.status !== 401 && error.status !== 403) throw error
  }

  return normalizeEvent(event, {
    socialLinks,
    eventRating: averageRating(ratings),
    reviews: ratings.map(normalizeRating),
    userReview,
    saved,
  })
}

export const getEvents = async () => apiPageContent(await apiRequest('/api/events', {
  auth: false,
  query: pageAll,
})).map(normalizeEvent)

export const getPublicEvents = async () => (await getEvents()).filter((event) => publicStatuses.includes(event.status))

export const getEventById = (id) => loadEventDetails(id)

export const getFeaturedEvents = async () => (await apiRequest('/api/events/featured', { auth: false })).map(normalizeEvent)
export const getFeaturedEvent = async () => {
  const events = await getFeaturedEvents()
  return events[0] || null
}

export const getCategories = async () => {
  const [categories, events] = await Promise.all([
    apiRequest('/api/categories', { auth: false }),
    getPublicEvents(),
  ])
  return [
    { id: 'all', slug: 'all', title: 'Todos', icon: 'all', count: events.length },
    ...categories.map((category) => normalizeCategory(
      category,
      events.filter((event) => event.categorySlug === category.slug).length,
    )),
  ]
}

export const getCategoryBySlug = async (slug) => {
  if (slug === 'all') return { id: 'all', slug: 'all', title: 'Todos', icon: 'all' }
  return normalizeCategory(await apiRequest(`/api/categories/${slug}`, { auth: false }))
}

export const getEventsByCategorySlug = async (slug) => {
  if (!slug || slug === 'all') return getPublicEvents()
  return apiPageContent(await apiRequest(`/api/categories/${slug}/events`, {
    auth: false,
    query: pageAll,
  })).map(normalizeEvent)
}

export const groupEventsByCategory = async () => {
  const [categories, events] = await Promise.all([getCategories(), getPublicEvents()])
  return categories
    .filter((category) => category.id !== 'all')
    .map((category) => ({
      ...category,
      events: events.filter((event) => event.categorySlug === category.slug),
    }))
    .filter((category) => category.events.length > 0)
}

export const searchEvents = async (filters) => {
  const events = apiPageContent(await apiRequest('/api/events', {
    auth: false,
    query: {
      search: filters.query,
      category: filters.category,
      status: 'PUBLISHED',
      ...pageAll,
    },
  })).map(normalizeEvent)
  return searchAndSortEvents(events, filters)
}

export const getEventStatusLabel = (status) => eventStatusLabels[status] || 'Status desconhecido'

export const formatEventSchedule = (event) => ({
  start: formatEventDateTime(event.startDate, event.startTime),
  end: formatEventDateTime(event.endDate, event.endTime),
})

export const formatParticipants = (event) => {
  const count = event.interestCount ?? event.participants ?? 0
  return `${count} interessado${count === 1 ? '' : 's'}`
}

export const saveOwnEventRating = async (eventId, review) => {
  await apiRequest(`/api/events/${eventId}/ratings`, {
    method: 'POST',
    body: { score: review.rating, description: review.comment },
  })
  return loadEventDetails(eventId)
}

export const deleteOwnEventRating = async (eventId) => {
  await apiRequest(`/api/events/${eventId}/ratings/me`, { method: 'DELETE' })
  return loadEventDetails(eventId)
}

export const saveEvent = async (eventId) => apiRequest(`/api/events/${eventId}/save`, { method: 'POST' })
export const unsaveEvent = async (eventId) => apiRequest(`/api/events/${eventId}/save`, { method: 'DELETE' })

export const getSavedEvents = async () => (await apiRequest('/api/me/saved-events')).map((saved) => (
  normalizeEvent(saved.event, { saved: true, savedAt: saved.savedAt })
))

export const getMyRatings = async () => apiRequest('/api/me/ratings')

export const getMyRatedEvents = async () => {
  const ratings = await getMyRatings()
  const events = await Promise.all(ratings.map(async (rating) => {
    try {
      const event = await loadEventDetails(rating.eventId)
      return {
        ...event,
        userReview: { rating: rating.score, comment: rating.description },
        ratedAt: rating.ratedAt,
      }
    } catch {
      return {
        id: rating.eventId?.toString(),
        title: rating.eventTitle || `Evento ${rating.eventId}`,
        locationName: '',
        categoryName: '',
        categorySlug: '',
        userReview: { rating: rating.score, comment: rating.description },
        ratedAt: rating.ratedAt,
      }
    }
  }))
  return events
}

export const getMyEvents = async () => {
  const events = []
  let page = 0
  let totalPages
  do {
    const result = await apiRequest('/api/events/mine', { query: { ...pageAll, page, sort: 'id,asc' } })
    events.push(...apiPageContent(result).map(normalizeEvent))
    totalPages = result.totalPages ?? 1
    page += 1
  } while (page < totalPages)
  return events
}

export const buildEventPayload = (payload) => ({
  title: limit(payload.title || payload.name, 40),
  description: limit(payload.description, 150),
  categorySlug: payload.categorySlug || payload.category,
  location: limit(payload.address || payload.locationName, 100),
  address: {
    cep: payload.addressFields?.cep || '',
    rua: limit(payload.addressFields?.rua, 100),
    numero: Number(payload.addressFields?.numero),
    cidade: limit(payload.addressFields?.cidade, 40),
    estado: limit(payload.addressFields?.estado, 30),
    pais: limit(payload.addressFields?.pais || 'Brasil', 40),
  },
  latitude: payload.latitude,
  longitude: payload.longitude,
  startDateTime: joinDateTime(payload.startDate, payload.startTime),
  endDateTime: joinDateTime(payload.endDate, payload.endTime),
  status: statusToApi(payload.status || 'draft'),
})

export const createEvent = async (_events, payload) => normalizeEvent(await apiRequest('/api/events', {
  method: 'POST',
  body: buildEventPayload(payload),
}))

export const updateEvent = async (events, eventId, payload) => {
  const body = buildEventPayload(payload)
  const original = events.find(event => event.id === eventId)
  // Preserve seconds/fractions from SQL Server when editing only content of an older event.
  if (original?.startDateTime && original.startDate === payload.startDate && original.startTime === payload.startTime) body.startDateTime = original.startDateTime
  if (original?.endDateTime && original.endDate === payload.endDate && original.endTime === payload.endTime) body.endDateTime = original.endDateTime
  return normalizeEvent(await apiRequest(`/api/events/${eventId}`, { method: 'PUT', body }))
}

export const updateEventStatus = async (eventId, status) => normalizeEvent(await apiRequest(`/api/events/${eventId}/status`, {
  method: 'PUT',
  body: {
    status: statusToApi(status),
  },
}))

export const setFeaturedEvent = async (eventId) => normalizeEvent(await apiRequest(`/api/events/${eventId}/featured`, {
  method: 'PUT',
}))

export const clearFeaturedEvent = async () => apiRequest('/api/events/featured', { method: 'DELETE' })

export const deleteEvent = async (_events, eventId) => {
  await apiRequest(`/api/events/${eventId}`, { method: 'DELETE' })
  return eventId
}

export const uploadEventImage = async (eventId, file) => {
  if (!file) return null
  const formData = new FormData()
  formData.append('file', file)
  return apiRequest(`/api/events/${eventId}/image`, { method: 'POST', body: formData })
}

export const searchAdminEvents = (events, { query = '', category = 'all', status = 'all', sort = 'upcoming' }) => {
  const searched = searchAndSortEvents(events, {
    query,
    category,
    sort: sort === 'upcoming' ? 'nearest' : sort,
  }).filter((event) => status === 'all' || event.status === status)

  if (sort === 'newest') return [...searched].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  if (sort === 'oldest') return [...searched].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))

  return searched
}

export const publishEventToApi = async (event) => {
  const createdEvent = await createEvent([], { ...event, status: 'pending_review' })
  return { persisted: true, event: createdEvent }
}
