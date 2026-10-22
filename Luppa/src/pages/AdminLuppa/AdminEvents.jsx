import { useOutletContext } from 'react-router-dom'
import EventManager from '../../components/events/EventManager'

export default function AdminEvents() {
  return <EventManager {...useOutletContext()} administrative />
}
