import useEventRevision from '../../hooks/useEventRevision'
import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import CreatorActionLink from '../../components/common/CreatorActionLink'
import Logo from '../../components/common/Logo'
import LuppaSymbol from '../../components/common/LuppaSymbol'
import { useToast } from '../../components/common/ToastProvider'
import UserMenu from '../../components/common/UserMenu'
import { CalendarIcon, CategoryIcon, LocationIcon } from '../../components/common/Icons'
import EventDetailsModal from '../../components/events/EventDetailsModal'
import EventImage from '../../components/events/EventImage'
import EventSearch from '../../components/events/EventSearch'
import FieldError from '../../components/forms/FieldError'
import Seo from '../../components/seo/Seo'
import { breadcrumbSchema, organizationSchema, websiteSchema } from '../../components/seo/seoMetadata'
import useEventDetails from '../../hooks/useEventDetails'
import { formatEventDateTime } from '../../services/eventDateService'
import { getCategories, getFeaturedEvents, searchEvents } from '../../services/eventService'
import { sendSupportMessage, supportErrorMessage } from '../../services/supportService'
import { isEmail } from '../../utils/validation'
import { focusFirstInvalidField } from '../../utils/focusFirstInvalidField'
import styles from './MenuLuppa.module.css'

const defaultFilters = {
  query: '',
  category: 'all',
  sort: 'nearest',
}

const SocialIcon = ({ type }) => {
  if (type === 'instagram') {
    return <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="5" /><circle cx="12" cy="12" r="4" /><circle cx="17.5" cy="6.5" r="1" /></svg>
  }

  if (type === 'facebook') {
    return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 8h3V4h-3c-3 0-5 2-5 5v3H6v4h3v5h4v-5h3l1-4h-4V9c0-.6.4-1 1-1Z" /></svg>
  }

  if (type === 'x') {
    return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 4l16 16M20 4 4 20" /></svg>
  }

  return <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m4 7 8 6 8-6" /></svg>
}

