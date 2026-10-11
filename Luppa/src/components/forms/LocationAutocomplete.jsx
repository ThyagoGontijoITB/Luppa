import { useEffect, useRef, useState } from 'react'
import { searchLocations } from '../../services/eventLocationService'
import styles from './LocationAutocomplete.module.css'

const LocationAutocomplete = ({ value, selectedLocation, error, onQueryChange, onSelect }) => {
  const [suggestions, setSuggestions] = useState([])
  const [status, setStatus] = useState('idle')
  const requestId = useRef(0)

  useEffect(() => {
    const query = value.trim()

    if (query.length < 3) {
      return undefined
    }

    const controller = new AbortController()
    const currentRequest = requestId.current + 1
    requestId.current = currentRequest

    const timeoutId = window.setTimeout(() => {
      setStatus('loading')
      searchLocations(query, controller.signal)
        .then((results) => {
          if (requestId.current !== currentRequest) return
          setSuggestions(results)
          setStatus(results.length ? 'ready' : 'empty')
        })
        .catch((locationError) => {
          if (locationError.name !== 'AbortError' && requestId.current === currentRequest) {
            setStatus('error')
          }
        })
    }, 450)

    return () => {
      window.clearTimeout(timeoutId)
      controller.abort()
    }
  }, [value])

  const query = value.trim()
  const visibleSuggestions = query.length >= 3 ? suggestions : []
  const visibleStatus = query.length < 3 ? (query ? 'too-short' : 'idle') : status

  return (
    <div className={styles.field}>
      <label>
        <span>Local do evento</span>
        <input
          type="search"
          value={value}
          placeholder="Ex: Parque Municipal Dom José"
          autoComplete="off"
          onChange={(event) => onQueryChange(event.target.value)}
        />
      </label>
      {visibleStatus === 'loading' && <p className={styles.state}>Buscando locais...</p>}
      {visibleStatus === 'empty' && <p className={styles.state}>Nenhum local encontrado para essa busca.</p>}
      {visibleStatus === 'error' && <p className={styles.error}>Não foi possível buscar locais agora.</p>}
      {visibleSuggestions.length > 0 && (
        <div className={styles.suggestions} role="listbox" aria-label="Sugestões de local">
          {visibleSuggestions.map((suggestion) => (
            <button type="button" key={suggestion.id} onClick={() => onSelect(suggestion)}>
              <strong>{suggestion.venue}</strong>
              <span>{suggestion.formattedAddress}</span>
              {suggestion.cityRegion && <small>{suggestion.cityRegion}</small>}
            </button>
          ))}
        </div>
      )}
      {selectedLocation?.formattedAddress && (
        <div className={styles.selected}>
          <span>Endereço</span>
          <strong>{selectedLocation.venue}</strong>
          <p>{selectedLocation.formattedAddress}</p>
        </div>
      )}
      {error && <span className={styles.error}>{error}</span>}
    </div>
  )
}

export default LocationAutocomplete
