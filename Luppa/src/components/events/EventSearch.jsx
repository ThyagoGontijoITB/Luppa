import { useEffect, useState } from 'react'
import { getCategories } from '../../services/eventService'
import { SearchIcon } from '../common/Icons'
import styles from './EventSearch.module.css'

const sortOptions = [
  { value: 'nearest', label: 'Data mais próxima' },
  { value: 'furthest', label: 'Data mais distante' },
  { value: 'az', label: 'A-Z' },
  { value: 'za', label: 'Z-A' },
]

const EventSearch = ({ filters, onChange }) => {
  const [categories, setCategories] = useState([])

  useEffect(() => {
    let active = true
    getCategories()
      .then((items) => {
        if (active) setCategories(items)
      })
      .catch(() => {
        if (active) setCategories([])
      })
    return () => {
      active = false
    }
  }, [])

  const updateFilter = (field, value) => {
    onChange({ ...filters, [field]: value })
  }

  const resetFilters = () => {
    onChange({ query: '', category: 'all', sort: 'nearest' })
  }

  return (
    <section className={styles.searchPanel} aria-label="Busca de eventos">
      <label className={styles.searchBox}>
        <SearchIcon className={styles.searchIcon} />
        <span className={styles.visuallyHidden}>Buscar eventos</span>
        <input
          type="search"
          value={filters.query}
          placeholder="Buscar por evento, local ou categoria"
          onChange={(event) => updateFilter('query', event.target.value)}
        />
      </label>
      <label>
        <span>Categoria</span>
        <select value={filters.category} onChange={(event) => updateFilter('category', event.target.value)}>
          <option value="all">Todas</option>
          {categories.filter((category) => category.id !== 'all').map((category) => (
            <option key={category.slug} value={category.slug}>{category.title}</option>
          ))}
        </select>
      </label>
      <label>
        <span>Ordenar</span>
        <select value={filters.sort} onChange={(event) => updateFilter('sort', event.target.value)}>
          {sortOptions.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </label>
      <button className={styles.resetButton} type="button" onClick={resetFilters}>
        Limpar filtros
      </button>
    </section>
  )
}

export default EventSearch
