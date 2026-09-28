import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { aDocument, aPage } from '../../test/fixtures'
import { renderApp } from '../../test/render'
import { server } from '../../test/server'
import { MAX_FILE_SIZE } from './UploadPage'

// applyAccept: false lets tests choose files the browser's picker would normally hide.
const user = () => userEvent.setup({ applyAccept: false })

function pdf(name = 'po-1001.pdf', size = 1024) {
  return new File([new Uint8Array(size)], name, { type: 'application/pdf' })
}

describe('Upload page', () => {
  it('uploads a file and returns to the list with a confirmation', async () => {
    let uploadedSize: number | null = null
    server.use(
      http.post('*/api/documents', async ({ request }) => {
        // The test environment's fetch bridge drops the file name (see docs/review-log.md, R-012),
        // so the request is checked by its content and the response supplies the name.
        const file = (await request.formData()).get('file') as Blob
        uploadedSize = file.size
        return HttpResponse.json(aDocument({ originalFileName: 'po-1001.pdf' }), { status: 201 })
      }),
      http.get('*/api/documents', () => HttpResponse.json(aPage([aDocument({ originalFileName: 'po-1001.pdf' })]))),
    )
    const { router } = renderApp('/documents/upload')

    await user().upload(screen.getByLabelText('Purchase order file'), pdf())
    await user().click(screen.getByRole('button', { name: 'Upload' }))

    expect(await screen.findByText('po-1001.pdf was uploaded.')).toHaveAttribute('role', 'status')
    expect(router.state.location.pathname).toBe('/documents')
    expect(uploadedSize).toBe(1024)
  })

  it('rejects unsupported file types before uploading', async () => {
    renderApp('/documents/upload')

    await user().upload(
      screen.getByLabelText('Purchase order file'),
      new File(['hello'], 'notes.txt', { type: 'text/plain' }),
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Only PDF, PNG and JPEG files are accepted.')
    expect(screen.getByRole('button', { name: 'Upload' })).toBeDisabled()
  })

  it('rejects files over 10 MB before uploading', async () => {
    renderApp('/documents/upload')

    await user().upload(screen.getByLabelText('Purchase order file'), pdf('huge.pdf', MAX_FILE_SIZE + 1))

    expect(screen.getByRole('alert')).toHaveTextContent('The file exceeds the maximum size of 10 MB.')
    expect(screen.getByRole('button', { name: 'Upload' })).toBeDisabled()
  })

  it('rejects empty files before uploading', async () => {
    renderApp('/documents/upload')

    await user().upload(screen.getByLabelText('Purchase order file'), pdf('empty.pdf', 0))

    expect(screen.getByRole('alert')).toHaveTextContent('The selected file is empty.')
  })

  it("shows the server's reason when it rejects the file", async () => {
    server.use(
      http.post('*/api/documents', () =>
        HttpResponse.json(
          { title: 'Unsupported file type', status: 415, detail: 'Only PDF, PNG and JPEG files are accepted.' },
          { status: 415 },
        ),
      ),
    )
    const { router } = renderApp('/documents/upload')

    await user().upload(screen.getByLabelText('Purchase order file'), pdf('disguised.pdf'))
    await user().click(screen.getByRole('button', { name: 'Upload' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Only PDF, PNG and JPEG files are accepted.')
    expect(router.state.location.pathname).toBe('/documents/upload')
  })

  it('disables the button until a file is chosen', () => {
    renderApp('/documents/upload')

    expect(screen.getByRole('button', { name: 'Upload' })).toBeDisabled()
  })

  it('accepts a PDF whose type the operating system did not report', async () => {
    renderApp('/documents/upload')

    await user().upload(screen.getByLabelText('Purchase order file'), new File(['%PDF-1.7'], 'po-1004.PDF', { type: '' }))

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Upload' })).toBeEnabled()
  })
})
