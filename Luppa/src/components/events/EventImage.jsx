import { useEffect, useState } from 'react'
import styles from './EventImage.module.css'

const EventImage = ({ src, alt, className = '', fallbackLabel = 'Luppa' }) => {
  const [failed, setFailed] = useState(!src)

  useEffect(() => {
    setFailed(!src)
  }, [src])

  if (failed) {
    return (
      <div className={`${styles.fallback} ${className}`} role="img" aria-label={alt || 'Imagem indisponível'}>
        <span>{fallbackLabel}</span>
      </div>
    )
  }

  return (
    <img
      src={src}
      alt={alt}
      className={className}
      loading="lazy"
      onError={() => setFailed(true)}
    />
  )
}

export default EventImage
