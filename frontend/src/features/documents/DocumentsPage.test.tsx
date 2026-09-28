import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { aDocument, aPage } from '../../test/fixtures'
import { renderApp } from '../../test/render'
import { server } from '../../test/server'

describe('Documents page', () => {
  it('lists documents in the order the server returns them, with their status', async () => {
    server.use(
      http.get('*/api/documents', () =>
        HttpResponse.json(
          aPage([
            aDocument({ originalFileName: 'newest.pdf', status: 'EXTRACTED' }),
            aDocument({ originalFileName: 'older.png', status: 'UPLOADED' }),
          ]),
        ),
      ),
    )

    renderApp('/documents')

    const rows = await screen.findAllByRole('row')
    expect(within(rows[1]).getByRole('link', { name: 'newest.pdf' })).toBeInTheDocument()
    expect(within(rows[1]).getByText('Ready for review')).toBeInTheDocument()
    expect(within(rows[2]).getByRole('link', { name: 'older.png' })).toBeInTheDocument()
    expect(within(rows[2]).getByText('Uploaded')).toBeInTheDocument()
  })

  it('shows a loading state while documents are being fetched', async () => {
    server.use(http.get('*/api/documents', () => new Promise(() => {})))

    renderApp('/documents')

    expect(await screen.findByText('Loading documents…')).toBeInTheDocument()
  })

  it('shows a helpful empty state with a link to upload', async () => {
    server.use(http.get('*/api/documents', () => HttpResponse.json(aPage([]))))

    renderApp('/documents')

    expect(await screen.findByText('No documents yet.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Upload your first purchase order' })).toHaveAttribute(
      'href',
      '/documents/upload',
    )
  })

  it('shows the server error and lets the user try again', async () => {
    let calls = 0
    server.use(
      http.get('*/api/documents', () => {
        calls += 1
        return calls === 1
          ? HttpResponse.json({ title: 'Internal Server Error', detail: 'An unexpected error occurred.' }, { status: 500 })
          : HttpResponse.json(aPage([aDocument({ originalFileName: 'recovered.pdf' })]))
      }),
    )

    renderApp('/documents')

    expect(await screen.findByRole('alert')).toHaveTextContent('An unexpected error occurred.')
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('link', { name: 'recovered.pdf' })).toBeInTheDocument()
  })

  it('moves between pages', async () => {
    server.use(
      http.get('*/api/documents', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        const document = aDocument({ originalFileName: `page-${page + 1}.pdf` })
        return HttpResponse.json(aPage([document], { page, totalItems: 21, totalPages: 2 }))
      }),
    )

    renderApp('/documents')

    expect(await screen.findByRole('link', { name: 'page-1.pdf' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByRole('link', { name: 'page-2.pdf' })).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })

  it('opens a document when its name is selected', async () => {
    const document = aDocument({ originalFileName: 'po-2001.pdf' })
    server.use(
      http.get('*/api/documents', () => HttpResponse.json(aPage([document]))),
      http.get(`*/api/documents/${document.id}`, () => HttpResponse.json(document)),
    )

    const { router } = renderApp('/documents')
    await userEvent.click(await screen.findByRole('link', { name: 'po-2001.pdf' }))

    expect(router.state.location.pathname).toBe(`/documents/${document.id}`)
    expect(await screen.findByRole('heading', { name: 'po-2001.pdf' })).toBeInTheDocument()
  })

  it('redirects the home page to the documents list', async () => {
    server.use(http.get('*/api/documents', () => HttpResponse.json(aPage([]))))

    const { router } = renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Documents' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/documents')
  })

  it('keeps the page number in the address, so refresh and Back keep the user in place', async () => {
    server.use(
      http.get('*/api/documents', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        return HttpResponse.json(
          aPage([aDocument({ originalFileName: `page-${page + 1}.pdf` })], { page, totalItems: 21, totalPages: 2 }),
        )
      }),
    )

    const { router } = renderApp('/documents?page=2')

    expect(await screen.findByRole('link', { name: 'page-2.pdf' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Previous' }))
    expect(router.state.location.search).toBe('')
    expect(await screen.findByRole('link', { name: 'page-1.pdf' })).toBeInTheDocument()
  })

  it('offers a way back when the requested page is past the end', async () => {
    server.use(http.get('*/api/documents', () => HttpResponse.json(aPage([], { page: 8, totalItems: 3, totalPages: 1 }))))

    renderApp('/documents?page=9')

    expect(await screen.findByText('There are no documents on this page.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Go to the first page' })).toHaveAttribute('href', '/documents')
  })

  it('treats an invalid page number as the first page', async () => {
    let requestedPage: string | null = null
    server.use(
      http.get('*/api/documents', ({ request }) => {
        requestedPage = new URL(request.url).searchParams.get('page')
        return HttpResponse.json(aPage([]))
      }),
    )

    renderApp('/documents?page=abc')

    await screen.findByText('No documents yet.')
    expect(requestedPage).toBe('0')
  })

  it('keeps showing the current page while the next one loads', async () => {
    server.use(
      http.get('*/api/documents', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        if (page > 0) {
          return new Promise(() => {}) // the next page never arrives
        }
        return HttpResponse.json(aPage([aDocument({ originalFileName: 'first-page.pdf' })], { totalItems: 21, totalPages: 2 }))
      }),
    )

    renderApp('/documents')
    await userEvent.click(await screen.findByRole('button', { name: 'Next' }))

    expect(screen.getByRole('link', { name: 'first-page.pdf' })).toBeInTheDocument()
    expect(screen.queryByText('Loading documents…')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })
})
