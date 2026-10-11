import { eventStatusLabels } from '../../constants/events'
import styles from './EventInsightsDashboard.module.css'

const formatNumber = (value) => new Intl.NumberFormat('pt-BR').format(value || 0)

const formatRating = (value) => (value === null || value === undefined ? 'Sem avaliações' : value.toLocaleString('pt-BR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
}))

const BarList = ({ title, items, emptyLabel }) => {
  const max = Math.max(...items.map((item) => item.count), 0)

  return (
    <section className={styles.panel}>
      <div className={styles.panelHeader}>
        <h2>{title}</h2>
      </div>
      {items.length === 0 ? (
        <p className={styles.empty}>{emptyLabel}</p>
      ) : (
        <div className={styles.barList}>
          {items.map((item) => (
            <div className={styles.barRow} key={item.key || item.label}>
              <div className={styles.barMeta}>
                <span>{item.label}</span>
                <strong>{formatNumber(item.count)}</strong>
              </div>
              <div className={styles.barTrack}>
                <span style={{ width: `${max ? Math.max((item.count / max) * 100, 4) : 0}%` }} />
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

const RankedEvents = ({ title, items, metric, emptyLabel }) => (
  <section className={styles.panel}>
    <div className={styles.panelHeader}>
      <h2>{title}</h2>
    </div>
    {items.length === 0 ? (
      <p className={styles.empty}>{emptyLabel}</p>
    ) : (
      <div className={styles.ranking}>
        {items.map((event, index) => (
          <article className={styles.rankItem} key={`${metric}-${event.eventId}`}>
            <span className={styles.rankNumber}>{index + 1}</span>
            <div>
              <h3>{event.title}</h3>
              <p>{event.categoryName || 'Sem categoria'} · {eventStatusLabels[event.status] || event.status}</p>
            </div>
            <strong>
              {metric === 'saves' ? formatNumber(event.saveCount) : formatRating(event.averageRating)}
              <small>{metric === 'saves' ? 'salvos' : `${formatNumber(event.reviewCount)} avaliações`}</small>
            </strong>
          </article>
        ))}
      </div>
    )}
  </section>
)

const EventInsightsDashboard = ({ insights, scope = 'creator' }) => {
  const summary = insights?.summary || {}
  const cards = [
    { label: 'Total de eventos', value: formatNumber(summary.totalEvents) },
    { label: 'Publicados', value: formatNumber(summary.publishedEvents) },
    { label: 'Rascunhos', value: formatNumber(summary.draftEvents) },
    { label: 'Salvamentos', value: formatNumber(summary.totalSaves) },
    { label: 'Avaliações', value: formatNumber(summary.totalReviews) },
    { label: 'Nota média', value: formatRating(summary.averageRating) },
  ]

  return (
    <div className={styles.dashboard}>
      <section className={styles.metricGrid} aria-label="Resumo de eventos">
        {cards.map((card) => (
          <article className={styles.metricCard} key={card.label}>
            <span>{card.label}</span>
            <strong>{card.value}</strong>
          </article>
        ))}
      </section>

      <div className={styles.chartGrid}>
        <BarList
          title="Eventos por status"
          items={insights.byStatus}
          emptyLabel="Nenhum evento encontrado para calcular status."
        />
        <BarList
          title="Eventos por categoria"
          items={insights.byCategory}
          emptyLabel="Nenhum evento encontrado para calcular categorias."
        />
      </div>

      <div className={styles.chartGrid}>
        <RankedEvents
          title="Mais salvos"
          items={insights.mostSavedEvents}
          metric="saves"
          emptyLabel={scope === 'creator' ? 'Seus eventos ainda não receberam salvamentos.' : 'Nenhum evento salvo na plataforma.'}
        />
        <RankedEvents
          title="Melhores avaliados"
          items={insights.bestRatedEvents}
          metric="rating"
          emptyLabel={scope === 'creator' ? 'Seus eventos ainda não receberam avaliações.' : 'Nenhum evento avaliado na plataforma.'}
        />
      </div>
    </div>
  )
}

export default EventInsightsDashboard
