import { useState } from 'react'
import { Link } from 'react-router-dom'
import heroEvent from '../../assets/hero-event.png'
import eventMusic from '../../assets/event-music.png'
import eventGastronomy from '../../assets/event-gastronomy.png'
import eventCulture from '../../assets/event-culture.png'
import eventSports from '../../assets/event-sports.png'
import styles from './MenuLuppa.module.css'

const events = [
  {
    title: 'Festival de Jazz ao Ar Livre',
    category: 'Música',
    image: eventMusic,
    date: '15 de Maio, 2025 - 19h',
    place: 'Parque Municipal Dom José',
    participants: '324 participantes',
    description: 'Uma noite de jazz ao ar livre com artistas locais, praça de alimentação e programação para toda a família.',
    mapHint: 'Parque Municipal Dom José fica na região central de Barueri, próximo a vias de fácil acesso.',
  },
  {
    title: 'Festival Gastronômico de Barueri',
    category: 'Gastronomia',
    image: eventGastronomy,
    date: '22 de Maio, 2025 - 12h',
    place: 'Praça das Bandeiras',
    participants: '567 participantes',
    description: 'Barracas, chefs convidados e sabores de diferentes regiões reunidos em um encontro gastronômico no centro da cidade.',
    mapHint: 'Praça das Bandeiras fica no centro de Barueri, em uma área com circulação de pedestres e transporte público.',
  },
  {
    title: 'Exposição de Arte Contemporânea',
    category: 'Cultura',
    image: eventCulture,
    date: '29 de Maio, 2025 - 14h',
    place: 'Centro Cultural',
    participants: '189 participantes',
    description: 'Mostra com obras de artistas independentes, instalações visuais e visitação guiada durante a tarde.',
    mapHint: 'Centro Cultural de Barueri, ponto de referência para exposições, cursos e apresentações culturais.',
  },
  {
    title: 'Corrida Solidária 5KM Alphaville',
    category: 'Esportes',
    image: eventSports,
    date: '05 de Junho, 2025 - 7h',
    place: 'Parque Ecológico',
    participants: '421 participantes',
    description: 'Corrida de 5 km com inscrição solidária, percurso sinalizado e ponto de apoio para participantes.',
    mapHint: 'Parque Ecológico, área aberta indicada para atividades esportivas, caminhada e eventos ao ar livre.',
  },
]

const categories = [
  { title: 'Todos', count: `${events.length} eventos`, className: styles.categoryAll, icon: 'all' },
  { title: 'Música', count: '45 eventos', className: styles.categoryMusic, icon: 'music' },
  {
    title: 'Gastronomia',
    count: '32 eventos',
    className: styles.categoryGastronomy,
    icon: 'food',
  },
  {
    title: 'Arte & Cultura',
    count: '28 eventos',
    className: styles.categoryCulture,
    icon: 'culture',
  },
  {
    title: 'Esportes',
    count: '41 eventos',
    className: styles.categorySports,
    icon: 'sports',
  },
]

const CalendarIcon = ({ className }) => (
  <svg className={className} width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
    <line x1="16" y1="2" x2="16" y2="6" />
    <line x1="8" y1="2" x2="8" y2="6" />
    <line x1="3" y1="10" x2="21" y2="10" />
  </svg>
)

const LocationIcon = ({ className }) => (
  <svg className={className} width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
    <circle cx="12" cy="10" r="3" />
  </svg>
)

const UsersIcon = ({ className }) => (
  <svg className={className} width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
    <circle cx="9" cy="7" r="4" />
    <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
  </svg>
)

