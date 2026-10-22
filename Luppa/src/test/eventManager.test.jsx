import { it, expect, vi, beforeEach } from 'vitest'
import { render, screen, within, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import EventManager from '../components/events/EventManager'
import { ToastProvider } from '../components/common/ToastProvider'
import { deleteEvent, updateEvent } from '../services/eventService'

vi.mock('../components/forms/StructuredAddressFields', () => ({ default: () => <div>Endereço existente</div> }))
vi.mock('../services/eventService', async (original) => ({
  ...await original(),
  getCategories: async () => [{ id: 'musica', slug: 'musica', title: 'Música' }],
  deleteEvent: vi.fn(), updateEvent: vi.fn(),
}))
const event = { id: '10', title: 'Show', description: 'Música ao vivo', categorySlug: 'musica', categoryName: 'Música', locationName: 'Barueri', addressFields: { cep: '06400000', rua: 'Rua', numero: '1', cidade: 'Barueri', estado: 'SP', pais: 'Brasil' }, startDate: '2026-01-01', startTime: '10:00', endDate: '2026-01-01', endTime: '12:00', status: 'published', interestCount: 4 }
function Panel() {
  const [events, setEvents] = useState([event])
  return <ToastProvider><EventManager events={events} setEvents={setEvents} /></ToastProvider>
}
beforeEach(() => {
  vi.mocked(deleteEvent).mockReset()
  vi.mocked(updateEvent).mockReset()
  Element.prototype.scrollIntoView = vi.fn()
})
it('shows interests, hides admin controls, and deletes only after confirmation', async () => {
  const user = userEvent.setup()
  vi.mocked(deleteEvent).mockResolvedValue('10')
  render(<Panel />)
  expect(screen.getByText('4')).toBeTruthy()
  expect(screen.queryByText('Destacar')).toBeNull()
  await user.click(screen.getByRole('button', { name: 'Excluir' }))
  expect(deleteEvent).not.toHaveBeenCalled()
  await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Excluir evento' }))
  await waitFor(() => expect(screen.queryByRole('button', { name: 'Editar' })).toBeNull())
  expect(deleteEvent).toHaveBeenCalledWith([event], '10')
})
it('keeps a linked event visible when deletion conflicts', async () => {
  const user = userEvent.setup()
  vi.mocked(deleteEvent).mockRejectedValue(Object.assign(new Error('Cancele para preservar o histórico.'), { status: 409 }))
  render(<Panel />)
  await user.click(screen.getByRole('button', { name: 'Excluir' }))
  await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Excluir evento' }))
  await screen.findByText('Cancele para preservar o histórico.')
  expect(screen.getByRole('button', { name: 'Editar' })).toBeTruthy()
})
it('reuses the existing editor, permits unchanged past dates and submits published edits for review', async () => {
  const user = userEvent.setup()
  vi.mocked(updateEvent).mockResolvedValue({ ...event, title: 'Show novo', status: 'pending_review' })
  render(<Panel />)
  await user.click(screen.getByRole('button', { name: 'Editar' }))
  await user.clear(screen.getByLabelText('Nome'))
  await user.type(screen.getByLabelText('Nome'), 'Show novo')
  await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))
  await waitFor(() => expect(updateEvent).toHaveBeenCalled())
  expect(updateEvent.mock.calls[0][2]).toMatchObject({ title: 'Show novo', status: 'pending_review' })
  await screen.findByText('Show novo')
})
