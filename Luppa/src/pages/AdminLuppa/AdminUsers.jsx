import { useEffect, useRef, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import ConfirmDialog from '../../components/common/ConfirmDialog'
import { useToast } from '../../components/common/ToastProvider'
import { useAuth } from '../../auth/authContext'
import { userRoles } from '../../constants/users'
import { createUser, deleteUser, getUserRoleLabel, getUserRoleOptions, getUserStatusOptions, searchUsers, sendAdminUserMessage, updateUser } from '../../services/userService'
import { formatBrazilianPhone, isEmail, isPhone, isValidPastDate, todayDateInputValue } from '../../utils/validation'
import styles from './AdminLuppa.module.css'

const emptyForm = {
  name: '',
  email: '',
  phone: '',
  birthDate: '',
  password: '',
  role: 'USER',
  status: 'ACTIVE',
}

const formatNullableDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleDateString('pt-BR')
}

const AdminUsers = () => {
  const { users, setUsers, refreshAdminData } = useOutletContext()
  const auth = useAuth()
  const toast = useToast()
  const [filters, setFilters] = useState({ query: '', role: 'all', status: 'all', sort: 'name' })
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [confirmUser, setConfirmUser] = useState(null)
  const [messageUser, setMessageUser] = useState(null)
  const [messageForm, setMessageForm] = useState({ subject: '', message: '' })
  const [messageErrors, setMessageErrors] = useState({})
  const [messageStep, setMessageStep] = useState('compose')
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [sendingMessage, setSendingMessage] = useState(false)
  const [scrollEditRequest, setScrollEditRequest] = useState(0)
  const formSectionRef = useRef(null)
  const firstInputRef = useRef(null)
  const filteredUsers = searchUsers(users, filters)
  const isLuppaAdmin = auth.role === userRoles.LUPPA_ADMIN
  const roleOptions = getUserRoleOptions(auth.role)

  useEffect(() => {
    if (!scrollEditRequest) return
    formSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    window.setTimeout(() => firstInputRef.current?.focus({ preventScroll: true }), 350)
  }, [scrollEditRequest])

  const updateFilter = (field, value) => setFilters((current) => ({ ...current, [field]: value }))
  const isProtectedUser = (user) => user?.role === userRoles.LUPPA_ADMIN
  const canEditUser = (user) => !isProtectedUser(user) && (isLuppaAdmin || user?.role !== userRoles.ADMIN)
  const canDeleteUser = (user) => !isProtectedUser(user) && user?.id !== auth.userId && (isLuppaAdmin || user?.role !== userRoles.ADMIN)
  const canMessageUser = (user) => !isProtectedUser(user) && (isLuppaAdmin || user?.role !== userRoles.ADMIN)
  const updateForm = (field, value) => {
    const nextValue = field === 'phone' ? formatBrazilianPhone(value) : value
    setForm((current) => ({ ...current, [field]: nextValue }))
    setErrors((current) => ({ ...current, [field]: '' }))
  }

  const validate = () => {
    const nextErrors = {}
    if (!form.name.trim()) nextErrors.name = 'Informe o nome.'
    if (!form.email.trim()) nextErrors.email = 'Informe o email.'
    else if (!isEmail(form.email)) nextErrors.email = 'Informe um email válido.'
    if (!form.phone.trim()) nextErrors.phone = 'Informe o telefone.'
    else if (!isPhone(form.phone)) nextErrors.phone = 'Informe um telefone válido com DDD.'
    if (!form.birthDate) nextErrors.birthDate = 'Informe a data de nascimento.'
    else if (!isValidPastDate(form.birthDate)) nextErrors.birthDate = 'A data de nascimento deve ser anterior a hoje.'
    if (!editingId && form.password.length < 8) nextErrors.password = 'Informe uma senha com pelo menos 8 caracteres.'
    return nextErrors
  }

  const resetForm = () => {
    setForm(emptyForm)
    setEditingId(null)
    setErrors({})
  }

  const openMessageModal = (user) => {
    if (!canMessageUser(user)) return
    setMessageUser(user)
    setMessageForm({ subject: '', message: '' })
    setMessageErrors({})
    setMessageStep('compose')
  }

  const closeMessageModal = () => {
    if (sendingMessage) return
    setMessageUser(null)
    setMessageForm({ subject: '', message: '' })
    setMessageErrors({})
    setMessageStep('compose')
  }

  const updateMessageForm = (field, value) => {
    setMessageForm((current) => ({ ...current, [field]: value }))
    setMessageErrors((current) => ({ ...current, [field]: '' }))
  }

  const validateMessage = () => {
    const subject = messageForm.subject.trim()
    const message = messageForm.message.trim()
    const nextErrors = {}
    if (subject.length < 5) nextErrors.subject = 'Informe um assunto com pelo menos 5 caracteres.'
    else if (subject.length > 150) nextErrors.subject = 'O assunto deve ter no máximo 150 caracteres.'
    if (message.length < 10) nextErrors.message = 'Informe uma mensagem com pelo menos 10 caracteres.'
    else if (message.length > 5000) nextErrors.message = 'A mensagem deve ter no máximo 5000 caracteres.'
    return nextErrors
  }

  const reviewMessage = (event) => {
    event.preventDefault()
    const nextErrors = validateMessage()
    setMessageErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return
    setMessageStep('confirm')
  }

  const confirmSendMessage = async () => {
    if (!messageUser || sendingMessage) return
    const nextErrors = validateMessage()
    setMessageErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      setMessageStep('compose')
      return
    }
    setSendingMessage(true)
    try {
      await sendAdminUserMessage(messageUser.id, messageForm)
      toast.success('Mensagem aceita pelo servidor SMTP.')
      setMessageUser(null)
      setMessageForm({ subject: '', message: '' })
      setMessageErrors({})
      setMessageStep('compose')
    } catch (error) {
      toast.error(error.message || 'Não foi possível enviar a mensagem.')
    } finally {
      setSendingMessage(false)
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const nextErrors = validate()
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return
    if (saving) return

    setSaving(true)
    try {
      if (editingId) {
        const updated = await updateUser(users, editingId, form)
        setUsers((current) => current.map((user) => (user.id === updated.id ? updated : user)))
        toast.success('Usuário atualizado com sucesso.')
      } else {
        const created = await createUser(users, form)
        setUsers((current) => [...current, created])
        toast.success('Usuário criado com sucesso.')
      }
      refreshAdminData?.()
      resetForm()
    } catch (error) {
      toast.error(error.message || 'Não foi possível salvar as alterações.')
    } finally {
      setSaving(false)
    }
  }

  const editUser = (user) => {
    if (!user) return
    if (!canEditUser(user)) return
    setEditingId(user.id)
    setForm({
      name: user.name || '',
      email: user.email || '',
      phone: user.phone || '',
      birthDate: user.birthDate || '',
      password: '',
      role: user.role || 'USER',
      status: user.status || 'ACTIVE',
    })
    setErrors({})
    setScrollEditRequest((current) => current + 1)
  }

  const confirmDelete = async () => {
    if (!confirmUser || deleting) return
    setDeleting(true)
    try {
      await deleteUser(users, confirmUser.id)
      setUsers((current) => current.filter((user) => user.id !== confirmUser.id))
      setConfirmUser(null)
      toast.success('Usuário deletado com sucesso.')
      refreshAdminData?.()
    } catch (error) {
      toast.error(error.message || 'Não foi possível excluir o usuário.')
      setConfirmUser(null)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <>
      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <div>
            <h2>Gerenciar usuários</h2>
            <p>CRUD conectado à API de usuários.</p>
          </div>
          <button className={styles.secondaryButton} type="button" onClick={resetForm}>Novo usuário</button>
        </div>

        <div className={styles.filters}>
          <input value={filters.query} placeholder="Buscar por nome ou email" maxLength={120} onChange={(event) => updateFilter('query', event.target.value)} />
          <select value={filters.role} onChange={(event) => updateFilter('role', event.target.value)}>
            <option value="all">Todos os papéis</option>
            {Object.values(userRoles).map((role) => <option key={role} value={role}>{getUserRoleLabel(role)}</option>)}
          </select>
          <select value={filters.status} onChange={(event) => updateFilter('status', event.target.value)}>
            <option value="all">Todos os status</option>
            {getUserStatusOptions().map((status) => <option key={status}>{status}</option>)}
          </select>
          <select value={filters.sort} onChange={(event) => updateFilter('sort', event.target.value)}>
            <option value="name">Nome</option>
            <option value="newest">Mais recentes</option>
            <option value="oldest">Mais antigos</option>
          </select>
        </div>

        {filteredUsers.length === 0 ? <p className={styles.empty}>Nenhum usuário encontrado.</p> : (
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Email</th>
                  <th>Telefone</th>
                  <th>Nascimento</th>
                  <th>Papel</th>
                  <th>Status</th>
                  <th>Criado em</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id} className={isProtectedUser(user) ? styles.protectedRow : undefined}>
                    <td>
                      <div className={styles.userNameCell}>
                        <span>{user.name || '-'}</span>
                        {isProtectedUser(user) && <span className={styles.protectedBadge}>Protegido</span>}
                      </div>
                    </td>
                    <td>{user.email || '-'}</td>
                    <td>{user.phone || '-'}</td>
                    <td>{user.birthDate || '-'}</td>
                    <td><span className={styles.badge}>{getUserRoleLabel(user.role)}</span></td>
                    <td><span className={styles.badge}>{user.status}</span></td>
                    <td>{formatNullableDate(user.createdAt)}</td>
                    <td>
                      <div className={styles.actions}>
                        {canMessageUser(user) && <button className={styles.secondaryButton} type="button" onClick={() => openMessageModal(user)}>Enviar mensagem</button>}
                        {canEditUser(user) && <button className={styles.secondaryButton} type="button" onClick={() => editUser(user)}>Editar</button>}
                        {canDeleteUser(user) && (
                          <button
                            className={styles.dangerButton}
                            type="button"
                            onClick={() => setConfirmUser(user)}
                          >
                            Excluir
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className={styles.panel} ref={formSectionRef}>
        <div className={styles.panelHeader}>
          <div>
            <h2>{editingId ? 'Editar usuário' : 'Criar usuário'}</h2>
            <p>Usuários são criados e atualizados pela API administrativa.</p>
          </div>
        </div>
        <form className={styles.formGrid} noValidate onSubmit={handleSubmit}>
          <label>Nome<input ref={firstInputRef} value={form.name} maxLength={85} onChange={(event) => updateForm('name', event.target.value)} />{errors.name && <span className={styles.error}>{errors.name}</span>}</label>
          <label>Email<input type="email" inputMode="email" maxLength={60} value={form.email} onChange={(event) => updateForm('email', event.target.value)} />{errors.email && <span className={styles.error}>{errors.email}</span>}</label>
          <label>Telefone<input type="tel" inputMode="tel" maxLength={15} value={form.phone} placeholder="(11) 99999-9999" onChange={(event) => updateForm('phone', event.target.value)} />{errors.phone && <span className={styles.error}>{errors.phone}</span>}</label>
          <label>Data de nascimento<input type="date" max={todayDateInputValue()} value={form.birthDate} onChange={(event) => updateForm('birthDate', event.target.value)} />{errors.birthDate && <span className={styles.error}>{errors.birthDate}</span>}</label>
          {!editingId && <label>Senha<input type="password" minLength={8} maxLength={128} value={form.password} onChange={(event) => updateForm('password', event.target.value)} />{errors.password && <span className={styles.error}>{errors.password}</span>}</label>}
          <label>Papel<select value={form.role} onChange={(event) => updateForm('role', event.target.value)}>{roleOptions.map((role) => <option key={role.value} value={role.value}>{role.label}</option>)}</select></label>
          <label>Status<select value={form.status} onChange={(event) => updateForm('status', event.target.value)}>{getUserStatusOptions().map((status) => <option key={status}>{status}</option>)}</select></label>
          <div className={styles.formActions}>
            <button className={styles.primaryButton} type="submit" disabled={saving}>{saving ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Criar usuário'}</button>
            <button className={styles.secondaryButton} type="button" disabled={saving} onClick={resetForm}>Cancelar</button>
          </div>
        </form>
      </section>

      {confirmUser && (
        <ConfirmDialog
          title="Excluir usuário"
          message={`Tem certeza que deseja excluir ${confirmUser.name}? Esta ação excluirá permanentemente o usuário, seus eventos, avaliações e demais dados associados. Não será possível desfazer a exclusão.`}
          confirmLabel={deleting ? 'Excluindo...' : 'Excluir usuário'}
          confirmDisabled={deleting}
          onConfirm={confirmDelete}
          onCancel={() => {
            if (!deleting) setConfirmUser(null)
          }}
        />
      )}

      {messageUser && (
        <div className={styles.modalBackdrop} role="dialog" aria-modal="true" aria-labelledby="message-title">
          <div className={styles.modal}>
            {messageStep === 'compose' ? (
              <form noValidate onSubmit={reviewMessage}>
                <h2 id="message-title">Enviar mensagem</h2>
                <p>Mensagem individual para {messageUser.name || 'usuário'}.</p>
                <div className={styles.messageRecipient}>
                  <span>{messageUser.name || '-'}</span>
                  <strong>{messageUser.email || '-'}</strong>
                </div>
                <label className={styles.fullField}>
                  Assunto
                  <input
                    value={messageForm.subject}
                    maxLength={150}
                    onChange={(event) => updateMessageForm('subject', event.target.value)}
                  />
                  {messageErrors.subject && <span className={styles.error}>{messageErrors.subject}</span>}
                </label>
                <label className={styles.fullField}>
                  Mensagem
                  <textarea
                    value={messageForm.message}
                    maxLength={5000}
                    rows={8}
                    onChange={(event) => updateMessageForm('message', event.target.value)}
                  />
                  {messageErrors.message && <span className={styles.error}>{messageErrors.message}</span>}
                </label>
                <div className={styles.actions}>
                  <button className={styles.secondaryButton} type="button" onClick={closeMessageModal}>Cancelar</button>
                  <button className={styles.primaryButton} type="submit">Revisar</button>
                </div>
              </form>
            ) : (
              <>
                <h2 id="message-title">Confirmar envio</h2>
                <p>Revise os dados antes de enviar a mensagem.</p>
                <div className={styles.messageReview}>
                  <span>Destinatário</span>
                  <strong>{messageUser.name || '-'} · {messageUser.email || '-'}</strong>
                  <span>Assunto</span>
                  <strong>{messageForm.subject.trim()}</strong>
                  <span>Prévia</span>
                  <p>{messageForm.message.trim()}</p>
                </div>
                <div className={styles.actions}>
                  <button className={styles.secondaryButton} type="button" disabled={sendingMessage} onClick={() => setMessageStep('compose')}>Voltar</button>
                  <button className={styles.primaryButton} type="button" disabled={sendingMessage} onClick={confirmSendMessage}>
                    {sendingMessage ? 'Enviando...' : 'Enviar mensagem'}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </>
  )
}

export default AdminUsers
