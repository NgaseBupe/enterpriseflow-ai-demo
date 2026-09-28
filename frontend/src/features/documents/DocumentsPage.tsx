import { Link, useLocation, useSearchParams } from 'react-router'
import { errorMessage } from '../../api/client'
import { useDocuments } from '../../api/documents'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime } from '../../lib/format'
import styles from './DocumentsPage.module.css'

export interface DocumentsLocationState {
  uploadedFileName?: string
}

/** The address shows pages from 1 (?page=2); the API counts from 0. Invalid values mean the first page. */
function pageFromParams(params: URLSearchParams): number {
  const value = Number(params.get('page'))
  return Number.isInteger(value) && value > 1 ? value - 1 : 0
}

export function DocumentsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = pageFromParams(searchParams)
  const { data, isPending, isError, error, refetch, isPlaceholderData } = useDocuments(page)

  function goToPage(next: number) {
    setSearchParams(next === 0 ? {} : { page: String(next + 1) })
  }
  const uploadedFileName = (useLocation().state as DocumentsLocationState | null)?.uploadedFileName

  return (
    <section>
      <div className={styles.titleRow}>
        <h1>Documents</h1>
        <Link to="/documents/upload" className={styles.primaryButton}>
          Upload document
        </Link>
      </div>

      {uploadedFileName && (
        <p className={styles.success} role="status">
          {uploadedFileName} was uploaded.
        </p>
      )}

      {isPending && (
        <p className={styles.muted} role="status">
          Loading documents…
        </p>
      )}

      {isError && (
        <div className={styles.error} role="alert">
          <p>Documents could not be loaded. {errorMessage(error)}</p>
          <button type="button" onClick={() => refetch()}>
            Try again
          </button>
        </div>
      )}

      {data && data.totalItems === 0 && (
        <div className={styles.empty}>
          <p>No documents yet.</p>
          <Link to="/documents/upload">Upload your first purchase order</Link>
        </div>
      )}

      {data && data.items.length === 0 && data.totalItems > 0 && (
        <div className={styles.empty}>
          <p>There are no documents on this page.</p>
          <Link to="/documents">Go to the first page</Link>
        </div>
      )}

      {data && data.items.length > 0 && (
        <>
          <table className={styles.table}>
            <caption className={styles.visuallyHidden}>Uploaded documents, newest first</caption>
            <thead>
              <tr>
                <th scope="col">File name</th>
                <th scope="col">Status</th>
                <th scope="col">Uploaded</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((document) => (
                <tr key={document.id}>
                  <td>
                    <Link to={`/documents/${document.id}`}>{document.originalFileName}</Link>
                  </td>
                  <td>
                    <StatusBadge status={document.status} />
                  </td>
                  <td>{formatDateTime(document.uploadedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {data.totalPages > 1 && (
            <nav className={styles.pagination} aria-label="Pagination">
              <button type="button" onClick={() => goToPage(page - 1)} disabled={page === 0 || isPlaceholderData}>
                Previous
              </button>
              <span>
                Page {data.page + 1} of {data.totalPages}
              </span>
              <button
                type="button"
                onClick={() => goToPage(page + 1)}
                disabled={page + 1 >= data.totalPages || isPlaceholderData}
              >
                Next
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}
