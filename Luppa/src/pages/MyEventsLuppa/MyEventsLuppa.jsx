import useEventRevision from '../../hooks/useEventRevision'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import LuppaSymbol from '../../components/common/LuppaSymbol'
import EventManager from '../../components/events/EventManager'
import EventDetailsModal from '../../components/events/EventDetailsModal'
import { useAuth } from '../../auth/authContext'
import useEventDetails from '../../hooks/useEventDetails'
import { getMyEvents } from '../../services/eventService'
import styles from './MyEventsLuppa.module.css'

const MyEventsLuppa = () => {
  const eventRevision = useEventRevision()
  const auth = useAuth()
  const { selectedEvent, openEventDetails, closeEventDetails, updateSelectedEvent } = useEventDetails()
  const [events, setEvents] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const refreshEvents = async () => {
    const items = await getMyEvents()
    setEvents(items)
    setError('')
  }

  useEffect(() => {
    let active = true
    getMyEvents()
      .then((items) => {
        if (active) { setEvents(items); setError(''); setLoading(false) }
      })
      .catch(() => {
        if (active) { setError('Não foi possível carregar seus eventos.'); setLoading(false) }
      })
    return () => {
      active = false
    }
  }, [auth.userId, eventRevision])

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <Link to="/" aria-label="Luppa"><LuppaSymbol /></Link>
        <nav>
          <Link to="/">← Voltar para o Luppa</Link>
          <Link to="/events">Explorar eventos</Link>
          <Link to="/creator/insights">Insights</Link>
          <Link to="/cadastrar-evento">Criar evento</Link>
        </nav>
      </header>

      <section className={styles.hero}>
        <span>CREATOR</span>
        <h1>Meus eventos</h1>
        <p>Gerencie os eventos associados à sua conta.</p>
      </section>

      {loading ? <p className={styles.empty} role="status">Carregando seus eventos...</p> : error ? <p className={styles.empty} role="alert">{error}</p> : (
        <div className={styles.management}>
          {events.length === 0 && <p>Nenhum evento próprio encontrado. Crie um evento para começar.</p>}
          <EventManager events={events} setEvents={setEvents} refreshAdminData={refreshEvents} onDetails={openEventDetails} />
        </div>
      )}
      <EventDetailsModal event={selectedEvent} onClose={closeEventDetails} onEventUpdated={updateSelectedEvent} />
    </main>
  )
}

export default MyEventsLuppa
