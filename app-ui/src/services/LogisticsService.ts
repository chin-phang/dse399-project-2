import ApiService from './ApiService'
import type {
    CreateJobRequest,
    CreateVehicleRequest,
    Job,
    JobStatus,
    PlanRouteRequest,
    Route,
    RouteSummary,
    UpdateJobRequest,
    UpdateVehicleRequest,
    Vehicle,
    VehicleStatus,
} from '@/@types/logistics'

/* Vehicles — UC05 */

export const apiGetVehicles = (status?: VehicleStatus) =>
    ApiService.fetchDataWithAxios<Vehicle[]>({
        url: '/vehicles',
        method: 'get',
        params: status ? { status } : undefined,
    })

export const apiGetVehicle = (id: number) =>
    ApiService.fetchDataWithAxios<Vehicle>({ url: `/vehicles/${id}`, method: 'get' })

export const apiCreateVehicle = (data: CreateVehicleRequest) =>
    ApiService.fetchDataWithAxios<Vehicle>({ url: '/vehicles', method: 'post', data })

export const apiUpdateVehicle = (id: number, data: UpdateVehicleRequest) =>
    ApiService.fetchDataWithAxios<Vehicle>({ url: `/vehicles/${id}`, method: 'put', data })

export const apiDeleteVehicle = (id: number) =>
    ApiService.fetchDataWithAxios<void>({ url: `/vehicles/${id}`, method: 'delete' })

/* Jobs — UC04 */

export const apiGetJobs = (params?: { status?: JobStatus; unassigned?: boolean }) =>
    ApiService.fetchDataWithAxios<Job[]>({ url: '/jobs', method: 'get', params })

export const apiGetJob = (id: number) =>
    ApiService.fetchDataWithAxios<Job>({ url: `/jobs/${id}`, method: 'get' })

export const apiCreateJob = (data: CreateJobRequest) =>
    ApiService.fetchDataWithAxios<Job>({ url: '/jobs', method: 'post', data })

export const apiUpdateJob = (id: number, data: UpdateJobRequest) =>
    ApiService.fetchDataWithAxios<Job>({ url: `/jobs/${id}`, method: 'put', data })

export const apiDeleteJob = (id: number) =>
    ApiService.fetchDataWithAxios<void>({ url: `/jobs/${id}`, method: 'delete' })

/* Routes, planning, summary and export — UC06/UC07/UC08 */

export const apiGetRoutes = () =>
    ApiService.fetchDataWithAxios<Route[]>({ url: '/routes', method: 'get' })

export const apiGetRoute = (id: number) =>
    ApiService.fetchDataWithAxios<Route>({ url: `/routes/${id}`, method: 'get' })

export const apiPlanRoute = (data: PlanRouteRequest) =>
    ApiService.fetchDataWithAxios<Route>({ url: '/routes/plan', method: 'post', data })

export const apiOptimiseRoute = (id: number) =>
    ApiService.fetchDataWithAxios<Route>({ url: `/routes/${id}/optimise`, method: 'post' })

export const apiGetRouteSummary = (id: number) =>
    ApiService.fetchDataWithAxios<RouteSummary>({ url: `/routes/${id}/summary`, method: 'get' })

export const apiDeleteRoute = (id: number) =>
    ApiService.fetchDataWithAxios<void>({ url: `/routes/${id}`, method: 'delete' })

/**
 * CSV export. Requested as a blob and saved client-side, so the bearer token still travels on
 * the request — a plain `<a href>` could not carry it.
 */
export const apiExportRouteCsv = async (id: number) => {
    const blob = await ApiService.fetchDataWithAxios<Blob>({
        url: `/routes/${id}/export/csv`,
        method: 'get',
        responseType: 'blob',
    })
    const url = window.URL.createObjectURL(new Blob([blob], { type: 'text/csv' }))
    const link = document.createElement('a')
    link.href = url
    link.download = `route-${id}-report.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
}
