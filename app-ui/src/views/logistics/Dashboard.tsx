import { useEffect, useState } from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Table from '@/components/ui/Table'
import Tag from '@/components/ui/Tag'
import Alert from '@/components/ui/Alert'
import Spinner from '@/components/ui/Spinner'
import { readError } from './shared/errors'
import {
    apiExportRouteCsv,
    apiGetJobs,
    apiGetRouteSummary,
    apiGetRoutes,
    apiGetVehicles,
    apiPlanRoute,
} from '@/services/LogisticsService'
import type { Job, Route, RouteSummary, Vehicle } from '@/@types/logistics'

const { Tr, Th, Td, THead, TBody } = Table

const Kpi = ({ label, value }: { label: string; value: string | number }) => (
    <Card>
        <p className="text-sm mb-1">{label}</p>
        <h3>{value}</h3>
    </Card>
)

const formatKm = (metres: number | null) =>
    metres == null ? '—' : `${(metres / 1000).toFixed(1)} km`

const formatDuration = (seconds: number | null) => {
    if (seconds == null) {
        return '—'
    }
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.round((seconds % 3600) / 60)
    return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`
}

/** Dashboard — fleet/job KPIs, route summaries (UC07) and CSV export (UC08). */
const Dashboard = () => {
    const [vehicles, setVehicles] = useState<Vehicle[]>([])
    const [jobs, setJobs] = useState<Job[]>([])
    const [routes, setRoutes] = useState<Route[]>([])
    const [summaries, setSummaries] = useState<Record<number, RouteSummary>>({})
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [planning, setPlanning] = useState(false)

    const load = async () => {
        setLoading(true)
        setError(null)
        try {
            const [v, j, r] = await Promise.all([
                apiGetVehicles(),
                apiGetJobs(),
                apiGetRoutes(),
            ])
            setVehicles(v)
            setJobs(j)
            setRoutes(r)
        } catch (e) {
            setError(readError(e))
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        load()
    }, [])

    const loadSummary = async (routeId: number) => {
        setError(null)
        try {
            const summary = await apiGetRouteSummary(routeId)
            setSummaries((prev) => ({ ...prev, [routeId]: summary }))
        } catch (e) {
            setError(readError(e))
        }
    }

    const exportCsv = async (routeId: number) => {
        setError(null)
        try {
            await apiExportRouteCsv(routeId)
        } catch (e) {
            setError(readError(e))
        }
    }

    /** UC06 from the dashboard: plan every unassigned job into one route. */
    const planUnassigned = async () => {
        const unassigned = jobs.filter((job) => job.routeId == null)
        if (unassigned.length === 0) {
            setError('There are no unassigned jobs to plan.')
            return
        }
        setPlanning(true)
        setError(null)
        try {
            await apiPlanRoute({ jobIds: unassigned.map((job) => job.jobId) })
            await load()
        } catch (e) {
            setError(readError(e))
        } finally {
            setPlanning(false)
        }
    }

    if (loading) {
        return (
            <div className="flex justify-center py-20">
                <Spinner size={40} />
            </div>
        )
    }

    const available = vehicles.filter((v) => v.status === 'AVAILABLE').length
    const pending = jobs.filter((j) => j.status === 'PENDING').length
    const ungeocoded = jobs.filter(
        (j) => j.latitude == null || j.longitude == null,
    ).length

    return (
        <div className="flex flex-col gap-4">
            <div>
                <h4>Dashboard</h4>
                <p className="text-sm">Fleet, jobs and generated routes at a glance</p>
            </div>

            {error && (
                <Alert type="danger" showIcon>
                    {error}
                </Alert>
            )}

            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                <Kpi label="Vehicles available" value={`${available} / ${vehicles.length}`} />
                <Kpi label="Jobs pending" value={pending} />
                <Kpi label="Routes planned" value={routes.length} />
                <Kpi label="Jobs without coordinates" value={ungeocoded} />
            </div>

            {ungeocoded > 0 && (
                <Alert type="warning" showIcon>
                    {ungeocoded} job(s) have no coordinates yet. They can still be planned,
                    but they are ordered last and excluded from the distance statistic until a
                    latitude and longitude are added in the Jobs module.
                </Alert>
            )}

            <Card>
                <div className="flex items-center justify-between mb-4">
                    <h5>Routes</h5>
                    <Button
                        variant="solid"
                        loading={planning}
                        onClick={planUnassigned}
                    >
                        Plan unassigned jobs
                    </Button>
                </div>

                {routes.length === 0 ? (
                    <p className="py-6 text-center">
                        No routes yet. Plan one here, or ask the AI assistant for a
                        recommendation.
                    </p>
                ) : (
                    <Table>
                        <THead>
                            <Tr>
                                <Th>Route</Th>
                                <Th>Status</Th>
                                <Th>Stops</Th>
                                <Th>Distance</Th>
                                <Th>Duration</Th>
                                <Th>Fuel</Th>
                                <Th />
                            </Tr>
                        </THead>
                        <TBody>
                            {routes.map((route) => {
                                const summary = summaries[route.routeId]
                                return (
                                    <Tr key={route.routeId}>
                                        <Td className="font-semibold">
                                            {route.routeName}
                                        </Td>
                                        <Td>
                                            <Tag className="bg-blue-200 dark:text-gray-900">
                                                {route.routeStatus.replace(/_/g, ' ')}
                                            </Tag>
                                        </Td>
                                        <Td>{route.jobs.length}</Td>
                                        <Td>{formatKm(summary?.totalDistance ?? null)}</Td>
                                        <Td>
                                            {formatDuration(
                                                summary?.estimatedDuration ?? null,
                                            )}
                                        </Td>
                                        <Td>
                                            {summary?.fuelConsumption != null
                                                ? `${summary.fuelConsumption.toFixed(1)} L`
                                                : '—'}
                                        </Td>
                                        <Td className="text-right">
                                            <Button
                                                size="xs"
                                                className="mr-2"
                                                onClick={() => loadSummary(route.routeId)}
                                            >
                                                Summary
                                            </Button>
                                            <Button
                                                size="xs"
                                                onClick={() => exportCsv(route.routeId)}
                                            >
                                                Export CSV
                                            </Button>
                                        </Td>
                                    </Tr>
                                )
                            })}
                        </TBody>
                    </Table>
                )}
            </Card>
        </div>
    )
}

export default Dashboard
