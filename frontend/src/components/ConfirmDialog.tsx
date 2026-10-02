import { useEffect, useId, useRef, type ReactNode } from 'react'
import styles from './ConfirmDialog.module.css'

interface Props {
  title: string
  children: ReactNode
  confirmLabel: string
  busy?: boolean
  error?: string | null
  onConfirm: () => void
  onCancel: () => void
}

/**
 * A modal confirmation. Focus moves to the dialog's safe choice (Cancel), Escape cancels, and Tab stays
 * inside the dialog while it is open.
 */
export function ConfirmDialog({ title, children, confirmLabel, busy = false, error, onConfirm, onCancel }: Props) {
  const titleId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)
  const cancelRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    const previouslyFocused = document.activeElement as HTMLElement | null
    cancelRef.current?.focus()
    return () => previouslyFocused?.focus()
  }, [])

  function handleKeyDown(event: React.KeyboardEvent) {
    if (event.key === 'Escape' && !busy) {
      onCancel()
      return
    }
    if (event.key === 'Tab') {
      const focusable = dialogRef.current?.querySelectorAll<HTMLElement>('button:not([disabled])')
      if (!focusable || focusable.length === 0) {
        return
      }
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }
  }

  return (
    <div className={styles.backdrop}>
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className={styles.dialog}
        onKeyDown={handleKeyDown}
      >
        <h2 id={titleId} className={styles.title}>
          {title}
        </h2>
        <div className={styles.body}>{children}</div>
        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}
        <div className={styles.actions}>
          <button ref={cancelRef} type="button" className={styles.secondaryButton} onClick={onCancel} disabled={busy}>
            Cancel
          </button>
          <button type="button" className={styles.primaryButton} onClick={onConfirm} disabled={busy}>
            {busy ? 'Confirming…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
