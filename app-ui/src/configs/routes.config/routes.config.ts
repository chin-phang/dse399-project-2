import { lazy } from 'react'
import authRoute from './authRoute'
import othersRoute from './othersRoute'
import type { Routes } from '@/@types/routes'

export const publicRoutes: Routes = [...authRoute]

export const protectedRoutes: Routes = [
    {
        key: 'dashboard',
        path: '/dashboard',
        component: lazy(() => import('@/views/logistics/Dashboard')),
        authority: [],
    },
    {
        key: 'aiChat',
        path: '/ai-chat',
        component: lazy(() => import('@/views/logistics/AiChat')),
        authority: [],
    },
    {
        key: 'jobs',
        path: '/jobs',
        component: lazy(() => import('@/views/logistics/Jobs')),
        authority: [],
    },
    {
        key: 'vehicles',
        path: '/vehicles',
        component: lazy(() => import('@/views/logistics/Vehicles')),
        authority: [],
    },
    // The template's landing route, kept so /home does not 404 for anything still linking to it.
    {
        key: 'home',
        path: '/home',
        component: lazy(() => import('@/views/logistics/Dashboard')),
        authority: [],
    },
    ...othersRoute,
]
