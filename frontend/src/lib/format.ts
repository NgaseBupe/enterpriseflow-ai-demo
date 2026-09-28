const dateTime = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })

export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso))
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export function formatDate(isoDate: string | null): string {
  if (!isoDate) {
    return '—'
  }
  // Dates without a time are calendar dates; format them in UTC so they never shift by a day.
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeZone: 'UTC' }).format(new Date(isoDate))
}

export function formatMoney(amount: number | null, currency: string | null): string {
  if (amount === null) {
    return '—'
  }
  const formatted = new Intl.NumberFormat(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount)
  return currency ? `${currency} ${formatted}` : formatted
}

export function formatNumber(value: number | null): string {
  return value === null ? '—' : new Intl.NumberFormat().format(value)
}
