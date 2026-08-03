import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Navigate, NavLink, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import {
  BarChart3,
  Bell,
  CalendarDays,
  ChevronDown,
  ClipboardCheck,
  FileClock,
  Fish,
  LayoutDashboard,
  LogOut,
  Map,
  Menu,
  ReceiptText,
  ShoppingBag,
  Trophy,
  Users,
  Utensils,
  X
} from 'lucide-react'
import { api } from './api'
import { AuthProvider, resolveRole, useAuth } from './auth'
import { roleLabels } from './helpers'
import { MerchantContext } from './merchantContext'
import {
  AnnouncementsPage,
  CatchesPage,
  CheckinPage,
  DashboardPage,
  EventReviewsPage,
  EventSignupsPage,
  EventsPage,
  LogsPage,
  OrdersPage,
  PondBoardPage,
  PondsPage,
  RestaurantMenusPage,
  RestaurantOrdersPage,
  RevenuePage,
  SessionsPage,
  ShopProductsPage,
  SpotEditorPage,
  StaffPage
} from './pages'
import type { MerchantRoute, Pond, StaffRole } from './types'
import { Badge, Button, Card, Field, Input, Select, cn } from './ui'

const ownerManager: StaffRole[] = ['owner', 'manager']
const operatorRoles: StaffRole[] = ['owner', 'manager', 'operator']
const financeRoles: StaffRole[] = ['owner', 'manager', 'finance']
const checkerRoles: StaffRole[] = ['owner', 'manager', 'checker']

const merchantRoutes: MerchantRoute[] = [
  { path: 'dashboard', label: '工作台', icon: LayoutDashboard, roles: ownerManager, element: <DashboardPage /> },
  { path: 'ponds', label: '鱼塘管理', icon: Map, roles: operatorRoles, element: <PondsPage /> },
  { path: 'pond-board', label: '鱼塘看板', icon: LayoutDashboard, roles: ['owner', 'manager', 'operator', 'checker'], element: <PondBoardPage /> },
  { path: 'sessions', label: '场次管理', icon: CalendarDays, roles: operatorRoles, element: <SessionsPage /> },
  { path: 'announcements', label: '公告管理', icon: Bell, roles: operatorRoles, element: <AnnouncementsPage /> },
  { path: 'events', label: '活动管理', icon: Trophy, roles: operatorRoles, element: <EventsPage /> },
  { path: 'checkin', label: '核销签到', icon: ClipboardCheck, roles: checkerRoles, element: <CheckinPage /> },
  { path: 'orders', label: '订单管理', icon: ReceiptText, roles: financeRoles, element: <OrdersPage /> },
  { path: 'revenue', label: '收益统计', icon: BarChart3, roles: financeRoles, element: <RevenuePage /> },
  { path: 'shop/products', label: '商城商品', icon: ShoppingBag, roles: operatorRoles, element: <ShopProductsPage /> },
  { path: 'restaurant/menus', label: '餐厅菜单', icon: Utensils, roles: operatorRoles, element: <RestaurantMenusPage /> },
  { path: 'restaurant/orders', label: '餐厅订单', icon: ReceiptText, roles: ownerManager, element: <RestaurantOrdersPage /> },
  { path: 'catches', label: '渔获回收', icon: Fish, roles: operatorRoles, element: <CatchesPage /> },
  { path: 'staff', label: '员工管理', icon: Users, roles: ['owner'], element: <StaffPage /> },
  { path: 'logs', label: '操作日志', icon: FileClock, roles: ownerManager, element: <LogsPage /> },
  { path: 'ponds/:id/spots', label: '钓位编辑', icon: Map, roles: operatorRoles, element: <SpotEditorPage />, hidden: true },
  { path: 'events/:id/signups', label: '活动报名', icon: Users, roles: operatorRoles, element: <EventSignupsPage />, hidden: true },
  { path: 'events/:id/reviews', label: '活动评价', icon: Trophy, roles: operatorRoles, element: <EventReviewsPage />, hidden: true }
]

const defaultPath = (role: StaffRole | null) => {
  if (role === 'checker') return '/merchant/checkin'
  if (role === 'finance') return '/merchant/revenue'
  if (role === 'operator') return '/merchant/pond-board'
  return '/merchant/dashboard'
}

const isAllowed = (role: StaffRole | null, route: MerchantRoute) => Boolean(role && route.roles.includes(role))

function LoginPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('merchant')
  const [password, setPassword] = useState('merchant')
  const [captchaCode, setCaptchaCode] = useState('')
  const [captchaKey, setCaptchaKey] = useState('')
  const [captchaImage, setCaptchaImage] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const refreshCaptcha = useCallback(async () => {
    try {
      const res = await api.captcha()
      setCaptchaKey(res.captchaKey)
      setCaptchaImage(`data:image/png;base64,${res.imageBase64}`)
      setCaptchaCode('')
    } catch (err) {
      setError(err instanceof Error ? err.message : '验证码加载失败')
    }
  }, [])

  useEffect(() => {
    refreshCaptcha()
  }, [refreshCaptcha])

  useEffect(() => {
    if (auth.isAuthenticated) {
      navigate(defaultPath(auth.role), { replace: true })
    }
  }, [auth.isAuthenticated, auth.role, navigate])

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const result = await api.login({ username, password, captchaKey, captchaCode })
      const role = resolveRole(result)
      if (!role) {
        setError('当前账号不是商家或商家员工账号')
        await refreshCaptcha()
        return
      }
      auth.setLogin(username, result)
      const state = location.state as { from?: { pathname?: string } } | null
      const from = state?.from?.pathname?.startsWith('/merchant') ? state.from.pathname : defaultPath(role)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : '登录失败')
      await refreshCaptcha()
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="min-h-screen bg-slate-100">
      <div className="grid min-h-screen lg:grid-cols-[1fr_460px]">
        <section className="hidden bg-slate-950 px-12 py-10 text-white lg:flex lg:flex-col lg:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-emerald-500 text-slate-950">
              <Fish size={25} />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-normal">钓鱼系统商家后台</h1>
              <p className="mt-1 text-sm text-slate-400">Fishery Merchant Console</p>
            </div>
          </div>
          <div>
            <p className="max-w-xl text-4xl font-bold leading-tight tracking-normal">预约、钓位、核销、收益和餐厅商城统一管理。</p>
            <div className="mt-10 grid max-w-xl grid-cols-3 gap-3">
              {['鱼塘看板', '订单收益', '员工权限'].map((item) => (
                <div key={item} className="rounded-lg border border-white/10 bg-white/5 px-4 py-3 text-sm font-semibold text-slate-200">{item}</div>
              ))}
            </div>
          </div>
          <p className="text-xs text-slate-500">默认老板账号：merchant / merchant</p>
        </section>

        <section className="flex items-center justify-center px-4 py-10">
          <Card className="w-full max-w-md p-6">
            <div className="mb-6">
              <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-lg bg-emerald-700 text-white lg:hidden">
                <Fish size={24} />
              </div>
              <h2 className="text-2xl font-bold text-slate-950">商家端登录</h2>
              <p className="mt-2 text-sm text-slate-500">老板、店长和员工账号都从这里进入。</p>
            </div>
            {error ? <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">{error}</div> : null}
            <form className="space-y-4" onSubmit={submit}>
              <Field label="账号 / 手机号">
                <Input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
              </Field>
              <Field label="密码">
                <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
              </Field>
              <Field label="验证码">
                <div className="grid grid-cols-[1fr_132px] gap-2">
                  <Input value={captchaCode} onChange={(e) => setCaptchaCode(e.target.value)} />
                  <button type="button" className="h-10 overflow-hidden rounded-md border border-slate-200 bg-slate-50" onClick={refreshCaptcha}>
                    {captchaImage ? <img src={captchaImage} alt="验证码" className="h-full w-full object-cover" /> : <span className="text-xs text-slate-400">点击刷新</span>}
                  </button>
                </div>
              </Field>
              <Button className="w-full" size="lg" disabled={loading}>{loading ? '登录中' : '登录'}</Button>
            </form>
          </Card>
        </section>
      </div>
    </main>
  )
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const auth = useAuth()
  const location = useLocation()
  if (!auth.isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  if (!auth.role) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}

function RoleGate({ route }: { route: MerchantRoute }) {
  const auth = useAuth()
  if (!isAllowed(auth.role, route)) {
    return <Navigate to={defaultPath(auth.role)} replace />
  }
  return route.element
}

