import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { DocumentSummary } from '../../api/documents'
import type { ExtractionUpdate, OrderExtraction } from '../../api/extraction'
import { anExtraction } from '../../test/extractionFixtures'
import { aDocument } from '../../test/fixtures'
import { renderApp } from '../../test/render'
import { server } from '../../test/server'

function serve(document: DocumentSummary, extraction: OrderExtraction) {
  server.use(
    http.get(`*/api/documents/${document.id}`, () => HttpResponse.json(document)),
    http.get(`*/api/documents/${document.id}/extraction`, () => HttpResponse.json(extraction)),
  )
}

async function openEditor(document: DocumentSummary) {
  renderApp(`/documents/${document.id}`)
  await userEvent.click(await screen.findByRole('button', { name: 'Edit' }))
}

describe('Correcting the extracted data', () => {
  it('saves corrections and shows the updated values', async () => {
    const document = aDocument({ status: 'EXTRACTED' })
    const extraction = anExtraction(document.id, { version: 3 })
    serve(document, extraction)
    let sent: ExtractionUpdate | null = null
    server.use(
      http.put(`*/api/documents/${document.id}/extraction`, async ({ request }) => {
        sent = (await request.json()) as ExtractionUpdate
        const updated = { ...extraction, customerName: sent.customerName, version: 4 }
        serve({ ...document, status: 'IN_REVIEW' }, updated)
        return HttpResponse.json(updated)
      }),
    )
    await openEditor(document)

    const customer = screen.getByLabelText('Customer')
    await userEvent.clear(customer)
    await userEvent.type(customer, 'Chanda Hardware Limited')
    const quantity = screen.getByLabelText('Line 1 quantity')
    await userEvent.clear(quantity)
    await userEvent.type(quantity, '11')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Changes saved.')).toBeInTheDocument()
    expect(screen.getByText('Chanda Hardware Limited')).toBeInTheDocument()
    expect(await screen.findByText('In review')).toBeInTheDocument()
    expect(sent).toMatchObject({ version: 3, customerName: 'Chanda Hardware Limited' })
    expect(sent!.lines[0]).toMatchObject({ lineNumber: 1, quantity: 11 })
  })

  it('checks values before sending them', async () => {
    const document = aDocument({ status: 'EXTRACTED' })
    serve(document, anExtraction(document.id))
    let requests = 0
    server.use(http.put(`*/api/documents/${document.id}/extraction`, () => {
      requests += 1
      return HttpResponse.json({})
    }))
    await openEditor(document)

    const quantity = screen.getByLabelText('Line 1 quantity')
    await userEvent.clear(quantity)
    await userEvent.type(quantity, '-1')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Must be greater than 0')).toBeInTheDocument()
    expect(quantity).toHaveAttribute('aria-invalid', 'true')
    expect(requests).toBe(0)
  })

  it("shows the server's message next to the field it rejected", async () => {
    const document = aDocument({ status: 'EXTRACTED' })
    serve(document, anExtraction(document.id))
    server.use(
      http.put(`*/api/documents/${document.id}/extraction`, () =>
        HttpResponse.json(
          {
            title: 'Invalid input',
            status: 400,
            detail: 'Some fields are invalid. Correct them and try again.',
            errors: [{ field: 'customerPhone', message: 'size must be between 0 and 50' }],
          },
          { status: 400 },
        ),
      ),
    )
    await openEditor(document)

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('size must be between 0 and 50')).toBeInTheDocument()
    expect(screen.getByLabelText('Phone')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('Some fields are invalid.')
  })

  it('explains when someone else changed the data first', async () => {
    const document = aDocument({ status: 'IN_REVIEW' })
    serve(document, anExtraction(document.id))
    server.use(
      http.put(`*/api/documents/${document.id}/extraction`, () =>
        HttpResponse.json(
          {
            title: 'Changed by someone else',
            status: 409,
            detail: 'This extraction was changed by someone else after you opened it. Reload to see the latest version.',
          },
          { status: 409 },
        ),
      ),
    )
    await openEditor(document)

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('changed by someone else')
  })

  it('discards changes on cancel', async () => {
    const document = aDocument({ status: 'EXTRACTED' })
    serve(document, anExtraction(document.id))
    await openEditor(document)

    const customer = screen.getByLabelText('Customer')
    await userEvent.clear(customer)
    await userEvent.type(customer, 'Something else')
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(screen.getByText('Chanda Hardware Ltd')).toBeInTheDocument()
    expect(screen.queryByText('Something else')).not.toBeInTheDocument()
  })

  it('offers no editing once the order is confirmed', async () => {
    const document = aDocument({ status: 'CONFIRMED' })
    serve(document, anExtraction(document.id))
    renderApp(`/documents/${document.id}`)

    expect(await screen.findByRole('heading', { name: 'Extracted order' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
  })
})
