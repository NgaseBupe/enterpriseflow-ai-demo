import type { DocumentStatus } from '../api/documents'
import styles from './StatusBadge.module.css'

const LABELS: Record<DocumentStatus, string> = {
  UPLOADED: 'Uploaded',
  PROCESSING: 'Processing',
  EXTRACTED: 'Ready for review',
  EXTRACTION_FAILED: 'Extraction failed',
  IN_REVIEW: 'In review',
  CONFIRMED: 'Confirmed',
}

export function StatusBadge({ status }: { status: DocumentStatus }) {
  return (
    <span className={styles.badge} data-status={status}>
      {LABELS[status]}
    </span>
  )
}
