import { useEffect, useState } from 'react'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'
import Select from '@/components/ui/Select'
import Table from '@/components/ui/Table'
import Tag from '@/components/ui/Tag'
import Dialog from '@/components/ui/Dialog'
import Spinner from '@/components/ui/Spinner'
import Alert from '@/components/ui/Alert'
import ConfirmDialog from '@/components/shared/ConfirmDialog'
import { readError } from './shared/errors'
import {
    apiCreateJob,
    apiDeleteJob,
    apiGetJobs,
    apiUpdateJob,
} from '@/services/LogisticsService'
import type {
    CreateJobRequest,
    Job,
    JobPriority,
    JobStatus,
} from '@/@types/logistics'

const { Tr, Th, Td, THead, TBody } = Table

const PRIORITIES: JobPriority[] = ['LOW', 'MEDIUM', 'HIGH']
const STATUSES: JobStatus[] = [
    'PENDING',
    'ASSIGNED',
    'IN_TRANSIT',
    'DELIVERED',
    'CANCELLED',
]

const priorityColor: Record<JobPriority, string> = {
    LOW: 'bg-gray-200 dark:text-gray-900',
    MEDIUM: 'bg-blue-200 dark:text-gray-900',
    HIGH: 'bg-red-200 dark:text-gray-900',
}

const emptyForm: CreateJobRequest = {
    jobName: '',
    deliveryLocation: '',
    latitude: null,
    longitude: null,
    priority: 'MEDIUM',
}

/**
 * UC04 — delivery job management.
 *
 * Coordinates are optional throughout: a job created by the AI assistant arrives with only an
 * address, and this is where a planner attaches the pin later.
 */
