import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AuthContext } from '../auth/authContext'
import { ToastProvider } from '../components/common/ToastProvider'
import MenuLuppa from '../pages/MenuLuppa/MenuLuppa'

vi.mock('../components/events/EventDetailsModal', () => ({ default: () => null }))
vi.mock('../services/eventService', () => ({ getCategories: async () => [], searchEvents: async () => [], getFeaturedEvents: async () => [], saveEvent: vi.fn() }))

const renderHome = () => render(<MemoryRouter><AuthContext.Provider value={{ isAuthenticated: false }}><ToastProvider><MenuLuppa /></ToastProvider></AuthContext.Provider></MemoryRouter>)
const fillContact = async () => {
  const user = userEvent.setup()
  await user.type(screen.getByPlaceholderText('Seu nome'), 'Ana')
  await user.type(screen.getByPlaceholderText('seuemail@email.com'), 'ana@example.com')
  await user.type(screen.getByPlaceholderText('Escreva sua mensagem'), 'Preciso de ajuda')
  await user.click(screen.getByRole('button', { name: 'Enviar mensagem' }))
}

describe('Home contact', () => {
  it('posts publicly to the backend and reports success only after acceptance', async () => {
    let finish
    const fetch = vi.fn(() => new Promise(resolve => { finish = resolve }))
    vi.stubGlobal('fetch', fetch)
    renderHome()
    await fillContact()
    expect(screen.getByRole('button', { name: 'Enviando...' }).disabled).toBe(true)
    expect(screen.queryByText('Aceita pela equipe')).toBeNull()
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toMatchObject({ name: 'Ana', email: 'ana@example.com', message: 'Preciso de ajuda' })
    expect(fetch.mock.calls[0][0]).toContain('/api/support/contact')
    expect(fetch.mock.calls[0][1].headers.has('Authorization')).toBe(false)
    finish(new Response(JSON.stringify({ message: 'Aceita pela equipe' }), { status: 200 }))
    await screen.findByText('Aceita pela equipe')
    expect(screen.getByPlaceholderText('Escreva sua mensagem').value).toBe('')
  })

  it('preserves the message and shows a real error on provider failure', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({ message: 'Unexpected server error' }), { status: 500 })))
    renderHome()
    await fillContact()
    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('Não foi possível'))
    expect(screen.getByPlaceholderText('Escreva sua mensagem').value).toBe('Preciso de ajuda')
  })
})
