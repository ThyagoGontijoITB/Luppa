import symbol from '../../assets/luppa-symbol.png'
import styles from './LuppaSymbol.module.css'

const LuppaSymbol = ({ className = '' }) => (
  <img className={`${styles.symbol} ${className}`} src={symbol} alt="" aria-hidden="true" />
)

export default LuppaSymbol
