import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/authContext'
import { permissions } from '../../services/permissionService'
import Avatar from './Avatar'
import styles from './UserMenu.module.css'

const UserMenu = ({ className = '' }) => {
  const auth = useAuth()
  const [open, setOpen] = useState(false)
  const menuRef = useRef(null)

  useEffect(() => {
    if (!open) return undefined

    const handlePointerDown = (event) => {
      if (!menuRef.current?.contains(event.target)) {
        setOpen(false)
      }
    }

    window.addEventListener('pointerdown', handlePointerDown)
    return () => window.removeEventListener('pointerdown', handlePointerDown)
  }, [open])

  if (!auth.isAuthenticated) {
    return <Link className={`${styles.loginButton} ${className}`} to="/login-cadastro" data-global-nav-action="true">Login/Cadastro</Link>
  }

  const canAccessAdmin = auth.hasPermission(permissions.ACCESS_ADMIN)

  return (
    <div className={`${styles.menu} ${className}`} ref={menuRef}>
      <button
        className={styles.trigger}
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((isOpen) => !isOpen)}
      >
        <Avatar user={auth.user} size="sm" />
        <span>{auth.name}</span>
        <b aria-hidden="true">⌄</b>
      </button>
      {open && (
        <div className={styles.dropdown} role="menu">
          <Link role="menuitem" to="/profile" onClick={() => setOpen(false)}>Perfil</Link>
          <Link role="menuitem" to="/my-events" onClick={() => setOpen(false)}>Meus eventos</Link>
          {canAccessAdmin && <Link role="menuitem" to="/admin" onClick={() => setOpen(false)}>Administração</Link>}
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              auth.logout()
              setOpen(false)
            }}
          >
            Sair
          </button>
        </div>
      )}
    </div>
  )
}

export default UserMenu
