import type { OrderExtraction } from '../../api/extraction'
import { formatDate, formatMoney, formatNumber } from '../../lib/format'
import styles from './ExtractedOrder.module.css'

function text(value: string | null) {
  return value ?? '—'
}

/** Read-only view of the extracted order. Editing arrives with the review screen. */
export function ExtractedOrder({ extraction }: { extraction: OrderExtraction }) {
  const { currency } = extraction
  return (
    <section aria-labelledby="extracted-order-heading" className={styles.section}>
      <p className={styles.banner} role="note">
        AI-generated information — human review required.
      </p>
      <h2 id="extracted-order-heading">Extracted order</h2>
      <p className={styles.source}>
        Extracted by {extraction.aiProvider} ({extraction.aiModel})
        {extraction.aiConfidence !== null && <> · confidence {Math.round(extraction.aiConfidence * 100)}%</>}
      </p>

      <dl className={styles.fields}>
        <dt>PO number</dt>
        <dd>{text(extraction.poNumber)}</dd>
        <dt>PO date</dt>
        <dd>{formatDate(extraction.poDate)}</dd>
        <dt>Customer</dt>
        <dd>{text(extraction.customerName)}</dd>
        <dt>Email</dt>
        <dd>{text(extraction.customerEmail)}</dd>
        <dt>Phone</dt>
        <dd>{text(extraction.customerPhone)}</dd>
        <dt>Delivery address</dt>
        <dd>{text(extraction.deliveryAddress)}</dd>
        <dt>Requested delivery</dt>
        <dd>{formatDate(extraction.requestedDeliveryDate)}</dd>
        <dt>Notes</dt>
        <dd>{text(extraction.notes)}</dd>
      </dl>

      <table className={styles.lines}>
        <caption>Line items</caption>
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">Product</th>
            <th scope="col">Description</th>
            <th scope="col" className={styles.number}>Qty</th>
            <th scope="col">Unit</th>
            <th scope="col" className={styles.number}>Unit price</th>
            <th scope="col" className={styles.number}>Line total</th>
          </tr>
        </thead>
        <tbody>
          {extraction.lines.map((line) => (
            <tr key={line.lineNumber}>
              <td>{line.lineNumber}</td>
              <td>{text(line.productCode)}</td>
              <td>{text(line.description)}</td>
              <td className={styles.number}>{formatNumber(line.quantity)}</td>
              <td>{text(line.unitOfMeasure)}</td>
              <td className={styles.number}>{formatMoney(line.unitPrice, currency)}</td>
              <td className={styles.number}>{formatMoney(line.lineTotal, currency)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <th scope="row" colSpan={6}>Subtotal</th>
            <td className={styles.number}>{formatMoney(extraction.subtotal, currency)}</td>
          </tr>
          <tr>
            <th scope="row" colSpan={6}>Tax</th>
            <td className={styles.number}>{formatMoney(extraction.taxAmount, currency)}</td>
          </tr>
          <tr>
            <th scope="row" colSpan={6}>Total</th>
            <td className={styles.number}>{formatMoney(extraction.totalAmount, currency)}</td>
          </tr>
        </tfoot>
      </table>
    </section>
  )
}
