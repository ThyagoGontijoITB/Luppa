import { useEffect, useState } from 'react'
import { CalendarIcon, LocationIcon, UsersIcon } from '../common/Icons'
import StarRating from '../common/StarRating'
import { formatEventDate, formatEventTime, getEventEnd, getEventStart } from '../../services/eventDateService'
import { formatParticipants, getEventStatusLabel } from '../../services/eventService'
import EventImage from './EventImage'
import EventMap from './EventMap'
import EventRating from './EventRating'
import EventReportDialog from './EventReportDialog'
import EventShareDialog from './EventShareDialog'
import { useToast } from '../common/ToastProvider'
import { createReport } from '../../services/reportService'
import styles from './EventDetailsModal.module.css'

const buildShareUrl = (event) => {
  if (event?.id && typeof window !== 'undefined') {
    return `${window.location.origin}${window.location.pathname}#event-${event.id}`
  }

  return typeof window !== 'undefined' ? window.location.href : ''
}

const EventDetailsModal = ({ event, onClose, onInterest, onEventUpdated, preview = false }) => {
  const toast = useToast()
  const [shareOpen, setShareOpen] = useState(false)
  const [reportOpen, setReportOpen] = useState(false)
  const [reportedReview, setReportedReview] = useState(null)
  const [reports, setReports] = useState([])
  const [activeTab, setActiveTab] = useState('details')

  useEffect(() => {
    if (!event) return undefined
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [event])

  if (!event) return null

  const submitReport = async (payload) => {
    try {
      const result = await createReport(reports, payload)
      setReports((current) => [...current, result.report])
      setReportOpen(false)
      setReportedReview(null)
      toast.success('Denúncia enviada.')
    } catch {
      toast.error('Não foi possível enviar a denúncia.')
    }
  }

  return (
    <>
      <div className={styles.backdrop} role="dialog" aria-modal="true" aria-labelledby="event-details-title" onClick={onClose}>
        <div className={styles.modal} onClick={(clickEvent) => clickEvent.stopPropagation()}>
          <button className={styles.close} type="button" aria-label="Fechar detalhes" onClick={onClose}>
            ×
          </button>
          <EventImage src={event.coverImageUrl} alt={event.title} className={styles.image} fallbackLabel={event.categoryName} />
          <div className={styles.content}>
          <div className={styles.titleRow}>
            <span className={styles.category}>{event.categoryName}</span>
            <span className={`${styles.status} ${styles[event.status] || ''}`}>{getEventStatusLabel(event.status)}</span>
          </div>
          <h2 id="event-details-title">{event.title}</h2>
          <div className={styles.tabs} role="tablist" aria-label="Seções do evento">
            <button
              className={activeTab === 'details' ? styles.activeTab : ''}
              type="button"
              role="tab"
              aria-selected={activeTab === 'details'}
              aria-controls="event-details-panel"
              id="event-details-tab"
              onClick={() => setActiveTab('details')}
            >
              Detalhes
            </button>
            <button
              className={activeTab === 'reviews' ? styles.activeTab : ''}
              type="button"
              role="tab"
              aria-selected={activeTab === 'reviews'}
              aria-controls="event-reviews-panel"
              id="event-reviews-tab"
              onClick={() => setActiveTab('reviews')}
            >
              Avaliações
            </button>
          </div>
          <div
            id="event-details-panel"
            role="tabpanel"
            aria-labelledby="event-details-tab"
            hidden={activeTab !== 'details'}
            className={styles.tabPanel}
          >
            <div className={styles.detailBlock}>
              <span>Descrição</span>
              <p>{event.description}</p>
            </div>
            <div className={styles.info}>
              <div className={styles.row}>
                <CalendarIcon className={styles.icon} />
                <span><strong>Início</strong>{formatEventDate(getEventStart(event))} às {formatEventTime(event.startTime)}</span>
              </div>
              <div className={styles.row}>
                <CalendarIcon className={styles.icon} />
                <span><strong>Término</strong>{formatEventDate(getEventEnd(event))} às {formatEventTime(event.endTime)}</span>
              </div>
              <div className={styles.row}>
                <LocationIcon className={styles.icon} />
                <span><strong>Local</strong>{event.locationName}<small>{event.address}</small></span>
              </div>
              <div className={styles.row}>
                <UsersIcon className={styles.icon} />
                <span><strong>Interesse</strong>{formatParticipants(event)}</span>
              </div>
              {event.creatorName && (
                <div className={styles.organizer}>
                  <div className={styles.organizerHeader}>
                    <UsersIcon className={styles.icon} />
                    <span><strong>Organizado por</strong>{event.creatorName}</span>
                  </div>
                  {event.organizerReputation ? (
                    <div className={styles.reputation} aria-label="Reputação demonstrativa do organizador">
                      <StarRating value={event.organizerReputation.rating} readOnly label="Reputação do organizador" />
                      <span>{event.organizerReputation.reviewCount} avaliações</span>
                      <span>{event.organizerReputation.completedEvents} eventos realizados</span>
                      <button type="button">Ver perfil</button>
                    </div>
                  ) : (
                    <small className={styles.demoNote}>Reputação do organizador ainda não disponível.</small>
                  )}
                  {event.organizerSocials && (
                    <div className={styles.socialLinks} aria-label="Links sociais do organizador">
                      {event.organizerSocials.instagram && <a href={event.organizerSocials.instagram} target="_blank" rel="noreferrer">Instagram</a>}
                      {event.organizerSocials.facebook && <a href={event.organizerSocials.facebook} target="_blank" rel="noreferrer">Facebook</a>}
                      {event.organizerSocials.website && <a href={event.organizerSocials.website} target="_blank" rel="noreferrer">Site</a>}
                    </div>
                  )}
                </div>
              )}
            </div>
            <EventMap event={event} />
            <div className={styles.actions}>
              <button className={styles.action} type="button" onClick={() => onInterest?.(event)} disabled={preview}>
                Tenho interesse
              </button>
              <button className={styles.secondaryAction} type="button" onClick={() => setShareOpen(true)}>
                Compartilhar
              </button>
              <button
                className={styles.reportAction}
                type="button"
                onClick={() => {
                  setReportedReview(null)
                  setReportOpen(true)
                }}
              >
                Denunciar evento
              </button>
            </div>
          </div>
          <div
            id="event-reviews-panel"
            role="tabpanel"
            aria-labelledby="event-reviews-tab"
            hidden={activeTab !== 'reviews'}
            className={styles.tabPanel}
          >
            <EventRating
              event={event}
              onEventUpdated={onEventUpdated}
              onReportReview={(review) => {
                setReportedReview(review)
                setReportOpen(true)
              }}
            />
          </div>
          </div>
        </div>
      </div>
      {shareOpen && (
        <EventShareDialog
          event={event}
          shareUrl={buildShareUrl(event)}
          toast={toast}
          onClose={() => setShareOpen(false)}
        />
      )}
      {reportOpen && (
        <EventReportDialog
          event={event}
          ratingId={reportedReview?.id}
          targetType={reportedReview ? 'review' : 'event'}
          subjectLabel={reportedReview ? `${reportedReview.author.name} · ${reportedReview.description || 'Avaliação sem comentário'}` : event.title}
          onClose={() => {
            setReportOpen(false)
            setReportedReview(null)
          }}
          onSubmit={submitReport}
        />
      )}
    </>
  )
}

export default EventDetailsModal
