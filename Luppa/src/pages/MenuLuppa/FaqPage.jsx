import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { SearchIcon } from '../../components/common/Icons'
import { UtilityPageHeader } from './LegalDocumentLayout'
import styles from './MenuLuppa.module.css'

const faqSections = [
  {
    id: 'conta',
    title: 'Conta',
    questions: [
      {
        question: 'Como criar uma conta na Luppa?',
        answer: 'Acesse Login/Cadastro, escolha Cadastro e preencha nome, e-mail, data de nascimento, telefone, senha, tipo de conta e aceite dos Termos de Uso e da Política de Privacidade. Depois disso, a Luppa envia um código para confirmar o e-mail informado.',
      },
      {
        question: 'Preciso verificar meu e-mail?',
        answer: 'Sim. O cadastro atual solicita a confirmação do e-mail por um código de 6 dígitos. Enquanto o e-mail não é confirmado, o acesso pode exigir nova verificação.',
      },
      {
        question: 'Como alterar meus dados?',
        answer: 'Depois de entrar na conta, acesse Perfil. Nessa área é possível editar nome, e-mail, data de nascimento, telefone e foto de perfil.',
      },
      {
        question: 'Esqueci minha senha. O que faço?',
        answer: 'Na tela de login, use Esqueci minha senha. A Luppa envia um link de redefinição para o e-mail informado quando houver uma conta correspondente.',
      },
    ],
  },
  {
    id: 'eventos',
    title: 'Eventos',
    questions: [
      {
        question: 'Como criar um evento?',
        answer: 'Use a opção Cadastrar Evento. A página solicita nome, categoria, descrição, endereço validado, datas, horários e imagem de capa. Ao enviar, o evento vai para análise.',
      },
      {
        question: 'Quem pode criar eventos?',
        answer: 'A criação de eventos é permitida para contas do tipo CREATOR e para administradores. Contas USER podem buscar, salvar, avaliar e interagir com eventos, mas não publicam eventos nesta versão.',
      },
      {
        question: 'Posso editar um evento depois de publicado?',
        answer: 'A interface atual mostra os eventos associados à conta em Meus eventos, mas não apresenta um fluxo público de edição de evento publicado. Se precisar corrigir algo, use o suporte.',
      },
      {
        question: 'Por que meu evento pode não aparecer imediatamente?',
        answer: 'Eventos enviados entram com status de análise antes de aparecerem publicamente. A moderação pode aprovar, rejeitar ou analisar informações quando necessário.',
      },
      {
        question: 'Como encontrar eventos?',
        answer: 'A página Eventos permite buscar, filtrar por categoria e ordenar resultados. A home também destaca eventos publicados e categorias disponíveis.',
      },
    ],
  },
  {
    id: 'eventos-salvos',
    title: 'Eventos salvos',
    questions: [
      {
        question: 'Como salvar um evento?',
        answer: 'Abra os detalhes de um evento e use Tenho interesse. É necessário entrar na conta para salvar eventos.',
      },
      {
        question: 'Onde encontro meus eventos salvos?',
        answer: 'Depois de entrar, acesse Perfil e abra a seção Eventos salvos. Ela lista os eventos associados à sua conta.',
      },
      {
        question: 'Posso remover um evento dos salvos?',
        answer: 'Sim. Na área Perfil, em Eventos salvos, use a ação Remover dos salvos no evento desejado.',
      },
    ],
  },
  {
    id: 'avaliacoes',
    title: 'Avaliações',
    questions: [
      {
        question: 'Quando posso avaliar um evento?',
        answer: 'A avaliação fica disponível depois que o evento termina. Antes disso, a área de avaliações informa que elas serão liberadas após o encerramento.',
      },
      {
        question: 'Posso alterar ou excluir minha avaliação?',
        answer: 'Sim. Ao abrir a área de avaliações de um evento encerrado, usuários autenticados podem enviar uma avaliação, atualizar a própria avaliação e excluir a avaliação já registrada.',
      },
      {
        question: 'Como denunciar uma avaliação inadequada?',
        answer: 'Na lista de avaliações, abra o menu de opções da avaliação e selecione Denunciar avaliação. É necessário estar logado, escolher um motivo e descrever o problema.',
      },
    ],
  },
  {
    id: 'denuncias',
    title: 'Denúncias e moderação',
    questions: [
      {
        question: 'Como denunciar um evento ou conteúdo?',
        answer: 'Abra os detalhes do evento e use Denunciar evento. Para avaliações, use o menu da avaliação. A denúncia exige login, motivo e descrição.',
      },
      {
        question: 'O que acontece depois de uma denúncia?',
        answer: 'A denúncia é enviada para análise. Os Termos informam que uma denúncia não remove conteúdo automaticamente; a equipe pode manter, restringir ou remover conteúdo conforme o contexto.',
      },
      {
        question: 'A pessoa denunciada sabe quem fez a denúncia?',
        answer: 'A Política de Privacidade lista denúncias como dados tratados pela plataforma, mas a interface pública não exibe o denunciante para outros usuários. A Luppa usa esses dados para análise, segurança e moderação.',
      },
    ],
  },
  {
    id: 'seguranca',
    title: 'Segurança e privacidade',
    questions: [
      {
        question: 'Como a Luppa utiliza meus dados?',
        answer: 'A Política de Privacidade informa que os dados podem ser usados para criar e administrar contas, autenticar usuários, verificar e-mails, permitir eventos, avaliações, denúncias, eventos salvos, suporte, comunicações transacionais e segurança da plataforma.',
      },
      {
        question: 'Onde posso consultar a Política de Privacidade?',
        answer: 'A Política de Privacidade está disponível em /privacy e também aparece nos links de documentos da plataforma.',
      },
      {
        question: 'Como entrar em contato com a Luppa?',
        answer: 'Você pode usar a página de suporte em /support ou enviar e-mail para projetoluppa@gmail.com.',
      },
    ],
  },
]

