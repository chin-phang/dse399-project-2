import ApiService from './ApiService'
import endpointConfig from '@/configs/endpoint.config'
import type {
    SignInCredential,
    SignUpCredential,
    ForgotPassword,
    ResetPassword,
    SignInResponse,
    SignUpResponse,
} from '@/@types/auth'

/** What POST /api/auth/login actually returns. */
type BackendLoginResponse = {
    accessToken: string
    refreshToken: string
    tokenType: string
    expiresInSeconds: number
    username: string
    role: string
}

/**
 * The template's auth contract is `{ token, user }`; the backend speaks
 * `{ accessToken, ..., username, role }`. Mapping here keeps the adaptation in one place
 * instead of spreading backend shapes through the UI.
 */
const toSignInResponse = (resp: BackendLoginResponse): SignInResponse => ({
    token: resp.accessToken,
    user: {
        userId: resp.username,
        userName: resp.username,
        authority: [resp.role],
        avatar: '',
        email: '',
    },
})

export async function apiSignIn(data: SignInCredential) {
    const resp = await ApiService.fetchDataWithAxios<BackendLoginResponse>({
        url: endpointConfig.signIn,
        method: 'post',
        data: { username: data.username, password: data.password },
    })
    return toSignInResponse(resp)
}

export async function apiSignUp(data: SignUpCredential) {
    await ApiService.fetchDataWithAxios<void>({
        url: endpointConfig.signUp,
        method: 'post',
        data: { username: data.userName, password: data.password },
    })
    // Register returns 201 with no body, so sign the new user straight in.
    return (await apiSignIn({
        username: data.userName,
        password: data.password,
    })) as SignUpResponse
}

export async function apiSignOut() {
    // Stateless JWT: there is nothing to invalidate server-side. The caller discards the token.
    return Promise.resolve()
}

export async function apiRefreshToken(refreshToken: string) {
    return ApiService.fetchDataWithAxios<BackendLoginResponse>({
        url: endpointConfig.refresh,
        method: 'post',
        data: { refreshToken },
    })
}

export async function apiForgotPassword<T>(data: ForgotPassword) {
    return ApiService.fetchDataWithAxios<T>({
        url: endpointConfig.forgotPassword,
        method: 'post',
        data,
    })
}

export async function apiResetPassword<T>(data: ResetPassword) {
    return ApiService.fetchDataWithAxios<T>({
        url: endpointConfig.resetPassword,
        method: 'post',
        data,
    })
}
