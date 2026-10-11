import { useState } from 'react'
import { EyeIcon, EyeOffIcon } from '../common/Icons'
import styles from './PasswordInput.module.css'

const PasswordInput = ({ value, placeholder, onChange, autoComplete, minLength, maxLength, ...inputProps }) => {
  const [visible, setVisible] = useState(false)

  return (
    <div className={styles.passwordField}>
      <input
        {...inputProps}
        autoComplete={autoComplete}
        type={visible ? 'text' : 'password'}
        value={value}
        placeholder={placeholder}
        minLength={minLength}
        maxLength={maxLength}
        onChange={onChange}
      />
      <button
        aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'}
        className={styles.toggle}
        type="button"
        onMouseDown={(event) => event.preventDefault()}
        onClick={() => setVisible((current) => !current)}
      >
        {visible ? <EyeOffIcon /> : <EyeIcon />}
      </button>
    </div>
  )
}

export default PasswordInput