const MenuLuppa = () => {
  const eventRevision = useEventRevision()
  const location = useLocation()
  const navigate = useNavigate()
  const toast = useToast()
  const [menuOpen, setMenuOpen] = useState(false)
  const { selectedEvent, openEventDetails, closeEventDetails, updateSelectedEvent } = useEventDetails()
  const [filters, setFilters] = useState(defaultFilters)
  const [contact, setContact] = useState({ name: '', email: '', message: '' })
  const [contactErrors, setContactErrors] = useState({})
  const [contactSending, setContactSending] = useState(false)
  const [contactResult, setContactResult] = useState(null)
  const [events, setEvents] = useState([])
  const [featuredEvents, setFeaturedEvents] = useState([])
  const [categories, setCategories] = useState([])
  const contactFormRef = useRef(null)
  const featuredEvent = featuredEvents[0] || events[0]
  const agendaEvents = events.filter((event) => event.id !== featuredEvent?.id).slice(0, 4)
  const visibleCategories = categories.filter((category) => category.id !== 'all')

  useEffect(() => {
    if (location.state?.toast) {
      toast.success(location.state.toast)
      navigate(location.pathname, { replace: true, state: {} })
    }
  }, [location, navigate, toast])

  useEffect(() => {
    let active = true
    const load = async () => {
      try {
        const [nextEvents, nextCategories, nextFeaturedEvents] = await Promise.all([
          searchEvents(filters),
          getCategories(),
          getFeaturedEvents(),
        ])
        if (!active) return
        setEvents(nextEvents)
        setFeaturedEvents(nextFeaturedEvents)
        setCategories(nextCategories)
      } catch {
        if (active) toast.error('Não foi possível carregar os eventos.')
      }
    }
    load()
    return () => {
      active = false
    }
  }, [filters, eventRevision, toast])

  const handleContactSubmit = async (event) => {
    event.preventDefault()
    if (contactSending) return
    setContactResult(null)
    const nextErrors = {}

    if (!contact.name.trim()) nextErrors.name = 'Informe seu nome.'
    if (!contact.email.trim()) nextErrors.email = 'Informe seu email.'
    else if (!isEmail(contact.email)) nextErrors.email = 'Informe um email válido.'
    if (!contact.message.trim()) nextErrors.message = 'Escreva uma mensagem.'

    setContactErrors(nextErrors)

    if (Object.keys(nextErrors).length === 0) {
      setContactSending(true)
      try {
        const result = await sendSupportMessage({ ...contact, category: 'Contato', subject: 'Contato pela página inicial' })
        setContactResult({ success: true, message: result.message })
        setContact({ name: '', email: '', message: '' })
      } catch (error) {
        setContactResult({ success: false, message: supportErrorMessage(error) })
      } finally {
        setContactSending(false)
      }
    } else {
      window.requestAnimationFrame(() => focusFirstInvalidField(contactFormRef.current))
    }
  }

  const updateContact = (field, value) => {
    setContact((current) => ({ ...current, [field]: value }))
    setContactErrors((current) => ({ ...current, [field]: '' }))
  }

  return (
    <div className={styles.page}>
      <Seo
        title="Luppa | Descubra e divulgue eventos em Barueri"
        description="Explore a agenda local de Barueri, filtre eventos por categoria e divulgue experiências de música, cultura, gastronomia e esporte no Luppa."
        canonicalPath="/"
        schema={[
          organizationSchema,
          websiteSchema,
          breadcrumbSchema([{ name: 'Início', path: '/' }]),
        ]}
      />
      <header className={styles.header}>
        <nav className={styles.navbar}>
          <a className={styles.logoLink} href="#home" aria-label="Luppa">
            <LuppaSymbol />
          </a>

          <ul className={`${styles.navLinks} ${menuOpen ? styles.navLinksActive : ''}`}>
            <li><a href="#home" onClick={() => setMenuOpen(false)}>Início</a></li>
            <li><Link to="/events" onClick={() => setMenuOpen(false)}>Eventos</Link></li>
            <li><a href="#categorias" onClick={() => setMenuOpen(false)}>Categorias</a></li>
            <li><CreatorActionLink variant="nav" onClick={() => setMenuOpen(false)}>Cadastrar Evento</CreatorActionLink></li>
            <li><a href="#sobre" onClick={() => setMenuOpen(false)}>Sobre</a></li>
            <li><a href="#contato" onClick={() => setMenuOpen(false)}>Contato</a></li>
            <li className={styles.mobileAuth}><UserMenu /></li>
          </ul>

          <UserMenu className={styles.authMenu} />

          <button
            className={`${styles.menuToggle} ${menuOpen ? styles.menuToggleOpen : ''}`}
            type="button"
            aria-label="Abrir menu"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((isOpen) => !isOpen)}
          >
            <span />
            <span />
            <span />
          </button>
        </nav>
      </header>

      <main className={styles.main} id="home">
        <section className={styles.heroSection}>
          <div className={styles.heroContainer}>
            <div className={styles.heroCopy}>
              <div className={styles.locationBadge}>
                <LocationIcon className={styles.locationIcon} />
                <span>Agenda local de Barueri</span>
              </div>

              <h1 className={styles.heroTitle}>
                Encontre o próximo programa da cidade sem perder tempo.
              </h1>

              <p className={styles.heroDescription}>
                O Luppa organiza eventos de música, cultura, gastronomia e esporte em uma experiência clara para quem participa e para quem divulga.
              </p>

              <div className={styles.heroButtons}>
                <Link className={`${styles.btn} ${styles.btnPrimary}`} to="/events">
                  <CalendarIcon className={styles.btnIcon} />
                  Ver agenda
                </Link>
                <CreatorActionLink className={`${styles.btn} ${styles.btnOutline}`}>
                  Divulgar evento
                </CreatorActionLink>
              </div>
            </div>

            <aside className={styles.heroAgenda} aria-label="Resumo da agenda">
              <div className={styles.heroImageFrame}>
                <EventImage src={featuredEvent?.coverImageUrl} alt={featuredEvent?.title || 'Eventos em Barueri'} className={styles.heroImage} fallbackLabel={featuredEvent?.categoryName || 'Agenda Luppa'} />
              </div>
              <div className={styles.agendaHeader}>
                <span>Em destaque</span>
                <strong>{featuredEvent?.title || 'Agenda Luppa'}</strong>
              </div>
              {featuredEvent && (
                <button className={styles.featureButton} type="button" onClick={() => openEventDetails(featuredEvent)}>
                  Ver detalhes do destaque
                </button>
              )}
            </aside>
          </div>

          <div className={styles.heroSearch}>
            <EventSearch filters={filters} onChange={setFilters} />
          </div>
        </section>

        <section id="eventos" className={styles.eventsSection}>
          <div className={styles.sectionContainer}>
            <div className={styles.editorialHeader}>
              <span>Próximos eventos</span>
              <h2>Uma agenda pensada para decisão rápida.</h2>
              <p>Compare data, local, categoria e interesse sem depender de uma grade de cards iguais.</p>
            </div>

            {events.length > 0 ? (
              <div className={styles.eventEditorial}>
                {featuredEvent && (
                  <article className={styles.featuredEvent}>
                    <EventImage src={featuredEvent.coverImageUrl} alt={featuredEvent.title} className={styles.featuredEventImage} fallbackLabel={featuredEvent.categoryName} />
                    <div>
                      <span>{featuredEvent.categoryName}</span>
                      <h3>{featuredEvent.title}</h3>
                      <p>{featuredEvent.description}</p>
                      <button type="button" onClick={() => openEventDetails(featuredEvent)}>Abrir detalhes</button>
                    </div>
                  </article>
                )}

                <div className={styles.eventList}>
                  {agendaEvents.map((event) => (
                    <button className={styles.eventRow} type="button" key={event.id} onClick={() => openEventDetails(event)}>
                      <span className={styles.eventDate}>{formatEventDateTime(event.startDate, event.startTime)}</span>
                      <span className={styles.eventName}>{event.title}</span>
                      <span className={styles.eventMeta}>{event.categoryName} · {event.locationName}</span>
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <p className={styles.emptyState}>Nenhum evento encontrado com esses filtros.</p>
            )}
          </div>
        </section>

        <section id="categorias" className={styles.categoriesSection}>
          <div className={styles.sectionContainer}>
            <div className={styles.categoryIndex}>
              <div className={styles.editorialHeader}>
                <span>Categorias</span>
                <h2>Entre pelo tipo de experiência.</h2>
              </div>

              <div className={styles.categoryList}>
                {visibleCategories.map((category, index) => (
                  <Link
                    className={styles.categoryRow}
                    key={category.id}
                    to={`/events/category/${category.slug}`}
                  >
                    <span>{String(index + 1).padStart(2, '0')}</span>
                    <CategoryIcon type={category.icon} />
                    <strong>{category.title}</strong>
                    <em>{category.count} evento{category.count === 1 ? '' : 's'}</em>
                  </Link>
                ))}
              </div>
            </div>
          </div>
        </section>

        <section id="cadastro" className={styles.createSection}>
          <div className={styles.createContent}>
            <h2>Organize seu próprio evento</h2>
            <p>Alcance milhares de pessoas em Barueri e região. Cadastre seu evento de forma rápida e gratuita.</p>
            <CreatorActionLink className={styles.registerBtn}>Cadastrar Meu Evento</CreatorActionLink>
          </div>
        </section>

        <section id="sobre" className={styles.aboutSection}>
          <div className={styles.sectionContainer}>
            <div className={styles.sectionHeader}>
              <h2>Sobre o Luppa</h2>
              <p>Uma vitrine local para descobrir, divulgar e acompanhar eventos em Barueri.</p>
            </div>
            <div className={styles.aboutGrid}>
              <article>
                <h3>Para moradores</h3>
                <p>Encontre atividades próximas, filtre por categoria e veja informações importantes antes de sair de casa.</p>
              </article>
              <article>
                <h3>Para organizadores</h3>
                <p>Divulgue eventos para a comunidade e reúna informações em uma página simples, clara e fácil de acessar.</p>
              </article>
              <article>
                <h3>Para a cidade</h3>
                <p>Fortaleça a circulação cultural, esportiva e gastronômica conectando pessoas aos espaços de Barueri.</p>
              </article>
            </div>
          </div>
        </section>

        <section id="contato" className={styles.contactSection}>
          <div className={styles.sectionContainer}>
            <div className={styles.contactGrid}>
              <div>
                <h2>Contato</h2>
                <p>Fale com a equipe do Luppa para tirar dúvidas, sugerir melhorias ou pedir apoio com divulgação.</p>
                <div className={styles.contactList}>
                  <span>Email: projetoluppa@gmail.com</span>
                  <span>Atendimento: segunda a sexta, 9h às 18h</span>
                  <span>Região: Barueri e Alphaville</span>
                </div>
              </div>
              <form className={styles.contactForm} noValidate ref={contactFormRef} onSubmit={handleContactSubmit} aria-busy={contactSending}>
                {contactResult && <p role={contactResult.success ? "status" : "alert"}>{contactResult.message}</p>}
                <label>
                  Nome
                  <input type="text" maxLength={120} disabled={contactSending} value={contact.name} placeholder="Seu nome" aria-invalid={Boolean(contactErrors.name)} aria-describedby={contactErrors.name ? 'contact-name-error' : undefined} onChange={(event) => updateContact('name', event.target.value)} />
                  <FieldError id="contact-name-error" message={contactErrors.name} />
                </label>
                <label>
                  Email
                  <input type="email" maxLength={120} disabled={contactSending} value={contact.email} placeholder="seuemail@email.com" aria-invalid={Boolean(contactErrors.email)} aria-describedby={contactErrors.email ? 'contact-email-error' : undefined} onChange={(event) => updateContact('email', event.target.value)} />
                  <FieldError id="contact-email-error" message={contactErrors.email} />
                </label>
                <label>
                  Mensagem
                  <textarea maxLength={4000} disabled={contactSending} value={contact.message} placeholder="Escreva sua mensagem" rows="4" aria-invalid={Boolean(contactErrors.message)} aria-describedby={contactErrors.message ? 'contact-message-error' : undefined} onChange={(event) => updateContact('message', event.target.value)} />
                  <FieldError id="contact-message-error" message={contactErrors.message} />
                </label>
                <button type="submit" disabled={contactSending}>{contactSending ? 'Enviando...' : 'Enviar mensagem'}</button>
              </form>
            </div>
          </div>
        </section>
      </main>

      <footer className={styles.footer}>
        <div className={styles.footerContainer}>
          <div className={styles.footerBrand}>
            <Logo variant="light" />
            <p className={styles.footerDescription}>
              Conectando a comunidade de Barueri aos melhores eventos de música, arte, gastronomia, esportes e cultura.
            </p>
            <div className={styles.socialIcons}>
              <a href="https://www.instagram.com/" aria-label="Instagram"><SocialIcon type="instagram" /></a>
              <a href="https://www.facebook.com/" aria-label="Facebook"><SocialIcon type="facebook" /></a>
              <a href="https://x.com/" aria-label="X"><SocialIcon type="x" /></a>
              <a href="mailto:projetoluppa@gmail.com" aria-label="Email"><SocialIcon type="email" /></a>
            </div>
          </div>

          <div className={styles.footerLinks}>
            <div className={styles.footerColumn}>
              <h3>Links Rápidos</h3>
              <ul>
                <li><a href="#home">Início</a></li>
                <li><Link to="/events">Eventos</Link></li>
                <li><a href="#categorias">Categorias</a></li>
                <li><CreatorActionLink>Para Organizadores</CreatorActionLink></li>
                <li><a href="#sobre">Sobre Nós</a></li>
              </ul>
            </div>

            <div className={styles.footerColumn}>
              <h3>Suporte</h3>
              <ul>
                <li><a href="#contato">Contato</a></li>
                <li><Link to="/faq">FAQ</Link></li>
                <li><Link to="/privacy">Privacidade</Link></li>
                <li><Link to="/terms">Termos de Uso</Link></li>
              </ul>
            </div>
          </div>
        </div>

        <div className={styles.footerBottom}>
          <p>© 2026 Luppa. Todos os direitos reservados. Feito em Barueri.</p>
        </div>
      </footer>

      <EventDetailsModal
        event={selectedEvent}
        onClose={closeEventDetails}
        onEventUpdated={updateSelectedEvent}

      />
    </div>
  )
}

export default MenuLuppa
