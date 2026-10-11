import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import styles from '../../pages/AdminLuppa/AdminLuppa.module.css'

const InfoDialog = ({ title, message, primaryLabel = 'Entendi', secondaryLabel, onPrimary, onSecondary }) => {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [])

  const dialog = (
    <div className={styles.modalBackdrop} role="dialog" aria-modal="true" aria-labelledby="info-dialog-title">
      <div className={styles.modal}>
        <h2 id="info-dialog-title">{title}</h2>
        <p>{message}</p>
        <div className={styles.actions}>
          <button className={styles.primaryButton} type="button" onClick={onPrimary}>{primaryLabel}</button>
          {secondaryLabel && <button className={styles.secondaryButton} type="button" onClick={onSecondary}>{secondaryLabel}</button>}
        </div>
      </div>
    </div>
  )

  return createPortal(dialog, document.body)
}

export default InfoDialog
