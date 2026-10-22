import { useEffect, useState } from 'react'
import { useAuth } from '../auth/authContext'
import { getEventById } from '../services/eventService'
import { useToast } from '../components/common/ToastProvider'
import useEventRevision from './useEventRevision'

const useEventDetails = () => {
  const auth = useAuth()
  const scope = auth.userId || 'anonymous'
  const [selection, setSelection] = useState(null)
  // Personal saved/review fields must disappear immediately when the account changes.
  const selectedEvent = selection?.scope === scope ? selection.event : null
  const revision = useEventRevision()
  const toast = useToast()
  const selectedId = selectedEvent?.id

  useEffect(() => {
    if (!selectedId) return
    let active = true
    getEventById(selectedId).then(event => {
      if (active) setSelection({ event, scope })
    }).catch(() => {
      if (active) toast.error('Não foi possível atualizar os detalhes do evento.')
    })
    return () => { active = false }
  }, [selectedId, revision, toast, scope])

  return {
    selectedEvent,
    openEventDetails: (event) => setSelection({ event, scope }),
    closeEventDetails: () => setSelection(null),
    updateSelectedEvent: async (event) => {
      if (!event?.id) return
      const next = await getEventById(event.id)
      setSelection(current => current?.scope === scope && current.event?.id === event.id ? { event: next, scope } : current)
    },
  }
}

export default useEventDetails
