const defaultProvider = 'nominatim'
const defaultNominatimUrl = 'https://nominatim.openstreetmap.org/search'

const getConfig = () => ({
  provider: import.meta.env.VITE_GEOCODING_PROVIDER || defaultProvider,
  nominatimBaseUrl: import.meta.env.VITE_NOMINATIM_BASE_URL || defaultNominatimUrl,
})

const hasCoordinates = (event) => Number.isFinite(event.latitude) && Number.isFinite(event.longitude)

export const formatCep = (cep) => {
  const digits = String(cep || '').replace(/\D/g, '').slice(0, 8)
  if (digits.length !== 8) return cep || ''
  return `${digits.slice(0, 5)}-${digits.slice(5)}`
}

export const buildStructuredAddress = (address = {}) => ({
  cep: String(address.cep || address.zipCode || '').replace(/\D/g, '').slice(0, 8),
  rua: String(address.rua || address.street || '').trim(),
  numero: String(address.numero ?? address.number ?? '').replace(/\D/g, '').slice(0, 9),
  cidade: String(address.cidade || address.city || '').trim(),
  estado: String(address.estado || address.state || '').trim().toUpperCase().slice(0, 30),
  pais: String(address.pais || address.country || '').trim(),
})

export const formatStructuredAddress = (address = {}) => {
  const normalized = buildStructuredAddress(address)
  const street = [normalized.rua, normalized.numero].filter(Boolean).join(', ')
  const cityState = [normalized.cidade, normalized.estado].filter(Boolean).join(' - ')
  const cep = normalized.cep ? `CEP ${formatCep(normalized.cep)}` : ''
  return [street, cityState, normalized.pais, cep].filter(Boolean).join(', ')
}

export const getEventAddressQuery = (event) => (
  formatStructuredAddress(event?.addressFields || event?.address || {}) || event?.address || event?.locationName || ''
)

const parseNominatimResponse = async (response) => {
  if (!response.ok) {
    throw new Error('geocoding_failed')
  }

  const results = await response.json()
  const firstResult = results?.[0]

  if (!firstResult) {
    return null
  }

  return {
    latitude: Number(firstResult.lat),
    longitude: Number(firstResult.lon),
    source: 'geocoding',
  }
}

const fetchNominatim = async (url, signal) => parseNominatimResponse(await fetch(url, {
  headers: {
    Accept: 'application/json',
    'Accept-Language': 'pt-BR,pt;q=0.9',
  },
  signal,
}))

const geocodeStructuredWithNominatim = async (addressFields, signal) => {
  const { nominatimBaseUrl } = getConfig()
  const url = new URL(nominatimBaseUrl)
  url.searchParams.set('format', 'jsonv2')
  url.searchParams.set('limit', '1')
  url.searchParams.set('street', [addressFields.numero, addressFields.rua].filter(Boolean).join(' '))
  url.searchParams.set('city', addressFields.cidade)
  url.searchParams.set('state', addressFields.estado)
  url.searchParams.set('country', addressFields.pais)
  if (addressFields.cep) url.searchParams.set('postalcode', addressFields.cep)

  return fetchNominatim(url, signal)
}

const geocodeQueryWithNominatim = async (address, signal) => {
  const { nominatimBaseUrl } = getConfig()
  const url = new URL(nominatimBaseUrl)
  url.searchParams.set('format', 'jsonv2')
  url.searchParams.set('limit', '1')
  url.searchParams.set('q', address)

  return fetchNominatim(url, signal)
}

const buildGeocodingFallbackQueries = (addressFields) => {
  const street = [addressFields.rua, addressFields.numero].filter(Boolean).join(' ')
  const cityStateCountry = [addressFields.cidade, addressFields.estado, addressFields.pais].filter(Boolean).join(' ')
  return [
    [street, cityStateCountry].filter(Boolean).join(' '),
    [addressFields.rua, cityStateCountry].filter(Boolean).join(' '),
  ].filter(Boolean)
}

