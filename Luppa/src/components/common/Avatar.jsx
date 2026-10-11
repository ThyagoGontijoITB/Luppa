import styles from './Avatar.module.css'

const getInitials = (name = '') => {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return 'LU'
  return parts.slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

const Avatar = ({ user, size = 'md' }) => {
  const label = user?.name || 'Usuário'

  if (user?.photoUrl) {
    return <img className={`${styles.avatar} ${styles[size]}`} src={user.photoUrl} alt={label} />
  }

  return (
    <span className={`${styles.avatar} ${styles[size]}`} aria-label={label}>
      {getInitials(label)}
    </span>
  )
}

export default Avatar
