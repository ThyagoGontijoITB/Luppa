import logo from '../../assets/luppa-wordmark-banner.png'
import styles from './Logo.module.css'

const Logo = ({ variant = 'dark', className = '' }) => (
  <img
    className={`${styles.logo} ${styles[variant]} ${className}`}
    src={logo}
    alt="Luppa"
  />
)

export default Logo
