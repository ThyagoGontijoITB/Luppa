import { useState } from 'react'
import { Link } from 'react-router-dom'
import styles from './LoginCadastroLuppa.module.css'

const LoginCadastroLuppa = () => {
  const [mode, setMode] = useState('login')
  const [feedback, setFeedback] = useState('')

  const showFeedback = (message) => {
    setFeedback(message)
    window.setTimeout(() => setFeedback(''), 2600)
  }

  return (
    <main className={styles.page}>
      <section className={styles.shell}>
        <Link className={styles.backLink} to="/" aria-label="Voltar para a página principal">
          <span>←</span>
          Voltar para o Luppa
        </Link>

        <div className={styles.header}>
          <span className={styles.logo}>L</span>
          <div>
            <h1>{mode === 'login' ? 'Entrar na conta' : 'Criar cadastro'}</h1>
            <p>{mode === 'login' ? 'Acesse sua conta para acompanhar eventos.' : 'Crie seu perfil para salvar interesses e divulgar eventos.'}</p>
          </div>
        </div>

        <div className={styles.tabs} role="tablist" aria-label="Escolha login ou cadastro">
          <button
            className={mode === 'login' ? styles.tabActive : ''}
            type="button"
            onClick={() => setMode('login')}
          >
            Login
          </button>
          <button
            className={mode === 'cadastro' ? styles.tabActive : ''}
            type="button"
            onClick={() => setMode('cadastro')}
          >
            Cadastro
          </button>
        </div>

        {mode === 'login' ? (
          <form className={styles.panel}>
            <h2>Login</h2>
            <label>
              Email
              <input type="email" placeholder="seuemail@email.com" />
            </label>
            <label>
              Senha
              <input type="password" placeholder="Sua senha" />
            </label>
            <button type="button" onClick={() => showFeedback('Login efetuado com sucesso!')}>
              Entrar
            </button>
          </form>
        ) : (
          <form className={styles.panel}>
            <h2>Cadastro</h2>
            <label>
              Nome
              <input type="text" placeholder="Seu nome" />
            </label>
            <label>
              Email
              <input type="email" placeholder="seuemail@email.com" />
            </label>
            <label>
              Senha
              <input type="password" placeholder="Crie uma senha" />
            </label>
            <button type="button" onClick={() => showFeedback('Cadastro efetuado com sucesso!')}>
              Cadastrar
            </button>
          </form>
        )}
      </section>

      {feedback && <div className={styles.toast}>{feedback}</div>}
    </main>
  )
}

export default LoginCadastroLuppa
