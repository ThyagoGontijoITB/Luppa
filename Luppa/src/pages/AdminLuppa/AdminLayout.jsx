import { NavLink } from 'react-router-dom'
import LuppaSymbol from '../../components/common/LuppaSymbol'
import Avatar from '../../components/common/Avatar'
import { useAuth } from '../../auth/authContext'
import styles from './AdminLuppa.module.css'

const AdminLayout = ({ children }) => {
  const auth = useAuth()

  return (
    <div className={styles.adminPage}>
      <aside className={styles.sidebar}>
        <div className={styles.sidebarBrand}>
          <LuppaSymbol />
          <strong>Luppa Admin</strong>
        </div>
        <nav className={styles.sidebarNav}>
          <NavLink to="/admin" end>Visão geral</NavLink>
          <NavLink to="/admin/insights">Insights</NavLink>
          <NavLink to="/admin/users">Usuários</NavLink>
          <NavLink to="/admin/events">Eventos</NavLink>
          <NavLink to="/admin/review">Eventos para análise</NavLink>
          <NavLink to="/admin/reports">Denúncias</NavLink>
          <NavLink to="/admin/audit">Auditoria</NavLink>
        </nav>
        <div className={styles.sidebarFooter}>
          <NavLink to="/">← Ir para o Luppa</NavLink>
          <div className={styles.adminAccount}>
            <Avatar user={auth.user} size="sm" />
            <span>{auth.name}</span>
          </div>
        </div>
      </aside>
      <main className={styles.adminMain}>
        <header className={styles.adminHeader}>
          <div>
            <span>Admin Dashboard</span>
            <h1>Luppa Admin</h1>
          </div>
          <button type="button" onClick={auth.logout}>Sair</button>
        </header>
        {children}
      </main>
    </div>
  )
}

export default AdminLayout
