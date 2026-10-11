import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import LuppaSymbol from '../../components/common/LuppaSymbol'
import EventCard from '../../components/events/EventCard'
import EventDetailsModal from '../../components/events/EventDetailsModal'
import { useAuth } from '../../auth/authContext'
import useEventDetails from '../../hooks/useEventDetails'
import { getMyEvents } from '../../services/eventService'
import styles from './MyEventsLuppa.module.css'

const MyEventsLuppa = () => {
  const auth = useAuth()
  const { selectedEvent, openEventDetails, closeEventDetails, updateSelectedEvent } = useEventDetails()
  const [events, setEvents] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    getMyEvents()
      .then((items) => {
        if (active) setEvents(items)
      })
      .catch(() => {
        if (active) setError('Não foi possível carregar seus eventos.')
      })
    return () => {
      active = false
    }
  }, [auth.userId])

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

      {error ? <p className={styles.empty}>{error}</p> : events.length === 0 ? (
        <section className={styles.empty}>
          <h2>Nenhum evento próprio encontrado.</h2>
          <p>Crie um evento para começar a gerenciar sua programação.</p>
          <Link to="/cadastrar-evento">Criar evento</Link>
        </section>
      ) : (
        <section className={styles.grid}>
          {events.map((event) => (
            <article className={styles.eventItem} key={event.id}>
              <EventCard event={event} onDetails={openEventDetails} />
              {event.rejectionReason && (
                <p className={styles.rejection}><strong>Motivo da rejeição:</strong> {event.rejectionReason}</p>
              )}
            </article>
          ))}
        </section>
      )}
      <EventDetailsModal event={selectedEvent} onClose={closeEventDetails} onEventUpdated={updateSelectedEvent} />
    </main>
  )
}

export default MyEventsLuppa
