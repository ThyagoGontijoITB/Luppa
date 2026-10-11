import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useToast } from '../../components/common/ToastProvider'
import FieldError from '../../components/forms/FieldError'
import { useAuth } from '../../auth/authContext'
import { sendSupportMessage } from '../../services/supportService'
import { isEmail } from '../../utils/validation'
import { focusFirstInvalidField } from '../../utils/focusFirstInvalidField'
import { UtilityPageHeader } from './LegalDocumentLayout'
import styles from './MenuLuppa.module.css'

const initialForm = { name: '', email: '', category: 'Conta', subject: '', message: '' }

const SupportPage = () => {
  const auth = useAuth()
  const toast = useToast()
  const [form, setForm] = useState({
    ...initialForm,
    name: auth.name || '',
    email: auth.email || '',
  })
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [success, setSuccess] = useState('')
  const [sending, setSending] = useState(false)
  const formRef = useRef(null)

  const update = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: '' }))
    setError('')
    setSuccess('')
  }

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    const nextErrors = {}
    if (!form.name.trim()) nextErrors.name = 'Informe seu nome.'
    if (!form.email.trim()) nextErrors.email = 'Informe seu e-mail.'
    else if (!isEmail(form.email)) nextErrors.email = 'Informe um e-mail válido.'
    if (!form.subject.trim()) nextErrors.subject = 'Informe o assunto.'
    if (!form.message.trim()) nextErrors.message = 'Escreva sua mensagem.'

    setFieldErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      window.requestAnimationFrame(() => focusFirstInvalidField(formRef.current))
      return
    }

    if (sending) return
    setSending(true)
    try {
      await sendSupportMessage(form, auth.isAuthenticated)
      setSuccess('Mensagem enviada para a equipe Luppa.')
      toast.success('Mensagem enviada.')
      setForm({ ...initialForm, name: auth.name || '', email: auth.email || '' })
    } catch {
      setError('Não foi possível enviar sua mensagem.')
      toast.error('Não foi possível enviar sua mensagem.')
    } finally {
      setSending(false)
    }
  }

  return (
    <div className={styles.page}>
      <UtilityPageHeader current="support" />
      <main className={styles.supportPage}>
        <div className={styles.supportContainer}>
          <Link className={styles.backLink} to="/">Voltar</Link>

          <div className={styles.supportIntro}>
            <span>Suporte</span>
            <h1>Como podemos ajudar?</h1>
            <p>Envie sua mensagem para a equipe Luppa. Vamos usar os dados abaixo apenas para analisar e responder sua solicitação.</p>
          </div>

          <div className={styles.supportShell}>
            <form className={styles.supportForm} noValidate ref={formRef} onSubmit={submit} aria-busy={sending}>
              {error && <p className={styles.formError} role="alert">{error}</p>}
              {success && <p className={styles.formSuccess} role="status">{success}</p>}

              <label htmlFor="support-name">
                <span>Nome <small>Obrigatório</small></span>
                <input
                  id="support-name"
                  value={form.name}
                  maxLength={120}
                  autoComplete="name"
                  disabled={sending}
                  aria-invalid={Boolean(fieldErrors.name)}
                  aria-describedby={fieldErrors.name ? 'support-name-error' : undefined}
                  onChange={(event) => update('name', event.target.value)}
                />
                <FieldError id="support-name-error" message={fieldErrors.name} />
              </label>

              <label htmlFor="support-email">
                <span>E-mail <small>Obrigatório</small></span>
                <input
                  id="support-email"
                  type="email"
                  inputMode="email"
                  value={form.email}
                  maxLength={120}
                  autoComplete="email"
                  disabled={sending}
                  placeholder="voce@email.com"
                  aria-invalid={Boolean(fieldErrors.email)}
                  aria-describedby={fieldErrors.email ? 'support-email-error' : undefined}
                  onChange={(event) => update('email', event.target.value)}
                />
                <FieldError id="support-email-error" message={fieldErrors.email} />
              </label>

              <label htmlFor="support-category">
                <span>Categoria <small>Obrigatório</small></span>
                <select
                  id="support-category"
                  value={form.category}
                  disabled={sending}
                  onChange={(event) => update('category', event.target.value)}
                >
                  <option>Conta</option>
                  <option>Eventos</option>
                  <option>Denúncias</option>
                  <option>Pagamentos</option>
                  <option>Outro</option>
                </select>
              </label>

              <label htmlFor="support-subject">
                <span>Assunto <small>Obrigatório</small></span>
                <input
                  id="support-subject"
                  value={form.subject}
                  maxLength={160}
                  disabled={sending}
                  placeholder="Resumo da solicitação"
                  aria-invalid={Boolean(fieldErrors.subject)}
                  aria-describedby={fieldErrors.subject ? 'support-subject-error' : undefined}
                  onChange={(event) => update('subject', event.target.value)}
                />
                <FieldError id="support-subject-error" message={fieldErrors.subject} />
              </label>

              <label className={styles.wide} htmlFor="support-message">
                <span>Mensagem <small>Obrigatório</small></span>
                <textarea
                  id="support-message"
                  value={form.message}
                  maxLength={4000}
                  rows={7}
                  disabled={sending}
                  placeholder="Descreva o que aconteceu, incluindo links ou nomes de eventos quando isso ajudar a entender a solicitação."
                  aria-invalid={Boolean(fieldErrors.message)}
                  aria-describedby={fieldErrors.message ? 'support-message-error support-message-help' : 'support-message-help'}
                  onChange={(event) => update('message', event.target.value)}
                />
                <small id="support-message-help" className={styles.fieldHint}>Não envie senhas ou dados sensíveis no formulário.</small>
                <FieldError id="support-message-error" message={fieldErrors.message} />
              </label>

              <div className={styles.supportActions}>
                <button type="submit" disabled={sending}>{sending ? 'Enviando...' : 'Enviar mensagem'}</button>
              </div>
            </form>

            <aside className={styles.supportAside} aria-label="Informações de suporte">
              <div>
                <span>Canal principal</span>
                <p>Use o formulário para enviar dúvidas sobre conta, eventos, denúncias ou uso da plataforma.</p>
              </div>
              <div>
                <span>Antes de enviar</span>
                <p>Confira se os dados de contato estão corretos para que a equipe consiga responder sua solicitação.</p>
              </div>
              <div className={styles.supportLegalLinks}>
                <span>Documentos úteis</span>
                <Link to="/faq">FAQ</Link>
                <Link to="/terms">Termos de Uso</Link>
                <Link to="/privacy">Política de Privacidade</Link>
              </div>
            </aside>
          </div>
        </div>
      </main>
    </div>
  )
}

export default SupportPage
