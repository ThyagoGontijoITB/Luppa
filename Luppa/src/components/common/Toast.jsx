import styles from './Toast.module.css'

const toastMeta = {
  success: {
    title: 'Tudo certo',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="m6 12.2 3.4 3.4L18.5 6.8" />
      </svg>
    ),
  },
  error: {
    title: 'Algo deu errado',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 7v6" />
        <path d="M12 17.2v.1" />
        <path d="M12 3.8 21 20H3L12 3.8Z" />
      </svg>
    ),
  },
  warning: {
    title: 'Atenção',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 8v5" />
        <path d="M12 17.2v.1" />
        <path d="M12 3.8 21 20H3L12 3.8Z" />
      </svg>
    ),
  },
  info: {
    title: 'Informação',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 10.5v6" />
        <path d="M12 7.1v.1" />
        <circle cx="12" cy="12" r="8.5" />
      </svg>
    ),
  },
}

const Toast = ({ toasts = [], onClose }) => {
  if (!toasts.length) return null

  return (
    <div className={styles.viewport} aria-live="polite" aria-atomic="false">
      {toasts.map((toast) => {
        const meta = toastMeta[toast.tone] || toastMeta.success
        return (
          <div
            className={`${styles.toast} ${styles[toast.tone] || styles.success} ${toast.exiting ? styles.exiting : ''}`}
            key={toast.id}
            role={toast.tone === 'error' ? 'alert' : 'status'}
          >
            <span className={styles.iconWrap}>{meta.icon}</span>
            <span className={styles.copy}>
              <strong>{toast.title || meta.title}</strong>
              <span>{toast.message}</span>
            </span>
            <button className={styles.close} type="button" aria-label="Fechar notificação" onClick={() => onClose?.(toast.id)}>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m7 7 10 10M17 7 7 17" />
              </svg>
            </button>
          </div>
        )
      })}
    </div>
  )
}

export default Toast
