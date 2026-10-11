import { LocationIcon, UsersIcon } from '../common/Icons'
import { formatEventDateTime } from '../../services/eventDateService'
import { formatParticipants, getEventStatusLabel } from '../../services/eventService'
import EventImage from './EventImage'
import styles from './EventCard.module.css'

const EventCard = ({ event, onDetails, compact = false }) => (
  <article className={`${styles.card} ${compact ? styles.compact : ''}`}>
    <div className={styles.imageWrapper}>
      <EventImage src={event.coverImageUrl} alt={event.title} className={styles.image} fallbackLabel={event.categoryName} />
    </div>
    <div className={styles.content}>
      <div className={styles.metaRow}>
        <span className={styles.category}>{event.categoryName}</span>
        <span className={`${styles.status} ${styles[event.status] || ''}`}>{getEventStatusLabel(event.status)}</span>
      </div>
      <h3>{event.title}</h3>
      <div className={styles.info}>
        <span className={styles.date}>{formatEventDateTime(event.startDate, event.startTime)}</span>
      </div>
      <div className={styles.info}>
        <LocationIcon className={styles.icon} />
        <span>{event.locationName}</span>
      </div>
      <div className={styles.info}>
        <UsersIcon className={styles.icon} />
        <span>{formatParticipants(event)}</span>
      </div>
      {onDetails && (
        <button className={styles.button} type="button" onClick={() => onDetails(event)}>
          Ver detalhes
        </button>
      )}
    </div>
  </article>
)

export default EventCard
