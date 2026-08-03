import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import QRCode from 'qrcode'
import {
  Banknote,
  CalendarClock,
  Camera,
  CheckCircle2,
  ClipboardCheck,
  Download,
  Edit3,
  ExternalLink,
  Eye,
  FileText,
  Fish,
  Grip,
  Hammer,
  LayoutDashboard,
  Plus,
  QrCode,
  RefreshCw,
  Save,
  Settings,
  Trash2,
  Upload,
  Users
} from 'lucide-react'
import { api, downloadBlob, normalizePage } from './api'
import { useMerchant } from './merchantContext'
import { Badge, Button, Card, ConfirmButton, EmptyState, Field, Input, LoadingBlock, Modal, Select, Table, Td, Textarea, Th, cn } from './ui'
import {
  boardStatusInfo,
  exportCsv,
  formatCent,
  formatDate,
  formatDateTime,
  formatTime,
  formatYuan,
  imageFromFile,
  nowDateTimeLocal,
  parseMoneyToCent,
  pondCategoryText,
  spotStatusText,
  statusText,
  todayString,
  tomorrowString,
  toDateTimeLocal,
  toServerDateTime,
  toTimeSeconds
} from './helpers'
import type {
  Announcement,
  BoardSpot,
  CatchRecord,
  CheckinResult,
  EventItem,
  EventReview,
  EventSignup,
  FishingSpot,
  OperationLog,
  Pond,
  Reservation,
  RestaurantMenu,
  RestaurantOrder,
  RevenueItem,
  RevenueSummary,
  ShopProduct,
  Staff,
  TimeSlot
} from './types'

type SaveState = 'idle' | 'saving'

const getError = (error: unknown) => (error instanceof Error ? error.message : '操作失败')

function ErrorBanner({ error }: { error?: string | null }) {
  if (!error) return null
  return <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{error}</div>
}

function RequirePond({ children }: { children: React.ReactNode }) {
  const { currentPondId } = useMerchant()
  if (!currentPondId) {
    return <EmptyState title="请先创建鱼塘" description="商家端业务数据都需要绑定鱼塘，先到鱼塘管理新增一个鱼塘。" />
  }
  return <>{children}</>
}

const actionCell = 'flex flex-wrap items-center gap-2'

function fallbackPosition(index: number, total: number) {
  const columns = Math.ceil(Math.sqrt(total || 1))
  const row = Math.floor(index / columns)
  const col = index % columns
  return {
    x: 12 + (col * 76) / Math.max(columns - 1, 1),
    y: 14 + (row * 72) / Math.max(Math.ceil(total / columns) - 1, 1)
  }
}

function spotCoords(spot: FishingSpot | BoardSpot, index: number, total: number) {
  const fallback = fallbackPosition(index, total)
  return {
    x: Number(spot.coordinateX ?? fallback.x),
    y: Number(spot.coordinateY ?? fallback.y)
  }
}

function StatusBadge({ status }: { status?: string | number | null }) {
  const text = statusText(status)
  let tone: 'slate' | 'green' | 'amber' | 'red' | 'blue' = 'slate'
  if (['正常', '上架', '已发布', '已完成', '已通过', '已核销', '已抽号'].includes(text)) tone = 'green'
  if (['待抽号', '待处理', '制作中', '草稿'].includes(text)) tone = 'amber'
  if (['禁用', '已拒绝', '已取消', '预约取消', '过期失效'].includes(text)) tone = 'red'
  if (['已送达'].includes(text)) tone = 'blue'
  return <Badge tone={tone}>{text}</Badge>
}

