import { QueryClient } from '@tanstack/query-core'
import { notifyEventChanges } from './eventChanges'

// No persister: cached data and session scope live only in this tab's memory.
export const requestCache = new QueryClient({ defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } } })
let currentSession
let sessionRevision = 0

export const resetRequestCache = () => {
  requestCache.clear() // Also cancels requests from the previous session.
  sessionRevision += 1
  currentSession = undefined
  notifyEventChanges()
}

export const syncCacheSession = (token) => {
  if (currentSession !== token) {
    requestCache.clear()
    currentSession = token
    sessionRevision += 1
  }
  return sessionRevision
}

export const cachePolicy = (path, authenticated) => {
  if (/^\/api\/categories(?:\/[^/]+)?$/.test(path)) return { staleTime: 300_000, gcTime: 600_000, scope: 'public' }
  if (/^\/api\/events(?:\/(?:\d+|featured)(?:\/(?:ratings|social-links))?)?$/.test(path)
      || /^\/api\/categories\/[^/]+\/events$/.test(path)) {
    return { staleTime: 30_000, gcTime: 300_000, scope: 'public' }
  }
  if (authenticated && (/^\/api\/me\/(?:saved-events|ratings)$/.test(path)
      || /^\/api\/events\/(?:mine|\d+\/save)$/.test(path)
      || /^\/api\/insights\/events\/(?:creator|platform)$/.test(path))) {
    return { staleTime: 15_000, gcTime: 60_000, scope: 'private' }
  }
  // Account details, preferences, admin users, audit records and reports are never cached.
  return null
}

export const invalidateEventRequests = async () => {
  await requestCache.cancelQueries({ queryKey: ['api'] })
  await requestCache.invalidateQueries({ queryKey: ['api'], refetchType: 'none' })
}
