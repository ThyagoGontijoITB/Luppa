import { useEffect, useMemo, useRef, useState } from 'react'
import { Map, Marker, NavigationControl } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { mapConfig } from '../../config/mapConfig'
import { resolveEventLocation } from '../../services/eventLocationService'
import styles from './EventMap.module.css'

const hasCoordinates = (event) => Number.isFinite(event.latitude) && Number.isFinite(event.longitude)

const getExternalMapUrl = (event, coordinates) => {
  const query = encodeURIComponent(event.address || event.locationName || (coordinates ? `${coordinates.latitude},${coordinates.longitude}` : ''))
  const hash = coordinates ? `#map=16/${coordinates.latitude}/${coordinates.longitude}` : ''
  return `https://www.openstreetmap.org/search?query=${query}${hash}`
}

const getDirectionsUrl = (event) => {
  const destination = encodeURIComponent(event.address || event.locationName)
  return `https://www.google.com/maps/dir/?api=1&destination=${destination}`
}

const createMarkerElement = () => {
  const marker = document.createElement('div')
  marker.className = styles.marker
  marker.setAttribute('aria-hidden', 'true')
  return marker
}

const EventMap = ({ event }) => {
  const mapContainerRef = useRef(null)
  const mapRef = useRef(null)
  const [status, setStatus] = useState('idle')
  const [resolvedCoordinates, setResolvedCoordinates] = useState(null)
  const eventCoordinates = useMemo(() => (hasCoordinates(event)
    ? { latitude: event.latitude, longitude: event.longitude }
    : null), [event.latitude, event.longitude])
  const coordinates = eventCoordinates || resolvedCoordinates
  const latitude = coordinates?.latitude
  const longitude = coordinates?.longitude
  const mapUnavailable = status !== 'loading' && (!coordinates || status === 'error')

  useEffect(() => {
    if (eventCoordinates) {
      setResolvedCoordinates(null)
      return undefined
    }

    const controller = new AbortController()
    setStatus('loading')
    resolveEventLocation(event, controller.signal)
      .then((location) => {
        setResolvedCoordinates(location ? { latitude: location.latitude, longitude: location.longitude } : null)
        if (!location) setStatus('idle')
      })
      .catch(() => {
        if (!controller.signal.aborted) setStatus('error')
      })

    return () => controller.abort()
  }, [event, eventCoordinates])

  useEffect(() => {
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude) || !mapContainerRef.current) {
      setStatus('idle')
      return undefined
    }

    let disposed = false
    let mapReady = false
    const animationFrames = []
    setStatus('loading')

    let map
    let marker

    const markReady = () => {
      if (disposed) return
      mapReady = true
      setStatus('ready')
      animationFrames.push(window.requestAnimationFrame(() => {
        if (!disposed) map?.resize()
      }))
    }

    const handleError = (mapError) => {
      if (import.meta.env.DEV) {
        console.warn('MapLibre error', mapError.error || mapError)
      }

      // MapLibre emits recoverable resource errors while a map may still render.
      // Fatal initialization failure is handled by the constructor catch and watchdog below.
    }

    try {
      map = new Map({
        attributionControl: mapConfig.attributionControl,
        center: [longitude, latitude],
        container: mapContainerRef.current,
        doubleClickZoom: true,
        dragRotate: false,
        keyboard: true,
        maxZoom: mapConfig.maxZoom,
        minZoom: mapConfig.minZoom,
        pitchWithRotate: false,
        scrollZoom: false,
        style: mapConfig.styleUrl,
        touchPitch: false,
        zoom: mapConfig.zoom,
      })

      mapRef.current = map
      map.on('error', handleError)
      map.once('load', markReady)
      map.once('style.load', markReady)
      map.once('idle', markReady)

      marker = new Marker({
        anchor: 'center',
        element: createMarkerElement(),
      })
        .setLngLat([longitude, latitude])
        .addTo(map)

      const navigationControl = new NavigationControl({
        showCompass: false,
        showZoom: true,
        visualizePitch: false,
      })

      map.addControl(navigationControl, 'top-right')
    } catch (mapError) {
      if (import.meta.env.DEV) {
        console.warn('MapLibre initialization failed', mapError)
      }
      window.queueMicrotask(() => {
        if (!disposed) setStatus('error')
      })
      return undefined
    }

    animationFrames.push(window.requestAnimationFrame(() => map.resize()))
    animationFrames.push(window.requestAnimationFrame(() => {
      animationFrames.push(window.requestAnimationFrame(() => map.resize()))
    }))

    const initializationWatchdog = window.setTimeout(() => {
      if (!disposed && !mapReady) {
        setStatus('error')
      }
    }, 8000)

    return () => {
      disposed = true
      window.clearTimeout(initializationWatchdog)
      animationFrames.forEach((frameId) => window.cancelAnimationFrame(frameId))
      marker?.remove()
      map?.off('error', handleError)
      map?.remove()
      mapRef.current = null
    }
  }, [latitude, longitude])

  return (
    <div className={styles.mapShell} aria-label={`Mapa de ${event.locationName || event.title}`}>
      <div className={styles.mapCanvas}>
        {coordinates && (
          <div
            className={styles.map}
            ref={mapContainerRef}
            role="application"
            aria-label={`Mapa mostrando ${event.locationName || event.address || event.title}`}
          />
        )}
        {status === 'loading' && (
          <div className={styles.loading} aria-live="polite">
            Carregando mapa...
          </div>
        )}
        {mapUnavailable && (
          <div className={styles.message} role="status">
            {coordinates ? 'Não foi possível carregar o mapa.' : 'Mapa indisponível para este evento sem coordenadas confirmadas.'}
          </div>
        )}
      </div>
      <div className={styles.mapActions}>
        <a href={getExternalMapUrl(event, coordinates)} target="_blank" rel="noreferrer">Abrir no mapa</a>
        <a href={getDirectionsUrl(event)} target="_blank" rel="noreferrer">Traçar rota</a>
      </div>
    </div>
  )
}

export default EventMap
