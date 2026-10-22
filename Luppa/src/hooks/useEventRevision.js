import { useSyncExternalStore } from 'react'
import { getEventRevision, subscribeEventChanges } from '../services/eventChanges'
export default function useEventRevision() {
  return useSyncExternalStore(subscribeEventChanges, getEventRevision)
}
