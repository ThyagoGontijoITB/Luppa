import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
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
import { getCategoryBySlug, getEventsByCategorySlug, saveEvent, searchEvents } from '../../services/eventService'
import styles from './CategoryEventsLuppa.module.css'

const CategoryEventsLuppa = () => {
  const { slug } = useParams()
  const [category, setCategory] = useState(null)
  const [events, setEvents] = useState([])
  const [error, setError] = useState('')
  const [filters, setFilters] = useState({ query: '', category: slug || 'all', sort: 'nearest' })
  const { selectedEvent, openEventDetails, closeEventDetails, updateSelectedEvent } = useEventDetails()
  const toast = useToast()
  const categoryTitle = category?.title || 'Categoria de eventos'
  const categoryDescription = category
    ? `Encontre eventos de ${category.title.toLowerCase()} publicados no Luppa em Barueri e região.`
    : 'Confira eventos publicados no Luppa por categoria em Barueri e região.'

  useEffect(() => {
    let active = true
    const load = async () => {
      try {
        const nextCategory = await getCategoryBySlug(slug)
        const nextEvents = nextCategory && (filters.query || filters.sort !== 'nearest' || filters.category !== slug)
          ? await searchEvents(filters)
          : nextCategory ? await getEventsByCategorySlug(slug) : []
        if (!active) return
        setCategory(nextCategory)
        setEvents(nextEvents)
        setError('')
      } catch {
        if (!active) return
        setCategory(null)
        setEvents([])
        setError('Não foi possível carregar esta categoria.')
      }
    }
    load()
    return () => {
      active = false
    }
  }, [filters, slug])

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
        title={`${categoryTitle} | Eventos no Luppa`}
        description={categoryDescription}
        canonicalPath={`/events/category/${slug}`}
        noindex={!category}
        schema={[
          organizationSchema,
          breadcrumbSchema([
            { name: 'Início', path: '/' },
            { name: 'Eventos', path: '/events' },
            { name: categoryTitle, path: `/events/category/${slug}` },
          ]),
          category && {
            '@context': 'https://schema.org',
            '@type': 'CollectionPage',
            name: `${category.title} no Luppa`,
            url: absoluteUrl(`/events/category/${slug}`),
            description: categoryDescription,
          },
        ]}
      />
      <header className={styles.header}>
        <Link to="/" aria-label="Voltar para o Luppa"><LuppaSymbol /></Link>
        <nav>
          <Link to="/events">Explorar eventos</Link>
          <CreatorActionLink variant="nav">Cadastrar Evento</CreatorActionLink>
          <UserMenu />
        </nav>
      </header>

      <section className={styles.hero}>
        <Link to="/events">← Voltar para eventos</Link>
        <h1>{category?.title || 'Categoria não encontrada'}</h1>
        <p>{category ? `${events.length} evento${events.length === 1 ? '' : 's'} disponíve${events.length === 1 ? 'l' : 'is'} nesta categoria.` : error || 'A categoria informada não existe.'}</p>
      </section>

      {category && <EventSearch filters={filters} onChange={setFilters} />}

      {category ? (
        <>
          <section className={styles.grid}>
            {events.map((event) => (
              <EventCard event={event} key={event.id} onDetails={openEventDetails} />
            ))}
          </section>
          {events.length === 0 && <p className={styles.empty}>Não há eventos nesta categoria ainda. Tente alterar sua busca ou filtros.</p>}
        </>
      ) : (
        <p className={styles.empty}>Confira todas as categorias disponíveis na página de eventos.</p>
      )}

      <EventDetailsModal event={selectedEvent} onClose={closeEventDetails} onInterest={showInterest} onEventUpdated={updateSelectedEvent} />
    </main>
  )
}

export default CategoryEventsLuppa
