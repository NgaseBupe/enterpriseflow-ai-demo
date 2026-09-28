import type { OrderExtraction } from '../api/extraction'

export function anExtraction(documentId: string, overrides: Partial<OrderExtraction> = {}): OrderExtraction {
  return {
    id: '01920000-0000-7000-8000-00000000e001',
    documentId,
    poNumber: 'PO-1001',
    poDate: '2026-09-01',
    customerName: 'Chanda Hardware Ltd',
    customerEmail: 'orders@chanda-hardware.example',
    customerPhone: '+260 211 000 101',
    deliveryAddress: 'Plot 12, Industrial Area, Lusaka',
    requestedDeliveryDate: '2026-09-15',
    currency: 'ZMW',
    subtotal: 1905,
    taxAmount: 304.8,
    totalAmount: 2209.8,
    notes: null,
    lines: [
      { lineNumber: 1, productCode: 'HB-M8-100', description: 'M8 hex bolts, box of 100', quantity: 12, unitOfMeasure: 'box', unitPrice: 85, lineTotal: 1020 },
      { lineNumber: 2, productCode: 'GW-M8-200', description: 'Galvanised washers M8, pack of 200', quantity: 10, unitOfMeasure: 'pack', unitPrice: 42.5, lineTotal: 425 },
    ],
    aiProvider: 'mock',
    aiModel: 'mock-v1',
    aiConfidence: 0.93,
    extractedAt: '2026-09-27T10:20:00Z',
    reviewedBy: null,
    reviewedAt: null,
    version: 0,
    ...overrides,
  }
}
