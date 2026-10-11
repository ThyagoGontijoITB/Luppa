import { useEffect, useMemo, useState } from 'react'
import { fetchAuditLogs, fetchAuditTables } from '../../services/auditLogService'
import styles from './AdminLuppa.module.css'

const defaultFilters = {
  search: '',
  operation: 'all',
  tableName: 'all',
  user: '',
  startDate: '',
  endDate: '',
  size: 25,
}

const operationOptions = ['INSERT', 'UPDATE', 'DELETE']

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('pt-BR')
}

const formatUser = (log) => {
  if (!log.userId) return 'Sistema'
  if (!log.user) return `ID ${log.userId}`
  return `${log.user.name || log.user.email || 'Usuário'} (#${log.userId})`
}

const summarizeChange = (log) => {
  const source = log.newData || log.oldData || ''
  if (!source) return '-'
  const compact = source.replace(/\s+/g, ' ').trim()
  return compact.length > 120 ? `${compact.slice(0, 120)}...` : compact
}

const parseDisplayData = (value) => {
  if (!value) return { type: 'empty', text: '-' }
  try {
    return { type: 'json', text: JSON.stringify(JSON.parse(value), null, 2) }
  } catch {
    return {
      type: 'text',
      text: value
        .split(';')
        .map((part) => part.trim())
        .filter(Boolean)
        .join('\n') || value,
    }
  }
}

const DataBlock = ({ title, value }) => {
  const display = parseDisplayData(value)
  return (
    <div className={styles.auditDataBlock}>
      <h3>{title}</h3>
      <pre>{display.text}</pre>
    </div>
  )
}

