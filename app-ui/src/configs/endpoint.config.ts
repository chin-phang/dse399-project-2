export const apiPrefix = '/api'

const endpointConfig = {
    signIn: '/auth/login',
    signUp: '/auth/register',
    refresh: '/auth/refresh',
    // The backend is stateless: signing out is discarding the token client-side, so there is no
    // server endpoint to call.
    signOut: '',
    forgotPassword: '/auth/forgot-password',
    resetPassword: '/auth/reset-password',
}

export default endpointConfig
