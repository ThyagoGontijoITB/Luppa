import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import LuppaSymbol from '../../components/common/LuppaSymbol'
import Seo from '../../components/seo/Seo'
import { absoluteUrl, breadcrumbSchema, organizationSchema } from '../../components/seo/seoMetadata'
import styles from './MenuLuppa.module.css'

const UtilityHeader = ({ current = 'legal' }) => (
  <header className={styles.utilityHeader}>
    <nav className={styles.utilityNav} aria-label="Navegação principal">
      <Link className={styles.utilityLogo} to="/" aria-label="Luppa">
        <LuppaSymbol />
      </Link>
      <div className={styles.utilityLinks}>
        <Link to="/">Início</Link>
        <Link to="/events">Eventos</Link>
        <Link to="/faq" aria-current={current === 'faq' ? 'page' : undefined}>FAQ</Link>
        <Link to="/support" aria-current={current === 'support' ? 'page' : undefined}>Suporte</Link>
        <Link to="/terms" aria-current={current === 'terms' ? 'page' : undefined}>Termos</Link>
        <Link to="/privacy" aria-current={current === 'privacy' ? 'page' : undefined}>Privacidade</Link>
      </div>
    </nav>
  </header>
)

export const UtilityPageHeader = UtilityHeader

const LegalTableOfContents = ({ sections, activeId }) => (
  <nav className={styles.legalToc} aria-label="Sumário do documento">
    <span>Sumário</span>
    <ol>
      {sections.map((section) => (
        <li key={section.id}>
          <a className={activeId === section.id ? styles.activeTocLink : ''} href={`#${section.id}`}>
            {section.number && <small>{section.number}</small>}
            {section.title}
          </a>
        </li>
      ))}
    </ol>
  </nav>
)

const LegalBlock = ({ block }) => {
  if (block.type === 'list') {
    return (
      <ul>
        {block.items.map((item) => <li key={item}>{item}</li>)}
      </ul>
    )
  }

  return <p>{block.text}</p>
}

const LegalDocumentLayout = ({ document, type }) => {
  const [activeId, setActiveId] = useState(document.sections[0]?.id)
  const sectionIds = useMemo(() => document.sections.map((section) => section.id), [document.sections])

  useEffect(() => {
    const headings = sectionIds
      .map((id) => window.document.getElementById(id))
      .filter(Boolean)

    if (headings.length === 0) return undefined

    const observer = new IntersectionObserver(
      (entries) => {
        const visibleEntry = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0]

        if (visibleEntry) {
          setActiveId(visibleEntry.target.id)
        }
      },
      { rootMargin: '-120px 0px -62% 0px', threshold: [0, 1] },
    )

    headings.forEach((heading) => observer.observe(heading))
    return () => observer.disconnect()
  }, [sectionIds])

  return (
    <div className={styles.page}>
      <Seo
        title={`${document.title} | Luppa`}
        description={document.intro}
        canonicalPath={`/${type === 'privacy' ? 'privacy' : 'terms'}`}
        schema={[
          organizationSchema,
          breadcrumbSchema([
            { name: 'Início', path: '/' },
            { name: document.title, path: `/${type === 'privacy' ? 'privacy' : 'terms'}` },
          ]),
          {
            '@context': 'https://schema.org',
            '@type': 'WebPage',
            name: document.title,
            url: absoluteUrl(`/${type === 'privacy' ? 'privacy' : 'terms'}`),
            description: document.intro,
            dateModified: document.updatedAt,
          },
        ]}
      />
      <UtilityHeader current={type} />
      <main className={styles.legalPage}>
        <div className={styles.legalContainer}>
          <Link className={styles.backLink} to="/">Voltar</Link>

          <header className={styles.legalHero}>
            <span>{document.eyebrow} / Luppa</span>
            <h1>{document.title}</h1>
            <p>{document.intro}</p>
            <small>Última atualização: {document.updatedAt}</small>
          </header>

          <div className={styles.mobileToc}>
            <label htmlFor={`${type}-toc`}>Neste documento</label>
            <select
              id={`${type}-toc`}
              value={activeId || document.sections[0]?.id}
              onChange={(event) => {
                window.document.getElementById(event.target.value)?.scrollIntoView({ block: 'start' })
              }}
            >
              {document.sections.map((section) => (
                <option key={section.id} value={section.id}>
                  {section.number ? `${section.number} ` : ''}{section.title}
                </option>
              ))}
            </select>
          </div>

          <div className={styles.legalShell}>
            <LegalTableOfContents sections={document.sections} activeId={activeId} />

            <article className={styles.legalDocument}>
              <div className={styles.legalOpening}>
                {document.opening.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
              </div>

              {document.sections.map((section) => (
                <section className={styles.legalSection} key={section.id} aria-labelledby={section.id}>
                  <h2 id={section.id}>
                    {section.number && <span>{section.number}</span>}
                    {section.title}
                  </h2>
                  {section.blocks.map((block, index) => (
                    <LegalBlock block={block} key={`${section.id}-${index.toString()}`} />
                  ))}
                </section>
              ))}

              <footer className={styles.legalRelated}>
                <p>{document.related.label}</p>
                <Link to={document.related.to}>{document.related.linkLabel}</Link>
              </footer>
            </article>
          </div>
        </div>
      </main>
    </div>
  )
}

export default LegalDocumentLayout
