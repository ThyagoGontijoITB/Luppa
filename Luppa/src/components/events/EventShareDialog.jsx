import { useEffect, useMemo, useRef, useState } from 'react'
import { CalendarIcon, CloseIcon, CopyIcon, FacebookIcon, LocationIcon, MoreIcon, WhatsAppIcon, XIcon } from '../common/Icons'
import EventImage from './EventImage'
import { formatEventDateTime } from '../../services/eventDateService'
import styles from './EventShareDialog.module.css'

const shareTargets = [
  {
    id: 'whatsapp',
    label: 'WhatsApp',
    Icon: WhatsAppIcon,
    href: ({ text, url }) => `https://wa.me/?text=${encodeURIComponent(`${text} ${url}`)}`,
  },
  {
    id: 'x',
    label: 'X',
    Icon: XIcon,
    href: ({ text, url }) => `https://twitter.com/intent/tweet?text=${encodeURIComponent(text)}&url=${encodeURIComponent(url)}`,
  },
  {
    id: 'facebook',
    label: 'Facebook',
    Icon: FacebookIcon,
    href: ({ url }) => `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`,
  },
]

const EventShareDialog = ({ event, shareUrl, onClose, toast }) => {
  const closeButtonRef = useRef(null)
  const previousFocusRef = useRef(null)
  const [copied, setCopied] = useState(false)
  const canUseNativeShare = typeof navigator !== 'undefined' && typeof navigator.share === 'function'
  const dateLabel = formatEventDateTime(event.startDate, event.startTime)
  const locationLabel = event.locationName || event.city || event.address || ''
  const shareText = useMemo(() => `Confira este evento no Luppa: ${event.title}`, [event.title])
  const shareData = useMemo(() => ({
    title: event.title,
    text: event.description || shareText,
    url: shareUrl,
  }), [event.description, event.title, shareText, shareUrl])

  useEffect(() => {
    previousFocusRef.current = document.activeElement
    closeButtonRef.current?.focus()

    const handleKeyDown = (keyboardEvent) => {
      if (keyboardEvent.key === 'Escape') {
        keyboardEvent.stopPropagation()
        onClose()
        return
      }

      if (keyboardEvent.key === 'Tab') {
        const focusableElements = Array.from(document.querySelectorAll(
          `.${styles.dialog} a[href], .${styles.dialog} button:not(:disabled), .${styles.dialog} input:not(:disabled), .${styles.dialog} select:not(:disabled), .${styles.dialog} textarea:not(:disabled), .${styles.dialog} [tabindex]:not([tabindex="-1"])`,
        ))

        if (focusableElements.length === 0) return
        const firstElement = focusableElements[0]
        const lastElement = focusableElements[focusableElements.length - 1]

        if (keyboardEvent.shiftKey && document.activeElement === firstElement) {
          keyboardEvent.preventDefault()
          lastElement.focus()
        } else if (!keyboardEvent.shiftKey && document.activeElement === lastElement) {
          keyboardEvent.preventDefault()
          firstElement.focus()
        }
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      previousFocusRef.current?.focus?.({ preventScroll: true })
    }
  }, [onClose])

  const copyLink = async () => {
    try {
      if (!navigator.clipboard?.writeText) {
        throw new Error('clipboard_unavailable')
      }
      await navigator.clipboard.writeText(shareUrl)
      setCopied(true)
      toast?.success('Link copiado.')
      window.setTimeout(() => setCopied(false), 2200)
    } catch {
      toast?.error('Não foi possível copiar o link.')
    }
  }

  const openNativeShare = async () => {
    if (!canUseNativeShare) return
    try {
      await navigator.share(shareData)
    } catch (error) {
      if (error?.name !== 'AbortError') {
        toast?.error('Não foi possível abrir as opções de compartilhamento.')
      }
    }
  }

  return (
    <div className={styles.backdrop} role="presentation" onMouseDown={onClose}>
      <section
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="event-share-title"
        onMouseDown={(mouseEvent) => mouseEvent.stopPropagation()}
      >
        <header className={styles.header}>
          <div>
            <span>Compartilhamento</span>
            <h2 id="event-share-title">Compartilhar evento</h2>
          </div>
          <button ref={closeButtonRef} className={styles.closeButton} type="button" aria-label="Fechar compartilhamento" onClick={onClose}>
            <CloseIcon />
          </button>
        </header>

        <div className={styles.preview}>
          <EventImage src={event.coverImageUrl} alt={event.title} className={styles.previewImage} fallbackLabel={event.categoryName} />
          <div className={styles.previewBody}>
            <strong>{event.title}</strong>
            <span><CalendarIcon />{dateLabel}</span>
            {locationLabel && <span><LocationIcon />{locationLabel}</span>}
          </div>
        </div>

        <div className={styles.actions} aria-label="Compartilhar em">
          {shareTargets.map(({ id, label, Icon, href }) => (
            <a
              className={styles.shareTarget}
              key={id}
              href={href({ text: shareText, url: shareUrl })}
              target="_blank"
              rel="noreferrer"
            >
              <Icon />
              <span>{label}</span>
            </a>
          ))}
        </div>

        <div className={styles.copyBlock}>
          <label htmlFor="event-share-link">Link do evento</label>
          <div className={styles.linkRow}>
            <input id="event-share-link" type="text" value={shareUrl} readOnly onFocus={(focusEvent) => focusEvent.target.select()} />
            <button type="button" onClick={copyLink}>
              <CopyIcon />
              {copied ? 'Copiado' : 'Copiar link'}
            </button>
          </div>
        </div>

        {canUseNativeShare && (
          <button className={styles.nativeButton} type="button" onClick={openNativeShare}>
            <MoreIcon />
            Mais opções
          </button>
        )}
      </section>
    </div>
  )
}

export default EventShareDialog
