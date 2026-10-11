import { Outlet } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { getEvents } from '../../services/eventService'
import { fetchUsers } from '../../services/userService'
import AdminLayout from './AdminLayout'

const AdminShell = () => {
  const [events, setEvents] = useState([])
  const [users, setUsers] = useState([])
  const [error, setError] = useState('')

  const refreshAdminData = async () => {
    const [nextEvents, nextUsers] = await Promise.all([getEvents(), fetchUsers()])
    setEvents(nextEvents)
    setUsers(nextUsers)
    setError('')
  }

  useEffect(() => {
    refreshAdminData().catch(() => setError('Não foi possível carregar dados administrativos.'))
  }, [])

  return (
    <AdminLayout>
      {error && <p>{error}</p>}
      <Outlet context={{ users, setUsers, events, setEvents, refreshAdminData }} />
    </AdminLayout>
  )
}

export default AdminShell
