import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { DocumentSummary } from '../../api/documents'
import type { OrderExtraction } from '../../api/extraction'
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

const confirmed = (extraction: OrderExtraction): OrderExtraction => ({
  ...extraction,
  reviewedBy: '01920000-0000-7000-8000-000000000001',
  reviewedByName: 'Demo Reviewer',
  reviewedAt: '2026-10-03T09:30:00Z',
  version: extraction.version + 1,
})

describe('Confirming the extracted order', () => {
  it('asks first, then confirms the version the reviewer saw and locks the data', async () => {
    const document = aDocument({ status: 'IN_REVIEW' })
    const extraction = anExtraction(document.id, { version: 2 })
    serve(document, extraction)
    let sentVersion: number | null = null
    server.use(
      http.post(`*/api/documents/${document.id}/review/confirm`, async ({ request }) => {
        sentVersion = ((await request.json()) as { version: number }).version
        serve({ ...document, status: 'CONFIRMED' }, confirmed(extraction))
        return HttpResponse.json(confirmed(extraction))
      }),
    )
    renderApp(`/documents/${document.id}`)

    await userEvent.click(await screen.findByRole('button', { name: 'Confirm' }))
    const dialog = screen.getByRole('dialog', { name: 'Confirm the extracted data?' })
    expect(within(dialog).getByText(/does not accept or reject the order/)).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus()

    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirm' }))

    expect(await screen.findByText('Extraction confirmed.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(sentVersion).toBe(2)
    expect(screen.getByRole('note')).toHaveTextContent(/Confirmed by Demo Reviewer on/)
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument()
    expect(await screen.findByText('Confirmed')).toBeInTheDocument()
  })

  it('does nothing if the reviewer cancels, by button or Escape', async () => {
    const document = aDocument({ status: 'EXTRACTED' })
    serve(document, anExtraction(document.id))
    let requests = 0
    server.use(
      http.post(`*/api/documents/${document.id}/review/confirm`, () => {
        requests += 1
        return HttpResponse.json({})
      }),
    )
    renderApp(`/documents/${document.id}`)

    await userEvent.click(await screen.findByRole('button', { name: 'Confirm' }))
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Confirm' }))
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    expect(requests).toBe(0)
    expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument()
  })

  it('keeps focus inside the dialog', async () => {
    const document = aDocument({ status: 'EXTRACTED' })
    serve(document, anExtraction(document.id))
    renderApp(`/documents/${document.id}`)
    await userEvent.click(await screen.findByRole('button', { name: 'Confirm' }))
    const dialog = screen.getByRole('dialog')

    await userEvent.tab()
    expect(within(dialog).getByRole('button', { name: 'Confirm' })).toHaveFocus()
    await userEvent.tab()
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus()
  })

  it("shows why the server refused, inside the dialog", async () => {
    const document = aDocument({ status: 'IN_REVIEW' })
    serve(document, anExtraction(document.id))
    server.use(
      http.post(`*/api/documents/${document.id}/review/confirm`, () =>
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
    renderApp(`/documents/${document.id}`)

    await userEvent.click(await screen.findByRole('button', { name: 'Confirm' }))
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }))

    expect(await within(screen.getByRole('dialog')).findByRole('alert')).toHaveTextContent('changed by someone else')
  })

  it('shows who confirmed an order that was confirmed earlier', async () => {
    const document = aDocument({ status: 'CONFIRMED' })
    serve(document, confirmed(anExtraction(document.id)))
    renderApp(`/documents/${document.id}`)

    expect(await screen.findByRole('note')).toHaveTextContent(/Confirmed by Demo Reviewer on .*can no longer be edited/)
    expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument()
  })
})
