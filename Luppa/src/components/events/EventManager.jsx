import { useEffect, useRef, useState } from 'react'
import ConfirmDialog from '../../components/common/ConfirmDialog'
import { useToast } from '../../components/common/ToastProvider'
import CoverImageUpload from '../../components/forms/CoverImageUpload'
import StructuredAddressFields from '../../components/forms/StructuredAddressFields'
import { clearFeaturedEvent as clearFeaturedEventRequest, createEvent, deleteEvent, getCategories, getEventStatusLabel, searchAdminEvents, setFeaturedEvent as setFeaturedEventRequest, updateEvent, uploadEventImage } from '../../services/eventService'
import { eventStatusLabels } from '../../constants/events'
import { parseEventDateTime } from '../../services/eventDateService'
import { buildStructuredAddress, formatStructuredAddress } from '../../services/eventLocationService'
import { validateCoverImage } from '../../utils/imageValidation'
import { isBrazilStateUf } from '../../constants/brazilStates'
import { cepDigits, formatBrazilianCep, isBrazilianCep, isValidDate, maxEventStartDateInputValue, todayDateInputValue } from '../../utils/validation'
import styles from '../../pages/AdminLuppa/AdminLuppa.module.css'

const emptyForm = {
  title: '',
  description: '',
  categorySlug: '',
  addressFields: {
    cep: '',
    rua: '',
    numero: '',
    cidade: '',
    estado: '',
    pais: 'Brasil',
  },
  latitude: null,
  longitude: null,
  startDate: '',
  startTime: '',
  endDate: '',
  endTime: '',
  status: 'draft',
}

const isFeatureEligible = (event) => event?.status === 'published'

