import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../test/server'
import { ApiError, errorMessage, request } from './client'

describe('API client', () => {
  it('returns parsed JSON for a successful response', async () => {
    server.use(http.get('*/api/thing', () => HttpResponse.json({ name: 'thing' })))

    await expect(request('/api/thing')).resolves.toEqual({ name: 'thing' })
  })

  it('returns undefined for a response with no content', async () => {
    server.use(http.post('*/api/auth/logout', () => new HttpResponse(null, { status: 204 })))

    await expect(request('/api/auth/logout', { method: 'POST' })).resolves.toBeUndefined()
  })

  it('turns a Problem Details response into an ApiError with its detail as the message', async () => {
    server.use(
      http.get('*/api/thing', () =>
        HttpResponse.json({ title: 'Document not found', status: 404, detail: 'No document with ID 42.' }, { status: 404 }),
      ),
    )

    const error = await request('/api/thing').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(404)
    expect(errorMessage(error)).toBe('No document with ID 42.')
  })

  it('still reports an error when the error response has no JSON body', async () => {
    server.use(http.get('*/api/thing', () => new HttpResponse('Bad Gateway', { status: 502 })))

    const error = await request('/api/thing').catch((e: unknown) => e)

    expect((error as ApiError).status).toBe(502)
    expect(errorMessage(error)).toBe('Request failed with status 502')
  })

  it('gives a friendly message when the server cannot be reached', async () => {
    server.use(http.get('*/api/thing', () => HttpResponse.error()))

    const error = await request('/api/thing').catch((e: unknown) => e)

    expect(errorMessage(error)).toBe('Something went wrong. Please check your connection and try again.')
  })
})
