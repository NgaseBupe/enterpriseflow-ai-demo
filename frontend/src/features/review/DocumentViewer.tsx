import type { DocumentSummary } from '../../api/documents'
import styles from './DocumentViewer.module.css'

export function contentUrl(documentId: string) {
  return `/api/documents/${encodeURIComponent(documentId)}/content`
}

/** Shows the original file using the browser's own PDF and image viewers. */
export function DocumentViewer({ document }: { document: DocumentSummary }) {
  const url = contentUrl(document.id)
  const label = `Original document: ${document.originalFileName}`

  return (
    <div className={styles.viewer}>
      {document.contentType === 'application/pdf' ? (
        <iframe src={url} title={label} className={styles.frame} />
      ) : (
        <img src={url} alt={label} className={styles.image} />
      )}
      <a href={url} target="_blank" rel="noopener noreferrer" className={styles.openLink}>
        Open in a new tab
      </a>
    </div>
  )
}
