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
    apiCreateVehicle,
    apiDeleteVehicle,
    apiGetVehicles,
    apiUpdateVehicle,
} from '@/services/LogisticsService'
import type { CreateVehicleRequest, Vehicle, VehicleStatus } from '@/@types/logistics'

const { Tr, Th, Td, THead, TBody } = Table

const STATUSES: VehicleStatus[] = ['AVAILABLE', 'IN_USE', 'MAINTENANCE', 'OUT_OF_SERVICE']

const statusColor: Record<VehicleStatus, string> = {
    AVAILABLE: 'bg-emerald-200 dark:text-gray-900',
    IN_USE: 'bg-blue-200 dark:text-gray-900',
    MAINTENANCE: 'bg-amber-200 dark:text-gray-900',
    OUT_OF_SERVICE: 'bg-red-200 dark:text-gray-900',
}

const emptyForm: CreateVehicleRequest = {
    licensePlate: '',
    type: '',
    capacity: 0,
    status: 'AVAILABLE',
}

/** UC05 — vehicle management (full CRUD). */
const Vehicles = () => {
    const [vehicles, setVehicles] = useState<Vehicle[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [dialogOpen, setDialogOpen] = useState(false)
    const [editing, setEditing] = useState<Vehicle | null>(null)
    const [form, setForm] = useState<CreateVehicleRequest>(emptyForm)
    const [deleting, setDeleting] = useState<Vehicle | null>(null)

    const load = async () => {
        setLoading(true)
        setError(null)
        try {
            setVehicles(await apiGetVehicles())
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

    const openEdit = (vehicle: Vehicle) => {
        setEditing(vehicle)
        setForm({
            licensePlate: vehicle.licensePlate,
            type: vehicle.type,
            capacity: vehicle.capacity,
            status: vehicle.status,
        })
        setDialogOpen(true)
    }

    const save = async () => {
        setError(null)
        try {
            if (editing) {
                await apiUpdateVehicle(editing.vehicleId, form)
            } else {
                await apiCreateVehicle(form)
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
            await apiDeleteVehicle(deleting.vehicleId)
            setDeleting(null)
            await load()
        } catch (e) {
            setError(readError(e))
            setDeleting(null)
        }
    }

    return (
        <Card>
            <div className="flex items-center justify-between mb-4">
                <div>
                    <h4>Vehicles</h4>
                    <p className="text-sm">Fleet available for route assignment</p>
                </div>
                <Button variant="solid" onClick={openCreate}>
                    Add vehicle
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
            ) : vehicles.length === 0 ? (
                <p className="py-10 text-center">
                    No vehicles yet. Add one to get started.
                </p>
            ) : (
                <Table>
                    <THead>
                        <Tr>
                            <Th>Licence plate</Th>
                            <Th>Type</Th>
                            <Th>Capacity</Th>
                            <Th>Status</Th>
                            <Th />
                        </Tr>
                    </THead>
                    <TBody>
                        {vehicles.map((vehicle) => (
                            <Tr key={vehicle.vehicleId}>
                                <Td className="font-semibold">
                                    {vehicle.licensePlate}
                                </Td>
                                <Td>{vehicle.type}</Td>
                                <Td>{vehicle.capacity}</Td>
                                <Td>
                                    <Tag className={statusColor[vehicle.status]}>
                                        {vehicle.status.replace(/_/g, ' ')}
                                    </Tag>
                                </Td>
                                <Td className="text-right">
                                    <Button
                                        size="xs"
                                        className="mr-2"
                                        onClick={() => openEdit(vehicle)}
                                    >
                                        Edit
                                    </Button>
                                    <Button
                                        size="xs"
                                        onClick={() => setDeleting(vehicle)}
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
                <h5 className="mb-4">
                    {editing ? 'Edit vehicle' : 'Add vehicle'}
                </h5>
                <div className="flex flex-col gap-3">
                    <div>
                        <label className="form-label mb-1">Licence plate</label>
                        <Input
                            value={form.licensePlate}
                            onChange={(e) =>
                                setForm({ ...form, licensePlate: e.target.value })
                            }
                        />
                    </div>
                    <div>
                        <label className="form-label mb-1">Type</label>
                        <Input
                            value={form.type}
                            onChange={(e) =>
                                setForm({ ...form, type: e.target.value })
                            }
                        />
                    </div>
                    <div>
                        <label className="form-label mb-1">Capacity</label>
                        <Input
                            type="number"
                            value={form.capacity}
                            onChange={(e) =>
                                setForm({
                                    ...form,
                                    capacity: Number(e.target.value),
                                })
                            }
                        />
                    </div>
                    <div>
                        <label className="form-label mb-1">Status</label>
                        <Select
                            value={{
                                value: form.status,
                                label: (form.status ?? 'AVAILABLE').replace(/_/g, ' '),
                            }}
                            options={STATUSES.map((s) => ({
                                value: s,
                                label: s.replace(/_/g, ' '),
                            }))}
                            onChange={(option) =>
                                setForm({
                                    ...form,
                                    status: (option?.value ??
                                        'AVAILABLE') as VehicleStatus,
                                })
                            }
                        />
                    </div>
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
                title="Delete vehicle"
                onClose={() => setDeleting(null)}
                onRequestClose={() => setDeleting(null)}
                onCancel={() => setDeleting(null)}
                onConfirm={confirmDelete}
            >
                <p>
                    Delete vehicle {deleting?.licensePlate}? A vehicle still assigned
                    to a route cannot be deleted.
                </p>
            </ConfirmDialog>
        </Card>
    )
}

export default Vehicles
