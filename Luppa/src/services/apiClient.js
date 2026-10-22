import { notifyEventChanges } from './eventChanges'
import { cachePolicy, invalidateEventRequests, requestCache, syncCacheSession } from './requestCache'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const TOKEN_KEY = 'luppa.auth.token'

export class ApiError extends Error {
  constructor(message, { status, payload } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }
}

const buildUrl = (path, query) => {
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin)
  Object.entries(query || {}).sort(([left], [right]) => left.localeCompare(right)).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '' && value !== 'all') {
      url.searchParams.set(key, value)
    }
  })
  return API_BASE_URL ? `${url.pathname}${url.search}`.replace(/^/, API_BASE_URL.replace(/\/$/, '')) : `${url.pathname}${url.search}`
}

const getToken = () => {
  try { return window.localStorage.getItem(TOKEN_KEY) } catch { return null }
}
export const hasApiSession = () => Boolean(getToken())

export const toApiAssetUrl = (path, basePath = '') => {
  if (!path) return ''
  if (/^(?:https?:)?\/\//i.test(path) || path.startsWith('blob:') || path.startsWith('data:')) return path
  const normalizedPath = path.startsWith('/') ? path : `${basePath}/${path}`.replace(/\/{2,}/g, '/')
  if (!API_BASE_URL) return normalizedPath
  return `${API_BASE_URL.replace(/\/$/, '')}${normalizedPath}`
}

const sendRequest = async (path, { method = 'GET', body, query, headers, auth = true, signal } = {}) => {
  const token = auth ? getToken() : null
  const requestHeaders = new Headers(headers)

  if (body !== undefined && !(body instanceof FormData)) {
    requestHeaders.set('Content-Type', 'application/json')
  }
  if (token) requestHeaders.set('Authorization', `Bearer ${token}`)

  const response = await fetch(buildUrl(path, query), {
    method,
    headers: requestHeaders,
    signal,
    body: body instanceof FormData || body === undefined ? body : JSON.stringify(body),
  })

  const text = await response.text()
  let payload
  try { payload = text ? JSON.parse(text) : null } catch {
    throw new ApiError('O servidor retornou uma resposta inválida.', { status: response.status })
  }

  if (!response.ok) {
    throw new ApiError(payload?.message || 'api_request_failed', { status: response.status, payload })
  }

  return payload
}

export const apiRequest = async (path, options = {}) => {
  const session = syncCacheSession(getToken())
  const method = (options.method || 'GET').toUpperCase()
  const policy = method === 'GET' && !options.headers && !options.signal
    ? cachePolicy(path, options.auth !== false && hasApiSession()) : null
  if (policy) {
    // Include the complete URL (filters and pagination), never the token itself.
    return requestCache.fetchQuery({
      queryKey: ['api', policy.scope, policy.scope === 'private' ? session : 0, buildUrl(path, options.query)],
      queryFn: ({ signal }) => sendRequest(path, { ...options, signal }),
      staleTime: policy.staleTime,
      gcTime: policy.gcTime,
    })
  }
  const result = await sendRequest(path, { ...options, method })
  if (method !== 'GET' && /^\/api\/(?:events|reports|users|admin\/users)(?:\/|$)/.test(path)) {
    await invalidateEventRequests()
    notifyEventChanges()
  }
  return result
}

export const apiPageContent = (page) => page?.content || []
