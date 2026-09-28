import type { DocumentSummary, Page } from '../api/documents'

let counter = 0

export function aDocument(overrides: Partial<DocumentSummary> = {}): DocumentSummary {
  counter += 1
  return {
    id: `01920000-0000-7000-8000-${String(counter).padStart(12, '0')}`,
    originalFileName: `po-${1000 + counter}.pdf`,
    contentType: 'application/pdf',
    fileSize: 48_213,
    status: 'UPLOADED',
    failureReason: null,
    uploadedAt: '2026-09-27T10:15:00Z',
    uploadedBy: '01920000-0000-7000-8000-000000000001',
    ...overrides,
  }
}

export function aPage<T>(items: T[], overrides: Partial<Page<T>> = {}): Page<T> {
  return { items, page: 0, size: 20, totalItems: items.length, totalPages: items.length > 0 ? 1 : 0, ...overrides }
}
