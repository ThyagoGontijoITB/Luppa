import { useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Logo from '../../components/common/Logo'
import FieldError from '../../components/forms/FieldError'
import PasswordInput from '../../components/forms/PasswordInput'
import { requestPasswordReset, resendVerification, resetPassword, verifyEmail } from '../../services/accountService'
import { isEmail } from '../../utils/validation'
import { focusFirstInvalidField } from '../../utils/focusFirstInvalidField'
import styles from './LoginCadastroLuppa.module.css'

const copy = {
  forgot: {
    title: 'Esqueci minha senha',
    text: 'Informe o e-mail da sua conta Luppa para receber o link de redefinição.',
    button: 'Enviar link de redefinição',
  },
  reset: {
    title: 'Redefinir senha',
    text: 'Crie uma nova senha para sua conta Luppa.',
    button: 'Salvar nova senha',
  },
  verify: {
    title: 'Verificação de e-mail',
    text: 'Confirme seu e-mail para manter sua conta Luppa atualizada.',
    button: 'Verificar e-mail',
  },
  resend: {
    title: 'Reenviar verificação',
    text: 'Informe seu e-mail para receber um novo link de confirmação.',
    button: 'Reenviar verificação',
  },
}

const AccountActionPage = ({ type }) => {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const formRef = useRef(null)
  const content = copy[type]

  const needsEmail = type === 'forgot' || type === 'resend'
  const needsPassword = type === 'reset'
  const needsToken = type === 'reset' || type === 'verify'

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setFieldErrors({})
    setMessage('')
    const nextErrors = {}

    if (needsEmail && (!email.trim() || !isEmail(email))) {
      nextErrors.email = 'Informe um e-mail válido.'
    }
    if (needsPassword && password.length < 8) {
      nextErrors.password = 'A senha deve ter pelo menos 8 caracteres.'
    }
    if (needsPassword && password !== confirmPassword) {
      nextErrors.confirmPassword = 'As senhas não conferem.'
    }

    setFieldErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      window.requestAnimationFrame(() => focusFirstInvalidField(formRef.current))
      return
    }

    if (needsToken && !token) {
      setError('Token ausente ou inválido.')
      return
    }

    setLoading(true)
    try {
      if (type === 'forgot') await requestPasswordReset(email)
      if (type === 'resend') await resendVerification(email)
      if (type === 'reset') await resetPassword({ token, password })
      if (type === 'verify') await verifyEmail(token)
      setMessage(type === 'reset' ? 'Senha atualizada com sucesso.' : type === 'verify' ? 'E-mail confirmado com sucesso.' : 'Se o e-mail estiver cadastrado, enviaremos as instruções.')
    } catch (requestError) {
      setError(requestError.message || 'Não foi possível concluir a solicitação.')
    } finally {
      setLoading(false)
    }
  }

  const headerText = useMemo(() => (message ? 'Solicitação concluída' : content.title), [content.title, message])

  return (
    <main className={styles.page}>
      <section className={styles.shell}>
        <Link className={styles.backLink} to="/login-cadastro"><span>←</span>Voltar para login</Link>
        <div className={styles.header}>
          <Logo />
          <div>
            <h1>{headerText}</h1>
            <p>{content.text}</p>
          </div>
        </div>
        <form className={`${styles.panel} ${styles.singlePanel}`} noValidate ref={formRef} onSubmit={submit}>
          <h2>{content.title}</h2>
          {error && <div className={styles.submitError}>{error}</div>}
          {message && <div className={styles.successMessage}>{message}</div>}
          {needsEmail && (
            <label>
              Email
              <input type="email" inputMode="email" value={email} maxLength={60} placeholder="seuemail@email.com" aria-invalid={Boolean(fieldErrors.email)} aria-describedby={fieldErrors.email ? 'account-email-error' : undefined} onChange={(event) => { setEmail(event.target.value); setFieldErrors((current) => ({ ...current, email: '' })) }} />
              <FieldError id="account-email-error" message={fieldErrors.email} />
            </label>
          )}
          {needsPassword && (
            <>
              <label>Nova senha<PasswordInput value={password} autoComplete="new-password" minLength={8} maxLength={128} aria-invalid={Boolean(fieldErrors.password)} aria-describedby={fieldErrors.password ? 'account-password-error' : undefined} onChange={(event) => { setPassword(event.target.value); setFieldErrors((current) => ({ ...current, password: '' })) }} /><FieldError id="account-password-error" message={fieldErrors.password} /></label>
              <label>Confirmar nova senha<PasswordInput value={confirmPassword} autoComplete="new-password" minLength={8} maxLength={128} aria-invalid={Boolean(fieldErrors.confirmPassword)} aria-describedby={fieldErrors.confirmPassword ? 'account-confirm-password-error' : undefined} onChange={(event) => { setConfirmPassword(event.target.value); setFieldErrors((current) => ({ ...current, confirmPassword: '' })) }} /><FieldError id="account-confirm-password-error" message={fieldErrors.confirmPassword} /></label>
            </>
          )}
          <button type="submit" disabled={loading}>{loading ? 'Enviando...' : content.button}</button>
        </form>
      </section>
    </main>
  )
}

export default AccountActionPage
