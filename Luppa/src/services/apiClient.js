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
  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '' && value !== 'all') {
      url.searchParams.set(key, value)
    }
  })
  return API_BASE_URL ? `${url.pathname}${url.search}`.replace(/^/, API_BASE_URL.replace(/\/$/, '')) : `${url.pathname}${url.search}`
}

const getToken = () => window.localStorage.getItem(TOKEN_KEY)

export const toApiAssetUrl = (path, basePath = '') => {
  if (!path) return ''
  if (/^(?:https?:)?\/\//i.test(path) || path.startsWith('blob:') || path.startsWith('data:')) return path
  const normalizedPath = path.startsWith('/') ? path : `${basePath}/${path}`.replace(/\/{2,}/g, '/')
  if (!API_BASE_URL) return normalizedPath
  return `${API_BASE_URL.replace(/\/$/, '')}${normalizedPath}`
}

export const apiRequest = async (path, { method = 'GET', body, query, headers, auth = true } = {}) => {
  const token = auth ? getToken() : null
  const requestHeaders = new Headers(headers)

  if (body !== undefined && !(body instanceof FormData)) {
    requestHeaders.set('Content-Type', 'application/json')
  }
  if (token) requestHeaders.set('Authorization', `Bearer ${token}`)

  const response = await fetch(buildUrl(path, query), {
    method,
    headers: requestHeaders,
    body: body instanceof FormData || body === undefined ? body : JSON.stringify(body),
  })

  const text = await response.text()
  const payload = text ? JSON.parse(text) : null

  if (!response.ok) {
    throw new ApiError(payload?.message || 'api_request_failed', { status: response.status, payload })
  }

  return payload
}

export const apiPageContent = (page) => page?.content || []
