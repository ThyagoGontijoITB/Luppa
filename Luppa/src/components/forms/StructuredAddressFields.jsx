import { useEffect, useMemo, useRef, useState } from 'react'
import EventMap from '../events/EventMap'
import FieldError from './FieldError'
import { brazilStates } from '../../constants/brazilStates'
import { lookupBrazilianCep } from '../../services/cepService'
import { buildStructuredAddress, formatStructuredAddress, resolveEventLocation, searchLocations } from '../../services/eventLocationService'
import { cepDigits, formatBrazilianCep, isBrazilianCep } from '../../utils/validation'
import styles from './StructuredAddressFields.module.css'

const emptyResolution = {
  verified: false,
  source: null,
  coordinates: null,
  message: '',
}

const hasAddressForMap = (address, source) => (
  Boolean(source)
  && isBrazilianCep(address.cep)
  && Boolean(String(address.rua || '').trim())
  && Boolean(String(address.numero || '').trim())
  && Boolean(String(address.cidade || '').trim())
  && Boolean(String(address.estado || '').trim())
  && Boolean(String(address.pais || '').trim())
)

const formatSuggestionLabel = (suggestion) => {
  const fields = suggestion.addressFields || {}
  const cityState = [fields.cidade, fields.estado].filter(Boolean).join(' - ')
  return [fields.rua || suggestion.venue, suggestion.formattedAddress?.split(',').slice(1, 3).join(', '), cityState]
    .filter(Boolean)
    .join(' · ')
}

