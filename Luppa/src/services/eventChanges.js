let revision = 0
const listeners = new Set()
export const subscribeEventChanges = (listener) => {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
export const getEventRevision = () => revision
export const notifyEventChanges = () => {
  revision += 1
  listeners.forEach(listener => listener())
}
