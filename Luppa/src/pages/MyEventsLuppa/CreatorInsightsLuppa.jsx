import useEventRevision from '../../hooks/useEventRevision'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import LuppaSymbol from '../../components/common/LuppaSymbol'
import EventInsightsDashboard from '../../components/insights/EventInsightsDashboard'
import { fetchCreatorEventInsights } from '../../services/eventInsightsService'
import styles from './MyEventsLuppa.module.css'

const CreatorInsightsLuppa = () => {
  const eventRevision = useEventRevision()
  const [insights, setInsights] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    fetchCreatorEventInsights()
      .then((data) => {
        if (active) {
          setInsights(data)
          setError('')
        }
      })
      .catch(() => {
        if (active) setError('Não foi possível carregar seus insights.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [eventRevision])

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <Link to="/" aria-label="Luppa"><LuppaSymbol /></Link>
        <nav>
          <Link to="/">← Voltar para o Luppa</Link>
          <Link to="/my-events">Meus eventos</Link>
          <Link to="/cadastrar-evento">Criar evento</Link>
        </nav>
      </header>

      <section className={styles.hero}>
        <span>CREATOR</span>
        <h1>Insights dos meus eventos</h1>
        <p>Métricas calculadas apenas a partir dos eventos associados à sua conta.</p>
      </section>

      {loading ? (
        <p className={styles.empty}>Carregando insights...</p>
      ) : error ? (
        <p className={styles.empty}>{error}</p>
      ) : insights.summary.totalEvents === 0 ? (
        <section className={styles.empty}>
          <h2>Nenhum dado disponível.</h2>
          <p>Crie eventos para acompanhar salvamentos, avaliações e distribuição por status.</p>
          <Link to="/cadastrar-evento">Criar evento</Link>
        </section>
      ) : (
        <section className={styles.insightsWrap}>
          <EventInsightsDashboard insights={insights} scope="creator" />
        </section>
      )}
    </main>
  )
}

export default CreatorInsightsLuppa
