import { useEffect, useState } from 'react'
import { useAuth } from '../../auth/authContext'
import { getReportCategories } from '../../services/reportService'
import styles from './EventReportDialog.module.css'

const EventReportDialog = ({ event, ratingId, targetType = 'event', subjectLabel, onClose, onSubmit }) => {
  const auth = useAuth()
  const [reason, setReason] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [categories, setCategories] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const isReviewReport = targetType === 'review'
  const title = isReviewReport ? 'Denunciar avaliação' : 'Denunciar evento'
  const targetText = isReviewReport ? 'esta avaliação' : 'este evento'

  useEffect(() => {
    let active = true
    getReportCategories()
      .then((items) => {
        if (active) setCategories(items)
      })
      .catch(() => {
        if (active) setError('Não foi possível carregar os motivos de denúncia.')
      })
    return () => {
      active = false
    }
  }, [])

  const handleSubmit = async (submitEvent) => {
    submitEvent.preventDefault()
    if (submitting) return

    if (!auth.isAuthenticated) {
      setError('Entre na sua conta para enviar uma denúncia.')
      return
    }

    if (!reason) {
      setError('Selecione um motivo.')
      return
    }

    if (!description.trim()) {
      setError('Descreva o motivo da denúncia.')
      return
    }

    setSubmitting(true)
    try {
      await onSubmit({
        eventId: event.id,
        ratingId,
        categoryId: Number(reason),
        description: description.trim(),
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.backdrop} role="dialog" aria-modal="true" aria-labelledby="report-title" onClick={onClose}>
      <form className={styles.dialog} noValidate onClick={(clickEvent) => clickEvent.stopPropagation()} onSubmit={handleSubmit}>
        <div>
          <span>Denúncia</span>
          <h2 id="report-title">{title}</h2>
          <p>Informe por que {targetText} deve ser analisado pela moderação.</p>
          {subjectLabel && <small className={styles.subject}>{subjectLabel}</small>}
        </div>

        <label>
          Motivo
          <select value={reason} onChange={(changeEvent) => setReason(changeEvent.target.value)}>
            <option value="">Selecione um motivo</option>
            {categories.map((item) => <option key={item.id} value={item.id}>{item.category}</option>)}
          </select>
        </label>

        <label>
          Descrição
          <textarea value={description} rows="4" placeholder="Descreva o problema" onChange={(changeEvent) => setDescription(changeEvent.target.value)} />
        </label>

        {error && <p className={styles.error}>{error}</p>}

        <div className={styles.actions}>
          <button type="submit" disabled={submitting}>{submitting ? 'Enviando...' : 'Enviar denúncia'}</button>
          <button type="button" disabled={submitting} onClick={onClose}>Cancelar</button>
        </div>
      </form>
    </div>
  )
}

export default EventReportDialog
