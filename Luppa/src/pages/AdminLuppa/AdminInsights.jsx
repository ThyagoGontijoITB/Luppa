import { useEffect, useState } from 'react'
import EventInsightsDashboard from '../../components/insights/EventInsightsDashboard'
import { fetchPlatformEventInsights } from '../../services/eventInsightsService'
import styles from './AdminLuppa.module.css'

const AdminInsights = () => {
  const [insights, setInsights] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    fetchPlatformEventInsights()
      .then((data) => {
        if (active) {
          setInsights(data)
          setError('')
        }
      })
      .catch(() => {
        if (active) setError('Não foi possível carregar os insights da plataforma.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  return (
    <section className={styles.panel}>
      <div className={styles.panelHeader}>
        <div>
          <h2>Insights de eventos</h2>
          <p>Métricas agregadas com base em eventos, salvamentos e avaliações registrados na base.</p>
        </div>
      </div>

      {loading ? (
        <p className={styles.empty}>Carregando insights...</p>
      ) : error ? (
        <p className={styles.empty}>{error}</p>
      ) : insights.summary.totalEvents === 0 ? (
        <p className={styles.empty}>Nenhum evento encontrado para calcular analytics.</p>
      ) : (
        <EventInsightsDashboard insights={insights} scope="admin" />
      )}
    </section>
  )
}

export default AdminInsights
