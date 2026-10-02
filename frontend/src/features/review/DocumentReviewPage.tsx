import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { errorMessage } from '../../api/client'
import type { DocumentStatus } from '../../api/documents'
import { useDocument } from '../../api/documents'
import { useConfirmExtraction, useExtractDocument, useExtraction } from '../../api/extraction'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatFileSize } from '../../lib/format'
import { DocumentViewer } from './DocumentViewer'
import styles from './DocumentReviewPage.module.css'
import { ExtractedOrder } from './ExtractedOrder'
import { ExtractionForm } from './ExtractionForm'

const HAS_EXTRACTION: DocumentStatus[] = ['EXTRACTED', 'IN_REVIEW', 'CONFIRMED']
const EDITABLE: DocumentStatus[] = ['EXTRACTED', 'IN_REVIEW']

const TYPE_LABELS: Record<string, string> = {
  'application/pdf': 'PDF',
  'image/png': 'PNG image',
  'image/jpeg': 'JPEG image',
}

/** The original document on one side, the extracted data on the other. */
export function DocumentReviewPage() {
  const { id = '' } = useParams()
  const { data, isPending, isError, error } = useDocument(id)
  const hasExtraction = !!data && HAS_EXTRACTION.includes(data.status)
  const extraction = useExtraction(id, hasExtraction)
  const extract = useExtractDocument(id)
  const [editing, setEditing] = useState(false)
  const [saved, setSaved] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [confirmedNow, setConfirmedNow] = useState(false)
  const confirm = useConfirmExtraction(id)
  const canEdit = !!data && EDITABLE.includes(data.status)

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
          <div className={styles.titleRow}>
            <h1 className={styles.title}>{data.originalFileName}</h1>
            <StatusBadge status={data.status} />
          </div>
          <p className={styles.meta}>
            Uploaded {formatDateTime(data.uploadedAt)} · {TYPE_LABELS[data.contentType] ?? data.contentType} ·{' '}
            {formatFileSize(data.fileSize)}
          </p>

          <div className={styles.layout}>
            <section aria-label="Original document" className={styles.documentPane}>
              <DocumentViewer document={data} />
            </section>

            <section aria-label="Extracted data" className={styles.dataPane}>
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
                <div className={styles.callToAction}>
                  {data.status === 'UPLOADED' && <p>This document has not been extracted yet.</p>}
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
                </div>
              )}

              {data.status === 'PROCESSING' && <p role="status">Extraction in progress…</p>}

              {hasExtraction && extraction.isPending && <p role="status">Loading extracted data…</p>}
              {hasExtraction && extraction.isError && (
                <p className={styles.error} role="alert">
                  {errorMessage(extraction.error)}
                </p>
              )}
              {confirmedNow && (
                <p className={styles.success} role="status">
                  Extraction confirmed.
                </p>
              )}
              {saved && !editing && !confirmedNow && (
                <p className={styles.success} role="status">
                  Changes saved.
                </p>
              )}
              {extraction.data && editing && (
                <ExtractionForm
                  extraction={extraction.data}
                  onCancel={() => setEditing(false)}
                  onSaved={() => {
                    setEditing(false)
                    setSaved(true)
                  }}
                />
              )}
              {extraction.data && !editing && (
                <ExtractedOrder
                  extraction={extraction.data}
                  onEdit={
                    canEdit
                      ? () => {
                          setSaved(false)
                          setEditing(true)
                        }
                      : undefined
                  }
                  onConfirm={
                    canEdit
                      ? () => {
                          confirm.reset()
                          setConfirming(true)
                        }
                      : undefined
                  }
                />
              )}
              {confirming && extraction.data && (
                <ConfirmDialog
                  title="Confirm the extracted data?"
                  confirmLabel="Confirm"
                  busy={confirm.isPending}
                  error={confirm.isError ? errorMessage(confirm.error) : null}
                  onCancel={() => setConfirming(false)}
                  onConfirm={() =>
                    confirm.mutate(extraction.data.version, {
                      onSuccess: () => {
                        setConfirming(false)
                        setSaved(false)
                        setConfirmedNow(true)
                      },
                    })
                  }
                >
                  <p>You are confirming that the extracted data matches the document.</p>
                  <p>After confirming, it can no longer be edited. This does not accept or reject the order.</p>
                </ConfirmDialog>
              )}
            </section>
          </div>
        </>
      )}
    </section>
  )
}