export const resolveEventLocation = async (event, signal) => {
  if (hasCoordinates(event)) {
    return {
      latitude: event.latitude,
      longitude: event.longitude,
      source: 'event',
    }
  }

  const addressFields = buildStructuredAddress(event?.addressFields || event?.address || {})
  const addressQuery = getEventAddressQuery(event)

  if (!addressQuery.trim()) {
    return null
  }

  const { provider } = getConfig()

  if (provider !== 'nominatim') {
    throw new Error('unsupported_geocoding_provider')
  }

  if (addressFields.rua && addressFields.cidade && addressFields.estado && addressFields.pais) {
    const structuredResult = await geocodeStructuredWithNominatim(addressFields, signal)
    if (structuredResult) return structuredResult
  }

  for (const query of buildGeocodingFallbackQueries(addressFields)) {
    const result = await geocodeQueryWithNominatim(query, signal)
    if (result) return result
  }

  return geocodeQueryWithNominatim(addressQuery, signal)
}

const stateAliases = {
  Acre: 'AC',
  Alagoas: 'AL',
  Amapá: 'AP',
  Amazonas: 'AM',
  Bahia: 'BA',
  Ceará: 'CE',
  'Distrito Federal': 'DF',
  'Espírito Santo': 'ES',
  Goiás: 'GO',
  Maranhão: 'MA',
  'Mato Grosso': 'MT',
  'Mato Grosso do Sul': 'MS',
  'Minas Gerais': 'MG',
  Pará: 'PA',
  Paraíba: 'PB',
  Paraná: 'PR',
  Pernambuco: 'PE',
  Piauí: 'PI',
  'Rio de Janeiro': 'RJ',
  'Rio Grande do Norte': 'RN',
  'Rio Grande do Sul': 'RS',
  Rondônia: 'RO',
  Roraima: 'RR',
  'Santa Catarina': 'SC',
  'São Paulo': 'SP',
  Sergipe: 'SE',
  Tocantins: 'TO',
}

const normalizeState = (value) => {
  const trimmed = String(value || '').trim()
  if (trimmed.length === 2) return trimmed.toUpperCase()
  return stateAliases[trimmed] || trimmed.toUpperCase()
}

const buildContextualSearchQuery = (query, context = {}) => {
  const parts = [
    query,
    context.cidade,
    context.estado,
    context.cep,
    context.pais || 'Brasil',
  ]
  return parts.filter(Boolean).join(', ')
}

export const searchLocations = async (query, signal, context = {}) => {
  const trimmedQuery = query.trim()
  if (trimmedQuery.length < 3) return []

  const { provider, nominatimBaseUrl } = getConfig()
  if (provider !== 'nominatim') {
    throw new Error('unsupported_geocoding_provider')
  }

  const url = new URL(nominatimBaseUrl)
  url.searchParams.set('format', 'jsonv2')
  url.searchParams.set('addressdetails', '1')
  url.searchParams.set('limit', '5')
  url.searchParams.set('countrycodes', 'br')
  url.searchParams.set('q', buildContextualSearchQuery(trimmedQuery, context))

  const response = await fetch(url, {
    headers: {
      Accept: 'application/json',
      'Accept-Language': 'pt-BR,pt;q=0.9',
    },
    signal,
  })

  if (!response.ok) {
    throw new Error('location_search_failed')
  }

  const results = await response.json()

  return results.map((result) => {
    const address = result.address || {}
    const city = address.city || address.town || address.village || address.municipality || address.county || ''
    const region = address.state || ''
    const venue = result.name || address.amenity || address.road || result.display_name?.split(',')[0] || trimmedQuery
    const street = address.road || address.pedestrian || address.footway || address.cycleway || venue
    const postcode = String(address.postcode || '').replace(/\D/g, '').slice(0, 8)

    return {
      id: result.place_id?.toString() || `${result.lat}-${result.lon}`,
      venue,
      formattedAddress: result.display_name,
      cityRegion: [city, region].filter(Boolean).join(', '),
      addressFields: buildStructuredAddress({
        cep: postcode || context.cep || '',
        rua: street,
        numero: address.house_number || context.numero || '',
        cidade: city || context.cidade || '',
        estado: normalizeState(region || context.estado || ''),
        pais: address.country || context.pais || 'Brasil',
      }),
      latitude: Number(result.lat),
      longitude: Number(result.lon),
    }
  })
}
