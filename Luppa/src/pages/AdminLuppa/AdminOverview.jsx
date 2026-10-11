import { useOutletContext } from 'react-router-dom'
import { userRoles } from '../../constants/users'
import styles from './AdminLuppa.module.css'

const AdminOverview = () => {
  const { users, events } = useOutletContext()
  const totalCreators = users.filter((user) => user.role === userRoles.CREATOR).length

  const cards = [
    { label: 'Total de usuários', value: users.length },
    { label: 'Criadores', value: totalCreators },
    { label: 'Total de eventos', value: events.length },
    { label: 'Publicados', value: events.filter((event) => event.status === 'published').length },
    { label: 'Rascunhos', value: events.filter((event) => event.status === 'draft').length },
    { label: 'Cancelados', value: events.filter((event) => event.status === 'cancelled').length },
  ]

  return (
    <section className={styles.panel}>
      <div className={styles.panelHeader}>
        <div>
          <h2>Visão geral</h2>
          <p>Métricas calculadas a partir dos dados carregados da API.</p>
        </div>
      </div>
      <div className={styles.metricGrid}>
        {cards.map((card) => (
          <article className={styles.metricCard} key={card.label}>
            <span>{card.label}</span>
            <strong>{card.value}</strong>
          </article>
        ))}
      </div>
    </section>
  )
}

export default AdminOverview
