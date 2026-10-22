import { useEffect, useState } from 'react'
import { it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import EventCard from '../components/events/EventCard'
import EventDetailsModal from '../components/events/EventDetailsModal'
import { ToastProvider } from '../components/common/ToastProvider'
import { AuthContext } from '../auth/authContext'
import useEventDetails from '../hooks/useEventDetails'
import useEventRevision from '../hooks/useEventRevision'
import { getPublicEvents } from '../services/eventService'

vi.mock('../components/events/EventMap', () => ({ default: () => <div>Mapa</div> }))
const event = { id: 10, title: 'Show', description: 'Música', category: { slug: 'musica', name: 'Música' }, location: 'Barueri', startDateTime: '2026-12-01T10:00:00', endDateTime: '2026-12-01T12:00:00', status: 'PUBLISHED' }
function Harness() {
  const revision = useEventRevision()
  const [events, setEvents] = useState([])
  const details = useEventDetails()
  useEffect(() => { let active = true; getPublicEvents().then(items => { if (active) setEvents(items) }); return () => { active = false } }, [revision])
  return <>{events.map(event => <EventCard key={event.id} event={event} onDetails={details.openEventDetails} />)}<EventDetailsModal event={details.selectedEvent} onClose={details.closeEventDetails} onEventUpdated={details.updateSelectedEvent} /></>
}
it('refreshes authoritative counts in both cards and open details after save and unsave, including a remount', async () => {
  let saved = false
  localStorage.setItem('luppa.auth.token', 'test-session')
  vi.stubGlobal('fetch', vi.fn(async (url, options) => {
    const path = new URL(url, window.location.origin).pathname
    if (path.endsWith('/save') && options.method !== 'GET') saved = options.method === 'POST'
    const current = { ...event, interestCount: saved ? 1 : 0 }
    let body = []
    if (path === '/api/events') body = { content: [current] }
    if (path === '/api/events/10') body = current
    if (path.endsWith('/save')) body = { saved }
    return new Response(JSON.stringify(body), { status: 200 })
  }))
  const user = userEvent.setup()
  const ui = <AuthContext.Provider value={{ isAuthenticated: true, userId: '1' }}><ToastProvider><Harness /></ToastProvider></AuthContext.Provider>
  const rendered = render(ui)
  await user.click(await screen.findByRole('button', { name: 'Ver detalhes' }))
  await user.click(await screen.findByRole('button', { name: 'Tenho interesse' }))
  await waitFor(() => expect(screen.getAllByText('1 interessado')).toHaveLength(2))
  await user.click(await screen.findByRole('button', { name: 'Remover interesse' }))
  await waitFor(() => expect(screen.getAllByText('0 interessados')).toHaveLength(2))
  rendered.rerender(<AuthContext.Provider value={{ isAuthenticated: true, userId: '2' }}><ToastProvider><Harness /></ToastProvider></AuthContext.Provider>)
  expect(screen.queryByRole('dialog')).toBeNull()
  rendered.unmount()
  render(ui)
  expect(await screen.findByText('0 interessados')).toBeTruthy()
  localStorage.clear()
})
