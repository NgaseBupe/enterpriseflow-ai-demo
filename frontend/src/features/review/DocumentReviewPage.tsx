import { Link, useParams } from 'react-router'
import { errorMessage } from '../../api/client'
import type { DocumentStatus } from '../../api/documents'
import { useDocument } from '../../api/documents'
import { useExtractDocument, useExtraction } from '../../api/extraction'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatFileSize } from '../../lib/format'
import styles from './DocumentDetailsPage.module.css'
import { ExtractedOrder } from './ExtractedOrder'

const HAS_EXTRACTION: DocumentStatus[] = ['EXTRACTED', 'IN_REVIEW', 'CONFIRMED']

export function DocumentDetailsPage() {
  const { id = '' } = useParams()
  const { data, isPending, isError, error } = useDocument(id)
  const hasExtraction = !!data && HAS_EXTRACTION.includes(data.status)
  const extraction = useExtraction(id, hasExtraction)
  const extract = useExtractDocument(id)

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

          {data.status === 'EXTRACTION_FAILED' && (
            <p className={styles.error} role="alert">
              Extraction failed: {data.failureReason ?? 'unknown reason.'}
            </p>
          )}

          {extract.isError && (
            <p className={styles.error} role="alert">
              {errorMessage(extract.error)}
            </p>
          )}

          {(data.status === 'UPLOADED' || data.status === 'EXTRACTION_FAILED') && (
            <button
              type="button"
              className={styles.primaryButton}
              onClick={() => extract.mutate()}
              disabled={extract.isPending}
            >
              {extract.isPending
                ? 'Extracting…'
                : data.status === 'EXTRACTION_FAILED'
                  ? 'Retry extraction'
                  : 'Extract data'}
            </button>
          )}

          {data.status === 'PROCESSING' && <p role="status">Extraction in progress…</p>}

          {hasExtraction && extraction.isPending && <p role="status">Loading extracted data…</p>}
          {hasExtraction && extraction.isError && (
            <p className={styles.error} role="alert">
              {errorMessage(extraction.error)}
            </p>
          )}
          {extraction.data && <ExtractedOrder extraction={extraction.data} />}
        </>
      )}
    </section>
  )
}
