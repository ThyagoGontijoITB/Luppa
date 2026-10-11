import styles from '../../pages/AdminLuppa/AdminLuppa.module.css'

const ConfirmDialog = ({ title, message, confirmLabel = 'Confirmar', confirmDisabled = false, onConfirm, onCancel }) => (
  <div className={styles.modalBackdrop} role="dialog" aria-modal="true" aria-labelledby="confirm-title">
    <div className={styles.modal}>
      <h2 id="confirm-title">{title}</h2>
      <p>{message}</p>
      <div className={styles.actions}>
        <button className={styles.secondaryButton} type="button" disabled={confirmDisabled} onClick={onCancel}>Cancelar</button>
        <button className={styles.dangerButton} type="button" disabled={confirmDisabled} onClick={onConfirm}>{confirmLabel}</button>
      </div>
    </div>
  </div>
)

export default ConfirmDialog