const CategoryIcon = ({ type }) => {
  const props = {
    width: 40,
    height: 40,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
  }

  if (type === 'food') {
    return (
      <svg {...props}>
        <path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2" />
        <path d="M7 2v20" />
        <path d="M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7" />
      </svg>
    )
  }

  if (type === 'culture') {
    return (
      <svg {...props}>
        <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
        <line x1="8" y1="21" x2="16" y2="21" />
        <line x1="12" y1="17" x2="12" y2="21" />
      </svg>
    )
  }

  if (type === 'sports') {
    return (
      <svg {...props}>
        <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6" />
        <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18" />
        <path d="M4 22h16" />
        <path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22" />
        <path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22" />
        <path d="M18 2H6v7a6 6 0 0 0 12 0V2Z" />
      </svg>
    )
  }

  if (type === 'all') {
    return (
      <svg {...props}>
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </svg>
    )
  }

  return (
    <svg {...props}>
      <path d="M9 18V5l12-2v13" />
      <circle cx="6" cy="18" r="3" />
      <circle cx="18" cy="16" r="3" />
    </svg>
  )
}

const SocialIcon = ({ type }) => {
  if (type === 'instagram') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <rect x="3" y="3" width="18" height="18" rx="5" />
        <circle cx="12" cy="12" r="4" />
        <circle cx="17.5" cy="6.5" r="1" />
      </svg>
    )
  }

  if (type === 'facebook') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M14 8h3V4h-3c-3 0-5 2-5 5v3H6v4h3v5h4v-5h3l1-4h-4V9c0-.6.4-1 1-1Z" />
      </svg>
    )
  }

  if (type === 'x') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 4l16 16M20 4 4 20" />
      </svg>
    )
  }

  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="m4 7 8 6 8-6" />
    </svg>
  )
}

