import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/authContext'
import { userRoles } from '../../constants/users'
import InfoDialog from './InfoDialog'
import styles from './CreatorActionLink.module.css'

const creatorRoles = [userRoles.CREATOR, userRoles.ADMIN, userRoles.LUPPA_ADMIN]

const CreatorActionLink = ({ children, className, onClick, to = '/cadastrar-evento', variant = 'plain' }) => {
  const auth = useAuth()
  const [blocked, setBlocked] = useState(false)
  const isNavAction = variant === 'nav'
  const linkClassName = className || (isNavAction ? styles.navAction : undefined)

  const handleClick = (event) => {
    if (auth.isAuthenticated && !creatorRoles.includes(auth.role)) {
      event.preventDefault()
      onClick?.(event)
      setBlocked(true)
      return
    }

    onClick?.(event)
  }

  return (
    <>
      <Link className={linkClassName} to={to} onClick={handleClick} data-global-nav-action={isNavAction ? 'true' : undefined}>
        {children}
      </Link>
      {blocked && (
        <InfoDialog
          title="Conta de criador necessária"
          message="Sua conta atual é do tipo padrão e não possui permissão para publicar eventos. A alteração para uma conta de criador ainda não está disponível nesta versão do Luppa."
          primaryLabel="Entendi"
          onPrimary={() => setBlocked(false)}
        />
      )}
    </>
  )
}

export default CreatorActionLink
