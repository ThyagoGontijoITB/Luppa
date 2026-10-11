import { apiRequest } from './apiClient'
import { reportStatuses } from '../constants/reports'

const normalizeReport = (report) => ({
  id: report.id?.toString(),
  eventId: report.eventId?.toString(),
  eventTitle: report.eventTitle,
  ratingId: report.ratingId?.toString(),
  rating: report.rating ? {
    id: report.rating.id?.toString(),
    authorName: report.rating.author?.name,
    authorPhotoUrl: report.rating.author?.profileImageUrl,
    score: report.rating.score,
    description: report.rating.description,
    ratedAt: report.rating.ratedAt,
  } : null,
  reporterUserId: report.reporter?.id?.toString(),
  reporterName: report.reporter?.name,
  reason: report.category?.category,
  categoryId: report.category?.id,
  description: report.description,
  createdAt: report.reportedAt,
  status: report.status || reportStatuses.PENDING,
  moderationResponse: report.moderationResponse || '',
  moderationRespondedAt: report.moderationRespondedAt || '',
})

export const getReports = async () => (await apiRequest('/api/reports')).map(normalizeReport)

export const getReportCategories = async () => apiRequest('/api/reports/categories', { auth: false })

export const createReport = async (_reports, payload) => {
  const report = await apiRequest(`/api/events/${payload.eventId}/reports`, {
    method: 'POST',
    body: {
      categoryId: payload.categoryId,
      description: payload.description,
      ratingId: payload.ratingId ? Number(payload.ratingId) : undefined,
    },
  })

  return {
    duplicate: false,
    report: normalizeReport(report),
  }
}

export const updateReportStatus = async (reportId, status, responseMessage) => normalizeReport(await apiRequest(`/api/reports/${reportId}/status`, {
  method: 'PUT',
  body: { status, responseMessage },
}))
