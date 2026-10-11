import { useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import EventDetailsModal from '../../components/events/EventDetailsModal'
import useEventDetails from '../../hooks/useEventDetails'
import { useToast } from '../../components/common/ToastProvider'
import { getReports, updateReportStatus } from '../../services/reportService'
import { reportStatusLabels } from '../../constants/reports'
import styles from './AdminLuppa.module.css'

const AdminReports = () => {
  const { events } = useOutletContext()
  const toast = useToast()
  const [reports, setReports] = useState([])
  const [error, setError] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [pendingAction, setPendingAction] = useState(null)
  const [responseAction, setResponseAction] = useState(null)
  const [responseMessage, setResponseMessage] = useState('')
  const [responseError, setResponseError] = useState('')
  const { selectedEvent, openEventDetails, closeEventDetails } = useEventDetails()
  const filteredReports = reports.filter((report) => statusFilter === 'all' || report.status === statusFilter)

  useEffect(() => {
    let active = true
    getReports()
      .then((items) => {
        if (active) setReports(items)
      })
      .catch(() => {
        if (active) setError('Não foi possível carregar denúncias.')
      })
    return () => {
      active = false
    }
  }, [])

  const getEvent = (eventId) => events.find((event) => event.id === eventId)

  const changeStatus = async (report, status, message = '') => {
    setPendingAction(`${report.id}:${status}`)
    setError('')
    try {
      const updated = await updateReportStatus(report.id, status, message)
      setReports((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      toast.success('Status da denúncia atualizado.')
      setResponseAction(null)
      setResponseMessage('')
      setResponseError('')
    } catch (requestError) {
      toast.error(requestError.message || 'Não foi possível atualizar a denúncia.')
    } finally {
      setPendingAction(null)
    }
  }

  const openResponseDialog = (report, status) => {
    setResponseAction({ report, status })
    setResponseMessage('')
    setResponseError('')
  }

  const submitResponse = async (submitEvent) => {
    submitEvent.preventDefault()
    if (!responseAction || pendingAction) return
    const message = responseMessage.trim()
    if (!message) {
      setResponseError('Informe a resposta que será enviada ao denunciante.')
      return
    }
    await changeStatus(responseAction.report, responseAction.status, message)
  }

  const isPending = (report, status) => pendingAction === `${report.id}:${status}`

  return (
    <>
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <div>
            <h2>Denúncias</h2>
            <p>Moderação pós-publicação separada da análise prévia de eventos.</p>
          </div>
          <select className={styles.formSelect} value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="all">Todos os status</option>
            {Object.entries(reportStatusLabels).map(([status, label]) => <option key={status} value={status}>{label}</option>)}
          </select>
        </div>

        {error ? <p className={styles.empty}>{error}</p> : filteredReports.length === 0 ? <p className={styles.empty}>Nenhuma denúncia encontrada.</p> : (
          <div className={styles.reviewList}>
            {filteredReports.map((report) => {
              const event = getEvent(report.eventId)
              const eventTitle = report.eventTitle || event?.title || 'Evento não encontrado'
              const isReviewReport = Boolean(report.ratingId)
              const review = report.rating
              return (
                <article className={styles.reportCard} key={report.id}>
                  <div>
                    <span>{isReviewReport ? 'Denúncia de avaliação' : 'Denúncia de evento'}</span>
                    <h3>{eventTitle}</h3>
                    <p>{report.reason}</p>
                    {isReviewReport && review && (
                      <div className={styles.reportReviewContext}>
                        <strong>{eventTitle}</strong>
                        <span>{review.authorName || 'Autor da avaliação'} · Nota {review.score}</span>
                        {review.description ? <p>{review.description}</p> : <p>Avaliação sem comentário.</p>}
                      </div>
                    )}
                    {report.description && <small>{report.description}</small>}
                    <small>
                      {reportStatusLabels[report.status]} · Denunciante: {report.reporterName} · {new Date(report.createdAt).toLocaleString('pt-BR')}
                    </small>
                    {report.moderationResponse && <small>Resposta enviada: {report.moderationResponse}</small>}
                  </div>
                  <div className={styles.actions}>
                    {event && <button className={styles.secondaryButton} type="button" onClick={() => openEventDetails(event)}>Ver evento</button>}
                    <button
                      className={styles.secondaryButton}
                      type="button"
                      disabled={Boolean(pendingAction) || report.status === 'UNDER_REVIEW' || report.status === 'RESOLVED' || report.status === 'DISMISSED'}
                      onClick={() => changeStatus(report, 'UNDER_REVIEW')}
                    >
                      {isPending(report, 'UNDER_REVIEW') ? 'Atualizando...' : 'Marcar em análise'}
                    </button>
                    <button
                      className={styles.secondaryButton}
                      type="button"
                      disabled={Boolean(pendingAction) || report.status === 'DISMISSED'}
                      onClick={() => openResponseDialog(report, 'DISMISSED')}
                    >
                      {isPending(report, 'DISMISSED') ? 'Atualizando...' : 'Descartar'}
                    </button>
                    <button
                      className={styles.primaryButton}
                      type="button"
                      disabled={Boolean(pendingAction) || report.status === 'RESOLVED' || report.status === 'DISMISSED'}
                      onClick={() => openResponseDialog(report, 'RESOLVED')}
                    >
                      {isPending(report, 'RESOLVED') ? 'Atualizando...' : 'Resolver'}
                    </button>
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </section>
      <EventDetailsModal event={selectedEvent} onClose={closeEventDetails} />
      {responseAction && (
        <div className={styles.modalBackdrop} role="dialog" aria-modal="true" aria-labelledby="report-response-title">
          <form className={styles.modal} noValidate onSubmit={submitResponse}>
            <h2 id="report-response-title">
              {responseAction.status === 'RESOLVED' ? 'Resolver denúncia' : 'Descartar denúncia'}
            </h2>
            <p>Esta resposta será enviada ao denunciante por e-mail.</p>
            <label className={styles.rejectField}>
              Resposta da moderação
              <textarea
                value={responseMessage}
                rows="5"
                maxLength={1000}
                onChange={(event) => {
                  setResponseMessage(event.target.value)
                  setResponseError('')
                }}
              />
            </label>
            {responseError && <p className={styles.formError}>{responseError}</p>}
            <div className={styles.actions}>
              <button
                className={responseAction.status === 'RESOLVED' ? styles.primaryButton : styles.dangerButton}
                type="submit"
                disabled={Boolean(pendingAction)}
              >
                {isPending(responseAction.report, responseAction.status) ? 'Enviando...' : 'Enviar resposta'}
              </button>
              <button
                className={styles.secondaryButton}
                type="button"
                disabled={Boolean(pendingAction)}
                onClick={() => {
                  setResponseAction(null)
                  setResponseMessage('')
                  setResponseError('')
                }}
              >
                Cancelar
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  )
}

export default AdminReports
