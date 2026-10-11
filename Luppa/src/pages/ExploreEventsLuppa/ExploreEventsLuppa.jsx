import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import CreatorActionLink from '../../components/common/CreatorActionLink'
import LuppaSymbol from '../../components/common/LuppaSymbol'
import { useToast } from '../../components/common/ToastProvider'
import UserMenu from '../../components/common/UserMenu'
import EventCard from '../../components/events/EventCard'
import EventDetailsModal from '../../components/events/EventDetailsModal'
import EventSearch from '../../components/events/EventSearch'
import Seo from '../../components/seo/Seo'
import { absoluteUrl, breadcrumbSchema, organizationSchema } from '../../components/seo/seoMetadata'
import useEventDetails from '../../hooks/useEventDetails'
import { getFeaturedEvents, groupEventsByCategory, saveEvent, searchEvents } from '../../services/eventService'
import styles from './ExploreEventsLuppa.module.css'

const defaultFilters = {
  query: '',
  category: 'all',
  sort: 'nearest',
}

const EventRail = ({ events, onDetails }) => {
  const railRef = useRef(null)
  const dragRef = useRef({ active: false, startX: 0, scrollLeft: 0, moved: false })
  const suppressClickUntilRef = useRef(0)
  const [position, setPosition] = useState('start')
  const [dragging, setDragging] = useState(false)
  const dragThreshold = 8

  const updatePosition = () => {
    const rail = railRef.current
    if (!rail) return
    const atStart = rail.scrollLeft <= 4
    const atEnd = rail.scrollLeft + rail.clientWidth >= rail.scrollWidth - 4
    setPosition(atStart ? 'start' : atEnd ? 'end' : 'middle')
  }

  const scroll = (direction) => {
    const rail = railRef.current
    if (!rail) return
    rail.scrollBy({ left: direction * Math.round(rail.clientWidth * 0.82), behavior: 'smooth' })
    window.setTimeout(updatePosition, 220)
  }

  const handlePointerDown = (event) => {
    if (event.pointerType === 'touch') return
    if (event.target.closest('button, a, input, select, textarea, label, [role="button"]')) return
    const rail = railRef.current
    if (!rail) return
    dragRef.current = {
      active: true,
      startX: event.clientX,
      scrollLeft: rail.scrollLeft,
      moved: false,
    }
  }

  const handlePointerMove = (event) => {
    const rail = railRef.current
    const drag = dragRef.current
    if (!rail || !drag.active) return
    const delta = event.clientX - drag.startX
    if (Math.abs(delta) <= dragThreshold) return

    if (!drag.moved) {
      setDragging(true)
      drag.moved = true
      rail.setPointerCapture(event.pointerId)
    }

    event.preventDefault()
    rail.scrollLeft = drag.scrollLeft - delta
    updatePosition()
  }

  const handlePointerEnd = (event) => {
    const rail = railRef.current
    const moved = dragRef.current.moved
    if (rail?.hasPointerCapture?.(event.pointerId)) {
      rail.releasePointerCapture(event.pointerId)
    }
    if (moved) suppressClickUntilRef.current = Date.now() + 280
    setDragging(false)
    window.setTimeout(() => {
      dragRef.current.active = false
      dragRef.current.moved = false
    }, 320)
  }

  const openDetails = (event) => {
    if (dragRef.current.moved || Date.now() < suppressClickUntilRef.current) return
    onDetails(event)
  }

  return (
    <div className={styles.railShell}>
      <div className={styles.railFrame}>
        <button className={styles.railArrow} type="button" onClick={() => scroll(-1)} disabled={position === 'start'} aria-label="Eventos anteriores">‹</button>
        <div
          className={`${styles.rail} ${dragging ? styles.dragging : ''}`}
          ref={railRef}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerEnd}
          onPointerCancel={handlePointerEnd}
          onScroll={updatePosition}
          tabIndex={0}
        >
          {events.map((event) => (
            <EventCard event={event} key={event.id} onDetails={openDetails} compact />
          ))}
        </div>
        <button className={styles.railArrow} type="button" onClick={() => scroll(1)} disabled={position === 'end'} aria-label="Próximos eventos">›</button>
      </div>
    </div>
  )
}

