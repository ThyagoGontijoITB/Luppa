import { useState } from 'react'
import { getEventById } from '../services/eventService'

const useEventDetails = () => {
  const [selectedEvent, setSelectedEvent] = useState(null)

  const openEventDetails = async (event) => {
    if (!event) return
    setSelectedEvent(event)
    setSelectedEvent(await getEventById(event.id))
  }

  const closeEventDetails = () => setSelectedEvent(null)

  const updateSelectedEvent = async (event) => {
    setSelectedEvent(event?.id ? await getEventById(event.id) : null)
  }

  return {
    selectedEvent,
    openEventDetails,
    closeEventDetails,
    updateSelectedEvent,
  }
}

export default useEventDetails
