import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, type FieldPath } from 'react-hook-form'
import { ApiError, errorMessage } from '../../api/client'
import { useUpdateExtraction, type OrderExtraction } from '../../api/extraction'
import styles from './ExtractionForm.module.css'
import {
  extractionFormSchema,
  toFormField,
  toFormValues,
  toUpdate,
  type ExtractionFormValues,
} from './formValues'

interface Props {
  extraction: OrderExtraction
  onCancel: () => void
  onSaved: () => void
}

/** Lets the reviewer correct the extracted header and line values. */
export function ExtractionForm({ extraction, onCancel, onSaved }: Props) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ExtractionFormValues>({
    resolver: zodResolver(extractionFormSchema),
    defaultValues: toFormValues(extraction),
  })
  const update = useUpdateExtraction(extraction.documentId)
  const inCurrency = extraction.currency ? ` (${extraction.currency})` : ''

  function onSubmit(values: ExtractionFormValues) {
    update.mutate(toUpdate(values, extraction.version), {
      onSuccess: onSaved,
      onError: (error) => {
        if (error instanceof ApiError) {
          for (const fieldError of error.problem?.errors ?? []) {
            setError(toFormField(fieldError.field) as FieldPath<ExtractionFormValues>, {
              message: fieldError.message,
            })
          }
        }
      },
    })
  }

  function field(name: FieldPath<ExtractionFormValues>, label: string, type: 'text' | 'date' | 'email' = 'text') {
    const error = fieldError(name)
    const id = `field-${name}`
    return (
      <div className={styles.field}>
        <label htmlFor={id}>{label}</label>
        <input
          id={id}
          type={type}
          aria-invalid={!!error}
          aria-describedby={error ? `${id}-error` : undefined}
          {...register(name)}
        />
        {error && (
          <span id={`${id}-error`} className={styles.fieldError}>
            {error}
          </span>
        )}
      </div>
    )
  }

  function cell(name: FieldPath<ExtractionFormValues>, label: string, numeric = false) {
    const error = fieldError(name)
    return (
      <>
        <input
          aria-label={label}
          aria-invalid={!!error}
          inputMode={numeric ? 'decimal' : undefined}
          className={numeric ? styles.numberInput : undefined}
          {...register(name)}
        />
        {error && <span className={styles.fieldError}>{error}</span>}
      </>
    )
  }

  function fieldError(name: FieldPath<ExtractionFormValues>): string | undefined {
    const parts = name.split('.')
    let node: unknown = errors
    for (const part of parts) {
      node = (node as Record<string, unknown> | undefined)?.[part]
    }
    return (node as { message?: string } | undefined)?.message
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className={styles.form} noValidate aria-labelledby="edit-heading">
      <h2 id="edit-heading">Correct the extracted order</h2>

      {update.isError && (
        <p className={styles.error} role="alert">
          {errorMessage(update.error)}
        </p>
      )}

      <div className={styles.fields}>
        {field('poNumber', 'PO number')}
        {field('poDate', 'PO date', 'date')}
        {field('customerName', 'Customer')}
        {field('customerEmail', 'Email', 'email')}
        {field('customerPhone', 'Phone')}
        {field('requestedDeliveryDate', 'Requested delivery', 'date')}
        {field('currency', 'Currency')}
        {field('deliveryAddress', 'Delivery address')}
        {field('notes', 'Notes')}
      </div>

      <div className={styles.tableScroll}>
        <table className={styles.lines}>
          <caption>Line items</caption>
          <thead>
            <tr>
              <th scope="col">#</th>
              <th scope="col">Description</th>
              <th scope="col">Product code</th>
              <th scope="col">Qty</th>
              <th scope="col">Unit</th>
              <th scope="col">Unit price{inCurrency}</th>
              <th scope="col">Line total{inCurrency}</th>
            </tr>
          </thead>
          <tbody>
            {extraction.lines.map((line, index) => (
              <tr key={line.lineNumber}>
                <td>{line.lineNumber}</td>
                <td>{cell(`lines.${index}.description`, `Line ${line.lineNumber} description`)}</td>
                <td>{cell(`lines.${index}.productCode`, `Line ${line.lineNumber} product code`)}</td>
                <td>{cell(`lines.${index}.quantity`, `Line ${line.lineNumber} quantity`, true)}</td>
                <td>{cell(`lines.${index}.unitOfMeasure`, `Line ${line.lineNumber} unit`)}</td>
                <td>{cell(`lines.${index}.unitPrice`, `Line ${line.lineNumber} unit price`, true)}</td>
                <td>{cell(`lines.${index}.lineTotal`, `Line ${line.lineNumber} line total`, true)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row" colSpan={6}>
                Subtotal
              </th>
              <td>{cell('subtotal', 'Subtotal', true)}</td>
            </tr>
            <tr>
              <th scope="row" colSpan={6}>
                Tax
              </th>
              <td>{cell('taxAmount', 'Tax', true)}</td>
            </tr>
            <tr>
              <th scope="row" colSpan={6}>
                Total{inCurrency}
              </th>
              <td>{cell('totalAmount', 'Total', true)}</td>
            </tr>
          </tfoot>
        </table>
      </div>

      <div className={styles.actions}>
        <button type="submit" className={styles.primaryButton} disabled={update.isPending}>
          {update.isPending ? 'Saving…' : 'Save changes'}
        </button>
        <button type="button" className={styles.secondaryButton} onClick={onCancel} disabled={update.isPending}>
          Cancel
        </button>
      </div>
    </form>
  )
}
