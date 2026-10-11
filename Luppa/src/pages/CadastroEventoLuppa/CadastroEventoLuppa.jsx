import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/authContext'
import Logo from '../../components/common/Logo'
import InfoDialog from '../../components/common/InfoDialog'
import { useToast } from '../../components/common/ToastProvider'
import EventDetailsModal from '../../components/events/EventDetailsModal'
import CoverImageUpload from '../../components/forms/CoverImageUpload'
import FieldError from '../../components/forms/FieldError'
import StructuredAddressFields from '../../components/forms/StructuredAddressFields'
import { parseEventDateTime } from '../../services/eventDateService'
import { getCategories, publishEventToApi, uploadEventImage } from '../../services/eventService'
import { buildStructuredAddress, formatStructuredAddress } from '../../services/eventLocationService'
import { validateCoverImage } from '../../utils/imageValidation'
import { isBrazilStateUf } from '../../constants/brazilStates'
import { cepDigits, isBrazilianCep, isValidDate, maxEventStartDateInputValue, todayDateInputValue } from '../../utils/validation'
import { focusFirstInvalidField } from '../../utils/focusFirstInvalidField'
import styles from './CadastroEventoLuppa.module.css'

const initialForm = {
  name: '',
  description: '',
  category: '',
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
}

const CadastroEventoLuppa = () => {
  const auth = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const [form, setForm] = useState(initialForm)
  const [coverFile, setCoverFile] = useState(null)
  const [coverPreview, setCoverPreview] = useState('')
  const [errors, setErrors] = useState({})
  const [mode, setMode] = useState('edit')
  const [publishing, setPublishing] = useState(false)
  const [draftInfoOpen, setDraftInfoOpen] = useState(false)
  const [categories, setCategories] = useState([])
  const [addressResolution, setAddressResolution] = useState({ verified: false, message: '' })
  const formRef = useRef(null)

  const errorId = (field) => `event-create-${field}-error`

  useEffect(() => () => {
    if (coverPreview) URL.revokeObjectURL(coverPreview)
  }, [coverPreview])

  useEffect(() => {
    let active = true
    getCategories()
      .then((items) => {
        if (active) setCategories(items.filter((category) => category.id !== 'all'))
      })
      .catch(() => toast.error('Não foi possível carregar categorias.'))
    return () => {
      active = false
    }
  }, [])

  const updateField = (field, value) => {
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

  const handleCoverChange = (file) => {
    const imageError = validateCoverImage(file)

    if (coverPreview) URL.revokeObjectURL(coverPreview)

    setCoverFile(imageError ? null : file)
    setCoverPreview(imageError || !file ? '' : URL.createObjectURL(file))
    setErrors((current) => ({ ...current, cover: imageError }))
  }

  const removeCover = () => {
    if (coverPreview) URL.revokeObjectURL(coverPreview)
    setCoverFile(null)
    setCoverPreview('')
    setErrors((current) => ({ ...current, cover: '' }))
  }

  const validateForm = () => {
    const nextErrors = {}
    const start = parseEventDateTime(form.startDate, form.startTime)
    const end = parseEventDateTime(form.endDate, form.endTime)

    if (!form.name.trim()) nextErrors.name = 'Informe o nome do evento.'
    if (!form.description.trim()) nextErrors.description = 'Informe a descrição do evento.'
    if (!form.category) nextErrors.category = 'Selecione uma categoria.'
    if (!isBrazilianCep(form.addressFields.cep)) nextErrors.cep = 'Informe um CEP com 8 dígitos.'
    if (!form.addressFields.rua.trim()) nextErrors.rua = 'Informe a rua.'
    if (!form.addressFields.numero.trim()) nextErrors.numero = 'Informe o número.'
    if (!form.addressFields.cidade.trim()) nextErrors.cidade = 'Informe a cidade.'
    if (!isBrazilStateUf(form.addressFields.estado)) nextErrors.estado = 'Selecione uma UF válida.'
    if (form.addressFields.pais !== 'Brasil') nextErrors.pais = 'O país deve ser Brasil.'
    if (!addressResolution.verified) nextErrors.location = addressResolution.message || 'Confirme o endereço pelo CEP ou por uma sugestão e aguarde a prévia do mapa.'
    if (!form.startDate || !isValidDate(form.startDate)) nextErrors.startDate = 'Informe uma data de início válida.'
    else if (!form.startTime || !start) nextErrors.startTime = 'Informe um horário de início válido.'
    else if (start < new Date()) nextErrors.startDate = 'A data de início não pode estar no passado.'
    else if (start > parseEventDateTime(maxEventStartDateInputValue(), '23:59')) nextErrors.startDate = 'A data de início deve ser no máximo daqui a 1 ano.'
    if (!form.endDate || !isValidDate(form.endDate)) nextErrors.endDate = 'Informe uma data de término válida.'
    else if (!form.endTime || !end) nextErrors.endTime = 'Informe um horário de término válido.'
    if (start && end && end < start) nextErrors.endDate = 'O término não pode ser anterior ao início.'

    const imageError = validateCoverImage(coverFile)
    if (imageError) nextErrors.cover = imageError

    return nextErrors
  }

  const buildPreviewEvent = () => {
    const category = categories.find((item) => item.slug === form.category)
    const addressFields = buildStructuredAddress({
      ...form.addressFields,
      cep: cepDigits(form.addressFields.cep),
    })
    const address = formatStructuredAddress(addressFields)
    return {
      id: 'preview',
      title: form.name || 'Nome do evento',
      description: form.description || 'Descrição do evento',
      categorySlug: form.category,
      categoryName: category?.title || 'Categoria',
      locationName: address || 'Local do evento',
      address: address || 'Endereço ainda não informado',
      addressFields,
      latitude: form.latitude,
      longitude: form.longitude,
      startDate: form.startDate,
      startTime: form.startTime,
      endDate: form.endDate,
      endTime: form.endTime,
      coverImageUrl: coverPreview,
      creatorName: auth.name || 'Organizador Luppa',
      status: 'pending_review',
      participants: 0,
    }
  }

  const validateAndPreview = () => {
    const nextErrors = validateForm()
    setErrors(nextErrors)

    if (Object.keys(nextErrors).length === 0) {
      setMode('preview')
    } else {
      window.requestAnimationFrame(() => focusFirstInvalidField(formRef.current))
    }
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    validateAndPreview()
  }

  const saveDraft = () => {
    setDraftInfoOpen(true)
  }

  const publishEvent = async () => {
    if (publishing) return
    const nextErrors = validateForm()
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      setMode('edit')
      window.requestAnimationFrame(() => focusFirstInvalidField(formRef.current))
      return
    }

    setPublishing(true)
    try {
      const result = await publishEventToApi(buildPreviewEvent())
      if (coverFile) await uploadEventImage(result.event.id, coverFile)
      navigate('/', { state: { toast: 'Evento publicado com sucesso.' } })
    } catch {
      toast.error('Não foi possível enviar o evento para a API.')
    } finally {
      setPublishing(false)
    }
  }

  return (
    <main className={styles.page}>
      <section className={styles.shell}>
        <Link className={styles.backLink} to="/">Voltar para o Luppa</Link>
        <div className={styles.header}>
          <Logo />
          <div>
            <h1>Cadastrar Evento</h1>
            <p>Preencha as informações principais para enviar o evento para análise.</p>
          </div>
        </div>

        {mode === 'preview' && (
          <section className={styles.previewPanel}>
            <div>
              <span>Pré-visualização</span>
              <h2>Revise antes de publicar</h2>
              <p>A prévia usa a mesma apresentação de detalhes vista pelos participantes.</p>
            </div>
            <div className={styles.previewActions}>
              <button className={styles.secondaryButton} type="button" onClick={() => setMode('edit')}>Voltar para editar</button>
              <button className={styles.secondaryButton} type="button" onClick={saveDraft}>Salvar rascunho</button>
              <button className={styles.primaryButton} type="button" disabled={publishing} onClick={publishEvent}>{publishing ? 'Enviando...' : 'Enviar para análise'}</button>
            </div>
          </section>
        )}

        <form className={styles.panel} noValidate ref={formRef} onSubmit={handleSubmit}>
          <div className={styles.row}>
            <label>
              Nome do evento
              <input type="text" value={form.name} maxLength={40} placeholder="Ex: Festival de Música no Parque" aria-invalid={Boolean(errors.name)} aria-describedby={errors.name ? errorId('name') : undefined} onChange={(event) => updateField('name', event.target.value)} />
              <FieldError id={errorId('name')} message={errors.name} />
            </label>
            <label>
              Categoria
              <select value={form.category} aria-invalid={Boolean(errors.category)} aria-describedby={errors.category ? errorId('category') : undefined} onChange={(event) => updateField('category', event.target.value)}>
                <option value="">Selecione</option>
                {categories.map((category) => (
                  <option key={category.slug} value={category.slug}>{category.title}</option>
                ))}
              </select>
              <FieldError id={errorId('category')} message={errors.category} />
            </label>
          </div>

          <label>
            Descrição
            <textarea rows="5" maxLength={150} value={form.description} placeholder="Conte o que vai acontecer, para quem é o evento e o que o público pode esperar." aria-invalid={Boolean(errors.description)} aria-describedby={errors.description ? errorId('description') : undefined} onChange={(event) => updateField('description', event.target.value)} />
            <FieldError id={errorId('description')} message={errors.description} />
          </label>

          <StructuredAddressFields
            value={form.addressFields}
            errors={errors}
            onChange={updateAddressFields}
            onResolutionChange={updateAddressResolution}
          />

          <div className={styles.row}>
            <label>
              Data de início
              <input type="date" min={todayDateInputValue()} max={maxEventStartDateInputValue()} value={form.startDate} aria-invalid={Boolean(errors.startDate)} aria-describedby={errors.startDate ? errorId('startDate') : undefined} onChange={(event) => updateField('startDate', event.target.value)} />
              <FieldError id={errorId('startDate')} message={errors.startDate} />
            </label>
            <label>
              Horário de início
              <input type="time" step="60" value={form.startTime} aria-invalid={Boolean(errors.startTime)} aria-describedby={errors.startTime ? errorId('startTime') : undefined} onChange={(event) => updateField('startTime', event.target.value)} />
              <FieldError id={errorId('startTime')} message={errors.startTime} />
            </label>
          </div>

          <div className={styles.row}>
            <label>
              Data de término
              <input type="date" min={form.startDate || todayDateInputValue()} value={form.endDate} aria-invalid={Boolean(errors.endDate)} aria-describedby={errors.endDate ? errorId('endDate') : undefined} onChange={(event) => updateField('endDate', event.target.value)} />
              <FieldError id={errorId('endDate')} message={errors.endDate} />
            </label>
            <label>
              Horário de término
              <input type="time" step="60" value={form.endTime} aria-invalid={Boolean(errors.endTime)} aria-describedby={errors.endTime ? errorId('endTime') : undefined} onChange={(event) => updateField('endTime', event.target.value)} />
              <FieldError id={errorId('endTime')} message={errors.endTime} />
            </label>
          </div>

          <CoverImageUpload
            file={coverFile}
            preview={coverPreview}
            error={errors.cover}
            onChange={handleCoverChange}
            onRemove={removeCover}
          />

          <div className={styles.actions}>
            <button className={styles.secondaryButton} type="button" onClick={saveDraft}>Salvar rascunho</button>
            <button className={styles.secondaryButton} type="submit">Visualizar prévia</button>
            <button className={styles.primaryButton} type="button" disabled={publishing} onClick={publishEvent}>{publishing ? 'Enviando...' : 'Enviar para análise'}</button>
          </div>
        </form>
      </section>
      {mode === 'preview' && (
        <EventDetailsModal
          event={buildPreviewEvent()}
          preview
          onClose={() => setMode('edit')}
        />
      )}
      {draftInfoOpen && (
        <InfoDialog
          title="Rascunhos indisponíveis"
          message="Salvar eventos como rascunho ainda não está disponível nesta versão do Luppa. Você pode continuar editando a página ou enviar o evento para análise."
          onPrimary={() => setDraftInfoOpen(false)}
        />
      )}
    </main>
  )
}

export default CadastroEventoLuppa
