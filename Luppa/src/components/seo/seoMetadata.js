export const SITE_URL = 'https://luppa-one.vercel.app'
export const SITE_NAME = 'Luppa'
export const DEFAULT_TITLE = 'Luppa | Descubra e divulgue eventos em Barueri'
export const DEFAULT_DESCRIPTION = 'Encontre, compare e divulgue eventos de música, cultura, gastronomia e esporte em Barueri e região com o Luppa.'
export const DEFAULT_IMAGE = `${SITE_URL}/icons.svg`

export const absoluteUrl = (path = '/') => `${SITE_URL}${path === '/' ? '' : path}`

export const organizationSchema = {
  '@context': 'https://schema.org',
  '@type': 'Organization',
  name: SITE_NAME,
  url: SITE_URL,
  logo: DEFAULT_IMAGE,
  email: 'projetoluppa@gmail.com',
  areaServed: {
    '@type': 'City',
    name: 'Barueri',
    addressCountry: 'BR',
  },
}

export const websiteSchema = {
  '@context': 'https://schema.org',
  '@type': 'WebSite',
  name: SITE_NAME,
  url: SITE_URL,
  description: DEFAULT_DESCRIPTION,
}

export const faqSchema = (sections) => ({
  '@context': 'https://schema.org',
  '@type': 'FAQPage',
  mainEntity: sections.flatMap((section) => section.questions).map((item) => ({
    '@type': 'Question',
    name: item.question,
    acceptedAnswer: {
      '@type': 'Answer',
      text: item.answer,
    },
  })),
})

export const breadcrumbSchema = (items) => ({
  '@context': 'https://schema.org',
  '@type': 'BreadcrumbList',
  itemListElement: items.map((item, index) => ({
    '@type': 'ListItem',
    position: index + 1,
    name: item.name,
    item: absoluteUrl(item.path),
  })),
})
