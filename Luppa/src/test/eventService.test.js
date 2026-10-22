import { it, expect, vi } from 'vitest'
import { buildEventPayload, getMyEvents, updateEvent } from '../services/eventService'

it('loads every creator page instead of silently truncating at 200 events', async () => {
  localStorage.setItem('luppa.auth.token', 'test-session')
  const fetch = vi.fn(async url => {
    const page = new URL(url, window.location.origin).searchParams.get('page')
    return new Response(JSON.stringify({ content: [{ id: page === '0' ? 1 : 2, status: 'DRAFT', interestCount: 7 }], totalPages: 2 }), { status: 200 })
  })
  vi.stubGlobal('fetch', fetch)
  expect((await getMyEvents()).map(event => event.id)).toEqual(['1','2'])
  expect(fetch).toHaveBeenCalledTimes(2)
  localStorage.clear()
})
it('preserves original SQL Server seconds on content-only updates and changes only requested dates', async () => {
  const original = { id: '10', startDate: '2026-01-01', startTime: '10:00', endDate: '2026-01-01', endTime: '12:00', startDateTime: '2026-01-01T10:00:30', endDateTime: '2026-01-01T12:00:40' }
  const payload = { ...original, title: 'Novo', addressFields: { numero: '1' }, status: 'pending_review' }
  const fetch = vi.fn(async () => new Response(JSON.stringify({ id: 10, status: 'REVIEW' }), { status: 200 }))
  vi.stubGlobal('fetch', fetch)
  await updateEvent([original], '10', payload)
  expect(JSON.parse(fetch.mock.calls[0][1].body)).toMatchObject({ startDateTime: original.startDateTime, endDateTime: original.endDateTime, status: 'REVIEW' })
  await updateEvent([original], '10', { ...payload, startTime: '11:00' })
  expect(JSON.parse(fetch.mock.calls[1][1].body).startDateTime).toBe('2026-01-01T11:00:00')
  expect(buildEventPayload(payload)).not.toHaveProperty('idUsuario')
})