const ExploreEventsLuppa = () => {
  const [filters, setFilters] = useState(defaultFilters)
  const toast = useToast()
  const { selectedEvent, openEventDetails, closeEventDetails, updateSelectedEvent } = useEventDetails()
  const [filteredEvents, setFilteredEvents] = useState([])
  const [featuredEvents, setFeaturedEvents] = useState([])
  const [groupedEvents, setGroupedEvents] = useState([])
  const [error, setError] = useState('')
  const hasActiveSearch = filters.query || filters.category !== 'all' || filters.sort !== 'nearest'

  useEffect(() => {
    let active = true
    const load = async () => {
      try {
        const [searchResult, featuredResult, groupedResult] = await Promise.all([
          searchEvents(filters),
          getFeaturedEvents(),
          groupEventsByCategory(),
        ])
        if (!active) return
        setFilteredEvents(searchResult)
        setFeaturedEvents(featuredResult)
        setGroupedEvents(groupedResult)
        setError('')
      } catch {
        if (active) setError('Não foi possível carregar os eventos.')
      }
    }
    load()
    return () => {
      active = false
    }
  }, [filters])

  const showInterest = async (event) => {
    try {
      await saveEvent(event.id)
      closeEventDetails()
      toast.success('Evento salvo.')
    } catch {
      toast.error('Entre na sua conta para salvar eventos.')
    }
  }

  return (
    <main className={styles.page}>
      <Seo
        title="Eventos em Barueri | Agenda Luppa"
        description="Busque eventos publicados no Luppa, filtre por categoria e encontre opções de música, cultura, gastronomia e esporte em Barueri e região."
        canonicalPath="/events"
        schema={[
          organizationSchema,
          breadcrumbSchema([
            { name: 'Início', path: '/' },
            { name: 'Eventos', path: '/events' },
          ]),
          {
            '@context': 'https://schema.org',
            '@type': 'CollectionPage',
            name: 'Eventos em Barueri',
            url: absoluteUrl('/events'),
            description: 'Agenda pública de eventos publicados no Luppa.',
          },
        ]}
      />
      <header className={styles.header}>
        <Link to="/" aria-label="Voltar para o Luppa"><LuppaSymbol /></Link>
        <nav>
          <Link to="/">← Voltar para o Luppa</Link>
          <CreatorActionLink variant="nav">Cadastrar Evento</CreatorActionLink>
          <UserMenu />
        </nav>
      </header>

      <section className={styles.hero}>
        <span>Explorar eventos</span>
        <h1>Filtre a agenda, compare opções e escolha o próximo destino.</h1>
        <p>Busca global, categorias e ordenação continuam iguais. A apresentação agora prioriza leitura rápida e seleção por contexto.</p>
      </section>

      <div className={styles.workspace}>
        <aside className={styles.filterColumn}>
          <div>
            <span>Filtros</span>
            <strong>Refine a agenda</strong>
          </div>
          <EventSearch filters={filters} onChange={setFilters} />
        </aside>

        <div className={styles.resultsColumn}>
          {error && <p className={styles.empty}>{error}</p>}
          {hasActiveSearch ? (
            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <h2>Resultados da busca</h2>
                <p>{filteredEvents.length} evento{filteredEvents.length === 1 ? '' : 's'} encontrado{filteredEvents.length === 1 ? '' : 's'}</p>
              </div>
              <div className={styles.grid}>
                {filteredEvents.map((event) => (
                  <EventCard event={event} key={event.id} onDetails={openEventDetails} />
                ))}
              </div>
              {filteredEvents.length === 0 && <p className={styles.empty}>Nenhum evento encontrado com esses filtros.</p>}
            </section>
          ) : (
            <>
              <section className={styles.section}>
                <div className={styles.sectionHeader}>
                  <h2>Eventos em destaque</h2>
                  <p>Eventos em destaque para começar a explorar.</p>
                </div>
                <EventRail events={featuredEvents} onDetails={openEventDetails} />
              </section>

              {groupedEvents.map((category) => (
                <section className={styles.section} key={category.slug}>
                  <div className={styles.sectionHeader}>
                    <h2>{category.title}</h2>
                    <Link to={`/events/category/${category.slug}`}>Ver categoria</Link>
                  </div>
                  <EventRail events={category.events} onDetails={openEventDetails} />
                </section>
              ))}
            </>
          )}
        </div>
      </div>

      <EventDetailsModal event={selectedEvent} onClose={closeEventDetails} onInterest={showInterest} onEventUpdated={updateSelectedEvent} />
    </main>
  )
}

export default ExploreEventsLuppa
