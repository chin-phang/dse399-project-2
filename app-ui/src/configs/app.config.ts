export type AppConfig = {
    apiPrefix: string
    authenticatedEntryPath: string
    unAuthenticatedEntryPath: string
    locale: string
    accessTokenPersistStrategy: 'localStorage' | 'sessionStorage' | 'cookies'
    enableMock: boolean
    activeNavTranslation: boolean
}

const appConfig: AppConfig = {
    // The Spring Boot API. Override per environment with VITE_API_BASE_URL.
    apiPrefix: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
    authenticatedEntryPath: '/dashboard',
    unAuthenticatedEntryPath: '/sign-in',
    locale: 'en',
    // Must not be 'cookies': the template's request interceptor only attaches the bearer token
    // for localStorage/sessionStorage, so a cookie strategy silently sends unauthenticated
    // requests to the JWT-protected API.
    accessTokenPersistStrategy: 'localStorage',
    // The template's axios-mock-adapter is off: every call goes to the real backend.
    enableMock: false,
    activeNavTranslation: false,
}

export default appConfig
