import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import { Outlet, Link } from 'react-router-dom'
import AppRouter from '../routes/AppRouter'
import vercel from '../../vercel.json'

const session = vi.hoisted(() => ({ authenticated: false, role: 'USER' }))
vi.mock('../auth/AuthProvider', async () => {
  const { AuthContext } = await import('../auth/authContext')
  return { AuthProvider: ({ children }) => <AuthContext.Provider value={{ loading: false, isAuthenticated: session.authenticated, hasRole: roles => roles.includes(session.role) }}>{children}</AuthContext.Provider> }
})
vi.mock('../pages/HomeLuppa/HomeLuppa', () => ({ default: () => <><h1>Home</h1><Link to="/events">Eventos</Link></> }))
vi.mock('../pages/ExploreEventsLuppa/ExploreEventsLuppa', () => ({ default: () => <h1>Eventos públicos</h1> }))
vi.mock('../pages/ProfileLuppa/ProfileLuppa', () => ({ default: () => <h1>Perfil privado</h1> }))
vi.mock('../pages/MyEventsLuppa/MyEventsLuppa', () => ({ default: () => <h1>Painel do criador</h1> }))
vi.mock('../pages/LoginCadastroLuppa/LoginCadastroLuppa', () => ({ default: () => <h1>Login</h1> }))
vi.mock('../pages/AdminLuppa/AdminShell', () => ({ default: () => <Outlet /> }))
vi.mock('../pages/AdminLuppa/AdminUsers', () => ({ default: () => <h1>Usuários administrativos</h1> }))

beforeEach(() => { session.authenticated = false; session.role = 'USER' })
afterEach(() => window.history.replaceState({}, '', '/'))
it.each(['/events','/events/category/musica','/profile','/my-events','/admin/users'])('Vercel serves the SPA for direct navigation to %s', path => {
  expect(vercel.routes[0]).toEqual({ handle: 'filesystem' })
  expect(new RegExp(`^${vercel.routes[1].src}$`).test(path)).toBe(true)
  expect(vercel.routes[1].dest).toBe('/index.html')
})
it.each(['/api/events','/api','/assets/missing.js','/uploads/event.jpg','/robots.txt','/favicon.ico'])('fallback preserves %s', path => {
  expect(new RegExp(`^${vercel.routes[1].src}$`).test(path)).toBe(false)
})
it.each([
  ['/events', false, 'USER', 'Eventos públicos'],
  ['/profile', false, 'USER', 'Login'],
  ['/profile', true, 'USER', 'Perfil privado'],
  ['/my-events', true, 'CREATOR', 'Painel do criador'],
  ['/admin/users', true, 'ADMIN', 'Usuários administrativos'],
  ['/admin/users', true, 'CREATOR', 'Home'],
])('resolves direct path %s preserving authorization', async (path, authenticated, role, heading) => {
  session.authenticated = authenticated; session.role = role
  window.history.replaceState({}, '', path)
  render(<AppRouter />)
  expect(await screen.findByRole('heading', { name: heading })).toBeTruthy()
})
it('supports back and forward with BrowserRouter', async () => {
  window.history.replaceState({}, '', '/')
  render(<AppRouter />)
  await act(async () => screen.getByRole('link', { name: 'Eventos' }).click())
  await screen.findByRole('heading', { name: 'Eventos públicos' })
  act(() => window.history.back())
  await waitFor(() => expect(screen.getByRole('heading', { name: 'Home' })).toBeTruthy())
  act(() => window.history.forward())
  await waitFor(() => expect(screen.getByRole('heading', { name: 'Eventos públicos' })).toBeTruthy())
})
