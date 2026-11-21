/** Pulls a readable message out of an RFC 7807 ProblemDetail body, or falls back sensibly. */
export const readError = (e: unknown): string => {
    const err = e as {
        response?: { data?: { detail?: string; title?: string } }
        message?: string
    }
    return (
        err?.response?.data?.detail ??
        err?.response?.data?.title ??
        err?.message ??
        'Request failed'
    )
}