const MenuLuppa = () => {
  const [menuOpen, setMenuOpen] = useState(false)
  const [selectedEvent, setSelectedEvent] = useState(null)
  const [selectedCategory, setSelectedCategory] = useState('Todos')
  const [feedback, setFeedback] = useState('')
  const filteredEvents = selectedCategory === 'Todos'
    ? events
    : events.filter((event) => event.category === selectedCategory || (selectedCategory === 'Arte & Cultura' && event.category === 'Cultura'))
  const handleCategorySelect = (category) => {
    setSelectedCategory(category)
  }
  const showFeedback = (message) => {
    setFeedback(message)
    window.setTimeout(() => setFeedback(''), 2600)
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <nav className={styles.navbar}>
          <a className={styles.logo} href="#home" aria-label="Luppa">
            <span className={styles.logoIcon}>L</span>
            <span>Luppa</span>
          </a>

          <ul className={`${styles.navLinks} ${menuOpen ? styles.navLinksActive : ''}`}>
            <li><a href="#home">Início</a></li>
            <li><a href="#eventos">Eventos</a></li>
            <li><a href="#categorias">Categorias</a></li>
            <li><Link to="/cadastrar-evento">Cadastrar Evento</Link></li>
            <li><a href="#sobre">Sobre</a></li>
            <li><a href="#contato">Contato</a></li>
          </ul>

          <Link className={styles.loginBtn} to="/login-cadastro">Login/Cadastro</Link>

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
          <div className={styles.heroBackground}>
            <img src={heroEvent} alt="Eventos vibrantes em Barueri" className={styles.heroImage} />
            <div className={styles.heroOverlay} />
          </div>

          <div className={styles.heroContainer}>
            <div className={styles.heroContent}>
              <div className={styles.locationBadge}>
                <LocationIcon className={styles.locationIcon} />
                <span>Barueri, SP</span>
              </div>

              <h1 className={styles.heroTitle}>
                Descubra o que acontece em <span className={styles.heroHighlight}>Barueri</span> com o Luppa
              </h1>

              <p className={styles.heroDescription}>
                Música, arte, gastronomia, esportes e cultura. Tudo em um só lugar.
              </p>

              <div className={styles.heroButtons}>
                <a className={`${styles.btn} ${styles.btnPrimary}`} href="#eventos">
                  <CalendarIcon className={styles.btnIcon} />
                  Explorar Eventos
                </a>
                <Link className={`${styles.btn} ${styles.btnOutline}`} to="/cadastrar-evento">
                  Cadastrar Evento
                </Link>
              </div>

              <div className={styles.heroStats}>
                <div className={styles.statItem}>
                  <div className={styles.statNumber}>150+</div>
                  <div className={styles.statLabel}>Eventos/mês</div>
                </div>
                <div className={styles.statItem}>
                  <div className={styles.statNumber}>50+</div>
                  <div className={styles.statLabel}>Organizadores</div>
                </div>
                <div className={styles.statItem}>
                  <div className={styles.statNumber}>10k+</div>
                  <div className={styles.statLabel}>Participantes</div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section id="eventos" className={styles.eventsSection}>
          <div className={styles.sectionContainer}>
            <div className={styles.sectionHeader}>
              <h2>Próximos Eventos</h2>
              <p>Descubra experiências incríveis acontecendo agora em Barueri</p>
            </div>

            <div className={styles.eventsGrid}>
              {filteredEvents.map((event) => (
                <article className={styles.eventCard} key={event.title}>
                  <div className={styles.eventImageWrapper}>
                    <img src={event.image} alt={event.title} className={styles.eventImage} />
                    <span className={styles.eventCategory}>{event.category}</span>
                  </div>
                  <div className={styles.eventContent}>
                    <h3>{event.title}</h3>
                    <div className={styles.eventInfo}>
                      <CalendarIcon className={styles.eventIcon} />
                      <span>{event.date}</span>
                    </div>
                    <div className={styles.eventInfo}>
                      <LocationIcon className={styles.eventIcon} />
                      <span>{event.place}</span>
                    </div>
                    <div className={styles.eventInfo}>
                      <UsersIcon className={styles.eventIcon} />
                      <span>{event.participants}</span>
                    </div>
                    <button
                      className={styles.eventDetailsBtn}
                      type="button"
                      onClick={() => setSelectedEvent(event)}
                    >
                      Ver Detalhes
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section id="categorias" className={styles.categoriesSection}>
          <div className={styles.sectionContainer}>
            <div className={styles.sectionHeader}>
              <h2>Explore por Categoria</h2>
              <p>Encontre eventos do seu interesse</p>
            </div>

            <div className={styles.categoriesGrid}>
              {categories.map((category) => (
                <button
                  className={`${styles.categoryCard} ${category.className} ${selectedCategory === category.title ? styles.categoryActive : ''}`}
                  key={category.title}
                  type="button"
                  onClick={() => handleCategorySelect(category.title)}
                >
                  <div className={styles.categoryIcon}>
                    <CategoryIcon type={category.icon} />
                  </div>
                  <h3>{category.title}</h3>
                  <p>{category.count}</p>
                </button>
              ))}
            </div>
            <p className={styles.filterFeedback}>
              Mostrando {filteredEvents.length} evento{filteredEvents.length === 1 ? '' : 's'} em {selectedCategory}.
            </p>
          </div>
        </section>

        <section id="cadastro" className={styles.createSection}>
          <div className={styles.createContent}>
            <h2>Organize seu próprio evento</h2>
            <p>Alcance milhares de pessoas em Barueri e região. Cadastre seu evento de forma rápida e gratuita.</p>
            <Link to="/cadastrar-evento" className={styles.registerBtn}>Cadastrar Meu Evento</Link>
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
                  <span>Email: contato@luppa.com</span>
                  <span>Atendimento: segunda a sexta, 9h às 18h</span>
                  <span>Região: Barueri e Alphaville</span>
                </div>
              </div>
              <form className={styles.contactForm}>
                <label>
                  Nome
                  <input type="text" placeholder="Seu nome" />
                </label>
                <label>
                  Email
                  <input type="email" placeholder="seuemail@email.com" />
                </label>
                <label>
                  Mensagem
                  <textarea placeholder="Escreva sua mensagem" rows="4" />
                </label>
                <button type="button" onClick={() => showFeedback('Mensagem enviada com sucesso!')}>
                  Enviar mensagem
                </button>
              </form>
            </div>
          </div>
        </section>
      </main>

      <footer className={styles.footer}>
        <div className={styles.footerContainer}>
          <div className={styles.footerBrand}>
            <div className={styles.footerLogo}>
              <span className={styles.footerLogoIcon}>L</span>
              <span className={styles.logoText}>Luppa</span>
            </div>
            <p className={styles.footerDescription}>
              Conectando a comunidade de Barueri aos melhores eventos de música, arte, gastronomia, esportes e cultura.
            </p>
            <div className={styles.socialIcons}>
              <a href="https://www.instagram.com/" aria-label="Instagram">
                <SocialIcon type="instagram" />
              </a>
              <a href="https://www.facebook.com/" aria-label="Facebook">
                <SocialIcon type="facebook" />
              </a>
              <a href="https://x.com/" aria-label="X">
                <SocialIcon type="x" />
              </a>
              <a href="mailto:contato@luppa.com" aria-label="Email">
                <SocialIcon type="email" />
              </a>
            </div>
          </div>

          <div className={styles.footerLinks}>
            <div className={styles.footerColumn}>
              <h3>Links Rápidos</h3>
              <ul>
                <li><a href="#home">Início</a></li>
                <li><a href="#eventos">Eventos</a></li>
                <li><a href="#categorias">Categorias</a></li>
                <li><Link to="/cadastrar-evento">Para Organizadores</Link></li>
                <li><a href="#sobre">Sobre Nós</a></li>
              </ul>
            </div>

            <div className={styles.footerColumn}>
              <h3>Suporte</h3>
              <ul>
                <li><a href="#contato">Contato</a></li>
                <li><a href="#faq">FAQ</a></li>
                <li><a href="#privacidade">Privacidade</a></li>
                <li><a href="#termos">Termos de Uso</a></li>
              </ul>
            </div>
          </div>
        </div>

        <div className={styles.footerBottom}>
          <p>© 2025 Luppa. Todos os direitos reservados. Feito em Barueri.</p>
        </div>
      </footer>

      {selectedEvent && (
        <div
          className={styles.modalBackdrop}
          role="dialog"
          aria-modal="true"
          aria-labelledby="event-details-title"
          onClick={() => setSelectedEvent(null)}
        >
          <div className={styles.modal} onClick={(event) => event.stopPropagation()}>
            <button
              className={styles.modalClose}
              type="button"
              aria-label="Fechar detalhes"
              onClick={() => setSelectedEvent(null)}
            >
              ×
            </button>
            <img src={selectedEvent.image} alt={selectedEvent.title} className={styles.modalImage} />
            <div className={styles.modalContent}>
              <span className={styles.eventCategory}>{selectedEvent.category}</span>
              <div className={styles.detailBlock}>
                <span>Título</span>
                <h2 id="event-details-title">{selectedEvent.title}</h2>
              </div>
              <div className={styles.detailBlock}>
                <span>Descrição</span>
                <p>{selectedEvent.description}</p>
              </div>
              <div className={styles.modalInfo}>
                <div className={styles.detailRow}>
                  <CalendarIcon className={styles.eventIcon} />
                  <span><strong>Data e horário</strong>{selectedEvent.date}</span>
                </div>
                <div className={styles.detailRow}>
                  <LocationIcon className={styles.eventIcon} />
                  <span><strong>Local</strong>{selectedEvent.place}</span>
                </div>
                <div className={styles.detailRow}>
                  <UsersIcon className={styles.eventIcon} />
                  <span><strong>Participantes</strong>{selectedEvent.participants}</span>
                </div>
              </div>
              <div className={styles.mapPreview}>
                <div className={styles.mapPin}>
                  <LocationIcon />
                </div>
                <div className={styles.mapRoadOne} />
                <div className={styles.mapRoadTwo} />
                <div className={styles.mapLabel}>
                  <strong>Mapa do local</strong>
                  <span>{selectedEvent.mapHint}</span>
                </div>
              </div>
              <button
                className={styles.modalAction}
                type="button"
                onClick={() => {
                  setSelectedEvent(null)
                  showFeedback('Interesse registrado!')
                }}
              >
                Tenho interesse
              </button>
            </div>
          </div>
        </div>
      )}
      {feedback && <div className={styles.toast}>{feedback}</div>}
    </div>
  )
}

export default MenuLuppa