const StructuredAddressFields = ({
  value,
  errors = {},
  onChange,
  onResolutionChange,
  legend = 'Endereço do evento',
}) => {
  const [cepStatus, setCepStatus] = useState('idle')
  const [cepMessage, setCepMessage] = useState('')
  const [streetStatus, setStreetStatus] = useState('idle')
  const [suggestions, setSuggestions] = useState([])
  const [source, setSource] = useState(null)
  const [mapStatus, setMapStatus] = useState('idle')
  const [coordinates, setCoordinates] = useState(null)
  const requestId = useRef(0)
  const address = useMemo(() => ({
    cep: value.cep || '',
    rua: value.rua || '',
    numero: String(value.numero || ''),
    cidade: value.cidade || '',
    estado: value.estado || '',
    pais: value.pais || 'Brasil',
  }), [value])

  const notifyResolution = (next) => {
    onResolutionChange?.({
      ...emptyResolution,
      ...next,
    })
  }

  const invalidateResolution = (message = '') => {
    setCoordinates(null)
    setMapStatus('idle')
    notifyResolution({ message })
  }

  const patchAddress = (patch, { invalidate = true, nextSource = source } = {}) => {
    onChange?.({
      ...patch,
      pais: patch.pais ?? address.pais ?? 'Brasil',
    })
    if (invalidate) {
      setSource(nextSource)
      invalidateResolution(nextSource ? 'Confirme o número para atualizar o mapa.' : 'Resolva o endereço antes de publicar.')
    }
  }

  const updateCep = (inputValue) => {
    const digits = cepDigits(inputValue)
    const nextCep = formatBrazilianCep(digits)
    setCepStatus(digits.length === 0 ? 'idle' : digits.length === 8 ? 'idle' : 'incomplete')
    setCepMessage('')
    setSuggestions([])
    setStreetStatus('idle')
    setSource(null)
    patchAddress({
      cep: nextCep,
      rua: '',
      cidade: '',
      estado: '',
      pais: 'Brasil',
    }, { nextSource: null })
  }

  const updateAddressField = (field, inputValue) => {
    const nextValue = field === 'numero'
      ? inputValue.replace(/\D/g, '').slice(0, 9)
      : inputValue

    if (field === 'numero') {
      patchAddress({ numero: nextValue }, { invalidate: true, nextSource: source })
      return
    }

    setSource(null)
    patchAddress({ [field]: nextValue }, { nextSource: null })
  }

  useEffect(() => {
    const digits = cepDigits(address.cep)
    if (digits.length !== 8) return undefined

    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => {
      setCepStatus('loading')
      lookupBrazilianCep(digits, controller.signal)
        .then((result) => {
          if (!result) {
            setCepStatus('not-found')
            setCepMessage('CEP não encontrado. Verifique os 8 dígitos.')
            setSource(null)
            invalidateResolution('CEP não encontrado.')
            return
          }

          setCepStatus('resolved')
          setCepMessage('')
          setSource('cep')
          patchAddress({
            cep: formatBrazilianCep(result.cep),
            rua: result.rua,
            cidade: result.cidade,
            estado: result.estado,
            pais: 'Brasil',
          }, { invalidate: true, nextSource: 'cep' })
        })
        .catch((error) => {
          if (error.name === 'AbortError') return
          setCepStatus('error')
          setCepMessage('Não foi possível consultar o CEP agora.')
          setSource(null)
          invalidateResolution('Consulta de CEP indisponível.')
        })
    }, 300)

    return () => {
      window.clearTimeout(timeoutId)
      controller.abort()
    }
  }, [address.cep])

  useEffect(() => {
    const query = address.rua?.trim() || ''

    if (query.length < 3 || source === 'cep') {
      setSuggestions([])
      setStreetStatus('idle')
      return undefined
    }

    const controller = new AbortController()
    const currentRequest = requestId.current + 1
    requestId.current = currentRequest

    const timeoutId = window.setTimeout(() => {
      setStreetStatus('loading')
      searchLocations(query, controller.signal, buildStructuredAddress({
        ...address,
        cep: cepDigits(address.cep),
      }))
        .then((results) => {
          if (requestId.current !== currentRequest) return
          setSuggestions(results)
          setStreetStatus(results.length ? 'ready' : 'empty')
        })
        .catch((error) => {
          if (error.name !== 'AbortError' && requestId.current === currentRequest) {
            setStreetStatus('error')
          }
        })
    }, 450)

    return () => {
      window.clearTimeout(timeoutId)
      controller.abort()
    }
  }, [address.rua, address.cidade, address.estado, address.cep, source])

  useEffect(() => {
    const normalized = buildStructuredAddress({
      ...address,
      cep: cepDigits(address.cep),
    })

    if (!hasAddressForMap(normalized, source)) {
      setCoordinates(null)
      setMapStatus('idle')
      notifyResolution({
        source,
        message: source ? 'Informe CEP, rua, número, cidade e UF para ver o mapa.' : 'Use um CEP válido ou selecione uma sugestão de endereço.',
      })
      return undefined
    }

    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => {
      setMapStatus('loading')
      resolveEventLocation({ addressFields: normalized, address: formatStructuredAddress(normalized) }, controller.signal)
        .then((result) => {
          if (!result) {
            setCoordinates(null)
            setMapStatus('error')
            notifyResolution({ source, message: 'Não foi possível localizar esse endereço no mapa.' })
            return
          }

          const nextCoordinates = {
            latitude: result.latitude,
            longitude: result.longitude,
          }
          setCoordinates(nextCoordinates)
          setMapStatus('ready')
          notifyResolution({
            verified: true,
            source,
            coordinates: nextCoordinates,
            message: '',
          })
        })
        .catch((error) => {
          if (error.name === 'AbortError') return
          setCoordinates(null)
          setMapStatus('error')
          notifyResolution({ source, message: 'Não foi possível confirmar o endereço no mapa.' })
        })
    }, 700)

    return () => {
      window.clearTimeout(timeoutId)
      controller.abort()
    }
  }, [address.cep, address.rua, address.numero, address.cidade, address.estado, address.pais, source])

  const selectSuggestion = (suggestion) => {
    const fields = buildStructuredAddress({
      ...suggestion.addressFields,
      numero: address.numero || suggestion.addressFields?.numero || '',
      pais: 'Brasil',
    })
    setSuggestions([])
    setStreetStatus('idle')
    setSource('suggestion')
    patchAddress({
      cep: fields.cep ? formatBrazilianCep(fields.cep) : address.cep,
      rua: fields.rua,
      numero: fields.numero || address.numero,
      cidade: fields.cidade,
      estado: fields.estado,
      pais: 'Brasil',
    }, { invalidate: true, nextSource: 'suggestion' })
  }

  const mapEvent = coordinates && {
    id: 'address-preview',
    title: 'Prévia do endereço',
    locationName: formatStructuredAddress(buildStructuredAddress({
      ...address,
      cep: cepDigits(address.cep),
    })),
    addressFields: buildStructuredAddress({
      ...address,
      cep: cepDigits(address.cep),
    }),
    address: formatStructuredAddress(buildStructuredAddress({
      ...address,
      cep: cepDigits(address.cep),
    })),
    latitude: coordinates.latitude,
    longitude: coordinates.longitude,
  }
  const fieldErrorId = (field) => `${legend.toLowerCase().replace(/\W+/g, '-')}-${field}-error`
  const cepError = errors.cep || cepMessage
  const streetError = streetStatus === 'error' ? 'Não foi possível buscar sugestões agora.' : errors.rua

  return (
    <fieldset className={styles.fieldset}>
      <legend>{legend}</legend>
      <div className={styles.grid}>
        <label>
          CEP
          <input
            type="text"
            inputMode="numeric"
            autoComplete="postal-code"
            value={address.cep}
            maxLength={9}
            placeholder="00000-000"
            aria-invalid={Boolean(cepError)}
            aria-describedby={cepError ? fieldErrorId('cep') : undefined}
            onChange={(event) => updateCep(event.target.value)}
          />
          {cepStatus === 'loading' && <small>Consultando CEP...</small>}
          {cepStatus === 'resolved' && <small>CEP confirmado.</small>}
          <FieldError id={fieldErrorId('cep')} message={cepError} />
        </label>
        <label>
          Número
          <input
            type="text"
            inputMode="numeric"
            autoComplete="address-line2"
            value={address.numero}
            maxLength={9}
            placeholder="123"
            aria-invalid={Boolean(errors.numero)}
            aria-describedby={errors.numero ? fieldErrorId('numero') : undefined}
            onChange={(event) => updateAddressField('numero', event.target.value)}
          />
          <FieldError id={fieldErrorId('numero')} message={errors.numero} />
        </label>
      </div>

      <label className={styles.streetField}>
        Rua, avenida ou travessa
        <input
          type="search"
          autoComplete="off"
          value={address.rua}
          maxLength={100}
          placeholder="Ex: Paulista"
          aria-invalid={Boolean(streetError)}
          aria-describedby={streetError ? fieldErrorId('rua') : undefined}
          onChange={(event) => updateAddressField('rua', event.target.value)}
        />
        {streetStatus === 'loading' && <small>Buscando sugestões...</small>}
        {streetStatus === 'empty' && <small>Nenhuma sugestão encontrada. Tente incluir bairro ou cidade.</small>}
        <FieldError id={fieldErrorId('rua')} message={streetError} />
        {suggestions.length > 0 && (
          <div className={styles.suggestions} role="listbox" aria-label="Sugestões de endereço">
            {suggestions.map((suggestion) => (
              <button type="button" key={suggestion.id} onClick={() => selectSuggestion(suggestion)}>
                <strong>{suggestion.venue}</strong>
                <span>{formatSuggestionLabel(suggestion)}</span>
              </button>
            ))}
          </div>
        )}
      </label>

      <div className={styles.grid}>
        <label>
          Cidade
          <input
            type="text"
            autoComplete="address-level2"
            value={address.cidade}
            maxLength={40}
            placeholder="São Paulo"
            aria-invalid={Boolean(errors.cidade)}
            aria-describedby={errors.cidade ? fieldErrorId('cidade') : undefined}
            onChange={(event) => updateAddressField('cidade', event.target.value)}
          />
          <FieldError id={fieldErrorId('cidade')} message={errors.cidade} />
        </label>
        <label>
          Estado
          <select
            value={address.estado}
            aria-invalid={Boolean(errors.estado)}
            aria-describedby={errors.estado ? fieldErrorId('estado') : undefined}
            onChange={(event) => updateAddressField('estado', event.target.value)}
          >
            <option value="">Selecione a UF</option>
            {brazilStates.map((state) => (
              <option key={state.uf} value={state.uf}>{state.uf} - {state.name}</option>
            ))}
          </select>
          <FieldError id={fieldErrorId('estado')} message={errors.estado} />
        </label>
      </div>

      <label>
        País
        <input
          type="text"
          value="Brasil"
          readOnly
          aria-invalid={Boolean(errors.pais)}
          aria-describedby={errors.pais ? fieldErrorId('pais') : undefined}
        />
        <FieldError id={fieldErrorId('pais')} message={errors.pais} />
      </label>

      <div className={styles.mapPreview}>
        <div className={styles.mapHeader}>
          <strong>Prévia do mapa</strong>
          {mapStatus === 'loading' && <span>Localizando endereço...</span>}
          {mapStatus === 'ready' && <span>Endereço confirmado no mapa.</span>}
          {mapStatus === 'error' && <span>Não foi possível confirmar este endereço.</span>}
          {mapStatus === 'idle' && <span>Complete o endereço para ver a prévia.</span>}
        </div>
        {mapEvent ? <EventMap event={mapEvent} /> : null}
      </div>

      <FieldError id={fieldErrorId('location')} message={errors.location} />
    </fieldset>
  )
}

export default StructuredAddressFields
