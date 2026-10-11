const acceptedTypes = ['image/jpeg', 'image/png', 'image/webp']
const maxSize = 2 * 1024 * 1024

export const validateCoverImage = (file) => {
  if (!file) return ''
  if (!acceptedTypes.includes(file.type)) return 'Use uma imagem JPEG, PNG ou WebP.'
  if (file.size > maxSize) return 'A imagem deve ter no máximo 2 MB.'
  return ''
}
