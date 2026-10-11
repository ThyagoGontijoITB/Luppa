import FieldError from './FieldError'
import styles from './CoverImageUpload.module.css'

const CoverImageUpload = ({ file, preview, error, onChange, onRemove }) => {
  const errorId = 'cover-image-error'

  const handleChange = (event) => {
    const selectedFile = event.target.files?.[0] || null
    onChange(selectedFile)
    event.target.value = ''
  }

  return (
    <div className={styles.field}>
      <div className={styles.labelRow}>
        <span>Imagem de capa</span>
        <small>JPEG, PNG ou WebP até 2 MB</small>
      </div>

      {preview ? (
        <div className={styles.preview}>
          <img src={preview} alt="Prévia da imagem de capa" />
          <div className={styles.actions}>
            <label className={styles.replace}>
              Trocar imagem
              <input className={styles.fileInput} type="file" accept="image/jpeg,image/png,image/webp" onChange={handleChange} />
            </label>
            <button type="button" onClick={onRemove}>Remover</button>
          </div>
          {file && <p>{file.name}</p>}
        </div>
      ) : (
        <label className={`${styles.dropzone} ${error ? styles.invalid : ''}`} data-invalid={error ? 'true' : undefined}>
          <input className={styles.fileInput} type="file" accept="image/jpeg,image/png,image/webp" onChange={handleChange} />
          <strong>Selecionar imagem</strong>
          <span>A prévia aparece aqui após a seleção.</span>
        </label>
      )}

      <FieldError id={errorId} message={error} />
    </div>
  )
}

export default CoverImageUpload