const normalize = (value) => value
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .toLowerCase()

const FaqPage = () => {
  const [query, setQuery] = useState('')
  const [openItem, setOpenItem] = useState('')

  const filteredSections = useMemo(() => {
    const term = normalize(query.trim())
    if (!term) return faqSections

    return faqSections
      .map((section) => ({
        ...section,
        questions: section.questions.filter((item) => (
          normalize(`${section.title} ${item.question} ${item.answer}`).includes(term)
        )),
      }))
      .filter((section) => section.questions.length > 0)
  }, [query])

  const resultCount = filteredSections.reduce((total, section) => total + section.questions.length, 0)

  return (
    <div className={styles.page}>
      <UtilityPageHeader current="faq" />
      <main className={styles.faqPage}>
        <div className={styles.faqContainer}>
          <Link className={styles.backLink} to="/">Voltar</Link>

          <header className={styles.faqHero}>
            <span>Ajuda / Luppa</span>
            <h1>Perguntas frequentes</h1>
            <p>Encontre respostas para as principais dúvidas sobre a Luppa.</p>
          </header>

          <section className={styles.faqSearch} aria-label="Busca no FAQ">
            <SearchIcon className={styles.faqSearchIcon} />
            <label htmlFor="faq-search">Buscar pergunta</label>
            <input
              id="faq-search"
              type="search"
              value={query}
              placeholder="Busque por conta, eventos, denúncias..."
              onChange={(event) => {
                setQuery(event.target.value)
                setOpenItem('')
              }}
            />
            <small>{resultCount} resposta{resultCount === 1 ? '' : 's'}</small>
          </section>

          {filteredSections.length > 0 ? (
            <div className={styles.faqShell}>
              <nav className={styles.faqCategoryNav} aria-label="Categorias do FAQ">
                <span>Categorias</span>
                {filteredSections.map((section) => (
                  <a key={section.id} href={`#faq-${section.id}`}>{section.title}</a>
                ))}
              </nav>

              <div className={styles.faqList}>
                {filteredSections.map((section, index) => (
                  <section className={styles.faqSection} id={`faq-${section.id}`} key={section.id} aria-labelledby={`faq-${section.id}-title`}>
                    <div className={styles.faqSectionHeader}>
                      <span>{(index + 1).toString().padStart(2, '0')}</span>
                      <h2 id={`faq-${section.id}-title`}>{section.title}</h2>
                    </div>

                    <div className={styles.faqItems}>
                      {section.questions.map((item, index) => {
                        const itemId = `${section.id}-${index}`
                        const isOpen = openItem === itemId
                        return (
                          <article className={styles.faqItem} key={item.question}>
                            <h3>
                              <button
                                type="button"
                                aria-expanded={isOpen}
                                aria-controls={`faq-answer-${itemId}`}
                                id={`faq-question-${itemId}`}
                                onClick={() => setOpenItem(isOpen ? '' : itemId)}
                              >
                                <span>{item.question}</span>
                                <strong aria-hidden="true">{isOpen ? '-' : '+'}</strong>
                              </button>
                            </h3>
                            <div
                              className={styles.faqAnswer}
                              id={`faq-answer-${itemId}`}
                              role="region"
                              aria-labelledby={`faq-question-${itemId}`}
                              hidden={!isOpen}
                            >
                              <p>{item.answer}</p>
                            </div>
                          </article>
                        )
                      })}
                    </div>
                  </section>
                ))}
              </div>
            </div>
          ) : (
            <section className={styles.faqEmpty} role="status">
              <span>Nenhum resultado</span>
              <h2>Não encontramos uma resposta para sua busca.</h2>
              <p>Tente outro termo ou fale com a equipe Luppa pelo suporte.</p>
              <Link to="/support">Abrir suporte</Link>
            </section>
          )}

          <section className={styles.faqSupport}>
            <div>
              <span>Suporte</span>
              <h2>Não encontrou o que precisava?</h2>
              <p>Envie uma mensagem para a equipe Luppa pelo formulário de suporte ou pelo e-mail oficial.</p>
              <a href="mailto:projetoluppa@gmail.com">projetoluppa@gmail.com</a>
            </div>
            <Link to="/support">Falar com suporte</Link>
          </section>
        </div>
      </main>
    </div>
  )
}

export default FaqPage
