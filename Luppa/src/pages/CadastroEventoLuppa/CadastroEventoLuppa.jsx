import { Link } from 'react-router-dom'
import styles from './CadastroEventoLuppa.module.css'

const CadastroEventoLuppa = () => {
  return (
    <main className={styles.page}>
      <section className={styles.shell}>
        <Link className={styles.backLink} to="/">Voltar para o Luppa</Link>
        <div className={styles.header}>
          <span className={styles.logo}>L</span>
          <div>
            <h1>Cadastrar Evento</h1>
            <p>Preencha as informações principais para simular a criação de uma página de evento.</p>
          </div>
        </div>

        <form className={styles.panel}>
          <div className={styles.row}>
            <label>
              Título do evento
              <input type="text" placeholder="Ex: Festival de Música no Parque" />
            </label>
            <label>
              Categoria
              <select defaultValue="">
                <option value="" disabled>Selecione</option>
                <option>Música</option>
                <option>Gastronomia</option>
                <option>Cultura</option>
                <option>Esportes</option>
              </select>
            </label>
          </div>

          <div className={styles.row}>
            <label>
              Data
              <input type="date" />
            </label>
            <label>
              Horário
              <input type="time" />
            </label>
          </div>

          <label>
            Local
            <input type="text" placeholder="Nome do espaço, praça, teatro ou parque" />
          </label>

          <label>
            Descrição
            <textarea rows="5" placeholder="Conte o que vai acontecer, para quem é o evento e o que o público pode esperar." />
          </label>

          <button type="button">Salvar rascunho do evento</button>
        </form>
      </section>
    </main>
  )
}

export default CadastroEventoLuppa
