export const clampRating = (value) => Math.min(5, Math.max(0, Math.round(value * 2) / 2))

export const clampSelectedRating = (value) => Math.min(5, Math.max(0.5, Math.round(value * 2) / 2))

export const formatRatingValue = (value) => Number(value || 0).toLocaleString('pt-BR', {
  minimumFractionDigits: Number.isInteger(Number(value || 0)) ? 0 : 1,
  maximumFractionDigits: 1,
})
