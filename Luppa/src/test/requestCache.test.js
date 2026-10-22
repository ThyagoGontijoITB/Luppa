import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../services/apiClient'
import { cachePolicy, requestCache, resetRequestCache } from '../services/requestCache'
import { getEventRevision } from '../services/eventChanges'

const ok = data => new Response(JSON.stringify(data), { status: 200 })
beforeEach(() => { localStorage.clear(); resetRequestCache() })
afterEach(() => { vi.useRealTimers(); vi.unstubAllGlobals() })
describe('request cache', () => {
  it('deduplicates concurrent and fresh reads including reordered query parameters', async () => {
    const fetch = vi.fn(async () => ok({ content: [{ id: 10, interestCount: 2 }] }))
    vi.stubGlobal('fetch', fetch)
    const requests = await Promise.all([
      apiRequest('/api/events', { auth: false, query: { size: 200, page: 0 } }),
      apiRequest('/api/events', { auth: false, query: { page: 0, size: 200 } }),
    ])
    expect(requests[0]).toEqual(requests[1])
    await apiRequest('/api/events', { auth: false, query: { page: 0, size: 200 } })
    expect(fetch).toHaveBeenCalledTimes(1)
    await apiRequest('/api/events', { auth: false, query: { page: 1, size: 200 } })
    expect(fetch).toHaveBeenCalledTimes(2)
  })
  it('expires event reads and uses longer category freshness', async () => {
    vi.useFakeTimers()
    const fetch = vi.fn(async () => ok([])); vi.stubGlobal('fetch', fetch)
    await apiRequest('/api/events'); await apiRequest('/api/categories')
    vi.setSystemTime(Date.now() + 31_000)
    await apiRequest('/api/events'); await apiRequest('/api/categories')
    expect(fetch).toHaveBeenCalledTimes(3)
    expect(cachePolicy('/api/me/saved-events', true).gcTime).toBe(60_000)
  })
  it.each(['POST', 'PUT', 'DELETE'])('invalidates lists, details, saved events and insights after %s', async method => {
    localStorage.setItem('luppa.auth.token', 'test-session-A')
    const fetch = vi.fn(async () => ok({ interestCount: 4 })); vi.stubGlobal('fetch', fetch)
    const paths = ['/api/events', '/api/events/10', '/api/me/saved-events', '/api/insights/events/creator']
    for (const path of paths) await apiRequest(path)
    const revision = getEventRevision()
    await apiRequest('/api/events/10/save', { method })
    for (const path of paths) await apiRequest(path)
    expect(fetch).toHaveBeenCalledTimes(9)
    expect(getEventRevision()).toBeGreaterThan(revision)
  })
  it('does not invalidate on a rejected mutation or cache provider errors', async () => {
    const fetch = vi.fn(async () => ok([])); vi.stubGlobal('fetch', fetch)
    await apiRequest('/api/events')
    fetch.mockResolvedValueOnce(new Response('{"message":"denied"}', { status: 403 }))
    const revision = getEventRevision()
    await expect(apiRequest('/api/events/10', { method: 'DELETE' })).rejects.toMatchObject({ status: 403 })
    await apiRequest('/api/events')
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(getEventRevision()).toBe(revision)
    fetch.mockResolvedValueOnce(new Response('failure', { status: 502 }))
    await expect(apiRequest('/api/events/11')).rejects.toMatchObject({ status: 502 })
    await apiRequest('/api/events/11')
    expect(fetch).toHaveBeenCalledTimes(4)
  })
  it('isolates users, clears on logout and never includes tokens in cache keys', async () => {
    const fetch = vi.fn(async () => ok([])); vi.stubGlobal('fetch', fetch)
    localStorage.setItem('luppa.auth.token', 'test-session-A')
    await apiRequest('/api/me/saved-events')
    expect(JSON.stringify(requestCache.getQueryCache().getAll().map(q => q.queryKey))).not.toContain('test-session-A')
    localStorage.setItem('luppa.auth.token', 'test-session-B')
    await apiRequest('/api/me/saved-events')
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(fetch.mock.calls[1][1].headers.get('Authorization')).toBe('Bearer test-session-B')
    localStorage.removeItem('luppa.auth.token'); resetRequestCache()
    expect(requestCache.getQueryCache().getAll()).toHaveLength(0)
  })
  it('does not cache account, support, admin users, or audit information', async () => {
    const fetch = vi.fn(async () => ok([])); vi.stubGlobal('fetch', fetch)
    for (const path of ['/api/auth/me','/api/me/communication-preferences','/api/admin/users','/api/admin/audit-logs']) {
      await apiRequest(path); await apiRequest(path)
    }
    expect(fetch).toHaveBeenCalledTimes(8)
    expect(requestCache.getQueryCache().getAll()).toHaveLength(0)
  })
  it('cancels old in-flight data before invalidation so it cannot overwrite fresh counters', async () => {
    let resolveOld
    const fetch = vi.fn(() => new Promise(resolve => { resolveOld = resolve })); vi.stubGlobal('fetch', fetch)
    const old = apiRequest('/api/events')
    const rejected = expect(old).rejects.toBeDefined()
    fetch.mockResolvedValueOnce(ok({}))
    await apiRequest('/api/events/10/save', { method: 'POST' })
    resolveOld(ok([{ interestCount: 0 }]))
    await rejected
    fetch.mockResolvedValueOnce(ok([{ interestCount: 1 }]))
    expect(await apiRequest('/api/events')).toEqual([{ interestCount: 1 }])
  })
})
