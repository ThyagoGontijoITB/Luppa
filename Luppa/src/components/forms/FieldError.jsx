import { ErrorIcon } from '../common/Icons'
import styles from './FieldError.module.css'

const FieldError = ({ id, message, className = '' }) => {
  if (!message) return null

  return (
    <span className={`${styles.error} ${className}`} id={id} role="alert">
      <ErrorIcon className={styles.icon} />
      <span>{message}</span>
    </span>
  )
}

export default FieldError
