/** Mirrors the backend DTOs in com.logistics.assistant.dto. */

export type VehicleStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE' | 'OUT_OF_SERVICE'
export type RouteStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type JobStatus = 'PENDING' | 'ASSIGNED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELLED'
export type JobPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export type Vehicle = {
    vehicleId: number
    licensePlate: string
    type: string
    capacity: number
    status: VehicleStatus
}

export type CreateVehicleRequest = {
    licensePlate: string
    type: string
    capacity: number
    status?: VehicleStatus
}

export type UpdateVehicleRequest = Partial<CreateVehicleRequest>

export type Job = {
    jobId: number
    routeId: number | null
    jobName: string
    deliveryLocation: string
    latitude: number | null
    longitude: number | null
    sequenceOrder: number | null
    priority: JobPriority
    status: JobStatus
}

export type CreateJobRequest = {
    jobName: string
    deliveryLocation: string
    latitude?: number | null
    longitude?: number | null
    priority?: JobPriority
    status?: JobStatus
}

export type UpdateJobRequest = Partial<CreateJobRequest>

export type Route = {
    routeId: number
    routeName: string
    routeStatus: RouteStatus
    generatedTime: string
    vehicleId: number | null
    jobs: Job[]
}

export type RouteSummary = {
    summaryId: number
    routeId: number
    totalDistance: number | null
    estimatedDuration: number | null
    totalJobs: number | null
    fuelConsumption: number | null
    generatedTime: string
}

export type PlanRouteRequest = {
    jobIds: number[]
    routeName?: string
    vehicleId?: number | null
}

/** The custom `data-toolCall` stream part the AI Chat tool panel renders. */
export type ToolCallEvent = {
    toolName: string
    arguments: Record<string, unknown>
    result: string | null
    error: string | null
}
