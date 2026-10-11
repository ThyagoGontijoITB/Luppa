const invalidSelector = [
  'input[aria-invalid="true"]',
  'select[aria-invalid="true"]',
  'textarea[aria-invalid="true"]',
  '[data-invalid="true"] input',
  '[data-invalid="true"] select',
  '[data-invalid="true"] textarea',
].join(',')

export const focusFirstInvalidField = (scope = document) => {
  const field = scope?.querySelector?.(invalidSelector)
  if (!field) return

  field.scrollIntoView({ behavior: 'smooth', block: 'center' })
  window.setTimeout(() => field.focus?.({ preventScroll: true }), 220)
}
