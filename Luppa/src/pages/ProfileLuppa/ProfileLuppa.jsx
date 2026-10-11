import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../../auth/authContext'
import Avatar from '../../components/common/Avatar'
import LuppaSymbol from '../../components/common/LuppaSymbol'
import StarRating from '../../components/common/StarRating'
import { useToast } from '../../components/common/ToastProvider'
import EventCard from '../../components/events/EventCard'
import EventDetailsModal from '../../components/events/EventDetailsModal'
import FieldError from '../../components/forms/FieldError'
import useEventDetails from '../../hooks/useEventDetails'
import { updateCurrentUser, uploadCurrentUserImage } from '../../services/authService'
import { getCommunicationPreferences, updateCommunicationPreferences } from '../../services/accountService'
import { getEventStart, getEventEnd } from '../../services/eventDateService'
import { getCategories, getMyEvents, getMyRatedEvents, getSavedEvents, unsaveEvent } from '../../services/eventService'
import { validateCoverImage } from '../../utils/imageValidation'
import { formatBrazilianPhone, isEmail, isPhone, isValidPastDate, todayDateInputValue } from '../../utils/validation'
import { focusFirstInvalidField } from '../../utils/focusFirstInvalidField'
import { userRoles } from '../../constants/users'
import styles from './ProfileLuppa.module.css'

const defaultSavedFilters = { query: '', category: 'all', period: 'all', sort: 'nearest' }
const defaultReviewFilters = { query: '', category: 'all', rating: 'all', sort: 'recent' }

