import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import {
  DEFAULT_DESCRIPTION,
  DEFAULT_IMAGE,
  DEFAULT_TITLE,
  SITE_NAME,
  absoluteUrl,
  organizationSchema,
} from './seoMetadata'

const privatePathPatterns = [
  /^\/admin(?:\/|$)/,
  /^\/profile(?:\/|$)/,
  /^\/communication-preferences(?:\/|$)/,
  /^\/cadastrar-evento(?:\/|$)/,
  /^\/my-events(?:\/|$)/,
  /^\/creator(?:\/|$)/,
  /^\/login-cadastro(?:\/|$)/,
  /^\/forgot-password(?:\/|$)/,
  /^\/reset-password(?:\/|$)/,
  /^\/verify-email(?:\/|$)/,
  /^\/resend-verification(?:\/|$)/,
]

const ensureMeta = (selector, createAttributes) => {
  let element = document.head.querySelector(selector)
  if (!element) {
    element = document.createElement('meta')
    Object.entries(createAttributes).forEach(([key, value]) => element.setAttribute(key, value))
    document.head.appendChild(element)
  }
  return element
}

const setMeta = (name, content) => {
  const element = ensureMeta(`meta[name="${name}"]`, { name })
  element.setAttribute('content', content)
}

const setProperty = (property, content) => {
  const element = ensureMeta(`meta[property="${property}"]`, { property })
  element.setAttribute('content', content)
}

const setLink = (rel, href) => {
  let element = document.head.querySelector(`link[rel="${rel}"]`)
  if (!element) {
    element = document.createElement('link')
    element.setAttribute('rel', rel)
    document.head.appendChild(element)
  }
  element.setAttribute('href', href)
}

const removeStructuredData = () => {
  document.head.querySelectorAll('script[data-seo-schema="true"]').forEach((element) => element.remove())
}

const setStructuredData = (schema) => {
  removeStructuredData()
  if (!schema) return

  const items = Array.isArray(schema) ? schema : [schema]
  items.filter(Boolean).forEach((item) => {
    const script = document.createElement('script')
    script.type = 'application/ld+json'
    script.dataset.seoSchema = 'true'
    script.textContent = JSON.stringify(item)
    document.head.appendChild(script)
  })
}

const normalizePath = (pathname) => (pathname === '/' ? '/' : pathname.replace(/\/+$/, ''))
const isPrivatePath = (pathname) => privatePathPatterns.some((pattern) => pattern.test(pathname))

const buildRobots = (pathname, explicitNoIndex) => (
  explicitNoIndex || isPrivatePath(pathname) ? 'noindex, nofollow' : 'index, follow'
)

const Seo = ({
  title = DEFAULT_TITLE,
  description = DEFAULT_DESCRIPTION,
  image = DEFAULT_IMAGE,
  type = 'website',
  canonicalPath,
  noindex = false,
  schema,
}) => {
  const location = useLocation()
  const pathname = normalizePath(location.pathname)
  const canonical = absoluteUrl(canonicalPath || pathname)
  const robots = buildRobots(pathname, noindex)

  useEffect(() => {
    document.documentElement.lang = 'pt-BR'
    document.title = title
    setMeta('description', description)
    setMeta('robots', robots)
    setMeta('googlebot', robots)
    setMeta('author', SITE_NAME)
    setMeta('theme-color', '#0f172a')
    setLink('canonical', canonical)

    setProperty('og:site_name', SITE_NAME)
    setProperty('og:type', type)
    setProperty('og:title', title)
    setProperty('og:description', description)
    setProperty('og:url', canonical)
    setProperty('og:image', image)
    setProperty('og:locale', 'pt_BR')

    setMeta('twitter:card', 'summary_large_image')
    setMeta('twitter:title', title)
    setMeta('twitter:description', description)
    setMeta('twitter:image', image)

    setStructuredData(schema || [organizationSchema])
  }, [canonical, description, image, robots, schema, title, type])

  return null
}

export default Seo
