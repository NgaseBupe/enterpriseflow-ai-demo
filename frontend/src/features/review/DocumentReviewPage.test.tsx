import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { DocumentSummary } from '../../api/documents'
import { anExtraction } from '../../test/extractionFixtures'
import { aDocument } from '../../test/fixtures'
import { renderApp } from '../../test/render'
import { server } from '../../test/server'

function serveDocument(document: DocumentSummary) {
  server.use(http.get(`*/api/documents/${document.id}`, () => HttpResponse.json(document)))
}

describe('Document review page', () => {
  it('extracts an uploaded document and shows the extracted order', async () => {
    const uploaded = aDocument({ originalFileName: 'po-1001.pdf', status: 'UPLOADED' })
    serveDocument(uploaded)
    server.use(
      http.post(`*/api/documents/${uploaded.id}/process`, () => {
        serveDocument({ ...uploaded, status: 'EXTRACTED' })
        return HttpResponse.json({ ...uploaded, status: 'EXTRACTED' })
      }),
      http.get(`*/api/documents/${uploaded.id}/extraction`, () => HttpResponse.json(anExtraction(uploaded.id))),
    )
    renderApp(`/documents/${uploaded.id}`)

    await userEvent.click(await screen.findByRole('button', { name: 'Extract data' }))

    expect(await screen.findByRole('heading', { name: 'Extracted order' })).toBeInTheDocument()
    expect(screen.getByRole('note')).toHaveTextContent('AI-generated information — human review required.')
    expect(screen.getByText('PO-1001')).toBeInTheDocument()
    expect(screen.getByText('Extracted by mock (mock-v1) · confidence 93%')).toBeInTheDocument()
    const lines = within(screen.getByRole('table', { name: 'Line items' }))
    expect(lines.getByText('M8 hex bolts, box of 100')).toBeInTheDocument()
    expect(lines.getByRole('columnheader', { name: 'Line total (ZMW)' })).toBeInTheDocument()
    expect(lines.getByRole('rowheader', { name: 'Total (ZMW)' })).toBeInTheDocument()
    expect(lines.getByText('2,209.80')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Extract data' })).not.toBeInTheDocument()
  })

  it('shows why extraction failed and lets the user retry', async () => {
    const failed = aDocument({ status: 'EXTRACTION_FAILED', failureReason: 'The AI service took too long to respond.' })
    serveDocument(failed)
    server.use(
      http.post(`*/api/documents/${failed.id}/process`, () => {
        serveDocument({ ...failed, status: 'EXTRACTED', failureReason: null })
        return HttpResponse.json({ ...failed, status: 'EXTRACTED', failureReason: null })
      }),
      http.get(`*/api/documents/${failed.id}/extraction`, () => HttpResponse.json(anExtraction(failed.id))),
    )
    renderApp(`/documents/${failed.id}`)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Extraction failed: The AI service took too long to respond.',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Retry extraction' }))

    expect(await screen.findByRole('heading', { name: 'Extracted order' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it("shows the server's reason when extraction cannot start", async () => {
    const uploaded = aDocument({ status: 'UPLOADED' })
    serveDocument(uploaded)
    server.use(
      http.post(`*/api/documents/${uploaded.id}/process`, () =>
        HttpResponse.json(
          { title: 'Action not allowed in the current state', status: 409, detail: 'Cannot start extraction while the document is processing.' },
          { status: 409 },
        ),
      ),
    )
    renderApp(`/documents/${uploaded.id}`)

    await userEvent.click(await screen.findByRole('button', { name: 'Extract data' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot start extraction while the document is processing.')
  })

  it('shows the extracted order for a document that was extracted earlier', async () => {
    const extracted = aDocument({ status: 'EXTRACTED' })
    serveDocument(extracted)
    server.use(
      http.get(`*/api/documents/${extracted.id}/extraction`, () =>
        HttpResponse.json(anExtraction(extracted.id, { customerEmail: null })),
      ),
    )
    renderApp(`/documents/${extracted.id}`)

    expect(await screen.findByText('Chanda Hardware Ltd')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /extract/i })).not.toBeInTheDocument()
  })

  it('shows a clear message for an unknown document', async () => {
    server.use(
      http.get('*/api/documents/missing', () =>
        HttpResponse.json({ title: 'Document not found', status: 404, detail: 'No document with ID missing.' }, { status: 404 }),
      ),
    )
    renderApp('/documents/missing')

    expect(await screen.findByRole('alert')).toHaveTextContent('No document with ID missing.')
  })

  it('shows a PDF next to the extracted data', async () => {
    const pdf = aDocument({ originalFileName: 'po-1001.pdf', contentType: 'application/pdf', status: 'EXTRACTED' })
    serveDocument(pdf)
    server.use(http.get(`*/api/documents/${pdf.id}/extraction`, () => HttpResponse.json(anExtraction(pdf.id))))
    renderApp(`/documents/${pdf.id}`)

    const original = await screen.findByRole('region', { name: 'Original document' })
    const viewer = within(original).getByTitle('Original document: po-1001.pdf')
    expect(viewer.tagName).toBe('IFRAME')
    expect(viewer).toHaveAttribute('src', `/api/documents/${pdf.id}/content`)
    expect(within(original).getByRole('link', { name: 'Open in a new tab' })).toHaveAttribute('target', '_blank')

    const data = screen.getByRole('region', { name: 'Extracted data' })
    expect(await within(data).findByRole('heading', { name: 'Extracted order' })).toBeInTheDocument()
  })

  it('shows an image document as an image', async () => {
    const photo = aDocument({ originalFileName: 'photo.jpg', contentType: 'image/jpeg', status: 'UPLOADED' })
    serveDocument(photo)
    renderApp(`/documents/${photo.id}`)

    const image = await screen.findByRole('img', { name: 'Original document: photo.jpg' })
    expect(image).toHaveAttribute('src', `/api/documents/${photo.id}/content`)
    expect(screen.getByText('This document has not been extracted yet.')).toBeInTheDocument()
  })

  it('summarises the file in plain words', async () => {
    const pdf = aDocument({ contentType: 'application/pdf', fileSize: 48_213, status: 'UPLOADED' })
    serveDocument(pdf)
    renderApp(`/documents/${pdf.id}`)

    expect(await screen.findByText(/PDF · 47\.1 KB/)).toBeInTheDocument()
  })
})

