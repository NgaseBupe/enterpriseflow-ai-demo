import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { request } from './client'

export type DocumentStatus =
  | 'UPLOADED'
  | 'PROCESSING'
  | 'EXTRACTED'
  | 'EXTRACTION_FAILED'
  | 'IN_REVIEW'
  | 'CONFIRMED'

export interface DocumentSummary {
  id: string
  originalFileName: string
  contentType: string
  fileSize: number
  status: DocumentStatus
  failureReason: string | null
  uploadedAt: string
  uploadedBy: string
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export const PAGE_SIZE = 20

const keys = {
  all: ['documents'] as const,
  list: (page: number) => ['documents', 'list', page] as const,
  detail: (id: string) => ['documents', 'detail', id] as const,
}

export function useDocuments(page: number) {
  return useQuery({
    queryKey: keys.list(page),
    queryFn: () => request<Page<DocumentSummary>>(`/api/documents?page=${page}&size=${PAGE_SIZE}`),
    // Keep showing the current page while the next one loads, instead of flashing a loading state.
    placeholderData: keepPreviousData,
  })
}

export function useDocument(id: string) {
  return useQuery({
    queryKey: keys.detail(id),
    queryFn: () => request<DocumentSummary>(`/api/documents/${encodeURIComponent(id)}`),
  })
}

export function useUploadDocument() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => {
      const body = new FormData()
      body.append('file', file)
      return request<DocumentSummary>('/api/documents', { method: 'POST', body })
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.all }),
  })
}
