import { useState } from 'react'
import { useAuth } from '../../auth/authContext'
import Avatar from '../common/Avatar'
import ConfirmDialog from '../common/ConfirmDialog'
import StarRating from '../common/StarRating'
import { useToast } from '../common/ToastProvider'
import { canRateEvent } from '../../services/eventDateService'
import { deleteOwnEventRating, saveOwnEventRating } from '../../services/eventService'
import { formatRatingValue } from '../../utils/ratingFormat'
import styles from './EventRating.module.css'

const formatReviewDate = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
}

const EventRating = ({ event, onEventUpdated, onReportReview }) => {
  const auth = useAuth()
  const toast = useToast()
  const [rating, setRating] = useState(event.userReview?.rating || 0)
  const [comment, setComment] = useState(event.userReview?.comment || '')
  const [submitted, setSubmitted] = useState(Boolean(event.userReview))
  const [expanded, setExpanded] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [openMenu, setOpenMenu] = useState('')
  const rateable = canRateEvent(event)
  const reviews = event.reviews || []
  const currentUserRating = submitted ? rating : event.userReview?.rating
  const actionLabel = currentUserRating
    ? `Sua avaliação: ${formatRatingValue(currentUserRating)} ★`
    : 'Avaliar evento'
  const hasAggregateRating = Boolean(event.eventRating?.reviewCount)
  const aggregateRating = event.eventRating && (
    <div className={styles.aggregateRating}>
      <StarRating value={event.eventRating.rating} readOnly label="Avaliação média do evento" />
      <small>{event.eventRating.reviewCount} avaliações</small>
    </div>
  )

  const reportReview = (review) => {
    setOpenMenu('')
    if (!auth.isAuthenticated) {
      toast.error('Entre na sua conta para denunciar uma avaliação.')
      return
    }
    onReportReview?.(review)
  }

  const reviewList = (
    <div className={styles.reviewList}>
      {reviews.length === 0 ? (
        <p className={styles.emptyReviews}>{rateable ? 'Nenhuma avaliação publicada ainda.' : 'As avaliações serão liberadas depois que o evento terminar.'}</p>
      ) : reviews.map((review) => (
        <article className={styles.reviewItem} key={review.id}>
          <Avatar user={review.author} size="sm" />
          <div className={styles.reviewBody}>
            <div className={styles.reviewTopline}>
              <div>
                <strong>{review.author.name}</strong>
                <small>{formatReviewDate(review.ratedAt)}</small>
              </div>
              <div className={styles.reviewActions}>
                <StarRating value={review.score} readOnly label={`Avaliação de ${review.author.name}`} />
                <button
                  className={styles.menuButton}
                  type="button"
                  aria-label="Mais opções da avaliação"
                  aria-haspopup="menu"
                  aria-expanded={openMenu === review.id}
                  onClick={() => setOpenMenu((current) => (current === review.id ? '' : review.id))}
                >
                  ...
                </button>
                {openMenu === review.id && (
                  <div className={styles.reviewMenu} role="menu">
                    <button type="button" role="menuitem" onClick={() => reportReview(review)}>Denunciar avaliação</button>
                  </div>
                )}
              </div>
            </div>
            {review.description && <p>{review.description}</p>}
          </div>
        </article>
      ))}
    </div>
  )

  if (!rateable) {
    return (
      <section className={styles.ratingPanel}>
        <div className={styles.ratingHeader}>
          <div>
            <span className={styles.eyebrow}>Avaliações</span>
            {hasAggregateRating ? aggregateRating : <p>As avaliações serão liberadas depois que o evento terminar.</p>}
          </div>
        </div>
        {reviewList}
      </section>
    )
  }

  return (
    <section className={styles.ratingPanel}>
      <div className={styles.ratingHeader}>
        <div>
          <span className={styles.eyebrow}>Avaliações</span>
          {aggregateRating || <p>Este evento ainda não tem avaliações agregadas.</p>}
        </div>
        {auth.isAuthenticated && (
          <button className={styles.toggleButton} type="button" onClick={() => setExpanded(true)}>
            {actionLabel}
          </button>
        )}
      </div>

      {auth.isAuthenticated && expanded ? (
        <form
          className={styles.form}
          onSubmit={async (submitEvent) => {
            submitEvent.preventDefault()
            if (rating > 0) {
              setSaving(true)
              try {
                const updatedEvent = await saveOwnEventRating(event.id, { rating, comment })
                setSubmitted(true)
                setError('')
                onEventUpdated?.(updatedEvent)
                toast.success('Avaliação enviada.')
                setExpanded(false)
              } catch {
                setError('Não foi possível salvar sua avaliação.')
                toast.error('Não foi possível salvar sua avaliação.')
              } finally {
                setSaving(false)
              }
            }
          }}
        >
          <label>Sua avaliação</label>
          <StarRating value={rating} onChange={setRating} label="Sua avaliação do evento" />
          <textarea
            value={comment}
            placeholder="Comentário opcional"
            rows="3"
            onChange={(eventChange) => setComment(eventChange.target.value)}
          />
          <div className={styles.formActions}>
            {submitted && (
              <button className={styles.deleteButton} type="button" disabled={saving} onClick={() => setConfirmDelete(true)}>
                Excluir avaliação
              </button>
            )}
            <button type="button" disabled={saving} onClick={() => setExpanded(false)}>Cancelar</button>
            <button type="submit" disabled={rating === 0 || saving}>{saving ? 'Enviando...' : 'Enviar avaliação'}</button>
          </div>
          {error && <p className={styles.note}>{error}</p>}
        </form>
      ) : !auth.isAuthenticated ? (
        <p className={styles.note}>Entre na sua conta para avaliar eventos encerrados.</p>
      ) : null}
      {reviewList}
      {confirmDelete && (
        <ConfirmDialog
          title="Excluir avaliação?"
          message="Sua nota e comentário serão removidos deste evento."
          confirmLabel="Excluir avaliação"
          onCancel={() => setConfirmDelete(false)}
          onConfirm={async () => {
            if (deleting) return
            setDeleting(true)
            try {
              const updatedEvent = await deleteOwnEventRating(event.id)
              setRating(0)
              setComment('')
              setSubmitted(false)
              setExpanded(false)
              setConfirmDelete(false)
              setError('')
              onEventUpdated?.(updatedEvent)
              toast.success('Avaliação removida.')
            } catch {
              setError('Não foi possível excluir sua avaliação.')
              toast.error('Não foi possível excluir sua avaliação.')
              setConfirmDelete(false)
            } finally {
              setDeleting(false)
            }
          }}
        />
      )}
    </section>
  )
}

export default EventRating