const EventManager = ({ events, setEvents, refreshAdminData, administrative = false, onDetails }) => {
  const toast = useToast()
  const [filters, setFilters] = useState({ query: '', category: 'all', status: 'all', sort: 'upcoming' })
  const [form, setForm] = useState(emptyForm)
  const [coverFile, setCoverFile] = useState(null)
  const [coverPreview, setCoverPreview] = useState('')
  const [editingId, setEditingId] = useState(null)
  const [confirmEvent, setConfirmEvent] = useState(null)
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [curationSavingId, setCurationSavingId] = useState(null)
  const [categories, setCategories] = useState([])
  const [addressResolution, setAddressResolution] = useState({ verified: false, message: '' })
  const [originalAddressFields, setOriginalAddressFields] = useState(null)
  const [originalSchedule, setOriginalSchedule] = useState(null)
  const [scrollEditRequest, setScrollEditRequest] = useState(0)
  const editFormSectionRef = useRef(null)
  const firstInputRef = useRef(null)
  const filteredEvents = searchAdminEvents(events, filters)
  const manualFeaturedEvent = events.find((event) => event.destaquePrincipal)
  const editingEvent = editingId ? events.find((event) => event.id === editingId) : null

  useEffect(() => () => {
    if (coverPreview?.startsWith('blob:')) URL.revokeObjectURL(coverPreview)
  }, [coverPreview])

  useEffect(() => {
    if (!scrollEditRequest) return
    editFormSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    window.setTimeout(() => firstInputRef.current?.focus({ preventScroll: true }), 350)
  }, [scrollEditRequest])

  useEffect(() => {
    getCategories()
      .then((items) => {
        const realCategories = items.filter((category) => category.id !== 'all')
        setCategories(realCategories)
        setForm((current) => current.categorySlug
          ? current
          : { ...current, categorySlug: realCategories[0]?.slug || '' })
      })
      .catch(() => toast.error('Não foi possível carregar categorias.'))
  }, [toast])

  const updateFilter = (field, value) => setFilters((current) => ({ ...current, [field]: value }))
  const updateForm = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: '' }))
  }
  const updateAddressFields = (patch) => {
    setForm((current) => ({
      ...current,
      addressFields: {
        ...current.addressFields,
        ...patch,
      },
      latitude: null,
      longitude: null,
    }))
    setErrors((current) => Object.keys(patch).reduce((next, field) => ({ ...next, [field]: '', location: '' }), current))
  }

  const updateAddressResolution = (resolution) => {
    setAddressResolution(resolution)
    setForm((current) => ({
      ...current,
      latitude: resolution.coordinates?.latitude ?? null,
      longitude: resolution.coordinates?.longitude ?? null,
    }))
  }

  const resetForm = () => {
    setForm({ ...emptyForm, categorySlug: categories[0]?.slug || '' })
    setCoverFile(null)
    setCoverPreview('')
    setEditingId(null)
    setErrors({})
    setAddressResolution({ verified: false, message: '' })
    setOriginalAddressFields(null)
    setOriginalSchedule(null)
  }

  const handleCoverChange = (file) => {
    const imageError = validateCoverImage(file)
    if (coverPreview?.startsWith('blob:')) URL.revokeObjectURL(coverPreview)
    setCoverFile(imageError ? null : file)
    setCoverPreview(imageError || !file ? '' : URL.createObjectURL(file))
    setErrors((current) => ({ ...current, cover: imageError }))
  }

  const removeCover = () => {
    if (coverPreview?.startsWith('blob:')) URL.revokeObjectURL(coverPreview)
    setCoverFile(null)
    setCoverPreview('')
    setErrors((current) => ({ ...current, cover: '' }))
  }

  const validate = () => {
    const nextErrors = {}
    const start = parseEventDateTime(form.startDate, form.startTime)
    const end = parseEventDateTime(form.endDate, form.endTime)
    const addressFields = buildStructuredAddress({
      ...form.addressFields,
      cep: cepDigits(form.addressFields.cep),
    })
    const originalAddress = originalAddressFields ? buildStructuredAddress(originalAddressFields) : null
    const addressChanged = !editingId || !originalAddress || JSON.stringify(addressFields) !== JSON.stringify(originalAddress)
    const scheduleChanged = !editingId
      || !originalSchedule
      || form.startDate !== originalSchedule.startDate
      || form.startTime !== originalSchedule.startTime
      || form.endDate !== originalSchedule.endDate
      || form.endTime !== originalSchedule.endTime

    if (!form.title.trim()) nextErrors.title = 'Informe o nome do evento.'
    if (!form.description.trim()) nextErrors.description = 'Informe a descrição.'
    if (!form.categorySlug) nextErrors.categorySlug = 'Selecione uma categoria.'
    if (addressChanged) {
      if (!isBrazilianCep(form.addressFields.cep)) nextErrors.cep = 'Informe um CEP com 8 dígitos.'
      if (!form.addressFields.rua.trim()) nextErrors.rua = 'Informe a rua.'
      if (!form.addressFields.numero.trim()) nextErrors.numero = 'Informe o número.'
      if (!form.addressFields.cidade.trim()) nextErrors.cidade = 'Informe a cidade.'
      if (!isBrazilStateUf(form.addressFields.estado)) nextErrors.estado = 'Selecione uma UF válida.'
      if (form.addressFields.pais !== 'Brasil') nextErrors.pais = 'O país deve ser Brasil.'
      if (!addressResolution.verified) nextErrors.location = addressResolution.message || 'Confirme o endereço pelo CEP ou por uma sugestão e aguarde a prévia do mapa.'
    }
    if (!form.startDate || !isValidDate(form.startDate)) nextErrors.startDate = 'Informe uma data de início válida.'
    else if (!form.startTime || !start) nextErrors.startTime = 'Informe um horário de início válido.'
    else if (scheduleChanged && start < new Date()) nextErrors.startDate = 'A data de início não pode estar no passado.'
    else if (scheduleChanged && start > parseEventDateTime(maxEventStartDateInputValue(), '23:59')) nextErrors.startDate = 'A data de início deve ser no máximo daqui a 1 ano.'
    if (!form.endDate || !isValidDate(form.endDate)) nextErrors.endDate = 'Informe uma data de término válida.'
    else if (!form.endTime || !end) nextErrors.endTime = 'Informe um horário de término válido.'
    if (start && end && end < start) nextErrors.endDate = 'O término não pode ser anterior ao início.'

    return nextErrors
  }

  const buildPayload = () => {
    const addressFields = buildStructuredAddress({
      ...form.addressFields,
      cep: cepDigits(form.addressFields.cep),
    })
    const address = formatStructuredAddress(addressFields)
    const unchangedAddress = editingEvent && originalAddressFields
      && JSON.stringify(addressFields) === JSON.stringify(buildStructuredAddress(originalAddressFields))
    return {
      ...form,
      status: !administrative && form.status === 'published' ? 'pending_review' : form.status,
      address,
      addressFields,
      latitude: unchangedAddress ? editingEvent.latitude : form.latitude,
      longitude: unchangedAddress ? editingEvent.longitude : form.longitude,
      locationName: address,
      coverImageUrl: coverPreview || '',
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const nextErrors = validate()
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return
    if (saving) return

    setSaving(true)
    try {
      if (editingId) {
        let updated = await updateEvent(events, editingId, buildPayload())
        if (coverFile) {
          try {
            const image = await uploadEventImage(updated.id, coverFile)
            updated = { ...updated, coverImageUrl: image?.url || updated.coverImageUrl }
          } catch {
            toast.error('Dados salvos, mas a imagem não foi atualizada. Edite o evento para tentar novamente.')
          }
        }
        setEvents((current) => current.map((item) => (item.id === updated.id ? updated : item)))
        toast.success('Evento atualizado com sucesso.')
      } else {
        let created = await createEvent(events, buildPayload())
        if (coverFile) {
          try {
            const image = await uploadEventImage(created.id, coverFile)
            created = { ...created, coverImageUrl: image?.url || created.coverImageUrl }
          } catch {
            toast.error('Evento criado, mas a imagem não foi enviada. Edite o evento para tentar novamente.')
          }
        }
        setEvents((current) => [...current, created])
        toast.success('Evento criado com sucesso.')
      }
      refreshAdminData?.().catch(() => toast.error('Alterações salvas, mas não foi possível atualizar a lista.'))
      resetForm()
    } catch {
      toast.error('Não foi possível salvar as alterações.')
    } finally {
      setSaving(false)
    }
  }

  const editEvent = (event) => {
    setEditingId(event.id)
    setForm({
      title: event.title,
      description: event.description,
      categorySlug: event.categorySlug,
      addressFields: {
        cep: formatBrazilianCep(event.addressFields?.cep || ''),
        rua: event.addressFields?.rua || '',
        numero: event.addressFields?.numero || '',
        cidade: event.addressFields?.cidade || '',
        estado: event.addressFields?.estado || '',
        pais: event.addressFields?.pais || 'Brasil',
      },
      latitude: event.latitude,
      longitude: event.longitude,
      startDate: event.startDate,
      startTime: event.startTime,
      endDate: event.endDate,
      endTime: event.endTime,
      status: event.status,
    })
    setCoverPreview(event.coverImageUrl || '')
    setCoverFile(null)
    setErrors({})
    setOriginalAddressFields({
      cep: event.addressFields?.cep || '',
      rua: event.addressFields?.rua || '',
      numero: event.addressFields?.numero || '',
      cidade: event.addressFields?.cidade || '',
      estado: event.addressFields?.estado || '',
      pais: event.addressFields?.pais || 'Brasil',
    })
    setOriginalSchedule({
      startDate: event.startDate,
      startTime: event.startTime,
      endDate: event.endDate,
      endTime: event.endTime,
    })
    setAddressResolution({ verified: true, message: '' })
    setScrollEditRequest((current) => current + 1)
  }

  const confirmDelete = async () => {
    if (deleting) return
    setDeleting(true)
    try {
      await deleteEvent(events, confirmEvent.id)
      setEvents((current) => current.filter((event) => event.id !== confirmEvent.id))
      if (editingId === confirmEvent.id) resetForm()
      setConfirmEvent(null)
      toast.success('Evento deletado com sucesso.')
      refreshAdminData?.().catch(() => toast.error('Alterações salvas, mas não foi possível atualizar a lista.'))
    } catch (error) {
      toast.error(error.status === 409 ? error.message : 'Não foi possível excluir o evento.')
    } finally {
      setDeleting(false)
    }
  }

  const updateLocalFeaturedState = (featuredEvent) => {
    setEvents((current) => current.map((item) => (
      item.id === featuredEvent.id
        ? { ...item, ...featuredEvent, destaquePrincipal: true, featured: true }
        : { ...item, destaquePrincipal: false, featured: false }
    )))
  }

  const clearLocalFeaturedState = () => {
    setEvents((current) => current.map((event) => ({
      ...event,
      destaquePrincipal: false,
      featured: false,
    })))
  }

  const setFeaturedEvent = async (event) => {
    if (curationSavingId) return
    if (!isFeatureEligible(event)) return
    setCurationSavingId(event.id)
    try {
      const updated = await setFeaturedEventRequest(event.id)
      updateLocalFeaturedState(updated)
      refreshAdminData?.().catch(() => toast.error('Alterações salvas, mas não foi possível atualizar a lista.'))
      toast.success('Evento definido como destaque.')
    } catch {
      toast.error('Não foi possível atualizar o destaque.')
    } finally {
      setCurationSavingId(null)
    }
  }

  const clearFeaturedEvent = async () => {
    if (curationSavingId) return
    setCurationSavingId('clear')
    try {
      await clearFeaturedEventRequest()
      clearLocalFeaturedState()
      refreshAdminData?.().catch(() => toast.error('Alterações salvas, mas não foi possível atualizar a lista.'))
      toast.success('Destaque removido.')
    } catch {
      toast.error('Não foi possível remover o destaque.')
    } finally {
      setCurationSavingId(null)
    }
  }

  return (
    <>
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <div>
            <h2>Gerenciar eventos</h2>
            <p>Consulte os interessados e gerencie as informações dos eventos.</p>
          </div>
          <div className={styles.actions}>
            {administrative && manualFeaturedEvent && (
              <button className={styles.secondaryButton} type="button" disabled={Boolean(curationSavingId)} onClick={clearFeaturedEvent}>
                Remover destaque
              </button>
            )}
            {administrative && <button className={styles.secondaryButton} type="button" onClick={resetForm}>Novo evento</button>}
          </div>
        </div>

        <div className={styles.filters}>
          <input value={filters.query} placeholder="Buscar por evento, local ou categoria" maxLength={120} onChange={(event) => updateFilter('query', event.target.value)} />
          <select value={filters.category} onChange={(event) => updateFilter('category', event.target.value)}>
            <option value="all">Todas as categorias</option>
            {categories.map((category) => <option key={category.slug} value={category.slug}>{category.title}</option>)}
          </select>
          <select value={filters.status} onChange={(event) => updateFilter('status', event.target.value)}>
            <option value="all">Todos os status</option>
            {Object.keys(eventStatusLabels).map((status) => <option key={status} value={status}>{getEventStatusLabel(status)}</option>)}
          </select>
          <select value={filters.sort} onChange={(event) => updateFilter('sort', event.target.value)}>
            <option value="upcoming">Próxima data</option>
            <option value="az">A-Z</option>
            <option value="za">Z-A</option>
            <option value="newest">Mais recentes</option>
            <option value="oldest">Mais antigos</option>
          </select>
        </div>

        {filteredEvents.length === 0 ? <p className={styles.empty}>Nenhum evento encontrado.</p> : (
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Evento</th>
                  <th>Categoria</th>
                  <th>Local</th>
                  <th>Início</th>
                  <th>Status</th>
                  <th>Interessados</th>
                  {administrative && <th>Curadoria</th>}
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {filteredEvents.map((event) => (
                  <tr key={event.id}>
                    <td>{event.title}</td>
                    <td><span className={styles.badge}>{event.categoryName}</span></td>
                    <td>{event.locationName}</td>
                    <td>{event.startDate} {event.startTime}</td>
                    <td><span className={styles.badge}>{getEventStatusLabel(event.status)}</span></td>
                    <td>{event.interestCount ?? 0}</td>
                    {administrative && <td>
                      {event.destaquePrincipal ? (
                        <span className={`${styles.badge} ${styles.successBadge}`}>Em destaque</span>
                      ) : (
                        <span className={styles.badge}>Normal</span>
                      )}
                    </td>}
                    <td>
                      <div className={styles.actions}>
                        {onDetails && <button className={styles.secondaryButton} type="button" onClick={() => onDetails(event)}>Detalhes</button>}
                        {administrative && (event.destaquePrincipal ? (
                          <button className={styles.secondaryButton} type="button" disabled={Boolean(curationSavingId)} onClick={clearFeaturedEvent}>
                            Remover destaque
                          </button>
                        ) : isFeatureEligible(event) ? (
                          <button className={styles.secondaryButton} type="button" disabled={Boolean(curationSavingId)} onClick={() => setFeaturedEvent(event)}>
                            Destacar
                          </button>
                        ) : (
                          <button className={styles.secondaryButton} type="button" disabled title="Publique o evento para destacá-lo.">
                            Publicar para destacar
                          </button>
                        ))}
                        <button className={styles.secondaryButton} type="button" disabled={saving || deleting} onClick={() => editEvent(event)}>Editar</button>
                        <button className={styles.dangerButton} type="button" disabled={saving || deleting} onClick={() => setConfirmEvent(event)}>Excluir</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {(administrative || editingId) && <section className={`${styles.panel} ${styles.editPanel}`} ref={editFormSectionRef}>
        <div className={styles.panelHeader}>
          <div>
            <h2>{editingId ? 'Editar evento' : 'Criar evento'}</h2>
            <p>Conteúdo do evento separado de dados derivados e curadoria da plataforma.</p>
          </div>
        </div>
        {administrative && <div className={styles.curationPanel}>
          <div>
            <span>Curadoria da plataforma</span>
            {editingEvent ? (
              editingEvent.destaquePrincipal ? (
                <strong>Em destaque</strong>
              ) : isFeatureEligible(editingEvent) ? (
                <strong>Elegível para destaque</strong>
              ) : (
                <strong>Destaque indisponível</strong>
              )
            ) : (
              <strong>Disponível após criar e publicar</strong>
            )}
            <p>
              {editingEvent
                ? (isFeatureEligible(editingEvent)
                  ? 'Use a ação dedicada para alterar o destaque manual deste evento.'
                  : 'Somente eventos publicados podem ser destacados manualmente.')
                : 'Crie o evento primeiro. Depois que ele existir e estiver publicado, ele poderá ser destacado.'}
            </p>
          </div>
          {editingEvent && isFeatureEligible(editingEvent) && (
            editingEvent.destaquePrincipal ? (
              <button className={styles.secondaryButton} type="button" disabled={Boolean(curationSavingId)} onClick={clearFeaturedEvent}>
                Remover destaque
              </button>
            ) : (
              <button className={styles.secondaryButton} type="button" disabled={Boolean(curationSavingId)} onClick={() => setFeaturedEvent(editingEvent)}>
                Destacar evento
              </button>
            )
          )}
        </div>}
        {!administrative && <p>Alterações em eventos publicados serão enviadas novamente para análise.</p>}
        <form className={styles.formGrid} noValidate onSubmit={handleSubmit}>
          <label>Nome<input ref={firstInputRef} value={form.title} maxLength={40} onChange={(event) => updateForm('title', event.target.value)} />{errors.title && <span className={styles.error}>{errors.title}</span>}</label>
          <label>Categoria<select value={form.categorySlug} onChange={(event) => updateForm('categorySlug', event.target.value)}>{categories.map((category) => <option key={category.slug} value={category.slug}>{category.title}</option>)}</select>{errors.categorySlug && <span className={styles.error}>{errors.categorySlug}</span>}</label>
          <label className={styles.wide}>Descrição<textarea rows="4" maxLength={150} value={form.description} onChange={(event) => updateForm('description', event.target.value)} />{errors.description && <span className={styles.error}>{errors.description}</span>}</label>
          <div className={styles.wide}>
            <StructuredAddressFields
              value={form.addressFields}
              errors={errors}
              onChange={updateAddressFields}
              onResolutionChange={updateAddressResolution}
              legend="Endereço"
            />
          </div>
          <label>Data início<input type="date" min={editingId ? undefined : todayDateInputValue()} max={maxEventStartDateInputValue()} value={form.startDate} onChange={(event) => updateForm('startDate', event.target.value)} />{errors.startDate && <span className={styles.error}>{errors.startDate}</span>}</label>
          <label>Hora início<input type="time" step="60" value={form.startTime} onChange={(event) => updateForm('startTime', event.target.value)} />{errors.startTime && <span className={styles.error}>{errors.startTime}</span>}</label>
          <label>Data término<input type="date" min={form.startDate || (editingId ? undefined : todayDateInputValue())} value={form.endDate} onChange={(event) => updateForm('endDate', event.target.value)} />{errors.endDate && <span className={styles.error}>{errors.endDate}</span>}</label>
          <label>Hora término<input type="time" step="60" value={form.endTime} onChange={(event) => updateForm('endTime', event.target.value)} />{errors.endTime && <span className={styles.error}>{errors.endTime}</span>}</label>
          <div className={styles.wide}>
            <CoverImageUpload file={coverFile} preview={coverPreview} error={errors.cover} onChange={handleCoverChange} onRemove={removeCover} />
          </div>
          <label>Status<select value={form.status} onChange={(event) => updateForm('status', event.target.value)}>{Object.keys(eventStatusLabels).filter(status => administrative || status !== 'published' || form.status === 'published').map((status) => <option key={status} value={status}>{getEventStatusLabel(status)}</option>)}</select></label>
          <div className={styles.formActions}>
            <button className={styles.primaryButton} type="submit" disabled={saving}>{saving ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Criar evento'}</button>
            <button className={styles.secondaryButton} type="button" disabled={saving} onClick={resetForm}>Cancelar</button>
          </div>
        </form>
      </section>}

      {confirmEvent && (
        <ConfirmDialog
          title="Excluir evento"
          message={`Tem certeza que deseja excluir ${confirmEvent.title}? Esta ação não pode ser desfeita.`}
          confirmLabel="Excluir evento"
          confirmDisabled={deleting}
          onConfirm={confirmDelete}
          onCancel={() => setConfirmEvent(null)}
        />
      )}
    </>
  )
}

export default EventManager
