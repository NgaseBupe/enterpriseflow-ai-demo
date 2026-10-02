import { describe, expect, it } from 'vitest'
import { anExtraction } from '../../test/extractionFixtures'
import { extractionFormSchema, toFormField, toFormValues, toUpdate } from './formValues'

describe('extraction form values', () => {
  it('shows amounts with two decimals and missing values as empty fields', () => {
    const values = toFormValues(anExtraction('doc-1', { notes: null, subtotal: 1905 }))

    expect(values.subtotal).toBe('1905.00')
    expect(values.notes).toBe('')
    expect(values.lines[1].unitPrice).toBe('42.50')
  })

  it('turns the form back into an update, with empty fields as null', () => {
    const values = toFormValues(anExtraction('doc-1'))
    values.customerName = '  Chanda Hardware Limited  '
    values.notes = '   '
    values.currency = 'zmw'
    values.lines[0].quantity = '11'

    const update = toUpdate(values, 4)

    expect(update.version).toBe(4)
    expect(update.customerName).toBe('Chanda Hardware Limited')
    expect(update.notes).toBeNull()
    expect(update.currency).toBe('ZMW')
    expect(update.lines[0].quantity).toBe(11)
    expect(update.subtotal).toBe(1905)
  })

  it('maps server field names to form field names', () => {
    expect(toFormField('lines[0].quantity')).toBe('lines.0.quantity')
    expect(toFormField('customerEmail')).toBe('customerEmail')
  })

  it('applies the same rules as the server', () => {
    const values = toFormValues(anExtraction('doc-1'))
    values.customerEmail = 'not-an-email'
    values.currency = 'ZM'
    values.lines[0].quantity = '0'
    values.lines[1].unitPrice = '-1'
    values.lines[1].lineTotal = 'abc'
    values.subtotal = '12.345'

    const result = extractionFormSchema.safeParse(values)

    expect(result.success).toBe(false)
    const messages = Object.fromEntries((result.error?.issues ?? []).map((i) => [i.path.join('.'), i.message]))
    expect(messages).toEqual({
      customerEmail: 'Enter a valid email address',
      currency: 'Use a three-letter code, e.g. ZMW',
      'lines.0.quantity': 'Must be greater than 0',
      'lines.1.unitPrice': 'Must be 0 or more',
      'lines.1.lineTotal': 'Enter an amount, e.g. 1250.00',
      subtotal: 'Enter an amount, e.g. 1250.00',
    })
  })

  it('accepts empty optional fields', () => {
    const values = toFormValues(anExtraction('doc-1', { customerEmail: null, poDate: null, currency: null }))

    expect(extractionFormSchema.safeParse(values).success).toBe(true)
  })
})
