export interface MerchantConsoleBridge {
  username: string
  result: {
    token: string
    userId: string
    role: number
    adminType?: number | null
    pondId?: number | null
    staffId?: string
    merchantId?: string
    staffRole?: 'checker' | 'operator' | 'finance' | 'manager' | ''
    staffName?: string
  }
}

export const merchantConsoleUrl = (path = '/merchant/dashboard', bridge?: MerchantConsoleBridge) => {
  const target = new URL(window.location.href)
  if (target.port === '3002') {
    target.port = '3001'
  }
  target.pathname = path.startsWith('/') ? path : `/${path}`
  target.search = ''
  target.hash = bridge ? `merchantAuth=${encodeURIComponent(JSON.stringify(bridge))}` : ''
  return target.toString()
}

export const redirectMerchantConsole = (path = '/merchant/dashboard', bridge?: MerchantConsoleBridge) => {
  window.location.assign(merchantConsoleUrl(path, bridge))
}
