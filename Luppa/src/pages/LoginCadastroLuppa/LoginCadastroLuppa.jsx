import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/authContext'
import Logo from '../../components/common/Logo'
import { useToast } from '../../components/common/ToastProvider'
import { HelpIcon } from '../../components/common/Icons'
import FieldError from '../../components/forms/FieldError'
import PasswordInput from '../../components/forms/PasswordInput'
import { resendRegistrationCode, verifyRegistrationCode } from '../../services/authService'
import { formatBrazilianPhone, isEmail, isPhone, isValidPastDate, todayDateInputValue } from '../../utils/validation'
import { focusFirstInvalidField } from '../../utils/focusFirstInvalidField'
import styles from './LoginCadastroLuppa.module.css'

const initialLogin = {
  email: '',
  password: '',
}

const initialRegister = {
  fullName: '',
  email: '',
  birthDate: '',
  phone: '',
  password: '',
  confirmPassword: '',
  accountType: 'standard',
  termsAccepted: false,
  marketingEmails: false,
}

const LoginCadastroLuppa = () => {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const toast = useToast()
  const [mode, setMode] = useState('login')
  const [login, setLogin] = useState(initialLogin)
  const [register, setRegister] = useState(initialRegister)
  const [errors, setErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const [registrationStep, setRegistrationStep] = useState('form')
  const [pendingEmail, setPendingEmail] = useState('')
  const [verificationCode, setVerificationCode] = useState('')
  const [resendCooldown, setResendCooldown] = useState(0)
  const loginFormRef = useRef(null)
  const registerFormRef = useRef(null)

  const errorId = (formName, field) => `${formName}-${field}-error`

  useEffect(() => {
    if (resendCooldown <= 0) return undefined
    const timer = window.setInterval(() => {
      setResendCooldown((current) => Math.max(0, current - 1))
    }, 1000)
    return () => window.clearInterval(timer)
  }, [resendCooldown])

  const updateLogin = (field, value) => {
    setLogin((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: '' }))
  }

  const updateRegister = (field, value) => {
    const nextValue = field === 'phone' ? formatBrazilianPhone(value) : value
    setRegister((current) => ({ ...current, [field]: nextValue }))
    setErrors((current) => ({ ...current, [field]: '' }))
  }

  const handleModeChange = (nextMode) => {
    setMode(nextMode)
    setErrors({})
    setSubmitError('')
    setRegistrationStep('form')
    setVerificationCode('')
  }

  const handleLoginSubmit = async (event) => {
    event.preventDefault()
    const nextErrors = {}

    if (!login.email.trim()) nextErrors.email = 'Informe seu email.'
    else if (!isEmail(login.email)) nextErrors.email = 'Informe um email válido.'
    if (!login.password) nextErrors.password = 'Informe sua senha.'

    setErrors(nextErrors)

    if (Object.keys(nextErrors).length === 0) {
      try {
        const authenticatedUser = await auth.login(login)
        toast.success('Login realizado com sucesso.')
        const destination = location.state?.from || (authenticatedUser.role === 'ADMIN' ? '/admin' : '/')
        window.setTimeout(() => navigate(destination), 500)
      } catch (requestError) {
        setSubmitError(requestError.message || 'Email ou senha inválidos, ou conta inativa.')
      }
    } else {
      window.requestAnimationFrame(() => focusFirstInvalidField(loginFormRef.current))
    }
  }

  const handleRegisterSubmit = async (event) => {
    event.preventDefault()
    const nextErrors = {}

    if (!register.fullName.trim()) nextErrors.fullName = 'Informe seu nome completo.'
    if (!register.email.trim()) nextErrors.email = 'Informe seu email.'
    else if (!isEmail(register.email)) nextErrors.email = 'Informe um email válido.'
    if (!register.birthDate) nextErrors.birthDate = 'Informe sua data de nascimento.'
    else if (!isValidPastDate(register.birthDate)) nextErrors.birthDate = 'A data de nascimento deve ser anterior a hoje.'
    if (!register.phone.trim()) nextErrors.phone = 'Informe seu telefone.'
    else if (!isPhone(register.phone)) nextErrors.phone = 'Informe um telefone válido com DDD.'
    if (!register.password) nextErrors.password = 'Crie uma senha.'
    else if (register.password.length < 8) nextErrors.password = 'A senha deve ter pelo menos 8 caracteres.'
    if (!register.confirmPassword) nextErrors.confirmPassword = 'Confirme sua senha.'
    else if (register.confirmPassword !== register.password) nextErrors.confirmPassword = 'As senhas não conferem.'
    if (!register.accountType) nextErrors.accountType = 'Escolha o tipo de conta.'
    if (!register.termsAccepted) nextErrors.termsAccepted = 'Você precisa aceitar os Termos de Uso e a Política de Privacidade.'

    setErrors(nextErrors)

    if (Object.keys(nextErrors).length === 0) {
      try {
        const result = await auth.register(register)
        if (result?.requiresEmailVerification) {
          setPendingEmail(result.email || register.email)
          setResendCooldown(result.resendCooldownSeconds || 60)
          setVerificationCode('')
          setRegistrationStep('verify')
          toast.success('Enviamos um código de verificação para seu e-mail.')
          return
        }
        toast.success('Cadastro validado com sucesso.')
        window.setTimeout(() => navigate('/'), 500)
      } catch (requestError) {
        setSubmitError(requestError.message || 'Não foi possível concluir o cadastro.')
      }
    } else {
      window.requestAnimationFrame(() => focusFirstInvalidField(registerFormRef.current))
    }
  }

  const handleVerificationSubmit = async (event) => {
    event.preventDefault()
    setSubmitError('')
    const normalizedCode = verificationCode.replace(/\D/g, '').slice(0, 6)
    setVerificationCode(normalizedCode)
    if (normalizedCode.length !== 6) {
      setSubmitError('Informe o código de 6 dígitos enviado para seu e-mail.')
      return
    }
    try {
      await verifyRegistrationCode({ email: pendingEmail, code: normalizedCode })
      setRegister(initialRegister)
      setVerificationCode('')
      setRegistrationStep('form')
      setMode('login')
      toast.success('E-mail verificado. Cadastro concluído. Faça login para continuar.')
    } catch (requestError) {
      setSubmitError(requestError.message || 'Código inválido ou expirado.')
    }
  }

  const handleResendCode = async () => {
    if (resendCooldown > 0) return
    setSubmitError('')
    try {
      const result = await resendRegistrationCode(pendingEmail)
      setResendCooldown(result.resendCooldownSeconds || 60)
      setVerificationCode('')
      toast.success('Enviamos um novo código para seu e-mail.')
    } catch (requestError) {
      setSubmitError(requestError.message || 'Não foi possível reenviar o código agora.')
    }
  }

  const handleBackToRegistration = () => {
    setRegistrationStep('form')
    setVerificationCode('')
    setSubmitError('')
    setErrors({})
  }

  return (
    <main className={styles.page}>
      <section className={styles.shell}>
        <Link className={styles.backLink} to="/" aria-label="Voltar para a página principal">
          <span>←</span>
          Voltar para o Luppa
        </Link>

        <div className={styles.header}>
          <Logo />
          <div>
            <h1>{mode === 'login' ? 'Entrar na conta' : 'Criar cadastro'}</h1>
            <p>{mode === 'login' ? 'Acesse sua conta para acompanhar eventos.' : 'Crie seu perfil para salvar interesses e divulgar eventos.'}</p>
          </div>
        </div>

        <div className={styles.tabs} role="tablist" aria-label="Escolha login ou cadastro">
          <button
            aria-selected={mode === 'login'}
            className={`${styles.tab} ${mode === 'login' ? styles.tabActive : ''}`}
            role="tab"
            type="button"
            onClick={() => handleModeChange('login')}
          >
            Login
          </button>
          <button
            aria-selected={mode === 'cadastro'}
            className={`${styles.tab} ${mode === 'cadastro' ? styles.tabActive : ''}`}
            role="tab"
            type="button"
            onClick={() => handleModeChange('cadastro')}
          >
            Cadastro
          </button>
        </div>

        {mode === 'login' ? (
          <form className={styles.panel} noValidate ref={loginFormRef} onSubmit={handleLoginSubmit}>
            <h2>Login</h2>
            {submitError && <div className={styles.submitError}>{submitError}</div>}
            <label>
              Email
              <input type="email" inputMode="email" maxLength={60} value={login.email} placeholder="seuemail@email.com" aria-invalid={Boolean(errors.email)} aria-describedby={errors.email ? errorId('login', 'email') : undefined} onChange={(event) => updateLogin('email', event.target.value)} />
              <FieldError id={errorId('login', 'email')} message={errors.email} />
            </label>
            <label>
              Senha
              <PasswordInput value={login.password} placeholder="Sua senha" autoComplete="current-password" maxLength={128} aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? errorId('login', 'password') : undefined} onChange={(event) => updateLogin('password', event.target.value)} />
              <FieldError id={errorId('login', 'password')} message={errors.password} />
            </label>
            <Link className={styles.forgotPassword} to="/forgot-password">
              Esqueci minha senha
            </Link>
            <button type="submit" disabled={auth.loading}>{auth.loading ? 'Entrando...' : 'Entrar'}</button>
          </form>
        ) : registrationStep === 'verify' ? (
          <form className={`${styles.panel} ${styles.verificationPanel}`} noValidate onSubmit={handleVerificationSubmit}>
            <h2>Verifique seu e-mail</h2>
            <p className={styles.verificationText}>
              Digite o código de 6 dígitos enviado para <strong>{pendingEmail}</strong>.
            </p>
            {submitError && <div className={styles.submitError}>{submitError}</div>}
            <label>
              Código de verificação
              <input
                className={styles.codeInput}
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={verificationCode}
                placeholder="000000"
                onChange={(event) => setVerificationCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
              />
            </label>
            <button type="submit" disabled={auth.loading}>Verificar código</button>
            <div className={styles.verificationActions}>
              <button type="button" onClick={handleResendCode} disabled={resendCooldown > 0}>
                {resendCooldown > 0 ? `Reenviar em ${resendCooldown}s` : 'Reenviar código'}
              </button>
              <button type="button" onClick={handleBackToRegistration}>Corrigir e-mail</button>
            </div>
          </form>
        ) : (
          <form className={styles.panel} noValidate ref={registerFormRef} onSubmit={handleRegisterSubmit}>
            <h2>Cadastro</h2>
            {submitError && <div className={styles.submitError}>{submitError}</div>}
            <div className={styles.formGrid}>
              <label>
                Nome completo
                <input type="text" value={register.fullName} placeholder="Seu nome completo" maxLength={85} aria-invalid={Boolean(errors.fullName)} aria-describedby={errors.fullName ? errorId('register', 'fullName') : undefined} onChange={(event) => updateRegister('fullName', event.target.value)} />
                <FieldError id={errorId('register', 'fullName')} message={errors.fullName} />
              </label>
              <label>
                Email
                <input type="email" inputMode="email" maxLength={60} value={register.email} placeholder="seuemail@email.com" aria-invalid={Boolean(errors.email)} aria-describedby={errors.email ? errorId('register', 'email') : undefined} onChange={(event) => updateRegister('email', event.target.value)} />
                <FieldError id={errorId('register', 'email')} message={errors.email} />
              </label>
              <label>
                Data de nascimento
                <input type="date" value={register.birthDate} max={todayDateInputValue()} aria-invalid={Boolean(errors.birthDate)} aria-describedby={errors.birthDate ? errorId('register', 'birthDate') : undefined} onChange={(event) => updateRegister('birthDate', event.target.value)} />
                <FieldError id={errorId('register', 'birthDate')} message={errors.birthDate} />
              </label>
              <label>
                Telefone
                <input type="tel" inputMode="tel" maxLength={15} value={register.phone} placeholder="(11) 99999-9999" aria-invalid={Boolean(errors.phone)} aria-describedby={errors.phone ? errorId('register', 'phone') : undefined} onChange={(event) => updateRegister('phone', event.target.value)} />
                <FieldError id={errorId('register', 'phone')} message={errors.phone} />
              </label>
              <label>
                Senha
                <PasswordInput value={register.password} placeholder="Mínimo de 8 caracteres" autoComplete="new-password" minLength={8} maxLength={128} aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? errorId('register', 'password') : undefined} onChange={(event) => updateRegister('password', event.target.value)} />
                <FieldError id={errorId('register', 'password')} message={errors.password} />
              </label>
              <label>
                Confirmar senha
                <PasswordInput value={register.confirmPassword} placeholder="Repita sua senha" autoComplete="new-password" minLength={8} maxLength={128} aria-invalid={Boolean(errors.confirmPassword)} aria-describedby={errors.confirmPassword ? errorId('register', 'confirmPassword') : undefined} onChange={(event) => updateRegister('confirmPassword', event.target.value)} />
                <FieldError id={errorId('register', 'confirmPassword')} message={errors.confirmPassword} />
              </label>
            </div>

            <fieldset className={styles.accountType}>
              <legend>
                Tipo de conta
                <span className={styles.help}>
                  <HelpIcon />
                  <span role="tooltip">
                    Standard User: pode buscar e interagir com eventos. Event Creator: pode criar e gerenciar seus próprios eventos.
                  </span>
                </span>
              </legend>
              <label>
                <input type="radio" name="accountType" value="standard" checked={register.accountType === 'standard'} onChange={(event) => updateRegister('accountType', event.target.value)} />
                <span>
                  <strong>USER</strong>
                  <small>Para navegar, buscar e interagir com eventos.</small>
                </span>
              </label>
              <label>
                <input type="radio" name="accountType" value="creator" checked={register.accountType === 'creator'} onChange={(event) => updateRegister('accountType', event.target.value)} />
                <span>
                  <strong>CREATOR</strong>
                  <small>Para criar e gerenciar seus próprios eventos.</small>
                </span>
              </label>
              <FieldError id={errorId('register', 'accountType')} message={errors.accountType} />
            </fieldset>

            <fieldset className={styles.consentBox}>
              <legend>Consentimentos</legend>
              <label>
                <input type="checkbox" checked={register.termsAccepted} aria-invalid={Boolean(errors.termsAccepted)} aria-describedby={errors.termsAccepted ? errorId('register', 'termsAccepted') : undefined} onChange={(event) => updateRegister('termsAccepted', event.target.checked)} />
                <span>
                  Li e concordo com os <Link to="/terms">Termos de Uso</Link> e a <Link to="/privacy">Política de Privacidade</Link>.
                </span>
              </label>
              <FieldError id={errorId('register', 'termsAccepted')} message={errors.termsAccepted} />
              <label>
                <input type="checkbox" checked={register.marketingEmails} onChange={(event) => updateRegister('marketingEmails', event.target.checked)} />
                <span>Quero receber novidades, recomendações e comunicações da Luppa por e-mail.</span>
              </label>
            </fieldset>

            <button type="submit" disabled={auth.loading}>{auth.loading ? 'Cadastrando...' : 'Cadastrar'}</button>
          </form>
        )}
      </section>
    </main>
  )
}

export default LoginCadastroLuppa