const ProfileLuppa = () => {
  const auth = useAuth()
  const toast = useToast()
  const location = useLocation()
  const [activeTab, setActiveTab] = useState(location.pathname === '/communication-preferences' ? 'preferences' : 'profile')
  const { selectedEvent, openEventDetails, closeEventDetails, updateSelectedEvent } = useEventDetails()
  const [savedFilters, setSavedFilters] = useState(defaultSavedFilters)
  const [reviewFilters, setReviewFilters] = useState(defaultReviewFilters)
  const [savedSourceEvents, setSavedSourceEvents] = useState([])
  const [evaluatedSourceEvents, setEvaluatedSourceEvents] = useState([])
  const [ownEvents, setOwnEvents] = useState([])
  const [categories, setCategories] = useState([])
  const [error, setError] = useState('')
  const [avatarPreview, setAvatarPreview] = useState('')
  const [avatarFile, setAvatarFile] = useState(null)
  const [avatarError, setAvatarError] = useState('')
  const [editingProfile, setEditingProfile] = useState(false)
  const [profileForm, setProfileForm] = useState({
    name: auth.name || '',
    email: auth.email || '',
    birthDate: auth.user?.birthDate || '',
    phone: auth.user?.phone || '',
  })
  const [profileErrors, setProfileErrors] = useState({})
  const [profileSaving, setProfileSaving] = useState(false)
  const [preferences, setPreferences] = useState({
    marketingEmails: false,
    luppaNews: false,
    eventRecommendations: false,
    savedEventReminders: false,
  })
  const [preferencesSaving, setPreferencesSaving] = useState(false)
  const [removingSavedEventId, setRemovingSavedEventId] = useState(null)
  const profileFormRef = useRef(null)
  const canCreateEvents = [userRoles.CREATOR, userRoles.ADMIN].includes(auth.role)
  const errorId = (field) => `profile-${field}-error`

  useEffect(() => {
    setProfileForm({
      name: auth.name || '',
      email: auth.email || '',
      birthDate: auth.user?.birthDate || '',
      phone: auth.user?.phone || '',
    })
  }, [auth.name, auth.email, auth.user?.birthDate, auth.user?.phone])

  useEffect(() => {
    let active = true
    getCommunicationPreferences()
      .then((nextPreferences) => {
        if (active) setPreferences({
          marketingEmails: Boolean(nextPreferences.marketingEmails),
          luppaNews: Boolean(nextPreferences.luppaNews),
          eventRecommendations: Boolean(nextPreferences.eventRecommendations),
          savedEventReminders: Boolean(nextPreferences.savedEventReminders),
        })
      })
      .catch(() => {
        if (active) setError('Não foi possível carregar preferências de comunicação.')
      })
    return () => {
      active = false
    }
  }, [auth.userId])

  useEffect(() => {
    let active = true
    const load = async () => {
      try {
        const [saved, ratings, mine, categoryItems] = await Promise.all([
          getSavedEvents(),
          getMyRatedEvents(),
          canCreateEvents ? getMyEvents() : Promise.resolve([]),
          getCategories(),
        ])
        if (!active) return
        setSavedSourceEvents(saved)
        setEvaluatedSourceEvents(ratings)
        setOwnEvents(mine)
        setCategories(categoryItems.filter((category) => category.id !== 'all'))
        setError('')
      } catch {
        if (active) setError('Não foi possível carregar os dados do perfil.')
      }
    }
    load()
    return () => {
      active = false
    }
  }, [auth.userId, canCreateEvents])

  const savedTerm = savedFilters.query.trim().toLowerCase()
  const savedEvents = savedSourceEvents
    .filter((event) => !savedTerm || [event.title, event.locationName, event.categoryName].some((value) => value.toLowerCase().includes(savedTerm)))
    .filter((event) => savedFilters.category === 'all' || event.categorySlug === savedFilters.category)
    .filter((event) => {
      const ended = getEventEnd(event) < new Date()
      if (savedFilters.period === 'upcoming') return !ended
      if (savedFilters.period === 'past') return ended
      return true
    })
    .sort((first, second) => {
      if (savedFilters.sort === 'furthest') return getEventStart(second) - getEventStart(first)
      if (savedFilters.sort === 'az') return first.title.localeCompare(second.title, 'pt-BR')
      if (savedFilters.sort === 'saved') return new Date(second.savedAt || 0) - new Date(first.savedAt || 0)
      return getEventStart(first) - getEventStart(second)
    })

  const reviewTerm = reviewFilters.query.trim().toLowerCase()
  const evaluatedEvents = evaluatedSourceEvents
    .filter((event) => event.userReview)
    .filter((event) => !reviewTerm || [event.title, event.locationName, event.categoryName].some((value) => value.toLowerCase().includes(reviewTerm)))
    .filter((event) => reviewFilters.category === 'all' || event.categorySlug === reviewFilters.category)
    .filter((event) => reviewFilters.rating === 'all' || event.userReview.rating === Number(reviewFilters.rating))
    .sort((first, second) => {
      if (reviewFilters.sort === 'best') return second.userReview.rating - first.userReview.rating
      if (reviewFilters.sort === 'worst') return first.userReview.rating - second.userReview.rating
      if (reviewFilters.sort === 'eventDate') return getEventStart(second) - getEventStart(first)
      return second.id.localeCompare(first.id)
    })

  const handleAvatarChange = (event) => {
    const file = event.target.files?.[0]
    const error = validateCoverImage(file)
    setAvatarError(error)
    if (avatarPreview) URL.revokeObjectURL(avatarPreview)
    setAvatarFile(error ? null : file || null)
    setAvatarPreview(error || !file ? '' : URL.createObjectURL(file))
  }

  const updateProfileField = (field, value) => {
    const nextValue = field === 'phone' ? formatBrazilianPhone(value) : value
    setProfileForm((current) => ({ ...current, [field]: nextValue }))
    setProfileErrors((current) => ({ ...current, [field]: '' }))
  }

  const validateProfile = () => {
    const nextErrors = {}
    if (!profileForm.name.trim()) nextErrors.name = 'Informe seu nome.'
    if (!profileForm.email.trim()) nextErrors.email = 'Informe seu email.'
    else if (!isEmail(profileForm.email)) nextErrors.email = 'Informe um email válido.'
    if (!profileForm.birthDate) nextErrors.birthDate = 'Informe sua data de nascimento.'
    else if (!isValidPastDate(profileForm.birthDate)) nextErrors.birthDate = 'A data de nascimento deve ser anterior a hoje.'
    if (!profileForm.phone.trim()) nextErrors.phone = 'Informe seu telefone.'
    else if (!isPhone(profileForm.phone)) nextErrors.phone = 'Informe um telefone válido com DDD.'
    return nextErrors
  }

  const saveProfile = async (event) => {
    event.preventDefault()
    const nextErrors = validateProfile()
    setProfileErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      window.requestAnimationFrame(() => focusFirstInvalidField(profileFormRef.current))
      return
    }

    setProfileSaving(true)
    try {
      const profileResult = await updateCurrentUser(profileForm)
      let updatedUser = profileResult.user
      auth.setAuthenticatedSession(profileResult.user, profileResult.token)
      if (avatarFile) {
        updatedUser = await uploadCurrentUserImage(avatarFile)
      }
      auth.setAuthenticatedUser(updatedUser)
      setEditingProfile(false)
      setAvatarFile(null)
      if (avatarPreview) URL.revokeObjectURL(avatarPreview)
      setAvatarPreview('')
      setAvatarError('')
      setError('')
      toast.success('Alterações salvas com sucesso.')
    } catch {
      setError('Não foi possível salvar as alterações do perfil.')
      toast.error('Não foi possível salvar as alterações.')
    } finally {
      setProfileSaving(false)
    }
  }

  const cancelProfileEdit = () => {
    setProfileForm({
      name: auth.name || '',
      email: auth.email || '',
      birthDate: auth.user?.birthDate || '',
      phone: auth.user?.phone || '',
    })
    setProfileErrors({})
    setEditingProfile(false)
    setAvatarFile(null)
    setAvatarError('')
    if (avatarPreview) URL.revokeObjectURL(avatarPreview)
    setAvatarPreview('')
  }

  const removeSavedEvent = async (eventId) => {
    if (removingSavedEventId) return
    setRemovingSavedEventId(eventId)
    try {
      await unsaveEvent(eventId)
      setSavedSourceEvents((current) => current.filter((event) => event.id !== eventId))
      setError('')
      toast.success('Evento removido dos salvos.')
    } catch {
      setError('Não foi possível remover o evento salvo.')
      toast.error('Não foi possível remover o evento salvo.')
    } finally {
      setRemovingSavedEventId(null)
    }
  }

  const tabs = [
    { id: 'profile', label: 'Perfil' },
    { id: 'preferences', label: 'Comunicações' },
    { id: 'saved', label: 'Eventos salvos' },
    { id: 'reviews', label: 'Eventos avaliados' },
    ...(canCreateEvents ? [{ id: 'mine', label: 'Meus eventos' }] : []),
  ]

  const togglePreference = (field) => {
    setPreferences((current) => ({ ...current, [field]: !current[field] }))
  }

  const savePreferences = async () => {
    setPreferencesSaving(true)
    try {
      const saved = await updateCommunicationPreferences(preferences)
      setPreferences({
        marketingEmails: Boolean(saved.marketingEmails),
        luppaNews: Boolean(saved.luppaNews),
        eventRecommendations: Boolean(saved.eventRecommendations),
        savedEventReminders: Boolean(saved.savedEventReminders),
      })
      setError('')
      toast.success('Preferências salvas com sucesso.')
    } catch {
      setError('Não foi possível salvar preferências de comunicação.')
      toast.error('Não foi possível salvar as alterações.')
    } finally {
      setPreferencesSaving(false)
    }
  }

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <Link to="/" aria-label="Luppa"><LuppaSymbol /></Link>
        <nav>
          <Link to="/">← Voltar para o Luppa</Link>
          {canCreateEvents && <Link to="/my-events">Meus eventos</Link>}
        </nav>
      </header>

      <section className={styles.profileHeader}>
        {avatarPreview ? <img className={styles.avatarPreview} src={avatarPreview} alt={auth.name} /> : <Avatar user={auth.user} />}
        <div>
          <span>Perfil</span>
          <h1>{auth.name}</h1>
          <p>{auth.email}</p>
          <strong>{auth.role}</strong>
        </div>
      </section>

      <nav className={styles.tabs} aria-label="Seções do perfil">
        {tabs.map((tab) => (
          <button className={activeTab === tab.id ? styles.tabActive : ''} key={tab.id} type="button" onClick={() => setActiveTab(tab.id)}>
            {tab.label}
          </button>
        ))}
      </nav>

      {activeTab === 'profile' && (
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>Editar perfil</h2>
              <p>Dados salvos na sua conta.</p>
            </div>
            {!editingProfile && <button className={styles.secondaryButton} type="button" onClick={() => setEditingProfile(true)}>Editar</button>}
          </div>
          <form className={styles.form} noValidate ref={profileFormRef} onSubmit={saveProfile}>
            <label>Nome de exibição<input value={profileForm.name} readOnly={!editingProfile} maxLength={85} aria-invalid={Boolean(profileErrors.name)} aria-describedby={profileErrors.name ? errorId('name') : undefined} onChange={(event) => updateProfileField('name', event.target.value)} /><FieldError id={errorId('name')} message={profileErrors.name} /></label>
            <label>Email<input type="email" inputMode="email" value={profileForm.email} readOnly={!editingProfile} maxLength={60} aria-invalid={Boolean(profileErrors.email)} aria-describedby={profileErrors.email ? errorId('email') : undefined} onChange={(event) => updateProfileField('email', event.target.value)} /><FieldError id={errorId('email')} message={profileErrors.email} /></label>
            <label>Data de nascimento<input type="date" value={profileForm.birthDate} readOnly={!editingProfile} max={todayDateInputValue()} aria-invalid={Boolean(profileErrors.birthDate)} aria-describedby={profileErrors.birthDate ? errorId('birthDate') : undefined} onChange={(event) => updateProfileField('birthDate', event.target.value)} /><FieldError id={errorId('birthDate')} message={profileErrors.birthDate} /></label>
            <label>Telefone<input type="tel" inputMode="tel" value={profileForm.phone} readOnly={!editingProfile} maxLength={15} placeholder="(11) 99999-9999" aria-invalid={Boolean(profileErrors.phone)} aria-describedby={profileErrors.phone ? errorId('phone') : undefined} onChange={(event) => updateProfileField('phone', event.target.value)} /><FieldError id={errorId('phone')} message={profileErrors.phone} /></label>
            <div className={styles.uploadField}>
              <span>Foto de perfil</span>
              <div className={styles.uploadBox}>
                {avatarPreview ? <img src={avatarPreview} alt="Prévia da foto de perfil" /> : <Avatar user={auth.user} />}
                <label className={styles.uploadButton} aria-disabled={!editingProfile}>
                  Alterar foto
                  <input type="file" accept="image/jpeg,image/png,image/webp" disabled={!editingProfile} onChange={handleAvatarChange} />
                </label>
                {avatarPreview && <button type="button" disabled={!editingProfile} onClick={() => { setAvatarFile(null); setAvatarPreview('') }}>Remover</button>}
              </div>
              <small>PNG, JPG ou WebP. Máx. 2 MB.</small>
              <FieldError id={errorId('avatar')} message={avatarError} />
            </div>
            {editingProfile && (
              <div className={styles.formActions}>
                <button className={styles.secondaryButton} type="button" onClick={cancelProfileEdit}>Cancelar</button>
                <button className={styles.primaryButton} type="submit" disabled={profileSaving || Boolean(avatarError)}>{profileSaving ? 'Salvando...' : 'Salvar alterações'}</button>
              </div>
            )}
          </form>
        </section>
      )}

      {activeTab === 'preferences' && (
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>Preferências de comunicação</h2>
              <p>Controle comunicações opcionais sem bloquear mensagens essenciais da sua conta.</p>
            </div>
          </div>
          <div className={styles.preferenceSection}>
            <h3>Comunicações essenciais</h3>
            <p>E-mails de segurança, recuperação de senha, verificação de e-mail, suporte e atualizações importantes sobre seus próprios eventos ou denúncias podem ser enviados quando necessário.</p>
          </div>
          <div className={styles.preferenceSection}>
            <h3>Comunicações opcionais</h3>
            <label><input type="checkbox" checked={preferences.luppaNews} onChange={() => togglePreference('luppaNews')} />Novidades da Luppa</label>
            <label><input type="checkbox" checked={preferences.eventRecommendations} onChange={() => togglePreference('eventRecommendations')} />Recomendações de eventos</label>
            <label><input type="checkbox" checked={preferences.savedEventReminders} onChange={() => togglePreference('savedEventReminders')} />Lembretes de eventos salvos</label>
            <label><input type="checkbox" checked={preferences.marketingEmails} onChange={() => togglePreference('marketingEmails')} />Novidades, recomendações e comunicações por e-mail</label>
          </div>
          <div className={styles.formActions}>
            <button className={styles.primaryButton} type="button" disabled={preferencesSaving} onClick={savePreferences}>{preferencesSaving ? 'Salvando...' : 'Salvar preferências'}</button>
          </div>
        </section>
      )}

      {activeTab === 'saved' && (
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>Eventos salvos</h2>
              <p>Eventos salvos na sua conta.</p>
            </div>
          </div>
          <div className={styles.filters}>
            <input placeholder="Buscar salvos" value={savedFilters.query} onChange={(event) => setSavedFilters((current) => ({ ...current, query: event.target.value }))} />
            <select value={savedFilters.category} onChange={(event) => setSavedFilters((current) => ({ ...current, category: event.target.value }))}>
              <option value="all">Todas as categorias</option>
              {categories.map((category) => <option key={category.slug} value={category.slug}>{category.title}</option>)}
            </select>
            <select value={savedFilters.period} onChange={(event) => setSavedFilters((current) => ({ ...current, period: event.target.value }))}>
              <option value="all">Todos</option>
              <option value="upcoming">Próximos</option>
              <option value="past">Encerrados</option>
            </select>
            <select value={savedFilters.sort} onChange={(event) => setSavedFilters((current) => ({ ...current, sort: event.target.value }))}>
              <option value="nearest">Data mais próxima</option>
              <option value="furthest">Data mais distante</option>
              <option value="saved">Salvo recentemente</option>
              <option value="az">Nome A-Z</option>
            </select>
          </div>
          <div className={styles.eventGrid}>
            {savedEvents.map((event) => (
              <article className={styles.savedItem} key={event.id}>
                <EventCard event={event} onDetails={openEventDetails} />
                <button type="button" disabled={removingSavedEventId === event.id} onClick={() => removeSavedEvent(event.id)}>
                  {removingSavedEventId === event.id ? 'Removendo...' : 'Remover dos salvos'}
                </button>
              </article>
            ))}
          </div>
        </section>
      )}

      {activeTab === 'reviews' && (
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>Eventos avaliados</h2>
              <p>Avaliações registradas na sua conta.</p>
            </div>
          </div>
          <div className={styles.filters}>
            <input placeholder="Buscar avaliados" value={reviewFilters.query} onChange={(event) => setReviewFilters((current) => ({ ...current, query: event.target.value }))} />
            <select value={reviewFilters.category} onChange={(event) => setReviewFilters((current) => ({ ...current, category: event.target.value }))}>
              <option value="all">Todas as categorias</option>
              {categories.map((category) => <option key={category.slug} value={category.slug}>{category.title}</option>)}
            </select>
            <select value={reviewFilters.rating} onChange={(event) => setReviewFilters((current) => ({ ...current, rating: event.target.value }))}>
              <option value="all">Todas as notas</option>
              {[5, 4, 3, 2, 1].map((rating) => <option key={rating} value={rating}>{rating} estrelas</option>)}
            </select>
            <select value={reviewFilters.sort} onChange={(event) => setReviewFilters((current) => ({ ...current, sort: event.target.value }))}>
              <option value="recent">Avaliado recentemente</option>
              <option value="best">Melhor avaliação</option>
              <option value="worst">Pior avaliação</option>
              <option value="eventDate">Data do evento</option>
            </select>
          </div>
          <div className={styles.reviewList}>
            {evaluatedEvents.map((event) => (
              <button type="button" key={event.id} onClick={() => openEventDetails(event)}>
                <strong>{event.title}</strong>
                <StarRating value={event.userReview.rating} readOnly label={`Sua avaliação de ${event.title}`} />
                <p>{event.userReview.comment}</p>
              </button>
            ))}
          </div>
        </section>
      )}

      {error && <p className={styles.note}>{error}</p>}

      {activeTab === 'mine' && canCreateEvents && (
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>Meus eventos</h2>
              <p>{ownEvents.length} evento{ownEvents.length === 1 ? '' : 's'} associado{ownEvents.length === 1 ? '' : 's'} a esta conta.</p>
            </div>
            <Link to="/my-events">Abrir gerenciamento</Link>
          </div>
        </section>
      )}

      <EventDetailsModal event={selectedEvent} onClose={closeEventDetails} onEventUpdated={updateSelectedEvent} />
    </main>
  )
}

export default ProfileLuppa
