import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/authContext'
import InfoDialog from '../components/common/InfoDialog'
import { userRoles } from '../constants/users'

const ProtectedRoute = ({ children, roles }) => {
  const auth = useAuth()
  const location = useLocation()

  if (auth.loading) return null

  if (!auth.isAuthenticated) {
    return <Navigate to="/login-cadastro" replace state={{ from: location.pathname }} />
  }

  if (roles?.length && !auth.hasRole(roles)) {
    const isCreatorRoute = roles.includes(userRoles.CREATOR)
    if (!isCreatorRoute) {
      return <Navigate to="/" replace state={{ toast: 'Você não tem permissão para acessar essa área.' }} />
    }

    return (
      <InfoDialog
        title="Conta de criador necessária"
        message="Sua conta atual é do tipo padrão e não possui permissão para publicar eventos. A alteração para uma conta de criador ainda não está disponível nesta versão do Luppa."
        primaryLabel="Entendi"
        onPrimary={() => window.history.back()}
      />
    )
  }

  return children
}

export default ProtectedRoute
