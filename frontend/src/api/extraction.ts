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