const AdminAuditLogs = () => {
  const [filters, setFilters] = useState(defaultFilters)
  const [appliedFilters, setAppliedFilters] = useState(defaultFilters)
  const [page, setPage] = useState(0)
  const [logsPage, setLogsPage] = useState({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 })
  const [tables, setTables] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedLog, setSelectedLog] = useState(null)

  const totalPages = Math.max(logsPage.totalPages || 0, 1)
  const canGoPrevious = page > 0 && !loading
  const canGoNext = page + 1 < totalPages && !loading

  const queryFilters = useMemo(() => ({ ...appliedFilters, page, size: Number(appliedFilters.size) || 25 }), [appliedFilters, page])

  useEffect(() => {
    let active = true
    fetchAuditTables()
      .then((items) => {
        if (active) setTables(Array.isArray(items) ? items : [])
      })
      .catch(() => {
        if (active) setTables([])
      })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    fetchAuditLogs(queryFilters)
      .then((response) => {
        if (active) setLogsPage(response)
      })
      .catch((requestError) => {
        if (active) {
          setLogsPage({ content: [], page, size: queryFilters.size, totalElements: 0, totalPages: 0 })
          setError(requestError.status === 403 ? 'Você não tem permissão para visualizar a auditoria.' : 'Não foi possível carregar registros de auditoria.')
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [queryFilters, page])

  const updateFilter = (field, value) => {
    setFilters((current) => ({ ...current, [field]: value }))
  }

  const applyFilters = (event) => {
    event.preventDefault()
    setPage(0)
    setAppliedFilters(filters)
  }

  const clearFilters = () => {
    setFilters(defaultFilters)
    setAppliedFilters(defaultFilters)
    setPage(0)
  }

  return (
    <>
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <div>
            <h2>Auditoria</h2>
            <p>Visualização somente leitura dos registros gravados pelos gatilhos do SQL Server.</p>
          </div>
        </div>

        <form className={`${styles.filters} ${styles.auditFilters}`} onSubmit={applyFilters}>
          <input value={filters.search} placeholder="Buscar por usuário, email, ID, registro ou tabela" maxLength={120} onChange={(event) => updateFilter('search', event.target.value)} />
          <select value={filters.operation} onChange={(event) => updateFilter('operation', event.target.value)}>
            <option value="all">Todas as operações</option>
            {operationOptions.map((operation) => <option key={operation} value={operation}>{operation}</option>)}
          </select>
          <select value={filters.tableName} onChange={(event) => updateFilter('tableName', event.target.value)}>
            <option value="all">Todas as tabelas</option>
            {tables.map((table) => <option key={table} value={table}>{table}</option>)}
          </select>
          <input value={filters.user} placeholder="Usuário, email ou ID" maxLength={120} onChange={(event) => updateFilter('user', event.target.value)} />
          <input type="date" value={filters.startDate} onChange={(event) => updateFilter('startDate', event.target.value)} />
          <input type="date" value={filters.endDate} onChange={(event) => updateFilter('endDate', event.target.value)} />
          <select value={filters.size} onChange={(event) => updateFilter('size', Number(event.target.value))}>
            <option value={20}>20 por página</option>
            <option value={25}>25 por página</option>
            <option value={50}>50 por página</option>
          </select>
          <div className={styles.auditFilterActions}>
            <button className={styles.primaryButton} type="submit">Aplicar</button>
            <button className={styles.secondaryButton} type="button" onClick={clearFilters}>Limpar filtros</button>
          </div>
        </form>

        {error && <p className={styles.empty}>{error}</p>}
        {loading && <p className={styles.empty}>Carregando auditoria...</p>}
        {!loading && !error && logsPage.content.length === 0 && (
          <p className={styles.empty}>Nenhum registro de auditoria encontrado para os filtros selecionados.</p>
        )}

        {!loading && !error && logsPage.content.length > 0 && (
          <>
            <div className={styles.tableWrap}>
              <table className={`${styles.table} ${styles.auditTable}`}>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Data/Hora</th>
                    <th>Usuário</th>
                    <th>Tabela</th>
                    <th>Operação</th>
                    <th>ID do registro</th>
                    <th>Resumo da alteração</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {logsPage.content.map((log) => (
                    <tr key={log.id}>
                      <td>{log.id}</td>
                      <td>{formatDateTime(log.operatedAt)}</td>
                      <td>
                        {formatUser(log)}
                        {log.user?.email && <small className={styles.auditMeta}>{log.user.email}</small>}
                      </td>
                      <td>{log.tableName}</td>
                      <td><span className={styles.badge}>{log.operation}</span></td>
                      <td>{log.recordId ?? '-'}</td>
                      <td>{summarizeChange(log)}</td>
                      <td>
                        <button className={styles.secondaryButton} type="button" onClick={() => setSelectedLog(log)}>Ver detalhes</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className={styles.pagination}>
              <button className={styles.secondaryButton} type="button" disabled={!canGoPrevious} onClick={() => setPage((current) => Math.max(current - 1, 0))}>Anterior</button>
              <span>Página {page + 1} de {totalPages}</span>
              <button className={styles.secondaryButton} type="button" disabled={!canGoNext} onClick={() => setPage((current) => current + 1)}>Próxima</button>
            </div>
          </>
        )}
      </section>

      {selectedLog && (
        <div className={styles.modalBackdrop} role="dialog" aria-modal="true" aria-labelledby="audit-details-title">
          <div className={`${styles.modal} ${styles.auditModal}`}>
            <div className={styles.panelHeader}>
              <div>
                <h2 id="audit-details-title">Detalhes da auditoria #{selectedLog.id}</h2>
                <p>{selectedLog.tableName} · {selectedLog.operation}</p>
              </div>
            </div>
            <dl className={styles.auditDetailsGrid}>
              <div><dt>ID do log</dt><dd>{selectedLog.id}</dd></div>
              <div><dt>Data/hora</dt><dd>{formatDateTime(selectedLog.operatedAt)}</dd></div>
              <div><dt>Usuário responsável</dt><dd>{formatUser(selectedLog)}{selectedLog.user?.email ? ` - ${selectedLog.user.email}` : ''}</dd></div>
              <div><dt>Tabela</dt><dd>{selectedLog.tableName}</dd></div>
              <div><dt>Operação</dt><dd>{selectedLog.operation}</dd></div>
              <div><dt>ID do registro</dt><dd>{selectedLog.recordId ?? '-'}</dd></div>
            </dl>
            <div className={styles.auditDataGrid}>
              <DataBlock title="Dados anteriores" value={selectedLog.oldData} />
              <DataBlock title="Dados novos" value={selectedLog.newData} />
            </div>
            <div className={styles.actions}>
              <button className={styles.primaryButton} type="button" onClick={() => setSelectedLog(null)}>Fechar</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

export default AdminAuditLogs
