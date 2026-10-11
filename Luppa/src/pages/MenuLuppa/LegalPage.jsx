import LegalDocumentLayout from './LegalDocumentLayout'
import { legalDocuments } from './legalDocuments'

const LegalPage = ({ type }) => (
  <LegalDocumentLayout document={legalDocuments[type]} type={type} />
)

export default LegalPage
