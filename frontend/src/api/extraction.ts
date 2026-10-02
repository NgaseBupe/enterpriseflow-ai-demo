import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { request } from './client'
import type { DocumentSummary } from './documents'

export interface ExtractionLine {
  lineNumber: number
  productCode: string | null
  description: string | null
  quantity: number | null
  unitOfMeasure: string | null
  unitPrice: number | null
  lineTotal: number | null
}

export interface OrderExtraction {
  id: string
  documentId: string
  poNumber: string | null
  poDate: string | null
  customerName: string | null
  customerEmail: string | null
  customerPhone: string | null
  deliveryAddress: string | null
  requestedDeliveryDate: string | null
  currency: string | null
  subtotal: number | null
  taxAmount: number | null
  totalAmount: number | null
  notes: string | null
  lines: ExtractionLine[]
  aiProvider: string
  aiModel: string
  aiConfidence: number | null
  extractedAt: string
  reviewedBy: string | null
  reviewedByName: string | null
  reviewedAt: string | null
  version: number
}

export function useExtraction(documentId: string, enabled: boolean) {
  return useQuery({
    queryKey: ['documents', 'detail', documentId, 'extraction'],
    queryFn: () => request<OrderExtraction>(`/api/documents/${encodeURIComponent(documentId)}/extraction`),
    enabled,
  })
}

export interface ExtractionUpdate {
  version: number
  poNumber: string | null
  poDate: string | null
  customerName: string | null
  customerEmail: string | null
  customerPhone: string | null
  deliveryAddress: string | null
  requestedDeliveryDate: string | null
  currency: string | null
  subtotal: number | null
  taxAmount: number | null
  totalAmount: number | null
  notes: string | null
  lines: ExtractionLine[]
}

/** Saves a reviewer's corrections. The response is the updated extraction. */
export function useUpdateExtraction(documentId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (update: ExtractionUpdate) =>
      request<OrderExtraction>(`/api/documents/${encodeURIComponent(documentId)}/extraction`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(update),
      }),
    onSuccess: (extraction) => {
      queryClient.setQueryData(['documents', 'detail', documentId, 'extraction'], extraction)
      // The document's status may have changed to In review.
      return queryClient.invalidateQueries({ queryKey: ['documents'], refetchType: 'active' })
    },
  })
}

/** Confirms that the extracted data matches the document. It never accepts or rejects the order. */
export function useConfirmExtraction(documentId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (version: number) =>
      request<OrderExtraction>(`/api/documents/${encodeURIComponent(documentId)}/review/confirm`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ version }),
      }),
    onSuccess: (extraction) => {
      queryClient.setQueryData(['documents', 'detail', documentId, 'extraction'], extraction)
      return queryClient.invalidateQueries({ queryKey: ['documents'], refetchType: 'active' })
    },
  })
}

/** Starts (or retries) extraction. The response is the document with its new status. */
export function useExtractDocument(documentId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () =>
      request<DocumentSummary>(`/api/documents/${encodeURIComponent(documentId)}/process`, { method: 'POST' }),
    onSuccess: (document) => {
      queryClient.setQueryData(['documents', 'detail', documentId], document)
      return queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
  })
}
