/** RFC 9457 Problem Details, as returned by the backend for every error. */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  /** Present on validation errors: one entry per invalid field, e.g. { field: 'lines[0].quantity', ... }. */
  errors?: { field: string; message: string }[]
}

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null) {
    super(problem?.detail ?? problem?.title ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  // An absolute URL works in the browser and in Node-based tests alike.
  const url = new URL(path, window.location.origin)
  const response = await fetch(url, {
    ...init,
    headers: { Accept: 'application/json', ...init.headers },
  })
  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function readProblem(response: Response): Promise<ProblemDetail | null> {
  try {
    return (await response.json()) as ProblemDetail
  } catch {
    return null
  }
}

/** A message that is safe and useful to show to the user. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }
  return 'Something went wrong. Please check your connection and try again.'
}
