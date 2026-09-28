import { Link, useParams } from 'react-router'
import { errorMessage } from '../../api/client'
import { useDocument } from '../../api/documents'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatFileSize } from '../../lib/format'
import styles from './DocumentDetailsPage.module.css'

export function DocumentDetailsPage() {
  const { id = '' } = useParams()
  const { data, isPending, isError, error } = useDocument(id)

  return (
    <section>
      <Link to="/documents">← All documents</Link>

      {isPending && <p role="status">Loading document…</p>}

      {isError && (
        <p className={styles.error} role="alert">
          {errorMessage(error)}
        </p>
      )}

      {data && (
        <>
          <h1 className={styles.title}>{data.originalFileName}</h1>
          <dl className={styles.details}>
            <dt>Status</dt>
            <dd>
              <StatusBadge status={data.status} />
            </dd>
            <dt>Uploaded</dt>
            <dd>{formatDateTime(data.uploadedAt)}</dd>
            <dt>Type</dt>
            <dd>{data.contentType}</dd>
            <dt>Size</dt>
            <dd>{formatFileSize(data.fileSize)}</dd>
          </dl>
        </>
      )}
    </section>
  )
}
