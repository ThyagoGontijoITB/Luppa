import { apiRequest } from './apiClient'

const emptyInsights = {
  summary: {
    totalEvents: 0,
    publishedEvents: 0,
    draftEvents: 0,
    reviewEvents: 0,
    cancelledEvents: 0,
    totalSaves: 0,
    totalReviews: 0,
    averageRating: null,
  },
  byCategory: [],
  byStatus: [],
  mostSavedEvents: [],
  bestRatedEvents: [],
}

const normalizeStatusKey = (key) => key?.toString?.().toLowerCase?.() || ''

const normalizeBreakdown = (item) => ({
  key: normalizeStatusKey(item.key),
  label: item.label,
  count: Number(item.count || 0),
})

const normalizeRankedEvent = (event) => ({
  eventId: event.eventId?.toString(),
  title: event.title,
  categoryName: event.categoryName,
  status: normalizeStatusKey(event.status),
  saveCount: Number(event.saveCount || 0),
  reviewCount: Number(event.reviewCount || 0),
  averageRating: event.averageRating === null || event.averageRating === undefined ? null : Number(event.averageRating),
})

const normalizeInsights = (payload = emptyInsights) => ({
  summary: {
    ...emptyInsights.summary,
    ...(payload.summary || {}),
  },
  byCategory: (payload.byCategory || []).map(normalizeBreakdown),
  byStatus: (payload.byStatus || []).map(normalizeBreakdown),
  mostSavedEvents: (payload.mostSavedEvents || []).map(normalizeRankedEvent),
  bestRatedEvents: (payload.bestRatedEvents || []).map(normalizeRankedEvent),
})

export const fetchCreatorEventInsights = async () => normalizeInsights(await apiRequest('/api/insights/events/creator'))

export const fetchPlatformEventInsights = async () => normalizeInsights(await apiRequest('/api/insights/events/platform'))
