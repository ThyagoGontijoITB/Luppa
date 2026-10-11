import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import EventDetailsModal from '../../components/events/EventDetailsModal'
import EventImage from '../../components/events/EventImage'
import { useToast } from '../../components/common/ToastProvider'
import { formatEventDateTime } from '../../services/eventDateService'
import { updateEventStatus } from '../../services/eventService'
import styles from './AdminLuppa.module.css'

const AdminModeration = () => {
  const { events, setEvents, refreshAdminData } = useOutletContext()
  const toast = useToast()
  const [reviewEvent, setReviewEvent] = useState(null)
  const [rejectEvent, setRejectEvent] = useState(null)
  const [reason, setReason] = useState('')
  const [pendingAction, setPendingAction] = useState('')
  const pendingEvents = events.filter((event) => event.status === 'pending_review')

  const approveEvent = async (event) => {
    if (pendingAction) return
    setPendingAction(`approve:${event.id}`)
    try {
      const updated = await updateEventStatus(event.id, 'published')
      setEvents((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      setReviewEvent(null)
      toast.success('Evento aprovado.')
      refreshAdminData?.()
    } catch {
      toast.error('Não foi possível aprovar o evento.')
    } finally {
      setPendingAction('')
    }
  }

  const rejectSelectedEvent = async () => {
    if (!reason.trim() || !rejectEvent || pendingAction) return
    setPendingAction(`reject:${rejectEvent.id}`)
    try {
      const updated = await updateEventStatus(rejectEvent.id, 'cancelled')
      setEvents((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      setRejectEvent(null)
      setReason('')
      toast.success('Evento cancelado. O backend não possui status de rejeição nem motivo de rejeição.')
      refreshAdminData?.()
    } catch {
      toast.error('Não foi possível rejeitar o evento.')
    } finally {
      setPendingAction('')
    }
  }

  return (
    <>
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <div>
            <h2>Eventos para análise</h2>
            <p>Fila de eventos em análise pela API.</p>
          </div>
        </div>

        {pendingEvents.length === 0 ? <p className={styles.empty}>Nenhum evento aguardando análise.</p> : (
          <div className={styles.reviewList}>
            {pendingEvents.map((event) => (
              <article className={styles.reviewCard} key={event.id}>
                <EventImage src={event.coverImageUrl} alt={event.title} fallbackLabel={event.categoryName} />
                <div>
                  <span>{event.categoryName}</span>
                  <h3>{event.title}</h3>
                  <p>{event.creatorName} · {event.locationName}</p>
                  <small>{formatEventDateTime(event.startDate, event.startTime)} · enviado em {new Date(event.submittedAt || event.createdAt).toLocaleDateString('pt-BR')}</small>
                </div>
                <button className={styles.primaryButton} type="button" onClick={() => setReviewEvent(event)}>Analisar</button>
              </article>
            ))}
          </div>
        )}
      </section>

      {reviewEvent && (
        <div className={styles.reviewOverlay}>
          <EventDetailsModal event={reviewEvent} preview onClose={() => setReviewEvent(null)} />
          <div className={styles.reviewActions}>
            <button className={styles.primaryButton} type="button" disabled={Boolean(pendingAction)} onClick={() => approveEvent(reviewEvent)}>
              {pendingAction === `approve:${reviewEvent.id}` ? 'Aprovando...' : 'Aprovar evento'}
            </button>
            <button className={styles.dangerButton} type="button" disabled={Boolean(pendingAction)} onClick={() => setRejectEvent(reviewEvent)}>Rejeitar evento</button>
          </div>
        </div>
      )}

      {rejectEvent && (
        <div className={styles.modalBackdrop} role="dialog" aria-modal="true" aria-labelledby="reject-title">
          <div className={styles.modal}>
            <h2 id="reject-title">Rejeitar evento</h2>
            <p>Informe o motivo para orientar o organizador.</p>
            <label className={styles.rejectField}>
              Motivo da rejeição
              <textarea value={reason} rows="4" onChange={(event) => setReason(event.target.value)} />
            </label>
            <div className={styles.actions}>
              <button className={styles.dangerButton} type="button" disabled={!reason.trim() || Boolean(pendingAction)} onClick={rejectSelectedEvent}>
                {rejectEvent && pendingAction === `reject:${rejectEvent.id}` ? 'Rejeitando...' : 'Rejeitar evento'}
              </button>
              <button className={styles.secondaryButton} type="button" disabled={Boolean(pendingAction)} onClick={() => setRejectEvent(null)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

export default AdminModeration