function MerchantLayout() {
  const auth = useAuth()
  const [ponds, setPonds] = useState<Pond[]>([])
  const [currentPondId, setCurrentPondId] = useState<number | undefined>(() => {
    const raw = localStorage.getItem('fishing_admin_current_pond')
    return raw ? Number(raw) : undefined
  })
  const [menuOpen, setMenuOpen] = useState(false)

  const reloadPonds = useCallback(async () => {
    const list = await api.ponds()
    setPonds(list)
    setCurrentPondId((prev) => {
      if (prev && list.some((pond) => pond.id === prev)) return prev
      const next = auth.user?.pondId || list[0]?.id
      return next || undefined
    })
  }, [auth.user?.pondId])

  useEffect(() => {
    reloadPonds().catch(() => setPonds([]))
  }, [reloadPonds])

  useEffect(() => {
    if (currentPondId) localStorage.setItem('fishing_admin_current_pond', String(currentPondId))
    else localStorage.removeItem('fishing_admin_current_pond')
  }, [currentPondId])

  const currentPond = useMemo(() => ponds.find((pond) => pond.id === currentPondId), [ponds, currentPondId])
  const contextValue = useMemo(() => ({ ponds, currentPondId, currentPond, setCurrentPondId, reloadPonds }), [ponds, currentPondId, currentPond, reloadPonds])
  const visibleRoutes = merchantRoutes.filter((route) => !route.hidden && isAllowed(auth.role, route))

  const returnUserSide = () => {
    const target = window.location.origin.replace(':3001', ':3002')
    window.open(target, '_blank')
  }

  const sidebar = (
    <aside className="flex h-full flex-col bg-slate-950 text-white">
      <div className="flex h-16 items-center justify-between border-b border-white/10 px-5">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-500 text-slate-950"><Fish size={21} /></div>
          <div>
            <p className="font-bold tracking-normal">商家后台</p>
            <p className="text-xs text-slate-500">Merchant</p>
          </div>
        </div>
        <button className="rounded-md p-1 text-slate-400 hover:bg-white/10 lg:hidden" onClick={() => setMenuOpen(false)}>
          <X size={20} />
        </button>
      </div>
      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
        {visibleRoutes.map((route) => {
          const Icon = route.icon
          return (
            <NavLink
              key={route.path}
              to={`/merchant/${route.path}`}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex h-10 items-center gap-3 rounded-md px-3 text-sm font-semibold text-slate-300 transition hover:bg-white/10 hover:text-white',
                  isActive && 'bg-white text-slate-950 hover:bg-white hover:text-slate-950'
                )
              }
            >
              <Icon size={17} />
              {route.label}
            </NavLink>
          )
        })}
      </nav>
      <div className="border-t border-white/10 p-4">
        <div className="rounded-lg bg-white/5 p-3">
          <p className="text-sm font-semibold text-white">{auth.user?.displayName || auth.user?.username}</p>
          <p className="mt-1 text-xs text-slate-400">{auth.role ? roleLabels[auth.role] : '商家账号'}</p>
        </div>
      </div>
    </aside>
  )

  return (
    <MerchantContext.Provider value={contextValue}>
      <div className="min-h-screen bg-slate-100 lg:grid lg:grid-cols-[248px_1fr]">
        <div className="hidden lg:block">{sidebar}</div>
        {menuOpen ? <div className="fixed inset-0 z-50 lg:hidden"><div className="absolute inset-0 bg-slate-950/50" onClick={() => setMenuOpen(false)} /><div className="relative h-full w-[280px]">{sidebar}</div></div> : null}
        <div className="min-w-0">
          <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur">
            <div className="flex min-h-16 flex-col gap-3 px-4 py-3 lg:flex-row lg:items-center lg:justify-between lg:px-6">
              <div className="flex items-center gap-3">
                <button className="rounded-md border border-slate-200 p-2 text-slate-700 lg:hidden" onClick={() => setMenuOpen(true)}>
                  <Menu size={20} />
                </button>
                <div>
                  <p className="text-sm font-semibold text-slate-500">当前商家</p>
                  <h1 className="text-lg font-bold text-slate-950">{auth.user?.displayName || auth.user?.username || '商家'}</h1>
                </div>
                {auth.role ? <Badge tone="green">{roleLabels[auth.role]}</Badge> : null}
              </div>
              <div className="grid gap-2 sm:grid-cols-[260px_auto_auto]">
                <label className="relative">
                  <Select value={currentPondId || ''} onChange={(e) => setCurrentPondId(e.target.value ? Number(e.target.value) : undefined)} className="pr-9">
                    <option value="">全部鱼塘 / 未选择</option>
                    {ponds.map((pond) => <option key={pond.id} value={pond.id}>{pond.name}</option>)}
                  </Select>
                  <ChevronDown className="pointer-events-none absolute right-3 top-2.5 text-slate-400" size={16} />
                </label>
                <Button variant="secondary" onClick={returnUserSide}>返回用户端</Button>
                <Button variant="ghost" icon={<LogOut size={16} />} onClick={auth.logout}>退出</Button>
              </div>
            </div>
          </header>
          <main className="mx-auto w-full max-w-[1480px] px-4 py-6 lg:px-6">
            <Outlet />
          </main>
        </div>
      </div>
    </MerchantContext.Provider>
  )
}

function AppRoutes() {
  const auth = useAuth()
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/merchant" element={<ProtectedRoute><MerchantLayout /></ProtectedRoute>}>
        <Route index element={<Navigate to={defaultPath(auth.role).replace('/merchant/', '')} replace />} />
        <Route path="time-slots" element={<Navigate to="/merchant/sessions" replace />} />
        <Route path="reservations" element={<Navigate to="/merchant/orders" replace />} />
        <Route path="spots" element={<Navigate to="/merchant/ponds" replace />} />
        <Route path="draw-results" element={<Navigate to="/merchant/pond-board" replace />} />
        {merchantRoutes.map((route) => (
          <Route key={route.path} path={route.path} element={<RoleGate route={route} />} />
        ))}
      </Route>
      <Route path="/" element={<Navigate to={auth.isAuthenticated ? defaultPath(auth.role) : '/login'} replace />} />
      <Route path="*" element={<Navigate to={auth.isAuthenticated ? defaultPath(auth.role) : '/login'} replace />} />
    </Routes>
  )
}

export function App() {
  useEffect(() => {
    document.title = '钓鱼系统商家后台'
  }, [])

  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