const Jobs = () => {
    const [jobs, setJobs] = useState<Job[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [dialogOpen, setDialogOpen] = useState(false)
    const [editing, setEditing] = useState<Job | null>(null)
    const [form, setForm] = useState<CreateJobRequest>(emptyForm)
    const [deleting, setDeleting] = useState<Job | null>(null)

    const load = async () => {
        setLoading(true)
        setError(null)
        try {
            setJobs(await apiGetJobs())
        } catch (e) {
            setError(readError(e))
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        load()
    }, [])

    const openCreate = () => {
        setEditing(null)
        setForm(emptyForm)
        setDialogOpen(true)
    }

    const openEdit = (job: Job) => {
        setEditing(job)
        setForm({
            jobName: job.jobName,
            deliveryLocation: job.deliveryLocation,
            latitude: job.latitude,
            longitude: job.longitude,
            priority: job.priority,
            status: job.status,
        })
        setDialogOpen(true)
    }

    const save = async () => {
        setError(null)
        try {
            if (editing) {
                await apiUpdateJob(editing.jobId, form)
            } else {
                await apiCreateJob(form)
            }
            setDialogOpen(false)
            await load()
        } catch (e) {
            setError(readError(e))
        }
    }

    const confirmDelete = async () => {
        if (!deleting) {
            return
        }
        setError(null)
        try {
            await apiDeleteJob(deleting.jobId)
            setDeleting(null)
            await load()
        } catch (e) {
            setError(readError(e))
            setDeleting(null)
        }
    }

    const numberOrNull = (raw: string) => (raw === '' ? null : Number(raw))

    return (
        <Card>
            <div className="flex items-center justify-between mb-4">
                <div>
                    <h4>Delivery jobs</h4>
                    <p className="text-sm">
                        Jobs exist before they are planned onto a route
                    </p>
                </div>
                <Button variant="solid" onClick={openCreate}>
                    Add job
                </Button>
            </div>

            {error && (
                <Alert type="danger" showIcon className="mb-4">
                    {error}
                </Alert>
            )}

            {loading ? (
                <div className="flex justify-center py-10">
                    <Spinner size={40} />
                </div>
            ) : jobs.length === 0 ? (
                <p className="py-10 text-center">
                    No delivery jobs yet. Add one, or ask the AI assistant to create one.
                </p>
            ) : (
                <Table>
                    <THead>
                        <Tr>
                            <Th>Job</Th>
                            <Th>Delivery location</Th>
                            <Th>Coordinates</Th>
                            <Th>Priority</Th>
                            <Th>Status</Th>
                            <Th>Route</Th>
                            <Th />
                        </Tr>
                    </THead>
                    <TBody>
                        {jobs.map((job) => (
                            <Tr key={job.jobId}>
                                <Td className="font-semibold">{job.jobName}</Td>
                                <Td>{job.deliveryLocation}</Td>
                                <Td>
                                    {job.latitude != null && job.longitude != null ? (
                                        `${job.latitude.toFixed(4)}, ${job.longitude.toFixed(4)}`
                                    ) : (
                                        <span className="text-amber-600">
                                            Not geocoded
                                        </span>
                                    )}
                                </Td>
                                <Td>
                                    <Tag className={priorityColor[job.priority]}>
                                        {job.priority}
                                    </Tag>
                                </Td>
                                <Td>{job.status.replace(/_/g, ' ')}</Td>
                                <Td>{job.routeId ?? '—'}</Td>
                                <Td className="text-right">
                                    <Button
                                        size="xs"
                                        className="mr-2"
                                        onClick={() => openEdit(job)}
                                    >
                                        Edit
                                    </Button>
                                    <Button
                                        size="xs"
                                        onClick={() => setDeleting(job)}
                                    >
                                        Delete
                                    </Button>
                                </Td>
                            </Tr>
                        ))}
                    </TBody>
                </Table>
            )}

            <Dialog
                isOpen={dialogOpen}
                onClose={() => setDialogOpen(false)}
                onRequestClose={() => setDialogOpen(false)}
            >
                <h5 className="mb-4">{editing ? 'Edit job' : 'Add job'}</h5>
                <div className="flex flex-col gap-3">
                    <div>
                        <label className="form-label mb-1">Job name</label>
                        <Input
                            value={form.jobName}
                            onChange={(e) =>
                                setForm({ ...form, jobName: e.target.value })
                            }
                        />
                    </div>
                    <div>
                        <label className="form-label mb-1">Delivery location</label>
                        <Input
                            value={form.deliveryLocation}
                            onChange={(e) =>
                                setForm({
                                    ...form,
                                    deliveryLocation: e.target.value,
                                })
                            }
                        />
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="form-label mb-1">
                                Latitude (optional)
                            </label>
                            <Input
                                type="number"
                                value={form.latitude ?? ''}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        latitude: numberOrNull(e.target.value),
                                    })
                                }
                            />
                        </div>
                        <div>
                            <label className="form-label mb-1">
                                Longitude (optional)
                            </label>
                            <Input
                                type="number"
                                value={form.longitude ?? ''}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        longitude: numberOrNull(e.target.value),
                                    })
                                }
                            />
                        </div>
                    </div>
                    <p className="text-xs">
                        Coordinates are only needed for route ordering and distance
                        reporting. A job without them still saves.
                    </p>
                    <div>
                        <label className="form-label mb-1">Priority</label>
                        <Select
                            value={{
                                value: form.priority,
                                label: form.priority ?? 'MEDIUM',
                            }}
                            options={PRIORITIES.map((p) => ({
                                value: p,
                                label: p,
                            }))}
                            onChange={(option) =>
                                setForm({
                                    ...form,
                                    priority: (option?.value ??
                                        'MEDIUM') as JobPriority,
                                })
                            }
                        />
                    </div>
                    {editing && (
                        <div>
                            <label className="form-label mb-1">Status</label>
                            <Select
                                value={{
                                    value: form.status,
                                    label: (form.status ?? 'PENDING').replace(
                                        /_/g,
                                        ' ',
                                    ),
                                }}
                                options={STATUSES.map((s) => ({
                                    value: s,
                                    label: s.replace(/_/g, ' '),
                                }))}
                                onChange={(option) =>
                                    setForm({
                                        ...form,
                                        status: (option?.value ??
                                            'PENDING') as JobStatus,
                                    })
                                }
                            />
                        </div>
                    )}
                </div>
                <div className="text-right mt-6">
                    <Button className="mr-2" onClick={() => setDialogOpen(false)}>
                        Cancel
                    </Button>
                    <Button variant="solid" onClick={save}>
                        Save
                    </Button>
                </div>
            </Dialog>

            <ConfirmDialog
                isOpen={Boolean(deleting)}
                type="danger"
                title="Delete job"
                onClose={() => setDeleting(null)}
                onRequestClose={() => setDeleting(null)}
                onCancel={() => setDeleting(null)}
                onConfirm={confirmDelete}
            >
                <p>Delete job {deleting?.jobName}? This cannot be undone.</p>
            </ConfirmDialog>
        </Card>
    )
}

export default Jobs
