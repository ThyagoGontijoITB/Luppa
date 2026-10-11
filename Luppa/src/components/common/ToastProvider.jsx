import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'
import Toast from './Toast'

const ToastContext = createContext(null)

const TOAST_DURATION = 3600
const EXIT_DURATION = 180

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([])
  const timersRef = useRef(new Map())

  const removeToast = useCallback((id) => {
    window.clearTimeout(timersRef.current.get(id))
    timersRef.current.delete(id)
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const dismissToast = useCallback((id) => {
    window.clearTimeout(timersRef.current.get(id))
    timersRef.current.delete(id)
    setToasts((current) => current.map((toast) => (
      toast.id === id ? { ...toast, exiting: true } : toast
    )))
    window.setTimeout(() => removeToast(id), EXIT_DURATION)
  }, [removeToast])

  const showToast = useCallback((message, tone = 'success') => {
    if (!message) return
    const key = `${tone}:${message}`
    const id = `${key}:${Date.now()}`

    setToasts((current) => {
      if (current.some((toast) => toast.key === key)) return current
      return [...current.slice(-2), { id, key, message, tone }]
    })

    const timer = window.setTimeout(() => dismissToast(id), TOAST_DURATION)
    timersRef.current.set(id, timer)
  }, [dismissToast])

  const value = useMemo(() => ({
    showToast,
    success: (message) => showToast(message, 'success'),
    error: (message) => showToast(message, 'error'),
    warning: (message) => showToast(message, 'warning'),
    info: (message) => showToast(message, 'info'),
  }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <Toast toasts={toasts} onClose={dismissToast} />
    </ToastContext.Provider>
  )
}

export const useToast = () => {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used inside ToastProvider')
  }
  return context
}
