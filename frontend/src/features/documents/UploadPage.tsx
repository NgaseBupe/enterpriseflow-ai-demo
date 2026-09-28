import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { errorMessage } from '../../api/client'
import { useUploadDocument } from '../../api/documents'
import { formatFileSize } from '../../lib/format'
import type { DocumentsLocationState } from './DocumentsPage'
import styles from './UploadPage.module.css'

const ACCEPTED_TYPES = ['application/pdf', 'image/png', 'image/jpeg']
const ACCEPTED_EXTENSIONS = ['pdf', 'png', 'jpg', 'jpeg']
export const MAX_FILE_SIZE = 10 * 1024 * 1024

/** Quick feedback before uploading. The server still checks the real file type and size. */
function validate(file: File): string | null {
  if (file.size === 0) {
    return 'The selected file is empty.'
  }
  if (!isAcceptedType(file)) {
    return 'Only PDF, PNG and JPEG files are accepted.'
  }
  if (file.size > MAX_FILE_SIZE) {
    return 'The file exceeds the maximum size of 10 MB.'
  }
  return null
}

function isAcceptedType(file: File): boolean {
  if (file.type) {
    return ACCEPTED_TYPES.includes(file.type)
  }
  // Some systems don't report a type; fall back to the extension. The server checks the real content.
  const extension = file.name.split('.').pop()?.toLowerCase() ?? ''
  return ACCEPTED_EXTENSIONS.includes(extension)
}

export function UploadPage() {
  const [file, setFile] = useState<File | null>(null)
  const [validationError, setValidationError] = useState<string | null>(null)
  const upload = useUploadDocument()
  const navigate = useNavigate()

  function selectFile(selected: File | null) {
    upload.reset()
    setFile(selected)
    setValidationError(selected ? validate(selected) : null)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!file || validationError) {
      return
    }
    upload.mutate(file, {
      onSuccess: (document) => {
        const state: DocumentsLocationState = { uploadedFileName: document.originalFileName }
        navigate('/documents', { state })
      },
    })
  }

  const error = validationError ?? (upload.isError ? errorMessage(upload.error) : null)

  return (
    <section className={styles.page}>
      <h1>Upload a purchase order</h1>
      <p className={styles.muted}>PDF, PNG or JPEG, up to 10 MB. Use synthetic documents only.</p>

      <form onSubmit={handleSubmit} className={styles.form}>
        <label
          className={styles.dropZone}
          onDragOver={(event) => event.preventDefault()}
          onDrop={(event) => {
            event.preventDefault()
            selectFile(event.dataTransfer.files[0] ?? null)
          }}
        >
          <span className={styles.dropZoneText}>
            {file ? (
              <>
                <strong>{file.name}</strong> ({formatFileSize(file.size)})
              </>
            ) : (
              'Drag a file here, or click to choose one'
            )}
          </span>
          <input
            type="file"
            accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
            aria-label="Purchase order file"
            className={styles.fileInput}
            onChange={(event) => selectFile(event.target.files?.[0] ?? null)}
          />
        </label>

        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}

        <div className={styles.actions}>
          <button type="submit" className={styles.primaryButton} disabled={!file || !!validationError || upload.isPending}>
            {upload.isPending ? 'Uploading…' : 'Upload'}
          </button>
          <Link to="/documents">Cancel</Link>
        </div>
      </form>
    </section>
  )
}
