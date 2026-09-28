import { setupServer } from 'msw/node'

/** Intercepts fetch calls in tests. Each test registers the handlers it needs with server.use(). */
export const server = setupServer()
