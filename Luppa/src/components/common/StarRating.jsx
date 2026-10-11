import { useId, useState } from 'react'
import { clampSelectedRating, formatRatingValue } from '../../utils/ratingFormat'
import styles from './StarRating.module.css'

const starValues = [1, 2, 3, 4, 5]

const getStarFill = (rating, starValue) => {
  const fill = (Number(rating || 0) - (starValue - 1)) * 100
  return Math.max(0, Math.min(100, fill))
}

const getPointerValue = (event, starValue) => {
  const rect = event.currentTarget.getBoundingClientRect()
  const isLeftHalf = event.clientX - rect.left <= rect.width / 2
  return clampSelectedRating(starValue - (isLeftHalf ? 0.5 : 0))
}

const StarIcon = ({ fill }) => (
  <span className={styles.starIcon} aria-hidden="true">
    <svg className={styles.starBase} viewBox="0 0 24 24" focusable="false">
      <path d="m12 2.7 2.85 5.78 6.38.93-4.62 4.5 1.09 6.35L12 17.26l-5.7 3 1.09-6.35-4.62-4.5 6.38-.93L12 2.7Z" />
    </svg>
    <span className={styles.starFill} style={{ width: `${fill}%` }}>
      <svg viewBox="0 0 24 24" focusable="false">
        <path d="m12 2.7 2.85 5.78 6.38.93-4.62 4.5 1.09 6.35L12 17.26l-5.7 3 1.09-6.35-4.62-4.5 6.38-.93L12 2.7Z" />
      </svg>
    </span>
  </span>
)

const StarRating = ({
  value = 0,
  onChange,
  readOnly = false,
  label = 'Nota',
  showValue = true,
  className = '',
}) => {
  const labelId = useId()
  const [previewValue, setPreviewValue] = useState(null)
  const selectedValue = Number(value || 0)
  const displayValue = previewValue ?? selectedValue

  const updateValue = (nextValue) => {
    if (!readOnly && onChange) onChange(clampSelectedRating(nextValue))
  }

  const handleKeyDown = (event) => {
    if (readOnly) return

    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
      event.preventDefault()
      updateValue((selectedValue || 0) + 0.5)
    }

    if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
      event.preventDefault()
      updateValue((selectedValue || 0.5) - 0.5)
    }
  }

  return (
    <div className={`${styles.rating} ${readOnly ? styles.readOnly : ''} ${className}`}>
      <div
        aria-labelledby={labelId}
        aria-readonly={readOnly || undefined}
        aria-valuemax={5}
        aria-valuemin={0.5}
        aria-valuenow={readOnly ? selectedValue : (selectedValue || 0)}
        aria-valuetext={`${formatRatingValue(displayValue)} de 5`}
        className={styles.stars}
        onKeyDown={handleKeyDown}
        onPointerLeave={() => setPreviewValue(null)}
        role="slider"
        tabIndex={readOnly ? -1 : 0}
      >
        <span className={styles.srOnly} id={labelId}>{label}</span>
        {starValues.map((starValue) => {
          const fill = getStarFill(displayValue, starValue)
          const content = <StarIcon fill={fill} />

          if (readOnly) {
            return <span className={styles.starSlot} key={starValue}>{content}</span>
          }

          return (
            <button
              aria-label={`${formatRatingValue(starValue - 0.5)} ou ${formatRatingValue(starValue)} de 5`}
              className={styles.starButton}
              key={starValue}
              onClick={(event) => updateValue(getPointerValue(event, starValue))}
              onPointerMove={(event) => setPreviewValue(getPointerValue(event, starValue))}
              type="button"
            >
              {content}
            </button>
          )
        })}
      </div>
      {showValue && <span className={styles.value}>{formatRatingValue(displayValue)} / 5</span>}
    </div>
  )
}

export default StarRating
