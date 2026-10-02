import { z } from 'zod'
import type { ExtractionUpdate, OrderExtraction } from '../../api/extraction'

/*
 * The form works with text, exactly as typed. It is validated here with the same rules as the server,
 * then converted to the API's types on save. The server checks everything again.
 */

const optional = (max: number) => z.string().trim().max(max, `Must be at most ${max} characters`)

function decimal({ min, exclusive, places }: { min: number; exclusive: boolean; places: number }) {
  // A minus sign is accepted by the format check so that "-1" gets the range message
  // ("Must be greater than 0") rather than a confusing format message.
  const pattern = new RegExp(`^-?\\d+(\\.\\d{1,${places}})?$`)
  return z
    .string()
    .trim()
    .refine((v) => v === '' || pattern.test(v), {
      message: places === 2 ? 'Enter an amount, e.g. 1250.00' : `Enter a number with up to ${places} decimals`,
    })
    .refine((v) => v === '' || !pattern.test(v) || (exclusive ? Number(v) > min : Number(v) >= min), {
      message: exclusive ? 'Must be greater than 0' : 'Must be 0 or more',
    })
}

const money = decimal({ min: 0, exclusive: false, places: 2 })
const quantity = decimal({ min: 0, exclusive: true, places: 3 })
const date = z.string().refine((v) => v === '' || /^\d{4}-\d{2}-\d{2}$/.test(v), { message: 'Enter a valid date' })

export const extractionFormSchema = z.object({
  poNumber: optional(100),
  poDate: date,
  customerName: optional(200),
  customerEmail: optional(254).refine((v) => v === '' || z.email().safeParse(v).success, {
    message: 'Enter a valid email address',
  }),
  customerPhone: optional(50),
  deliveryAddress: optional(500),
  requestedDeliveryDate: date,
  currency: z
    .string()
    .trim()
    .refine((v) => v === '' || /^[A-Za-z]{3}$/.test(v), { message: 'Use a three-letter code, e.g. ZMW' }),
  subtotal: money,
  taxAmount: money,
  totalAmount: money,
  notes: optional(2000),
  lines: z.array(
    z.object({
      lineNumber: z.number(),
      productCode: optional(100),
      description: optional(500),
      quantity,
      unitOfMeasure: optional(20),
      unitPrice: money,
      lineTotal: money,
    }),
  ),
})

export type ExtractionFormValues = z.infer<typeof extractionFormSchema>

const str = (value: string | number | null) => (value === null ? '' : String(value))

export function toFormValues(extraction: OrderExtraction): ExtractionFormValues {
  return {
    poNumber: str(extraction.poNumber),
    poDate: str(extraction.poDate),
    customerName: str(extraction.customerName),
    customerEmail: str(extraction.customerEmail),
    customerPhone: str(extraction.customerPhone),
    deliveryAddress: str(extraction.deliveryAddress),
    requestedDeliveryDate: str(extraction.requestedDeliveryDate),
    currency: str(extraction.currency),
    subtotal: money2(extraction.subtotal),
    taxAmount: money2(extraction.taxAmount),
    totalAmount: money2(extraction.totalAmount),
    notes: str(extraction.notes),
    lines: extraction.lines.map((line) => ({
      lineNumber: line.lineNumber,
      productCode: str(line.productCode),
      description: str(line.description),
      quantity: str(line.quantity),
      unitOfMeasure: str(line.unitOfMeasure),
      unitPrice: money2(line.unitPrice),
      lineTotal: money2(line.lineTotal),
    })),
  }
}

function money2(value: number | null) {
  return value === null ? '' : value.toFixed(2)
}

const text = (value: string) => (value.trim() === '' ? null : value.trim())
const number = (value: string) => (value.trim() === '' ? null : Number(value))

export function toUpdate(values: ExtractionFormValues, version: number): ExtractionUpdate {
  return {
    version,
    poNumber: text(values.poNumber),
    poDate: text(values.poDate),
    customerName: text(values.customerName),
    customerEmail: text(values.customerEmail),
    customerPhone: text(values.customerPhone),
    deliveryAddress: text(values.deliveryAddress),
    requestedDeliveryDate: text(values.requestedDeliveryDate),
    currency: text(values.currency)?.toUpperCase() ?? null,
    subtotal: number(values.subtotal),
    taxAmount: number(values.taxAmount),
    totalAmount: number(values.totalAmount),
    notes: text(values.notes),
    lines: values.lines.map((line) => ({
      lineNumber: line.lineNumber,
      productCode: text(line.productCode),
      description: text(line.description),
      quantity: number(line.quantity),
      unitOfMeasure: text(line.unitOfMeasure),
      unitPrice: number(line.unitPrice),
      lineTotal: number(line.lineTotal),
    })),
  }
}

/** Maps a server field name such as "lines[0].quantity" to the form's "lines.0.quantity". */
export function toFormField(serverField: string): string {
  return serverField.replace(/\[(\d+)\]/g, '.$1')
}