export function DashboardPage() {
  const { currentPondId, currentPond } = useMerchant()
  const [stats, setStats] = useState<RevenueSummary & Record<string, any>>({})
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [freeSpots, setFreeSpots] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [statsData, reservationPage, spotPage] = await Promise.all([
        api.dashboardStats(currentPondId),
        currentPondId ? api.reservations({ pondId: currentPondId, pageNum: 1, pageSize: 5 }) : Promise.resolve({ records: [], total: 0 }),
        currentPondId ? api.spots({ pondId: currentPondId, pageNum: 1, pageSize: 1000 }) : Promise.resolve({ records: [], total: 0 })
      ])
      setStats(statsData || {})
      setReservations(normalizePage(reservationPage).records)
      setFreeSpots(normalizePage(spotPage).records.filter((item) => item.status === 1).length)
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [currentPondId])

  useEffect(() => {
    load()
  }, [load])

  const pendingCheckins = reservations.filter((item) => item.status === '已抽号').length
  const cards = [
    { label: '今日预约数', value: stats.todayReservationCount || 0, icon: <CalendarClock size={20} />, tone: 'bg-emerald-50 text-emerald-700' },
    { label: '今日收入', value: formatYuan(stats.todayIncome || 0), icon: <Banknote size={20} />, tone: 'bg-blue-50 text-blue-700' },
    { label: '待核销人数', value: pendingCheckins || Math.max(Number(stats.todayReservationCount || 0) - Number(stats.todayCheckinCount || 0), 0), icon: <ClipboardCheck size={20} />, tone: 'bg-amber-50 text-amber-700' },
    { label: '空闲钓位数', value: freeSpots, icon: <Fish size={20} />, tone: 'bg-slate-100 text-slate-700' }
  ]

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">工作台</h1>
          <p className="mt-2 text-sm text-slate-500">{currentPond ? `${currentPond.name} 今日经营概览` : '当前商家的经营概览'}</p>
        </div>
        <Button variant="secondary" icon={<RefreshCw size={16} />} onClick={load}>刷新</Button>
      </div>
      <ErrorBanner error={error} />
      {loading ? <LoadingBlock /> : null}
      {!loading ? (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {cards.map((card) => (
              <Card key={card.label} className="p-5">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-sm font-semibold text-slate-500">{card.label}</p>
                    <p className="mt-3 text-3xl font-bold text-slate-950">{card.value}</p>
                  </div>
                  <span className={cn('rounded-md p-2', card.tone)}>{card.icon}</span>
                </div>
              </Card>
            ))}
          </div>

          <div className="mt-5 grid gap-5 xl:grid-cols-[1fr_360px]">
            <Card className="p-5">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="font-bold text-slate-950">近期预约</h2>
                <Link className="text-sm font-semibold text-emerald-700 hover:text-emerald-800" to="/merchant/orders">查看全部</Link>
              </div>
              {reservations.length === 0 ? <EmptyState title="暂无预约" /> : (
                <Table>
                  <table className="w-full min-w-[720px]">
                    <thead>
                      <tr>
                        <Th>用户</Th>
                        <Th>鱼塘</Th>
                        <Th>日期/场次</Th>
                        <Th>钓位</Th>
                        <Th>状态</Th>
                        <Th>创建时间</Th>
                      </tr>
                    </thead>
                    <tbody>
                      {reservations.map((item) => (
                        <tr key={item.id}>
                          <Td>
                            <div className="font-semibold text-slate-900">{item.userNickname || '-'}</div>
                            <div className="text-xs text-slate-400">{item.userPhone || '-'}</div>
                          </Td>
                          <Td>{item.pondName || currentPond?.name || '-'}</Td>
                          <Td>{formatDate(item.slotDate)} {item.slotName}</Td>
                          <Td>{item.spotCode || '-'}</Td>
                          <Td><StatusBadge status={item.status} /></Td>
                          <Td>{formatDateTime(item.createTime)}</Td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </Table>
              )}
            </Card>

            <Card className="p-5">
              <h2 className="mb-4 font-bold text-slate-950">快捷入口</h2>
              <div className="grid gap-3">
                <Link to="/merchant/checkin" className="flex items-center justify-between rounded-md border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 hover:border-emerald-300 hover:bg-emerald-50">
                  <span className="flex items-center gap-2"><ClipboardCheck size={18} />核销签到</span>
                  <ExternalLink size={15} />
                </Link>
                <Link to="/merchant/pond-board" className="flex items-center justify-between rounded-md border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 hover:border-emerald-300 hover:bg-emerald-50">
                  <span className="flex items-center gap-2"><LayoutDashboard size={18} />鱼塘看板</span>
                  <ExternalLink size={15} />
                </Link>
                <Link to="/merchant/sessions" className="flex items-center justify-between rounded-md border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 hover:border-emerald-300 hover:bg-emerald-50">
                  <span className="flex items-center gap-2"><CalendarClock size={18} />发布场次</span>
                  <ExternalLink size={15} />
                </Link>
              </div>
            </Card>
          </div>
        </>
      ) : null}
    </div>
  )
}

const emptyPondForm: Partial<Pond> = {
  name: '',
  category: 'pond',
  address: '',
  phone: '',
  latitude: undefined,
  longitude: undefined,
  coverImage: '',
  floorPlanUrl: '',
  bookingNotice: '',
  cancelRule: '',
  refundRule: '',
  status: 1
}

export function PondsPage() {
  const { ponds, reloadPonds } = useMerchant()
  const [form, setForm] = useState<Partial<Pond>>(emptyPondForm)
  const [editing, setEditing] = useState<Pond | null>(null)
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState<SaveState>('idle')
  const [error, setError] = useState('')

  const openCreate = () => {
    setEditing(null)
    setForm(emptyPondForm)
    setOpen(true)
  }

  const openEdit = (pond: Pond) => {
    setEditing(pond)
    setForm({ ...emptyPondForm, ...pond })
    setOpen(true)
  }

  const save = async () => {
    setSaving('saving')
    setError('')
    try {
      const latitude = form.latitude === undefined || form.latitude === null || String(form.latitude) === '' ? undefined : Number(form.latitude)
      const longitude = form.longitude === undefined || form.longitude === null || String(form.longitude) === '' ? undefined : Number(form.longitude)
      const payload = {
        ...form,
        latitude,
        longitude,
        status: Number(form.status ?? 1)
      }
      if (editing) await api.updatePond(editing.id, payload)
      else await api.createPond(payload)
      setOpen(false)
      await reloadPonds()
    } catch (err) {
      setError(getError(err))
    } finally {
      setSaving('idle')
    }
  }

  const updateStatus = async (pond: Pond) => {
    await api.updatePond(pond.id, { ...pond, status: pond.status === 1 ? 0 : 1 })
    await reloadPonds()
  }

  const share = (pond: Pond) => {
    const pcBase = window.location.origin.replace(':3001', ':3002')
    const url = `${pcBase}/share?pondId=${pond.id}`
    navigator.clipboard?.writeText(url).catch(() => undefined)
    window.open(url, '_blank')
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">鱼塘管理</h1>
          <p className="mt-2 text-sm text-slate-500">维护鱼塘资料、规则、封面和平面图，后续场次和钓位都会绑定到鱼塘。</p>
        </div>
        <Button icon={<Plus size={16} />} onClick={openCreate}>新增鱼塘</Button>
      </div>

      {ponds.length === 0 ? <EmptyState title="暂无鱼塘" description="点击新增鱼塘开始配置商家资料。" /> : (
        <div className="grid gap-4 xl:grid-cols-2">
          {ponds.map((pond) => (
            <Card key={pond.id} className="overflow-hidden">
              <div className="flex gap-4 p-4">
                <div className="h-28 w-36 shrink-0 overflow-hidden rounded-md bg-slate-100">
                  {pond.coverImage ? <img src={pond.coverImage} alt={pond.name} className="h-full w-full object-cover" /> : (
                    <div className="flex h-full w-full items-center justify-center text-slate-300"><Fish size={34} /></div>
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="truncate text-lg font-bold text-slate-950">{pond.name}</h2>
                    <Badge tone={pond.status === 1 ? 'green' : 'red'}>{pond.status === 1 ? '启用' : '停用'}</Badge>
                    <Badge>{pondCategoryText(pond.category)}</Badge>
                  </div>
                  <p className="mt-2 line-clamp-2 text-sm text-slate-500">{pond.address || '未填写地址'}</p>
                  <p className="mt-1 text-sm text-slate-500">{pond.phone || '未填写电话'}</p>
                  <div className="mt-4 flex flex-wrap gap-2">
                    <Button size="sm" variant="secondary" icon={<Edit3 size={14} />} onClick={() => openEdit(pond)}>编辑</Button>
                    <Link to={`/merchant/ponds/${pond.id}/spots`}>
                      <Button size="sm" variant="secondary" icon={<Grip size={14} />}>钓位编辑</Button>
                    </Link>
                    <Button size="sm" variant="secondary" icon={<ExternalLink size={14} />} onClick={() => share(pond)}>分享预览</Button>
                    <Button size="sm" variant={pond.status === 1 ? 'danger' : 'success'} onClick={() => updateStatus(pond)}>{pond.status === 1 ? '停用' : '启用'}</Button>
                    <ConfirmButton size="sm" variant="danger" message="确定删除这个鱼塘吗？" onConfirm={async () => { await api.deletePond(pond.id); await reloadPonds() }}>删除</ConfirmButton>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal
        title={editing ? '编辑鱼塘' : '新增鱼塘'}
        open={open}
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setOpen(false)}>取消</Button>
            <Button icon={<Save size={16} />} disabled={saving === 'saving'} onClick={save}>{saving === 'saving' ? '保存中' : '保存'}</Button>
          </>
        }
      >
        <ErrorBanner error={error} />
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="名称"><Input value={form.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
          <Field label="分类">
            <Select value={form.category || 'pond'} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              <option value="lake">湖库</option>
              <option value="river">江河</option>
              <option value="pond">鱼塘</option>
              <option value="sea">海区</option>
            </Select>
          </Field>
          <Field label="地址" className="md:col-span-2"><Input value={form.address || ''} onChange={(e) => setForm({ ...form, address: e.target.value })} /></Field>
          <Field label="电话"><Input value={form.phone || ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></Field>
          <Field label="状态">
            <Select value={String(form.status ?? 1)} onChange={(e) => setForm({ ...form, status: Number(e.target.value) })}>
              <option value="1">启用</option>
              <option value="0">停用</option>
            </Select>
          </Field>
          <Field label="纬度"><Input value={form.latitude ?? ''} onChange={(e) => setForm({ ...form, latitude: e.target.value === '' ? null : Number(e.target.value) })} /></Field>
          <Field label="经度"><Input value={form.longitude ?? ''} onChange={(e) => setForm({ ...form, longitude: e.target.value === '' ? null : Number(e.target.value) })} /></Field>
          <Field label="封面图 URL" className="md:col-span-2">
            <div className="flex gap-2">
              <Input value={form.coverImage || ''} onChange={(e) => setForm({ ...form, coverImage: e.target.value })} />
              <label className="inline-flex h-10 cursor-pointer items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50">
                <Upload size={15} />上传
                <input className="hidden" type="file" accept="image/*" onChange={async (e) => {
                  const file = e.target.files?.[0]
                  if (file) setForm({ ...form, coverImage: await imageFromFile(file) })
                }} />
              </label>
            </div>
          </Field>
          <Field label="平面图 URL" className="md:col-span-2"><Input value={form.floorPlanUrl || ''} onChange={(e) => setForm({ ...form, floorPlanUrl: e.target.value })} /></Field>
          <Field label="预约须知" className="md:col-span-2"><Textarea value={form.bookingNotice || ''} onChange={(e) => setForm({ ...form, bookingNotice: e.target.value })} /></Field>
          <Field label="取消规则"><Textarea value={form.cancelRule || ''} onChange={(e) => setForm({ ...form, cancelRule: e.target.value })} /></Field>
          <Field label="退款规则"><Textarea value={form.refundRule || ''} onChange={(e) => setForm({ ...form, refundRule: e.target.value })} /></Field>
        </div>
      </Modal>
    </div>
  )
}

export function SpotEditorPage() {
  const { id } = useParams()
  const pondId = Number(id)
  const [pond, setPond] = useState<Pond | null>(null)
  const [spots, setSpots] = useState<FishingSpot[]>([])
  const [editMode, setEditMode] = useState(true)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [savingId, setSavingId] = useState<number | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [actionSpot, setActionSpot] = useState<FishingSpot | null>(null)
  const [spotForm, setSpotForm] = useState<Partial<FishingSpot>>({ spotCode: '', status: 1 })
  const mapRef = useRef<HTMLDivElement | null>(null)
  const dragRef = useRef<{ spot: FishingSpot; active: boolean; timer?: number; moved: boolean } | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [pondData, spotPage] = await Promise.all([
        api.pond(pondId),
        api.spots({ pondId, pageNum: 1, pageSize: 1000 })
      ])
      setPond(pondData)
      setSpots(normalizePage(spotPage).records)
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [pondId])

  useEffect(() => {
    load()
  }, [load])

  const saveSpot = async (spot: FishingSpot, patch: Partial<FishingSpot>) => {
    setSavingId(spot.id)
    try {
      const payload = { ...spot, ...patch, pondId }
      await api.updateSpot(spot.id, payload)
      setSpots((items) => items.map((item) => (item.id === spot.id ? { ...item, ...patch } : item)))
    } catch (err) {
      setError(getError(err))
      await load()
    } finally {
      setSavingId(null)
    }
  }

  const createSpot = async () => {
    if (!spotForm.spotCode) {
      setError('请输入钓位编号')
      return
    }
    try {
      await api.createSpot({ ...spotForm, pondId, status: Number(spotForm.status ?? 1) })
      setCreateOpen(false)
      setSpotForm({ spotCode: '', status: 1 })
      await load()
    } catch (err) {
      setError(getError(err))
    }
  }

  const openCreateAt = (event: React.MouseEvent<HTMLDivElement>) => {
    if (!editMode || !mapRef.current || event.target !== event.currentTarget) return
    const rect = mapRef.current.getBoundingClientRect()
    setSpotForm({
      spotCode: '',
      status: 1,
      coordinateX: Number((((event.clientX - rect.left) / rect.width) * 100).toFixed(2)),
      coordinateY: Number((((event.clientY - rect.top) / rect.height) * 100).toFixed(2))
    })
    setCreateOpen(true)
  }

  const uploadPlan = async (file?: File) => {
    if (!file || !pond) return
    const floorPlanUrl = await imageFromFile(file)
    await api.updatePond(pond.id, { ...pond, floorPlanUrl })
    setPond({ ...pond, floorPlanUrl })
  }

  const pointerDown = (event: React.PointerEvent<HTMLButtonElement>, spot: FishingSpot) => {
    event.stopPropagation()
    if (!editMode) {
      setActionSpot(spot)
      return
    }
    ;(event.currentTarget as HTMLElement).setPointerCapture(event.pointerId)
    const next = { spot, active: false, moved: false, timer: window.setTimeout(() => {
      if (dragRef.current) dragRef.current.active = true
    }, 220) }
    dragRef.current = next
  }

  const pointerMove = (event: React.PointerEvent<HTMLButtonElement>) => {
    const drag = dragRef.current
    if (!drag || !mapRef.current) return
    if (!drag.active) return
    event.preventDefault()
    const rect = mapRef.current.getBoundingClientRect()
    const coordinateX = Math.max(0, Math.min(100, ((event.clientX - rect.left) / rect.width) * 100))
    const coordinateY = Math.max(0, Math.min(100, ((event.clientY - rect.top) / rect.height) * 100))
    drag.moved = true
    setSpots((items) =>
      items.map((item) =>
        item.id === drag.spot.id ? { ...item, coordinateX: Number(coordinateX.toFixed(2)), coordinateY: Number(coordinateY.toFixed(2)) } : item
      )
    )
  }

  const pointerUp = async (event: React.PointerEvent<HTMLButtonElement>, spot: FishingSpot) => {
    event.stopPropagation()
    const drag = dragRef.current
    if (!drag) {
      setActionSpot(spot)
      return
    }
    window.clearTimeout(drag.timer)
    dragRef.current = null
    const latest = spots.find((item) => item.id === drag.spot.id) || spot
    if (drag.moved) {
      await saveSpot(spot, { coordinateX: latest.coordinateX, coordinateY: latest.coordinateY })
    } else {
      setActionSpot(spot)
    }
  }

  const updateActionSpot = async () => {
    if (!actionSpot) return
    await saveSpot(actionSpot, {
      spotCode: spotForm.spotCode || actionSpot.spotCode,
      status: Number(spotForm.status ?? actionSpot.status)
    })
    setActionSpot(null)
  }

  useEffect(() => {
    if (actionSpot) setSpotForm(actionSpot)
  }, [actionSpot])

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">钓位可视化编辑器</h1>
          <p className="mt-2 text-sm text-slate-500">{pond?.name || '鱼塘'} 平面图、钓位坐标和钓位状态实时维护。</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <label className="inline-flex h-10 cursor-pointer items-center gap-2 rounded-md border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50">
            <Upload size={16} />上传平面图
            <input className="hidden" type="file" accept="image/*" onChange={(e) => uploadPlan(e.target.files?.[0])} />
          </label>
          <Button variant={editMode ? 'primary' : 'secondary'} icon={<Settings size={16} />} onClick={() => setEditMode((v) => !v)}>{editMode ? '编辑模式开启' : '编辑模式关闭'}</Button>
          <Button variant="secondary" icon={<RefreshCw size={16} />} onClick={load}>刷新</Button>
        </div>
      </div>
      <ErrorBanner error={error} />
      {loading ? <LoadingBlock /> : (
        <div className="grid gap-5 xl:grid-cols-[1fr_360px]">
          <Card className="p-4">
            <div
              ref={mapRef}
              className="relative min-h-[520px] overflow-hidden rounded-lg border border-slate-200 bg-slate-100"
              onClick={openCreateAt}
            >
              {pond?.floorPlanUrl ? <img src={pond.floorPlanUrl} alt="鱼塘平面图" className="pointer-events-none absolute inset-0 h-full w-full object-cover" /> : (
                <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(90deg,rgba(15,23,42,0.08)_1px,transparent_1px),linear-gradient(rgba(15,23,42,0.08)_1px,transparent_1px)] bg-[size:40px_40px]" />
              )}
              <div className="pointer-events-none absolute inset-0 bg-white/10" />
              {spots.map((spot, index) => {
                const pos = spotCoords(spot, index, spots.length)
                return (
                  <button
                    key={spot.id}
                    className={cn(
                      'absolute z-10 flex h-9 min-w-9 -translate-x-1/2 -translate-y-1/2 touch-none items-center justify-center rounded-full px-2 text-xs font-bold text-white shadow-lg ring-4 transition',
                      spot.status === 1 ? 'bg-emerald-600 ring-emerald-100' : spot.status === 2 ? 'bg-slate-500 ring-slate-200' : 'bg-red-600 ring-red-100',
                      editMode && 'cursor-grab active:cursor-grabbing',
                      savingId === spot.id && 'opacity-60'
                    )}
                    style={{ left: `${pos.x}%`, top: `${pos.y}%` }}
                    onPointerDown={(event) => pointerDown(event, spot)}
                    onPointerMove={pointerMove}
                    onPointerUp={(event) => pointerUp(event, spot)}
                    title={`${spot.spotCode} ${spotStatusText(spot.status)}`}
                  >
                    {spot.spotCode}
                  </button>
                )
              })}
              {spots.length === 0 ? (
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="rounded-md bg-white/90 px-4 py-3 text-sm font-semibold text-slate-500 shadow">开启编辑模式后点击空白处新增钓位</div>
                </div>
              ) : null}
            </div>
          </Card>

          <Card className="p-4">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="font-bold text-slate-950">钓位列表</h2>
              <Badge>{spots.length} 个</Badge>
            </div>
            <div className="max-h-[620px] space-y-3 overflow-y-auto pr-1">
              {spots.map((spot, index) => {
                const pos = spotCoords(spot, index, spots.length)
                return (
                  <div key={spot.id} className="rounded-md border border-slate-200 p-3">
                    <div className="mb-3 flex items-center justify-between">
                      <div className="font-bold text-slate-900">{spot.spotCode}</div>
                      <Badge tone={spot.status === 1 ? 'green' : spot.status === 2 ? 'slate' : 'red'}>{spotStatusText(spot.status)}</Badge>
                    </div>
                    <div className="grid grid-cols-2 gap-2">
                      <Field label="X 坐标">
                        <Input type="number" value={Number(pos.x).toFixed(2)} onChange={(e) => setSpots((items) => items.map((item) => item.id === spot.id ? { ...item, coordinateX: Number(e.target.value) } : item))} onBlur={() => saveSpot(spot, { coordinateX: spot.coordinateX })} />
                      </Field>
                      <Field label="Y 坐标">
                        <Input type="number" value={Number(pos.y).toFixed(2)} onChange={(e) => setSpots((items) => items.map((item) => item.id === spot.id ? { ...item, coordinateY: Number(e.target.value) } : item))} onBlur={() => saveSpot(spot, { coordinateY: spot.coordinateY })} />
                      </Field>
                    </div>
                  </div>
                )
              })}
            </div>
          </Card>
        </div>
      )}

      <Modal
        title="新增钓位"
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        width="max-w-md"
        footer={<><Button variant="secondary" onClick={() => setCreateOpen(false)}>取消</Button><Button onClick={createSpot}>保存钓位</Button></>}
      >
        <div className="grid gap-4">
          <Field label="钓位编号"><Input value={spotForm.spotCode || ''} onChange={(e) => setSpotForm({ ...spotForm, spotCode: e.target.value })} /></Field>
          <Field label="状态">
            <Select value={String(spotForm.status ?? 1)} onChange={(e) => setSpotForm({ ...spotForm, status: Number(e.target.value) })}>
              <option value="1">可用</option>
              <option value="0">维修</option>
              <option value="2">禁用</option>
            </Select>
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="X 坐标"><Input type="number" value={spotForm.coordinateX ?? ''} onChange={(e) => setSpotForm({ ...spotForm, coordinateX: Number(e.target.value) })} /></Field>
            <Field label="Y 坐标"><Input type="number" value={spotForm.coordinateY ?? ''} onChange={(e) => setSpotForm({ ...spotForm, coordinateY: Number(e.target.value) })} /></Field>
          </div>
        </div>
      </Modal>

      <Modal
        title={actionSpot ? `钓位 ${actionSpot.spotCode}` : '钓位操作'}
        open={Boolean(actionSpot)}
        onClose={() => setActionSpot(null)}
        width="max-w-md"
        footer={
          <>
            <Button variant="secondary" onClick={() => setActionSpot(null)}>取消</Button>
            <ConfirmButton variant="danger" message="确定删除这个钓位吗？" onConfirm={async () => {
              if (!actionSpot) return
              await api.deleteSpot(actionSpot.id)
              setActionSpot(null)
              await load()
            }}>删除</ConfirmButton>
            <Button onClick={updateActionSpot}>保存</Button>
          </>
        }
      >
        <div className="grid gap-4">
          <Field label="钓位编号"><Input value={spotForm.spotCode || ''} onChange={(e) => setSpotForm({ ...spotForm, spotCode: e.target.value })} /></Field>
          <Field label="状态">
            <Select value={String(spotForm.status ?? 1)} onChange={(e) => setSpotForm({ ...spotForm, status: Number(e.target.value) })}>
              <option value="1">可用</option>
              <option value="0">维修</option>
              <option value="2">禁用</option>
            </Select>
          </Field>
          <p className="text-xs leading-5 text-slate-500">在编辑模式下长按钓位点拖拽，松手会自动保存坐标。</p>
        </div>
      </Modal>
    </div>
  )
}

export function PondBoardPage() {
  const { ponds, currentPondId, setCurrentPondId } = useMerchant()
  const [date, setDate] = useState(todayString())
  const [sessions, setSessions] = useState<TimeSlot[]>([])
  const [slotId, setSlotId] = useState<number | undefined>()
  const [spots, setSpots] = useState<BoardSpot[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [detail, setDetail] = useState<BoardSpot | null>(null)
  const selectedPond = ponds.find((pond) => pond.id === currentPondId)

  const loadSessions = useCallback(async () => {
    if (!currentPondId) return
    try {
      const page = await api.sessions({ pondId: currentPondId, pageNum: 1, pageSize: 500 })
      const records = normalizePage(page).records.filter((item) => !date || item.slotDate === date)
      setSessions(records)
      setSlotId((current) => current && records.some((item) => item.id === current) ? current : records[0]?.id)
    } catch (err) {
      setError(getError(err))
    }
  }, [currentPondId, date])

  const loadBoard = useCallback(async () => {
    if (!currentPondId || !slotId) {
      setSpots([])
      return
    }
    setLoading(true)
    setError('')
    try {
      setSpots(await api.board({ pondId: currentPondId, slotId, date }))
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [currentPondId, slotId, date])

  useEffect(() => {
    loadSessions()
  }, [loadSessions])

  useEffect(() => {
    loadBoard()
  }, [loadBoard])

  useEffect(() => {
    if (!autoRefresh) return undefined
    const timer = window.setInterval(loadBoard, 5000)
    return () => window.clearInterval(timer)
  }, [autoRefresh, loadBoard])

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">鱼塘动态看板</h1>
          <p className="mt-2 text-sm text-slate-500">按鱼塘、日期和场次查看实时钓位状态，支持 5 秒自动刷新。</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant={autoRefresh ? 'primary' : 'secondary'} icon={<RefreshCw size={16} />} onClick={() => setAutoRefresh((value) => !value)}>{autoRefresh ? '自动刷新中' : '开启自动刷新'}</Button>
          <Button variant="secondary" icon={<RefreshCw size={16} />} onClick={loadBoard}>手动刷新</Button>
        </div>
      </div>
      <ErrorBanner error={error} />
      <Card className="mb-5 p-4">
        <div className="grid gap-3 md:grid-cols-3">
          <Field label="鱼塘">
            <Select value={currentPondId || ''} onChange={(e) => setCurrentPondId(Number(e.target.value))}>
              {ponds.map((pond) => <option key={pond.id} value={pond.id}>{pond.name}</option>)}
            </Select>
          </Field>
          <Field label="日期"><Input type="date" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
          <Field label="时段">
            <Select value={slotId || ''} onChange={(e) => setSlotId(Number(e.target.value))}>
              {sessions.map((session) => <option key={session.id} value={session.id}>{session.slotName} {formatTime(session.startTime)}-{formatTime(session.endTime)}</option>)}
            </Select>
          </Field>
        </div>
      </Card>

      <RequirePond>
        {sessions.length === 0 ? <EmptyState title="当前日期暂无场次" description="请先到场次管理发布对应日期的场次。" /> : (
          <div className="grid gap-5 xl:grid-cols-[1fr_280px]">
            <Card className="p-4">
              <div className="relative min-h-[620px] overflow-hidden rounded-lg border border-slate-200 bg-slate-100">
                {selectedPond?.floorPlanUrl ? <img src={selectedPond.floorPlanUrl} alt="鱼塘平面图" className="pointer-events-none absolute inset-0 h-full w-full object-cover" /> : (
                  <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(90deg,rgba(15,23,42,0.08)_1px,transparent_1px),linear-gradient(rgba(15,23,42,0.08)_1px,transparent_1px)] bg-[size:40px_40px]" />
                )}
                {loading ? <div className="absolute right-3 top-3 z-20 rounded-md bg-white px-3 py-2 text-xs font-semibold text-slate-500 shadow">刷新中</div> : null}
                {spots.map((spot, index) => {
                  const pos = spotCoords(spot, index, spots.length)
                  const info = boardStatusInfo(spot)
                  return (
                    <button
                      key={spot.spotId}
                      className={cn('absolute z-10 flex h-10 min-w-10 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full px-2 text-xs font-bold shadow-lg ring-4', info.className)}
                      style={{ left: `${pos.x}%`, top: `${pos.y}%` }}
                      onClick={() => setDetail(spot)}
                    >
                      {spot.spotCode}
                    </button>
                  )
                })}
              </div>
            </Card>
            <Card className="p-4">
              <h2 className="mb-4 font-bold text-slate-950">状态图例</h2>
              <div className="space-y-3">
                {[
                  ['bg-emerald-500', '空闲'],
                  ['bg-amber-400', '已预约'],
                  ['bg-blue-500', '使用中'],
                  ['bg-red-500', '维修'],
                  ['bg-slate-400', '禁用']
                ].map(([color, label]) => (
                  <div key={label} className="flex items-center gap-3 text-sm font-semibold text-slate-700">
                    <span className={cn('h-4 w-4 rounded-full', color)} />
                    {label}
                  </div>
                ))}
              </div>
              <div className="mt-6 rounded-md bg-slate-50 p-3 text-xs leading-5 text-slate-500">
                手机竖屏会优先展示平面图，点击钓位点可查看预约人、订单号、时段和状态。
              </div>
            </Card>
          </div>
        )}
      </RequirePond>

      <Modal title="钓位详情" open={Boolean(detail)} onClose={() => setDetail(null)} width="max-w-md">
        {detail ? (
          <div className="space-y-3 text-sm">
            <div className="flex items-center justify-between"><span className="text-slate-500">钓位</span><strong>{detail.spotCode}</strong></div>
            <div className="flex items-center justify-between"><span className="text-slate-500">状态</span><StatusBadge status={boardStatusInfo(detail).label} /></div>
            <div className="flex items-center justify-between"><span className="text-slate-500">预约人</span><strong>{detail.userNickname || '-'}</strong></div>
            <div className="flex items-center justify-between"><span className="text-slate-500">手机尾号</span><strong>{detail.userPhone || '-'}</strong></div>
            <div className="flex items-center justify-between"><span className="text-slate-500">订单号</span><strong>{detail.reservationId || '-'}</strong></div>
            <div className="flex items-center justify-between"><span className="text-slate-500">预约状态</span><strong>{detail.reservationStatus || '-'}</strong></div>
          </div>
        ) : null}
      </Modal>
    </div>
  )
}

type SessionForm = {
  slotDate: string
  slotName: string
  startTime: string
  endTime: string
  priceYuan: string
  maxBookings: number
  advanceDays: number
  drawStartTime: string
  drawEndTime: string
  status: number
}

const sessionPresets: Record<string, Pick<SessionForm, 'startTime' | 'endTime'>> = {
  早场: { startTime: '08:00', endTime: '12:00' },
  午场: { startTime: '12:00', endTime: '18:00' },
  晚场: { startTime: '18:00', endTime: '22:00' },
  全天场: { startTime: '08:00', endTime: '22:00' }
}

const presetSessionNames = ['早场', '午场', '晚场'] as const

const toDateTimeInput = (date: Date) => {
  const copy = new Date(date)
  copy.setMinutes(copy.getMinutes() - copy.getTimezoneOffset())
  return copy.toISOString().slice(0, 16)
}

const createDrawWindow = (slotDate: string, startTime: string) => {
  const startAt = new Date(`${slotDate}T${startTime}:00`)
  const now = new Date()
  if (Number.isNaN(startAt.getTime())) {
    return { drawStartTime: nowDateTimeLocal(), drawEndTime: nowDateTimeLocal() }
  }
  const drawStartAt = new Date(startAt.getTime() - 45 * 60 * 1000)
  const drawEndAt = new Date(startAt.getTime() - 15 * 60 * 1000)
  if (drawEndAt <= now) {
    return {
      drawStartTime: toDateTimeInput(new Date(now.getTime() + 5 * 60 * 1000)),
      drawEndTime: toDateTimeInput(new Date(now.getTime() + 35 * 60 * 1000))
    }
  }
  return {
    drawStartTime: toDateTimeInput(drawStartAt),
    drawEndTime: toDateTimeInput(drawEndAt)
  }
}

const createSessionForm = (slotDate = tomorrowString(), slotName = '早场'): SessionForm => {
  const preset = sessionPresets[slotName] || sessionPresets.早场
  const drawWindow = createDrawWindow(slotDate, preset.startTime)
  return {
    slotDate,
    slotName,
    startTime: preset.startTime,
    endTime: preset.endTime,
    priceYuan: '100',
    maxBookings: 30,
    advanceDays: 7,
    drawStartTime: drawWindow.drawStartTime,
    drawEndTime: drawWindow.drawEndTime,
    status: 1
  }
}

const sessionStartAt = (session: Pick<SessionForm, 'slotDate' | 'startTime'>) =>
  new Date(`${session.slotDate}T${session.startTime}:00`)

const sessionEndAt = (session: Pick<SessionForm, 'slotDate' | 'endTime'>) =>
  new Date(`${session.slotDate}T${session.endTime}:00`)

const isStartedOrEndedSession = (session: Pick<SessionForm, 'slotDate' | 'startTime' | 'endTime'>) => {
  const now = new Date()
  const startAt = sessionStartAt(session)
  const endAt = sessionEndAt(session)
  return Number.isNaN(startAt.getTime()) || Number.isNaN(endAt.getTime()) || startAt <= now || endAt <= now
}

export function SessionsPage() {
  const { ponds, currentPondId, currentPond, setCurrentPondId } = useMerchant()
  const [date, setDate] = useState(tomorrowString())
  const [sessions, setSessions] = useState<TimeSlot[]>([])
  const [form, setForm] = useState<SessionForm>(() => createSessionForm())
  const [editing, setEditing] = useState<TimeSlot | null>(null)
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [modalError, setModalError] = useState('')

  const load = useCallback(async (targetDate = date) => {
    if (!currentPondId) {
      setSessions([])
      return
    }
    setLoading(true)
    setError('')
    try {
      const page = await api.sessions({ pondId: currentPondId, slotDate: targetDate || undefined, pageNum: 1, pageSize: 500 })
      setSessions(normalizePage(page).records)
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [currentPondId, date])

  useEffect(() => {
    load()
  }, [load])

  const visibleSessions = useMemo(() => sessions.filter((item) => !date || item.slotDate === date), [sessions, date])

  const openCreate = () => {
    setEditing(null)
    setModalError('')
    setForm(createSessionForm(date || tomorrowString()))
    setOpen(true)
  }

  const openEdit = (session: TimeSlot) => {
    setEditing(session)
    setModalError('')
    setForm({
      slotDate: session.slotDate,
      slotName: session.slotName,
      startTime: formatTime(session.startTime),
      endTime: formatTime(session.endTime),
      priceYuan: String(Number(session.defaultPrice || 0)),
      maxBookings: session.maxBookings,
      advanceDays: session.advanceDays,
      drawStartTime: toDateTimeLocal(session.drawStartTime),
      drawEndTime: toDateTimeLocal(session.drawEndTime),
      status: session.status
    })
    setOpen(true)
  }

  const hasOrders = Boolean(editing && editing.remain !== undefined && editing.remain !== null && Number(editing.remain) < Number(editing.maxBookings))

  const applyPreset = (slotName: string) => {
    const preset = sessionPresets[slotName]
    setForm((value) => ({
      ...value,
      slotName,
      ...(preset && !hasOrders ? { ...preset, ...createDrawWindow(value.slotDate, preset.startTime) } : {})
    }))
  }

  const updateFormDate = (slotDate: string) => {
    setForm((value) => ({
      ...value,
      slotDate,
      ...(!hasOrders ? createDrawWindow(slotDate, value.startTime) : {})
    }))
  }

  const updateFormStartTime = (startTime: string) => {
    setForm((value) => ({
      ...value,
      startTime,
      ...(!hasOrders ? createDrawWindow(value.slotDate, startTime) : {})
    }))
  }

  const validateForm = () => {
    if (!currentPondId) return '请先选择要发布预约的鱼塘'
    if (!form.slotDate) return '请选择场次日期'
    if (!form.slotName.trim()) return '请选择时段名称'
    if (!form.startTime || !form.endTime) return '请填写开始和结束时间'
    if (form.startTime >= form.endTime) return '结束时间必须晚于开始时间'
    if (form.status === 1 && !editing && isStartedOrEndedSession(form)) return '不能发布已经开始或结束的场次，请选择未来时间'
    if (Number(form.priceYuan) < 0 || Number.isNaN(Number(form.priceYuan))) return '价格不能小于 0'
    if (Number(form.maxBookings) <= 0 || Number.isNaN(Number(form.maxBookings))) return '最大预约人数必须大于 0'
    if (Number(form.advanceDays) < 0 || Number.isNaN(Number(form.advanceDays))) return '预约开放天数不能小于 0'
    if (!form.drawStartTime || !form.drawEndTime) return '请填写抽号开始和结束时间'
    if (form.drawStartTime > form.drawEndTime) return '抽号结束时间必须晚于开始时间'
    return ''
  }

  const buildPayload = (source: SessionForm = form) => ({
    pondId: currentPondId,
    slotDate: source.slotDate,
    slotName: source.slotName,
    startTime: toTimeSeconds(source.startTime),
    endTime: toTimeSeconds(source.endTime),
    defaultPrice: Number(source.priceYuan || 0),
    maxBookings: Number(source.maxBookings || 0),
    advanceDays: Number(source.advanceDays || 0),
    drawStartTime: toServerDateTime(source.drawStartTime),
    drawEndTime: toServerDateTime(source.drawEndTime),
    status: Number(source.status)
  })

  const save = async () => {
    if (!currentPondId) return
    const validation = validateForm()
    if (validation) {
      setModalError(validation)
      return
    }
    setSaving(true)
    setModalError('')
    try {
      if (editing) await api.updateSession(editing.id, buildPayload())
      else await api.createSession(buildPayload())
      setOpen(false)
      setDate(form.slotDate)
      await load(form.slotDate)
    } catch (err) {
      setModalError(getError(err))
    } finally {
      setSaving(false)
    }
  }

  const publishPresetSessions = async (names: readonly string[]) => {
    if (!currentPondId) return
    const targetDate = date || tomorrowString()
    const existingKeys = new Set(visibleSessions.map((item) => `${item.slotName}-${formatTime(item.startTime)}-${formatTime(item.endTime)}`))
    const drafts = names
      .map((name) => createSessionForm(targetDate, name))
      .filter((item) => !isStartedOrEndedSession(item))
      .filter((item) => !existingKeys.has(`${item.slotName}-${item.startTime}-${item.endTime}`))
    if (drafts.length === 0) {
      setError('当天没有可发布的未来预设场次，或这些场次已存在')
      return
    }
    setSaving(true)
    setError('')
    const failed: string[] = []
    try {
      for (const draft of drafts) {
        try {
          await api.createSession(buildPayload(draft))
        } catch (err) {
          failed.push(`${draft.slotName}：${getError(err)}`)
        }
      }
      await load(targetDate)
      if (failed.length > 0) {
        setError(failed.join('；'))
      }
    } catch (err) {
      setError(getError(err))
    } finally {
      setSaving(false)
    }
  }

  const updateStatus = async (session: TimeSlot, status: number) => {
    setError('')
    try {
      await api.updateSession(session.id, {
        pondId: session.pondId,
        slotDate: session.slotDate,
        slotName: session.slotName,
        startTime: toTimeSeconds(session.startTime),
        endTime: toTimeSeconds(session.endTime),
        defaultPrice: Number(session.defaultPrice || 0),
        maxBookings: Number(session.maxBookings || 0),
        advanceDays: Number(session.advanceDays || 0),
        drawStartTime: toServerDateTime(session.drawStartTime),
        drawEndTime: toServerDateTime(session.drawEndTime),
        status
      })
      await load()
    } catch (err) {
      setError(getError(err))
    }
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">场次管理</h1>
          <p className="mt-2 text-sm text-slate-500">按鱼塘和日期发布预约场次，已有订单的场次会锁定关键字段。</p>
        </div>
        <Button icon={<Plus size={16} />} disabled={!currentPondId} onClick={openCreate}>发布预约场次</Button>
      </div>
      <ErrorBanner error={error} />
      <RequirePond>
        <Card className="mb-4 p-4">
          <div className="grid gap-3 lg:grid-cols-[280px_220px_1fr]">
            <Field label="发布鱼塘">
              <Select value={currentPondId || ''} onChange={(e) => setCurrentPondId(e.target.value ? Number(e.target.value) : undefined)}>
                {ponds.map((pond) => <option key={pond.id} value={pond.id}>{pond.name}</option>)}
              </Select>
            </Field>
            <Field label="日期筛选"><Input type="date" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
            <div className="flex flex-wrap items-end gap-2">
              <Button variant="secondary" icon={<RefreshCw size={16} />} onClick={() => load()}>刷新</Button>
              <Button icon={<Plus size={16} />} onClick={openCreate}>发布预约场次</Button>
              <Button variant="secondary" disabled={saving} onClick={() => publishPresetSessions(presetSessionNames)}>一键发布早午晚</Button>
              <Button variant="secondary" disabled={saving} onClick={() => publishPresetSessions(['全天场'])}>发布全天场</Button>
            </div>
          </div>
          <p className="mt-3 text-xs text-slate-500">当前发布对象：{currentPond?.name || '未选择鱼塘'}。发布后，PC 用户端只会展示未过期、未截止且在预约开放天数内的场次。</p>
        </Card>
        {loading ? <LoadingBlock /> : visibleSessions.length === 0 ? <EmptyState title="暂无场次" description="选择鱼塘和日期后，点击“发布预约场次”创建早/午/晚/全天预约。" /> : (
          <Table>
            <table className="w-full min-w-[920px]">
              <thead>
                <tr>
                  <Th>日期</Th>
                  <Th>时段</Th>
                  <Th>价格</Th>
                  <Th>容量/余量</Th>
                  <Th>预约开放</Th>
                  <Th>抽号时间</Th>
                  <Th>状态</Th>
                  <Th>操作</Th>
                </tr>
              </thead>
              <tbody>
                {visibleSessions.map((session) => (
                  <tr key={session.id}>
                    <Td>{formatDate(session.slotDate)}</Td>
                    <Td>
                      <div className="font-semibold text-slate-900">{session.slotName}</div>
                      <div className="text-xs text-slate-400">{formatTime(session.startTime)} - {formatTime(session.endTime)}</div>
                    </Td>
                    <Td>{formatYuan(session.defaultPrice || 0)}</Td>
                    <Td>{session.maxBookings} / {session.remain ?? '-'}</Td>
                    <Td>提前 {session.advanceDays} 天</Td>
                    <Td>{formatDateTime(session.drawStartTime)} - {formatDateTime(session.drawEndTime)}</Td>
                    <Td><Badge tone={session.status === 1 ? 'green' : 'red'}>{session.status === 1 ? '已发布' : '停用'}</Badge></Td>
                    <Td>
                      <div className={actionCell}>
                        <Button size="sm" variant="secondary" onClick={() => openEdit(session)}>编辑</Button>
                        <Button size="sm" variant={session.status === 1 ? 'danger' : 'success'} onClick={() => updateStatus(session, session.status === 1 ? 0 : 1)}>{session.status === 1 ? '停用' : '发布'}</Button>
                        <ConfirmButton size="sm" variant="danger" message="确定删除这个场次吗？已有有效订单的场次会被后端拦截，请先停用并走变更流程。" onConfirm={async () => { await api.deleteSession(session.id); await load() }}>删除</ConfirmButton>
                      </div>
                    </Td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Table>
        )}
      </RequirePond>

      <Modal
        title={editing ? '编辑预约场次' : '发布预约场次'}
        open={open}
        onClose={() => setOpen(false)}
        footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button disabled={saving} onClick={save}>{saving ? '保存中' : (editing ? '保存' : '保存并发布')}</Button></>}
      >
        <ErrorBanner error={modalError} />
        {hasOrders ? <div className="mb-4 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-800">该场次已有订单，日期、时段、时间、价格和容量等关键字段已锁定，请走变更流程。</div> : null}
        <div className="mb-4 rounded-md border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
          <span className="font-semibold text-slate-900">发布鱼塘：</span>{currentPond?.name || '未选择鱼塘'}
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="日期"><Input disabled={Boolean(hasOrders)} type="date" value={form.slotDate} onChange={(e) => updateFormDate(e.target.value)} /></Field>
          <Field label="时段名称">
            <Select disabled={Boolean(hasOrders)} value={form.slotName} onChange={(e) => applyPreset(e.target.value)}>
              <option>早场</option>
              <option>午场</option>
              <option>晚场</option>
              <option>全天场</option>
            </Select>
          </Field>
          <Field label="开始时间"><Input disabled={Boolean(hasOrders)} type="time" value={form.startTime} onChange={(e) => updateFormStartTime(e.target.value)} /></Field>
          <Field label="结束时间"><Input disabled={Boolean(hasOrders)} type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} /></Field>
          <Field label="价格（元）"><Input disabled={Boolean(hasOrders)} type="number" min="0" step="0.01" value={form.priceYuan} onChange={(e) => setForm({ ...form, priceYuan: e.target.value })} /></Field>
          <Field label="最大预约人数"><Input disabled={Boolean(hasOrders)} type="number" min="1" value={form.maxBookings} onChange={(e) => setForm({ ...form, maxBookings: Number(e.target.value) })} /></Field>
          <Field label="预约开放天数" hint="例如 7 表示最远可预约未来 7 天。"><Input type="number" min="0" value={form.advanceDays} onChange={(e) => setForm({ ...form, advanceDays: Number(e.target.value) })} /></Field>
          <Field label="状态">
            <Select value={form.status} onChange={(e) => setForm({ ...form, status: Number(e.target.value) })}>
              <option value={1}>发布</option>
              <option value={0}>停用</option>
            </Select>
          </Field>
          <Field label="抽号开始时间"><Input type="datetime-local" value={form.drawStartTime} onChange={(e) => setForm({ ...form, drawStartTime: e.target.value })} /></Field>
          <Field label="抽号结束时间"><Input type="datetime-local" value={form.drawEndTime} onChange={(e) => setForm({ ...form, drawEndTime: e.target.value })} /></Field>
        </div>
      </Modal>
    </div>
  )
}

const emptyAnnouncement: Partial<Announcement> = {
  title: '',
  content: '',
  coverImage: '',
  pushHome: false,
  status: 'published',
  publishTime: nowDateTimeLocal()
}

export function AnnouncementsPage() {
  const { currentPondId } = useMerchant()
  const [items, setItems] = useState<Announcement[]>([])
  const [form, setForm] = useState<Partial<Announcement>>(emptyAnnouncement)
  const [editing, setEditing] = useState<Announcement | null>(null)
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setItems(await api.announcements({ pondId: currentPondId }))
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [currentPondId])

  useEffect(() => {
    load()
  }, [load])

  const save = async () => {
    setError('')
    try {
      const payload = { ...form, pondId: currentPondId, publishTime: toServerDateTime(form.publishTime) }
      if (editing) await api.updateAnnouncement(editing.id, payload)
      else await api.createAnnouncement(payload)
      setOpen(false)
      await load()
    } catch (err) {
      setError(getError(err))
    }
  }

  const startCreate = () => {
    setEditing(null)
    setForm(emptyAnnouncement)
    setOpen(true)
  }

  const startEdit = (item: Announcement) => {
    setEditing(item)
    setForm({ ...item, publishTime: toDateTimeLocal(item.publishTime) })
    setOpen(true)
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">公告管理</h1>
          <p className="mt-2 text-sm text-slate-500">发布鱼塘公告，可同步推送到用户端首页展示。</p>
        </div>
        <Button icon={<Plus size={16} />} onClick={startCreate}>新增公告</Button>
      </div>
      <ErrorBanner error={error} />
      {loading ? <LoadingBlock /> : items.length === 0 ? <EmptyState title="暂无公告" /> : (
        <Table>
          <table className="w-full min-w-[820px]">
            <thead>
              <tr>
                <Th>标题</Th>
                <Th>发布时间</Th>
                <Th>首页推送</Th>
                <Th>状态</Th>
                <Th>操作</Th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <Td>
                    <div className="font-semibold text-slate-900">{item.title}</div>
                    <div className="line-clamp-1 text-xs text-slate-400">{item.content || '-'}</div>
                  </Td>
                  <Td>{formatDateTime(item.publishTime)}</Td>
                  <Td>{item.pushHome ? <Badge tone="green">是</Badge> : <Badge>否</Badge>}</Td>
                  <Td><StatusBadge status={item.status} /></Td>
                  <Td>
                    <div className={actionCell}>
                      <Button size="sm" variant="secondary" onClick={() => startEdit(item)}>编辑</Button>
                      <ConfirmButton size="sm" variant="danger" message="确定删除这个公告吗？" onConfirm={async () => { await api.deleteAnnouncement(item.id); await load() }}>删除</ConfirmButton>
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </table>
        </Table>
      )}

      <Modal title={editing ? '编辑公告' : '新增公告'} open={open} onClose={() => setOpen(false)} footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button onClick={save}>保存</Button></>}>
        <div className="grid gap-4">
          <Field label="标题"><Input value={form.title || ''} onChange={(e) => setForm({ ...form, title: e.target.value })} /></Field>
          <Field label="内容"><Textarea value={form.content || ''} onChange={(e) => setForm({ ...form, content: e.target.value })} /></Field>
          <Field label="封面图 URL"><Input value={form.coverImage || ''} onChange={(e) => setForm({ ...form, coverImage: e.target.value })} /></Field>
          <div className="grid gap-4 md:grid-cols-3">
            <Field label="发布时间"><Input type="datetime-local" value={form.publishTime || ''} onChange={(e) => setForm({ ...form, publishTime: e.target.value })} /></Field>
            <Field label="状态">
              <Select value={form.status || 'published'} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                <option value="published">发布</option>
                <option value="draft">草稿</option>
              </Select>
            </Field>
            <label className="flex items-end gap-2 pb-2 text-sm font-semibold text-slate-700">
              <input type="checkbox" checked={Boolean(form.pushHome)} onChange={(e) => setForm({ ...form, pushHome: e.target.checked })} />
              推送至首页
            </label>
          </div>
        </div>
      </Modal>
    </div>
  )
}

const emptyEvent: Partial<EventItem> = {
  title: '',
  coverImage: '',
  eventTime: nowDateTimeLocal(),
  signupDeadline: nowDateTimeLocal(),
  capacity: 50,
  location: '',
  introduction: '',
  auditEnabled: false,
  cancelRule: '',
  formFields: '姓名,手机号',
  status: 'published',
  recommended: false,
  pinned: false
}

export function EventsPage() {
  const { currentPondId } = useMerchant()
  const [items, setItems] = useState<EventItem[]>([])
  const [form, setForm] = useState<Partial<EventItem>>(emptyEvent)
  const [editing, setEditing] = useState<EventItem | null>(null)
  const [open, setOpen] = useState(false)
  const [qrUrl, setQrUrl] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setItems(await api.events())
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const save = async () => {
    setError('')
    try {
      const payload = {
        ...form,
        pondId: currentPondId,
        eventTime: toServerDateTime(form.eventTime),
        signupDeadline: toServerDateTime(form.signupDeadline),
        capacity: Number(form.capacity || 0)
      }
      if (editing) await api.updateEvent(editing.id, payload)
      else await api.createEvent(payload)
      setOpen(false)
      await load()
    } catch (err) {
      setError(getError(err))
    }
  }

  const openQr = async (item: EventItem) => {
    const url = `${window.location.origin.replace(':3001', ':3002')}/events/${item.id}/checkin`
    setQrUrl(await QRCode.toDataURL(url, { width: 220, margin: 1 }))
  }

  const startEdit = (item: EventItem) => {
    setEditing(item)
    setForm({ ...item, eventTime: toDateTimeLocal(item.eventTime), signupDeadline: toDateTimeLocal(item.signupDeadline) })
    setOpen(true)
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">活动管理</h1>
          <p className="mt-2 text-sm text-slate-500">维护活动信息、报名审核规则和推荐置顶状态。</p>
        </div>
        <Button icon={<Plus size={16} />} onClick={() => { setEditing(null); setForm(emptyEvent); setOpen(true) }}>新增活动</Button>
      </div>
      <ErrorBanner error={error} />
      {loading ? <LoadingBlock /> : items.length === 0 ? <EmptyState title="暂无活动" /> : (
        <div className="grid gap-4 xl:grid-cols-2">
          {items.map((item) => (
            <Card key={item.id} className="overflow-hidden">
              <div className="flex gap-4 p-4">
                <div className="h-28 w-36 shrink-0 overflow-hidden rounded-md bg-slate-100">
                  {item.coverImage ? <img src={item.coverImage} alt={item.title} className="h-full w-full object-cover" /> : <div className="flex h-full w-full items-center justify-center text-slate-300"><CalendarClock size={32} /></div>}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="truncate text-lg font-bold text-slate-950">{item.title}</h2>
                    <StatusBadge status={item.status} />
                    {item.recommended ? <Badge tone="blue">推荐</Badge> : null}
                    {item.pinned ? <Badge tone="amber">置顶</Badge> : null}
                  </div>
                  <p className="mt-2 text-sm text-slate-500">{formatDateTime(item.eventTime)} · {item.location || '未填写地点'}</p>
                  <p className="mt-1 text-sm text-slate-500">报名 {item.signupCount || 0} / {item.capacity || 0}</p>
                  <div className="mt-4 flex flex-wrap gap-2">
                    <Button size="sm" variant="secondary" onClick={() => startEdit(item)}>编辑</Button>
                    <Button size="sm" variant="secondary" onClick={() => api.updateEventStatus(item.id, { recommended: !item.recommended }).then(load)}>{item.recommended ? '取消推荐' : '推荐'}</Button>
                    <Button size="sm" variant="secondary" onClick={() => api.updateEventStatus(item.id, { pinned: !item.pinned }).then(load)}>{item.pinned ? '取消置顶' : '置顶'}</Button>
                    <Link to={`/merchant/events/${item.id}/signups`}><Button size="sm" variant="secondary">报名</Button></Link>
                    <Link to={`/merchant/events/${item.id}/reviews`}><Button size="sm" variant="secondary">评价</Button></Link>
                    <Button size="sm" variant="secondary" icon={<QrCode size={14} />} onClick={() => openQr(item)}>签到码</Button>
                    <ConfirmButton size="sm" variant="danger" message="确定删除这个活动吗？" onConfirm={async () => { await api.deleteEvent(item.id); await load() }}>删除</ConfirmButton>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal title={editing ? '编辑活动' : '新增活动'} open={open} onClose={() => setOpen(false)} footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button onClick={save}>保存</Button></>}>
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="标题" className="md:col-span-2"><Input value={form.title || ''} onChange={(e) => setForm({ ...form, title: e.target.value })} /></Field>
          <Field label="封面图 URL" className="md:col-span-2"><Input value={form.coverImage || ''} onChange={(e) => setForm({ ...form, coverImage: e.target.value })} /></Field>
          <Field label="活动时间"><Input type="datetime-local" value={form.eventTime || ''} onChange={(e) => setForm({ ...form, eventTime: e.target.value })} /></Field>
          <Field label="报名截止"><Input type="datetime-local" value={form.signupDeadline || ''} onChange={(e) => setForm({ ...form, signupDeadline: e.target.value })} /></Field>
          <Field label="人数上限"><Input type="number" value={form.capacity || 0} onChange={(e) => setForm({ ...form, capacity: Number(e.target.value) })} /></Field>
          <Field label="地点"><Input value={form.location || ''} onChange={(e) => setForm({ ...form, location: e.target.value })} /></Field>
          <Field label="介绍" className="md:col-span-2"><Textarea value={form.introduction || ''} onChange={(e) => setForm({ ...form, introduction: e.target.value })} /></Field>
          <Field label="取消规则"><Textarea value={form.cancelRule || ''} onChange={(e) => setForm({ ...form, cancelRule: e.target.value })} /></Field>
          <Field label="报名表单字段"><Textarea value={form.formFields || ''} onChange={(e) => setForm({ ...form, formFields: e.target.value })} /></Field>
          <div className="flex gap-5 md:col-span-2">
            <label className="flex items-center gap-2 text-sm font-semibold text-slate-700"><input type="checkbox" checked={Boolean(form.auditEnabled)} onChange={(e) => setForm({ ...form, auditEnabled: e.target.checked })} />开启审核</label>
            <label className="flex items-center gap-2 text-sm font-semibold text-slate-700"><input type="checkbox" checked={Boolean(form.recommended)} onChange={(e) => setForm({ ...form, recommended: e.target.checked })} />推荐</label>
            <label className="flex items-center gap-2 text-sm font-semibold text-slate-700"><input type="checkbox" checked={Boolean(form.pinned)} onChange={(e) => setForm({ ...form, pinned: e.target.checked })} />置顶</label>
          </div>
        </div>
      </Modal>

      <Modal title="活动签到二维码" open={Boolean(qrUrl)} onClose={() => setQrUrl('')} width="max-w-sm">
        <div className="flex justify-center">{qrUrl ? <img src={qrUrl} alt="签到二维码" className="h-56 w-56" /> : null}</div>
      </Modal>
    </div>
  )
}

export function EventSignupsPage() {
  const { id } = useParams()
  const eventId = Number(id)
  const [items, setItems] = useState<EventSignup[]>([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setItems(await api.eventSignups(eventId))
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [eventId])

  useEffect(() => {
    load()
  }, [load])

  const audit = async (item: EventSignup, auditStatus: string) => {
    const auditReason = auditStatus === 'rejected' ? window.prompt('拒绝理由') || '' : ''
    await api.auditSignup(eventId, item.id, { auditStatus, auditReason })
    await load()
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">报名列表</h1>
          <p className="mt-2 text-sm text-slate-500">查看报名资料，执行审核、签到和导出操作。</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" icon={<Download size={16} />} onClick={() => exportCsv(`event-${eventId}-signups.csv`, items.map((item) => ({ ID: item.id, 姓名: item.userName, 手机: item.phone, 资料: item.formData, 审核: statusText(item.auditStatus), 签到: item.checkedIn ? '是' : '否', 时间: item.createTime })))}>导出 Excel</Button>
          <ConfirmButton variant="danger" message="确定清空该活动报名数据吗？" onConfirm={async () => { await api.clearSignups(eventId); await load() }}>清空报名</ConfirmButton>
        </div>
      </div>
      <ErrorBanner error={error} />
      {loading ? <LoadingBlock /> : items.length === 0 ? <EmptyState title="暂无报名" /> : (
        <Table>
          <table className="w-full min-w-[900px]">
            <thead>
              <tr>
                <Th>报名人</Th>
                <Th>资料</Th>
                <Th>审核</Th>
                <Th>签到</Th>
                <Th>时间</Th>
                <Th>操作</Th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <Td><div className="font-semibold text-slate-900">{item.userName || '-'}</div><div className="text-xs text-slate-400">{item.phone || '-'}</div></Td>
                  <Td><pre className="max-w-[300px] whitespace-pre-wrap text-xs text-slate-500">{item.formData || '-'}</pre></Td>
                  <Td><StatusBadge status={item.auditStatus} /></Td>
                  <Td>{item.checkedIn ? <Badge tone="green">已签到</Badge> : <Badge>未签到</Badge>}</Td>
                  <Td>{formatDateTime(item.createTime)}</Td>
                  <Td>
                    <div className={actionCell}>
                      <Button size="sm" variant="success" onClick={() => audit(item, 'approved')}>通过</Button>
                      <Button size="sm" variant="danger" onClick={() => audit(item, 'rejected')}>拒绝</Button>
                      <Button size="sm" variant="secondary" onClick={async () => { await api.checkinSignup(eventId, item.id, !item.checkedIn); await load() }}>{item.checkedIn ? '取消签到' : '签到'}</Button>
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </table>
        </Table>
      )}
    </div>
  )
}

export function EventReviewsPage() {
  const { id } = useParams()
  const eventId = Number(id)
  const [items, setItems] = useState<EventReview[]>([])
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setItems(await api.eventReviews(eventId))
    } catch (err) {
      setError(getError(err))
    }
  }, [eventId])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div>
      <div className="mb-5">
        <h1 className="text-2xl font-bold text-slate-950">用户评价</h1>
        <p className="mt-2 text-sm text-slate-500">查看活动评价并删除违规内容。</p>
      </div>
      <ErrorBanner error={error} />
      {items.length === 0 ? <EmptyState title="暂无评价" /> : (
        <div className="space-y-3">
          {items.map((item) => (
            <Card key={item.id} className="p-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div>
                  <div className="flex items-center gap-2"><strong>{item.userName || '用户'}</strong><Badge tone="amber">{item.rating || 0} 星</Badge></div>
                  <p className="mt-2 text-sm leading-6 text-slate-600">{item.content || '-'}</p>
                  <p className="mt-2 text-xs text-slate-400">{formatDateTime(item.createTime)}</p>
                </div>
                <ConfirmButton size="sm" variant="danger" message="确定删除该评价吗？" onConfirm={async () => { await api.deleteEventReview(eventId, item.id); await load() }}>删除</ConfirmButton>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}

export function CheckinPage() {
  const { currentPondId } = useMerchant()
  const [code, setCode] = useState('')
  const [actualFee, setActualFee] = useState('')
  const [result, setResult] = useState<CheckinResult | null>(null)
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [cameraOpen, setCameraOpen] = useState(false)
  const [error, setError] = useState('')
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const streamRef = useRef<MediaStream | null>(null)

  const loadManual = useCallback(async () => {
    if (!currentPondId) return
    try {
      const page = await api.reservations({ pondId: currentPondId, pageNum: 1, pageSize: 50, status: '已抽号' })
      setReservations(normalizePage(page).records)
    } catch (err) {
      setError(getError(err))
    }
  }, [currentPondId])

  useEffect(() => {
    loadManual()
  }, [loadManual])

  const query = async () => {
    if (!code) return
    setError('')
    try {
      setResult(await api.queryCheckin(code))
    } catch (err) {
      setResult(null)
      setError(getError(err))
    }
  }

  const submit = async (targetCode = code) => {
    if (!targetCode) return
    setError('')
    try {
      const data = await api.checkin({ checkinCode: targetCode, actualFee: actualFee ? Number(actualFee) : undefined })
      setResult(data)
      setCode(targetCode)
      await loadManual()
    } catch (err) {
      setError(getError(err))
    }
  }

  const openCamera = async () => {
    setCameraOpen(true)
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
      streamRef.current = stream
      if (videoRef.current) videoRef.current.srcObject = stream
    } catch (err) {
      setError('摄像头无法打开，请检查浏览器权限或使用核销码输入。')
    }
  }

  const closeCamera = () => {
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    setCameraOpen(false)
  }

  useEffect(() => closeCamera, [])

  return (
    <div>
      <div className="mb-5">
        <h1 className="text-2xl font-bold text-slate-950">核销签到</h1>
        <p className="mt-2 text-sm text-slate-500">支持输入核销码、摄像头扫码预览和订单列表手动核销。</p>
      </div>
      <ErrorBanner error={error} />
      <div className="grid gap-5 xl:grid-cols-[420px_1fr]">
        <Card className="p-5">
          <h2 className="mb-4 font-bold text-slate-950">核销码核销</h2>
          <div className="space-y-4">
            <Field label="核销码"><Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="输入 6 位核销码" /></Field>
            <Field label="实收金额（元，可选）"><Input type="number" value={actualFee} onChange={(e) => setActualFee(e.target.value)} /></Field>
            <div className="grid grid-cols-2 gap-2">
              <Button variant="secondary" icon={<Eye size={16} />} onClick={query}>查询</Button>
              <Button icon={<CheckCircle2 size={16} />} onClick={() => submit()}>确认核销</Button>
            </div>
            <Button className="w-full" size="lg" variant="success" icon={<Camera size={18} />} onClick={openCamera}>扫码核销</Button>
          </div>
          {result ? (
            <div className="mt-5 rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm">
              <div className="mb-2 font-bold text-emerald-800">核销信息</div>
              <div className="space-y-1 text-emerald-900">
                <p>用户：{result.userNickname || '-'}</p>
                <p>鱼塘：{result.pondName || '-'}</p>
                <p>场次：{formatDate(result.slotDate)} {result.slotName}</p>
                <p>钓位：{result.spotCode || '-'}</p>
                <p>状态：{result.status || '-'}</p>
              </div>
            </div>
          ) : null}
        </Card>

        <Card className="p-5">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-bold text-slate-950">可核销订单</h2>
            <Button size="sm" variant="secondary" onClick={loadManual}>刷新</Button>
          </div>
          {reservations.length === 0 ? <EmptyState title="暂无待核销订单" /> : (
            <Table>
              <table className="w-full min-w-[720px]">
                <thead><tr><Th>用户</Th><Th>场次</Th><Th>钓位</Th><Th>核销码</Th><Th>操作</Th></tr></thead>
                <tbody>
                  {reservations.map((item) => (
                    <tr key={item.id}>
                      <Td><div className="font-semibold text-slate-900">{item.userNickname || '-'}</div><div className="text-xs text-slate-400">{item.userPhone || '-'}</div></Td>
                      <Td>{formatDate(item.slotDate)} {item.slotName}</Td>
                      <Td>{item.spotCode || '-'}</Td>
                      <Td>{item.checkinCode || '-'}</Td>
                      <Td><Button size="sm" onClick={() => submit(item.checkinCode || '')}>核销</Button></Td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Table>
          )}
        </Card>
      </div>
      <Modal title="扫码核销" open={cameraOpen} onClose={closeCamera} width="max-w-lg">
        <video ref={videoRef} autoPlay playsInline className="h-80 w-full rounded-lg bg-slate-950 object-cover" />
        <p className="mt-3 text-sm text-slate-500">当前版本打开摄像头用于扫码取景；如识别库不可用，请直接输入核销码完成核销。</p>
      </Modal>
    </div>
  )
}

export function OrdersPage() {
  const { currentPondId } = useMerchant()
  const [items, setItems] = useState<Reservation[]>([])
  const [status, setStatus] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [detail, setDetail] = useState<Reservation | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!currentPondId) return
    setLoading(true)
    setError('')
    try {
      const page = await api.reservations({ pondId: currentPondId, pageNum: 1, pageSize: 500, status: status || undefined })
      let records = normalizePage(page).records
      if (startDate) records = records.filter((item) => (item.slotDate || '') >= startDate)
      if (endDate) records = records.filter((item) => (item.slotDate || '') <= endDate)
      setItems(records)
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [currentPondId, status, startDate, endDate])

  useEffect(() => {
    load()
  }, [load])

  const cancel = async (item: Reservation) => {
    const reason = window.prompt('请输入取消原因') || ''
    if (!reason) return
    await api.cancelReservation(item.id, reason)
    await load()
  }

  const exportData = async () => {
    if (!currentPondId) return
    const response = await api.exportReservations({ pondId: currentPondId, startDate, endDate, status })
    downloadBlob(response as any, '预约订单.xlsx')
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">订单管理</h1>
          <p className="mt-2 text-sm text-slate-500">预约订单查询、订单详情、手动取消和 Excel 导出。</p>
        </div>
        <Button variant="secondary" icon={<Download size={16} />} onClick={exportData}>导出订单</Button>
      </div>
      <ErrorBanner error={error} />
      <RequirePond>
        <Card className="mb-4 p-4">
          <div className="grid gap-3 md:grid-cols-4">
            <Field label="开始日期"><Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} /></Field>
            <Field label="结束日期"><Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} /></Field>
            <Field label="订单状态">
              <Select value={status} onChange={(e) => setStatus(e.target.value)}>
                <option value="">全部</option>
                <option value="待抽号">待抽号</option>
                <option value="已抽号">已抽号</option>
                <option value="已核销">已核销</option>
                <option value="预约取消">预约取消</option>
                <option value="过期失效">过期失效</option>
              </Select>
            </Field>
            <div className="flex items-end"><Button variant="secondary" onClick={load}>查询</Button></div>
          </div>
        </Card>
        {loading ? <LoadingBlock /> : items.length === 0 ? <EmptyState title="暂无订单" /> : (
          <Table>
            <table className="w-full min-w-[980px]">
              <thead><tr><Th>订单号</Th><Th>用户</Th><Th>鱼塘/场次</Th><Th>金额</Th><Th>钓位</Th><Th>支付状态</Th><Th>状态</Th><Th>操作</Th></tr></thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <Td>#{item.id}</Td>
                    <Td><div className="font-semibold text-slate-900">{item.userNickname || '-'}</div><div className="text-xs text-slate-400">{item.userPhone || '-'}</div></Td>
                    <Td><div>{item.pondName || '-'}</div><div className="text-xs text-slate-400">{formatDate(item.slotDate)} {item.slotName}</div></Td>
                    <Td>{formatYuan(item.actualFee || 0)}</Td>
                    <Td>{item.spotCode || '-'}</Td>
                    <Td><Badge tone={item.status === '已核销' ? 'green' : 'amber'}>{item.status === '已核销' ? '已支付' : '待到场'}</Badge></Td>
                    <Td><StatusBadge status={item.status} /></Td>
                    <Td><div className={actionCell}><Button size="sm" variant="secondary" onClick={() => setDetail(item)}>详情</Button><Button size="sm" variant="danger" onClick={() => cancel(item)}>取消</Button></div></Td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Table>
        )}
      </RequirePond>
      <Modal title="订单详情" open={Boolean(detail)} onClose={() => setDetail(null)} width="max-w-lg">
        {detail ? (
          <div className="space-y-3 text-sm">
            {[
              ['订单号', `#${detail.id}`],
              ['用户', `${detail.userNickname || '-'} ${detail.userPhone || ''}`],
              ['鱼塘', detail.pondName || '-'],
              ['预约', `${formatDate(detail.slotDate)} ${detail.slotName || ''} ${formatTime(detail.startTime)}-${formatTime(detail.endTime)}`],
              ['金额', formatYuan(detail.actualFee || 0)],
              ['钓位', detail.spotCode || '-'],
              ['核销码', detail.checkinCode || '-'],
              ['状态', statusText(detail.status)],
              ['取消原因', detail.cancelReason || '-']
            ].map(([label, value]) => <div key={label} className="flex justify-between gap-4"><span className="text-slate-500">{label}</span><strong className="text-right text-slate-900">{value}</strong></div>)}
          </div>
        ) : null}
      </Modal>
    </div>
  )
}

export function RevenuePage() {
  const { currentPondId } = useMerchant()
  const [summary, setSummary] = useState<RevenueSummary>({})
  const [items, setItems] = useState<RevenueItem[]>([])
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!currentPondId) return
    setLoading(true)
    setError('')
    try {
      const [summaryData, list] = await Promise.all([
        api.revenueSummary(currentPondId),
        api.revenueList({ pondId: currentPondId, startDate, endDate })
      ])
      setSummary(summaryData)
      setItems(list)
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [currentPondId, startDate, endDate])

  useEffect(() => {
    load()
  }, [load])

  const exportData = async () => {
    if (!currentPondId) return
    const response = await api.exportRevenue({ pondId: currentPondId, startDate, endDate })
    downloadBlob(response as any, '收益报表.xlsx')
  }

  const maxIncome = Math.max(...items.map((item) => Number(item.totalIncome || 0)), 1)
  const points = items.slice().reverse().map((item, index, arr) => {
    const x = arr.length <= 1 ? 10 : 10 + (index * 80) / (arr.length - 1)
    const y = 90 - (Number(item.totalIncome || 0) / maxIncome) * 70
    return `${x},${y}`
  }).join(' ')

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">收益统计</h1>
          <p className="mt-2 text-sm text-slate-500">统计今日、本周、本月收入，并按日期范围导出明细。</p>
        </div>
        <Button variant="secondary" icon={<Download size={16} />} onClick={exportData}>导出收益报表</Button>
      </div>
      <ErrorBanner error={error} />
      <RequirePond>
        <div className="mb-5 grid gap-4 md:grid-cols-3">
          <Card className="p-5"><p className="text-sm font-semibold text-slate-500">今日收入</p><p className="mt-3 text-3xl font-bold text-slate-950">{formatYuan(summary.today || 0)}</p></Card>
          <Card className="p-5"><p className="text-sm font-semibold text-slate-500">本周收入</p><p className="mt-3 text-3xl font-bold text-slate-950">{formatYuan(summary.week || 0)}</p></Card>
          <Card className="p-5"><p className="text-sm font-semibold text-slate-500">本月收入</p><p className="mt-3 text-3xl font-bold text-slate-950">{formatYuan(summary.month || 0)}</p></Card>
        </div>
        <Card className="mb-5 p-4">
          <div className="grid gap-3 md:grid-cols-[220px_220px_1fr]">
            <Field label="开始日期"><Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} /></Field>
            <Field label="结束日期"><Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} /></Field>
            <div className="flex items-end"><Button variant="secondary" onClick={load}>查询</Button></div>
          </div>
        </Card>
        <Card className="mb-5 p-5">
          <h2 className="mb-4 font-bold text-slate-950">收入趋势</h2>
          <svg viewBox="0 0 100 100" className="h-56 w-full overflow-visible">
            <polyline fill="none" stroke="#047857" strokeWidth="2.4" points={points} />
            {items.slice().reverse().map((item, index, arr) => {
              const x = arr.length <= 1 ? 10 : 10 + (index * 80) / (arr.length - 1)
              const y = 90 - (Number(item.totalIncome || 0) / maxIncome) * 70
              return <circle key={`${item.slotDate}-${item.slotId}`} cx={x} cy={y} r="1.8" fill="#047857" />
            })}
          </svg>
        </Card>
        {loading ? <LoadingBlock /> : (
          <Table>
            <table className="w-full min-w-[980px]">
              <thead><tr><Th>日期</Th><Th>鱼塘</Th><Th>场次</Th><Th>订单总额</Th><Th>退款金额</Th><Th>平台服务费</Th><Th>商家实收</Th><Th>待结算</Th><Th>已结算</Th></tr></thead>
              <tbody>
                {items.map((item) => {
                  const income = Number(item.totalIncome || 0)
                  const fee = income * 0.03
                  return (
                    <tr key={`${item.slotDate}-${item.slotId}`}>
                      <Td>{formatDate(item.slotDate)}</Td>
                      <Td>{item.pondName || '-'}</Td>
                      <Td>{item.slotName || '-'}</Td>
                      <Td>{formatYuan(income)}</Td>
                      <Td>{formatYuan(0)}</Td>
                      <Td>{formatYuan(fee)}</Td>
                      <Td>{formatYuan(income - fee)}</Td>
                      <Td>{formatYuan(income - fee)}</Td>
                      <Td>{formatYuan(0)}</Td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </Table>
        )}
      </RequirePond>
    </div>
  )
}

const productCategoryText = (category?: string | null) => {
  const map: Record<string, string> = {
    equipment: '钓具',
    bait: '饵料',
    fish: '鱼获',
    food: '菜品'
  }
  return map[category || ''] || category || '-'
}

const menuCategoryText = (category?: string | null) => {
  const map: Record<string, string> = {
    fresh_fish: '鲜鱼',
    cooked: '加工菜品',
    drink: '饮品'
  }
  return map[category || ''] || category || '-'
}

const staffRoleText = (role?: string | null) => {
  const map: Record<string, string> = {
    checker: '核销员',
    operator: '运营员',
    finance: '财务员',
    manager: '店长'
  }
  return map[role || ''] || role || '-'
}

type ProductForm = Partial<ShopProduct> & { priceYuan?: string }

const emptyProductForm: ProductForm = {
  name: '',
  category: 'equipment',
  priceYuan: '',
  stock: 0,
  imageUrl: '',
  description: '',
  status: 'on'
}

export function ShopProductsPage() {
  const { currentPondId } = useMerchant()
  const [items, setItems] = useState<ShopProduct[]>([])
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('')
  const [status, setStatus] = useState('')
  const [form, setForm] = useState<ProductForm>(emptyProductForm)
  const [editing, setEditing] = useState<ShopProduct | null>(null)
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState<SaveState>('idle')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const page = await api.shopProducts({ keyword, category, status, pageNum: 1, pageSize: 500 })
      setItems(normalizePage(page).records)
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [keyword, category, status])

  useEffect(() => {
    load()
  }, [load])

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyProductForm, pondId: currentPondId })
    setOpen(true)
  }

  const openEdit = (item: ShopProduct) => {
    setEditing(item)
    setForm({ ...item, priceYuan: String(Number(item.price || 0) / 100) })
    setOpen(true)
  }

  const save = async () => {
    setSaving('saving')
    setError('')
    try {
      const payload: Partial<ShopProduct> = {
        ...form,
        pondId: currentPondId || form.pondId,
        price: parseMoneyToCent(form.priceYuan),
        stock: Number(form.stock || 0),
        status: form.status || 'on'
      }
      delete (payload as ProductForm).priceYuan
      if (editing) await api.updateShopProduct(editing.id, payload)
      else await api.createShopProduct(payload)
      setOpen(false)
      await load()
    } catch (err) {
      setError(getError(err))
    } finally {
      setSaving('idle')
    }
  }

  const uploadImage = async (file?: File) => {
    if (!file) return
    setForm({ ...form, imageUrl: await imageFromFile(file) })
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">商城商品管理</h1>
          <p className="mt-2 text-sm text-slate-500">维护钓具、饵料、鱼获和菜品商品，支持库存、图片和上下架管理。</p>
        </div>
        <Button icon={<Plus size={16} />} onClick={openCreate}>新增商品</Button>
      </div>
      <ErrorBanner error={error} />
      <Card className="mb-4 p-4">
        <div className="grid gap-3 md:grid-cols-[1fr_180px_180px_auto]">
          <Field label="关键词"><Input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="商品名称" /></Field>
          <Field label="分类">
            <Select value={category} onChange={(e) => setCategory(e.target.value)}>
              <option value="">全部</option>
              <option value="equipment">钓具</option>
              <option value="bait">饵料</option>
              <option value="fish">鱼获</option>
              <option value="food">菜品</option>
            </Select>
          </Field>
          <Field label="状态">
            <Select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">全部</option>
              <option value="on">上架</option>
              <option value="off">下架</option>
            </Select>
          </Field>
          <div className="flex items-end"><Button variant="secondary" onClick={load}>查询</Button></div>
        </div>
      </Card>
      {loading ? <LoadingBlock /> : items.length === 0 ? <EmptyState title="暂无商品" /> : (
        <Table>
          <table className="w-full min-w-[980px]">
            <thead><tr><Th>商品</Th><Th>分类</Th><Th>价格</Th><Th>库存</Th><Th>状态</Th><Th>更新时间</Th><Th>操作</Th></tr></thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <Td>
                    <div className="flex items-center gap-3">
                      <div className="h-12 w-12 overflow-hidden rounded-md bg-slate-100">
                        {item.imageUrl ? <img src={item.imageUrl} alt={item.name} className="h-full w-full object-cover" /> : null}
                      </div>
                      <div>
                        <div className="font-semibold text-slate-900">{item.name}</div>
                        <div className="line-clamp-1 max-w-sm text-xs text-slate-400">{item.description || '-'}</div>
                      </div>
                    </div>
                  </Td>
                  <Td>{productCategoryText(item.category)}</Td>
                  <Td>{formatCent(item.price)}</Td>
                  <Td>{item.stock}</Td>
                  <Td><StatusBadge status={item.status} /></Td>
                  <Td>{formatDateTime(item.updateTime)}</Td>
                  <Td>
                    <div className={actionCell}>
                      <Button size="sm" variant="secondary" icon={<Edit3 size={14} />} onClick={() => openEdit(item)}>编辑</Button>
                      <Button size="sm" variant={item.status === 'on' ? 'secondary' : 'success'} onClick={async () => { await api.updateShopProductStatus(item.id, item.status === 'on' ? 'off' : 'on'); await load() }}>{item.status === 'on' ? '下架' : '上架'}</Button>
                      <ConfirmButton size="sm" variant="danger" message="确认删除该商品？" onConfirm={async () => { await api.deleteShopProduct(item.id); await load() }}>删除</ConfirmButton>
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </table>
        </Table>
      )}
      <Modal
        title={editing ? '编辑商品' : '新增商品'}
        open={open}
        onClose={() => setOpen(false)}
        footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button icon={<Save size={16} />} disabled={saving === 'saving'} onClick={save}>保存</Button></>}
      >
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="商品名称"><Input value={form.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
          <Field label="分类">
            <Select value={form.category || 'equipment'} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              <option value="equipment">钓具</option>
              <option value="bait">饵料</option>
              <option value="fish">鱼获</option>
              <option value="food">菜品</option>
            </Select>
          </Field>
          <Field label="价格（元）"><Input type="number" min="0" step="0.01" value={form.priceYuan || ''} onChange={(e) => setForm({ ...form, priceYuan: e.target.value })} /></Field>
          <Field label="库存"><Input type="number" min="0" value={form.stock ?? 0} onChange={(e) => setForm({ ...form, stock: Number(e.target.value) })} /></Field>
          <Field label="状态">
            <Select value={form.status || 'on'} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              <option value="on">上架</option>
              <option value="off">下架</option>
            </Select>
          </Field>
          <Field label="商品图片">
            <Input type="file" accept="image/*" onChange={(e) => uploadImage(e.target.files?.[0])} />
          </Field>
          <Field label="描述" className="md:col-span-2"><Textarea value={form.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
          {form.imageUrl ? <img src={form.imageUrl} alt="商品预览" className="max-h-52 rounded-lg border border-slate-200 object-cover md:col-span-2" /> : null}
        </div>
      </Modal>
    </div>
  )
}

type MenuForm = Partial<RestaurantMenu> & { priceYuan?: string }

const emptyMenuForm: MenuForm = {
  name: '',
  category: 'fresh_fish',
  priceYuan: '',
  stock: -1,
  imageUrl: '',
  description: '',
  isSpecial: 0,
  status: 'on'
}

export function RestaurantMenusPage() {
  const { currentPondId } = useMerchant()
  const [items, setItems] = useState<RestaurantMenu[]>([])
  const [form, setForm] = useState<MenuForm>(emptyMenuForm)
  const [editing, setEditing] = useState<RestaurantMenu | null>(null)
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState<SaveState>('idle')

  const load = useCallback(async () => {
    if (!currentPondId) return
    setLoading(true)
    setError('')
    try {
      setItems(await api.restaurantMenus(currentPondId))
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [currentPondId])

  useEffect(() => {
    load()
  }, [load])

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyMenuForm, pondId: currentPondId })
    setOpen(true)
  }

  const openEdit = (item: RestaurantMenu) => {
    setEditing(item)
    setForm({ ...item, priceYuan: String(Number(item.price || 0) / 100) })
    setOpen(true)
  }

  const save = async () => {
    if (!currentPondId) return
    setSaving('saving')
    setError('')
    try {
      const payload: Partial<RestaurantMenu> = {
        ...form,
        pondId: currentPondId,
        price: parseMoneyToCent(form.priceYuan),
        stock: Number(form.stock ?? -1),
        isSpecial: form.isSpecial ? 1 : 0,
        status: form.status || 'on'
      }
      delete (payload as MenuForm).priceYuan
      if (editing) await api.updateRestaurantMenu(editing.id, payload)
      else await api.createRestaurantMenu(payload)
      setOpen(false)
      await load()
    } catch (err) {
      setError(getError(err))
    } finally {
      setSaving('idle')
    }
  }

  const uploadImage = async (file?: File) => {
    if (!file) return
    setForm({ ...form, imageUrl: await imageFromFile(file) })
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">餐厅菜单管理</h1>
          <p className="mt-2 text-sm text-slate-500">维护鱼塘餐厅菜品、库存、招牌菜和上下架状态。</p>
        </div>
        <Button icon={<Plus size={16} />} onClick={openCreate} disabled={!currentPondId}>新增菜品</Button>
      </div>
      <ErrorBanner error={error} />
      <RequirePond>
        {loading ? <LoadingBlock /> : items.length === 0 ? <EmptyState title="暂无菜品" /> : (
          <Table>
            <table className="w-full min-w-[960px]">
              <thead><tr><Th>菜品</Th><Th>分类</Th><Th>价格</Th><Th>库存</Th><Th>招牌</Th><Th>状态</Th><Th>操作</Th></tr></thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <Td>
                      <div className="flex items-center gap-3">
                        <div className="h-12 w-12 overflow-hidden rounded-md bg-slate-100">
                          {item.imageUrl ? <img src={item.imageUrl} alt={item.name} className="h-full w-full object-cover" /> : null}
                        </div>
                        <div>
                          <div className="font-semibold text-slate-900">{item.name}</div>
                          <div className="line-clamp-1 max-w-sm text-xs text-slate-400">{item.description || '-'}</div>
                        </div>
                      </div>
                    </Td>
                    <Td>{menuCategoryText(item.category)}</Td>
                    <Td>{formatCent(item.price)}</Td>
                    <Td>{item.stock === -1 ? '不限' : item.stock}</Td>
                    <Td>{item.isSpecial ? <Badge tone="amber">招牌</Badge> : '-'}</Td>
                    <Td><StatusBadge status={item.status} /></Td>
                    <Td>
                      <div className={actionCell}>
                        <Button size="sm" variant="secondary" icon={<Edit3 size={14} />} onClick={() => openEdit(item)}>编辑</Button>
                        <Button size="sm" variant={item.status === 'on' ? 'secondary' : 'success'} onClick={async () => { await api.updateRestaurantMenuStatus(item.id, item.status === 'on' ? 'off' : 'on'); await load() }}>{item.status === 'on' ? '下架' : '上架'}</Button>
                        <ConfirmButton size="sm" variant="danger" message="确认删除该菜品？" onConfirm={async () => { await api.deleteRestaurantMenu(item.id); await load() }}>删除</ConfirmButton>
                      </div>
                    </Td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Table>
        )}
      </RequirePond>
      <Modal
        title={editing ? '编辑菜品' : '新增菜品'}
        open={open}
        onClose={() => setOpen(false)}
        footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button icon={<Save size={16} />} disabled={saving === 'saving'} onClick={save}>保存</Button></>}
      >
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="菜品名称"><Input value={form.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
          <Field label="分类">
            <Select value={form.category || 'fresh_fish'} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              <option value="fresh_fish">鲜鱼</option>
              <option value="cooked">加工菜品</option>
              <option value="drink">饮品</option>
            </Select>
          </Field>
          <Field label="价格（元）"><Input type="number" min="0" step="0.01" value={form.priceYuan || ''} onChange={(e) => setForm({ ...form, priceYuan: e.target.value })} /></Field>
          <Field label="库存" hint="-1 表示不限库存"><Input type="number" value={form.stock ?? -1} onChange={(e) => setForm({ ...form, stock: Number(e.target.value) })} /></Field>
          <Field label="状态">
            <Select value={form.status || 'on'} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              <option value="on">上架</option>
              <option value="off">下架</option>
            </Select>
          </Field>
          <Field label="招牌菜">
            <label className="flex h-10 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm text-slate-700">
              <input type="checkbox" checked={Boolean(form.isSpecial)} onChange={(e) => setForm({ ...form, isSpecial: e.target.checked ? 1 : 0 })} />
              设为招牌
            </label>
          </Field>
          <Field label="菜品图片" className="md:col-span-2"><Input type="file" accept="image/*" onChange={(e) => uploadImage(e.target.files?.[0])} /></Field>
          <Field label="描述" className="md:col-span-2"><Textarea value={form.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
          {form.imageUrl ? <img src={form.imageUrl} alt="菜品预览" className="max-h-52 rounded-lg border border-slate-200 object-cover md:col-span-2" /> : null}
        </div>
      </Modal>
    </div>
  )
}

export function RestaurantOrdersPage() {
  const { currentPondId } = useMerchant()
  const [items, setItems] = useState<RestaurantOrder[]>([])
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!currentPondId) return
    setLoading(true)
    setError('')
    try {
      setItems(await api.restaurantOrders({ pondId: currentPondId, status }))
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [currentPondId, status])

  useEffect(() => {
    load()
  }, [load])

  const updateStatus = async (id: number, nextStatus: string) => {
    await api.updateRestaurantOrderStatus(id, nextStatus)
    await load()
  }

  const nextAction = (item: RestaurantOrder) => {
    if (item.status === 'pending') return <Button size="sm" onClick={() => updateStatus(item.id, 'cooking')}>接单</Button>
    if (item.status === 'cooking') return <Button size="sm" onClick={() => updateStatus(item.id, 'delivered')}>标记已送达</Button>
    if (item.status === 'delivered') return <Button size="sm" onClick={() => updateStatus(item.id, 'completed')}>完成</Button>
    return null
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">餐厅订单</h1>
          <p className="mt-2 text-sm text-slate-500">处理待接单、制作中、已送达和已完成的餐厅订单。</p>
        </div>
        <Button variant="secondary" icon={<RefreshCw size={16} />} onClick={load}>刷新</Button>
      </div>
      <ErrorBanner error={error} />
      <RequirePond>
        <Card className="mb-4 p-4">
          <div className="grid gap-3 md:grid-cols-[220px_auto]">
            <Field label="订单状态">
              <Select value={status} onChange={(e) => setStatus(e.target.value)}>
                <option value="">全部</option>
                <option value="pending">待接单</option>
                <option value="cooking">制作中</option>
                <option value="delivered">已送达</option>
                <option value="completed">已完成</option>
              </Select>
            </Field>
            <div className="flex items-end"><Button variant="secondary" onClick={load}>查询</Button></div>
          </div>
        </Card>
        {loading ? <LoadingBlock /> : items.length === 0 ? <EmptyState title="暂无餐厅订单" /> : (
          <div className="grid gap-4 xl:grid-cols-2">
            {items.map((item) => (
              <Card key={item.id} className="p-5">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="font-bold text-slate-950">{item.orderNo || `#${item.id}`}</h2>
                      <StatusBadge status={item.status} />
                    </div>
                    <p className="mt-1 text-sm text-slate-500">{item.userNickname || '-'} {item.userPhone || ''} · 钓位 {item.spotCode || '-'}</p>
                  </div>
                  <strong className="text-lg text-slate-950">{formatCent(item.totalAmount)}</strong>
                </div>
                <div className="mt-4 rounded-md bg-slate-50 p-3">
                  {(item.items || []).length === 0 ? <p className="text-sm text-slate-400">暂无菜品明细</p> : item.items?.map((menu, index) => (
                    <div key={`${menu.menuName}-${index}`} className="flex justify-between py-1 text-sm">
                      <span>{menu.menuName || '-'} × {menu.quantity}</span>
                      <span>{formatCent(menu.price)}</span>
                    </div>
                  ))}
                </div>
                {item.remark ? <p className="mt-3 text-sm text-slate-500">备注：{item.remark}</p> : null}
                <div className="mt-4 flex justify-end">{nextAction(item)}</div>
              </Card>
            ))}
          </div>
        )}
      </RequirePond>
    </div>
  )
}

export function CatchesPage() {
  const [items, setItems] = useState<CatchRecord[]>([])
  const [detail, setDetail] = useState<CatchRecord | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const page = await api.catches({ pageNum: 1, pageSize: 500 })
      setItems(normalizePage(page).records)
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const recycle = async (item: CatchRecord) => {
    const value = window.prompt('请输入回收价格（元）')
    if (!value) return
    await api.recycleCatch(item.id, parseMoneyToCent(value))
    await load()
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">渔获回收</h1>
          <p className="mt-2 text-sm text-slate-500">处理用户提交的待回收渔获，确认后会同步进入餐厅鲜鱼库存。</p>
        </div>
        <Button variant="secondary" icon={<RefreshCw size={16} />} onClick={load}>刷新</Button>
      </div>
      <ErrorBanner error={error} />
      {loading ? <LoadingBlock /> : items.length === 0 ? <EmptyState title="暂无待回收渔获" /> : (
        <Table>
          <table className="w-full min-w-[980px]">
            <thead><tr><Th>渔获</Th><Th>用户</Th><Th>鱼塘/钓位</Th><Th>重量</Th><Th>数量</Th><Th>提交时间</Th><Th>操作</Th></tr></thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <Td>
                    <div className="flex items-center gap-3">
                      <div className="h-12 w-12 overflow-hidden rounded-md bg-slate-100">
                        {item.imageUrl ? <img src={item.imageUrl} alt={item.fishType} className="h-full w-full object-cover" /> : null}
                      </div>
                      <strong className="text-slate-900">{item.fishType}</strong>
                    </div>
                  </Td>
                  <Td><div>{item.userNickname || '-'}</div><div className="text-xs text-slate-400">{item.userPhone || '-'}</div></Td>
                  <Td><div>{item.pondName || '-'}</div><div className="text-xs text-slate-400">钓位 {item.spotCode || '-'}</div></Td>
                  <Td>{item.weight} kg</Td>
                  <Td>{item.quantity}</Td>
                  <Td>{formatDateTime(item.createTime)}</Td>
                  <Td>
                    <div className={actionCell}>
                      <Button size="sm" variant="secondary" icon={<Eye size={14} />} onClick={() => setDetail(item)}>详情</Button>
                      <Button size="sm" onClick={() => recycle(item)}>确认回收</Button>
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </table>
        </Table>
      )}
      <Modal title="渔获详情" open={Boolean(detail)} onClose={() => setDetail(null)} width="max-w-lg">
        {detail ? (
          <div className="space-y-4">
            {detail.imageUrl ? <img src={detail.imageUrl} alt={detail.fishType} className="max-h-72 w-full rounded-lg border border-slate-200 object-cover" /> : null}
            <div className="grid gap-3 text-sm">
              {[
                ['用户', `${detail.userNickname || '-'} ${detail.userPhone || ''}`],
                ['鱼种', detail.fishType],
                ['重量', `${detail.weight} kg`],
                ['数量', detail.quantity],
                ['鱼塘', detail.pondName || '-'],
                ['钓位', detail.spotCode || '-'],
                ['状态', statusText(detail.status)]
              ].map(([label, value]) => <div key={label} className="flex justify-between gap-4"><span className="text-slate-500">{label}</span><strong className="text-right text-slate-900">{value}</strong></div>)}
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  )
}

type StaffForm = Partial<Staff> & { password?: string }

const emptyStaffForm: StaffForm = {
  staffName: '',
  phone: '',
  role: 'checker',
  password: '',
  status: 'normal'
}

export function StaffPage() {
  const [items, setItems] = useState<Staff[]>([])
  const [keyword, setKeyword] = useState('')
  const [form, setForm] = useState<StaffForm>(emptyStaffForm)
  const [editing, setEditing] = useState<Staff | null>(null)
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState<SaveState>('idle')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setItems(await api.staff(keyword))
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [keyword])

  useEffect(() => {
    load()
  }, [load])

  const openCreate = () => {
    setEditing(null)
    setForm(emptyStaffForm)
    setOpen(true)
  }

  const openEdit = (item: Staff) => {
    setEditing(item)
    setForm({ ...item, password: '' })
    setOpen(true)
  }

  const save = async () => {
    setSaving('saving')
    setError('')
    try {
      if (editing) await api.updateStaff(editing.id, { staffName: form.staffName, phone: form.phone, role: form.role })
      else await api.createStaff({ staffName: form.staffName, phone: form.phone, role: form.role, password: form.password })
      setOpen(false)
      await load()
    } catch (err) {
      setError(getError(err))
    } finally {
      setSaving('idle')
    }
  }

  const resetPassword = async (item: Staff) => {
    const password = window.prompt('请输入新密码', '123456')
    if (!password) return
    const result = await api.resetStaffPassword(item.id, password)
    window.alert(`新密码：${result || password}`)
  }

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">员工管理</h1>
          <p className="mt-2 text-sm text-slate-500">老板专属功能，用于配置核销员、运营员、财务员和店长账号。</p>
        </div>
        <Button icon={<Users size={16} />} onClick={openCreate}>新增员工</Button>
      </div>
      <ErrorBanner error={error} />
      <Card className="mb-4 p-4">
        <div className="grid gap-3 md:grid-cols-[1fr_auto]">
          <Field label="搜索"><Input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="姓名或手机号" /></Field>
          <div className="flex items-end"><Button variant="secondary" onClick={load}>查询</Button></div>
        </div>
      </Card>
      {loading ? <LoadingBlock /> : items.length === 0 ? <EmptyState title="暂无员工" /> : (
        <Table>
          <table className="w-full min-w-[900px]">
            <thead><tr><Th>姓名</Th><Th>手机号</Th><Th>角色</Th><Th>状态</Th><Th>创建时间</Th><Th>操作</Th></tr></thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <Td><strong className="text-slate-900">{item.staffName}</strong></Td>
                  <Td>{item.phone}</Td>
                  <Td>{staffRoleText(item.role)}</Td>
                  <Td><StatusBadge status={item.status} /></Td>
                  <Td>{formatDateTime(item.createTime)}</Td>
                  <Td>
                    <div className={actionCell}>
                      <Button size="sm" variant="secondary" icon={<Edit3 size={14} />} onClick={() => openEdit(item)}>编辑</Button>
                      <Button size="sm" variant="secondary" onClick={() => resetPassword(item)}>重置密码</Button>
                      <Button size="sm" variant={item.status === 'normal' ? 'secondary' : 'success'} onClick={async () => { await api.updateStaffStatus(item.id, item.status === 'normal' ? 'disabled' : 'normal'); await load() }}>{item.status === 'normal' ? '禁用' : '启用'}</Button>
                      <ConfirmButton size="sm" variant="danger" message="确认删除该员工？有历史记录时后端会自动改为禁用。" onConfirm={async () => { await api.deleteStaff(item.id); await load() }}>删除</ConfirmButton>
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </table>
        </Table>
      )}
      <Modal
        title={editing ? '编辑员工' : '新增员工'}
        open={open}
        onClose={() => setOpen(false)}
        footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button icon={<Save size={16} />} disabled={saving === 'saving'} onClick={save}>保存</Button></>}
      >
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="姓名"><Input value={form.staffName || ''} onChange={(e) => setForm({ ...form, staffName: e.target.value })} /></Field>
          <Field label="手机号"><Input value={form.phone || ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></Field>
          <Field label="角色">
            <Select value={form.role || 'checker'} onChange={(e) => setForm({ ...form, role: e.target.value as Staff['role'] })}>
              <option value="checker">核销员</option>
              <option value="operator">运营员</option>
              <option value="finance">财务员</option>
              <option value="manager">店长</option>
            </Select>
          </Field>
          {!editing ? <Field label="初始密码"><Input type="password" value={form.password || ''} onChange={(e) => setForm({ ...form, password: e.target.value })} /></Field> : null}
        </div>
      </Modal>
    </div>
  )
}

export function LogsPage() {
  const [items, setItems] = useState<OperationLog[]>([])
  const [operatorName, setOperatorName] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setItems(await api.logs({ operatorName, startDate, endDate }))
    } catch (err) {
      setError(getError(err))
    } finally {
      setLoading(false)
    }
  }, [operatorName, startDate, endDate])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div>
      <div className="mb-5 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">操作日志</h1>
          <p className="mt-2 text-sm text-slate-500">查看商家后台关键操作记录，支持按操作人和时间范围筛选。</p>
        </div>
        <Button variant="secondary" icon={<RefreshCw size={16} />} onClick={load}>刷新</Button>
      </div>
      <ErrorBanner error={error} />
      <Card className="mb-4 p-4">
        <div className="grid gap-3 md:grid-cols-[1fr_180px_180px_auto]">
          <Field label="操作人"><Input value={operatorName} onChange={(e) => setOperatorName(e.target.value)} placeholder="姓名或账号" /></Field>
          <Field label="开始日期"><Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} /></Field>
          <Field label="结束日期"><Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} /></Field>
          <div className="flex items-end"><Button variant="secondary" onClick={load}>查询</Button></div>
        </div>
      </Card>
      {loading ? <LoadingBlock /> : items.length === 0 ? <EmptyState title="暂无日志" /> : (
        <Table>
          <table className="w-full min-w-[980px]">
            <thead><tr><Th>操作人</Th><Th>操作类型</Th><Th>操作对象</Th><Th>对象 ID</Th><Th>时间</Th><Th>IP</Th><Th>详情</Th></tr></thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <Td><strong className="text-slate-900">{item.operatorName || '-'}</strong></Td>
                  <Td>{item.actionType}</Td>
                  <Td>{item.targetType || '-'}</Td>
                  <Td>{item.targetId || '-'}</Td>
                  <Td>{formatDateTime(item.createTime)}</Td>
                  <Td>{item.ip || '-'}</Td>
                  <Td className="max-w-md">{item.detail || '-'}</Td>
                </tr>
              ))}
            </tbody>
          </table>
        </Table>
      )}
    </div>
  )
}
