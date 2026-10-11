import { apiRequest } from './apiClient'

const normalizeAuditLog = (log) => ({
  id: log.id,
  operatedAt: log.operatedAt,
  userId: log.userId,
  historicalUserId: log.historicalUserId,
  user: log.user,
  tableName: log.tableName,
  operation: log.operation,
  recordId: log.recordId,
  oldData: log.oldData,
  newData: log.newData,
})

export const fetchAuditLogs = async (filters = {}) => {
  const page = await apiRequest('/api/admin/audit-logs', {
    query: {
      search: filters.search,
      operation: filters.operation,
      tableName: filters.tableName,
      user: filters.user,
      startDate: filters.startDate,
      endDate: filters.endDate,
      page: filters.page ?? 0,
      size: filters.size ?? 25,
    },
  })

  return {
    ...page,
    content: (page?.content || []).map(normalizeAuditLog),
  }
}

export const fetchAuditTables = async () => apiRequest('/api/admin/audit-logs/tables')
