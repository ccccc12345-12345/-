export type SseEventCallback = (data: any) => void

export interface MerchantSseClientOptions {
  baseUrl?: string
  token: string
  clientId?: string
}

interface ListenerMap {
  [key: string]: SseEventCallback[]
}

/**
 * 商家端 SSE 客户端
 *
 * 通过 EventSource 与后端 /api/merchant/sse/connect 建立长连接，
 * 接收实时推送事件并按事件类型分发给注册监听器。
 */
export class MerchantSseClient {
  private es: EventSource | null = null
  private url: string
  private errorCount = 0
  private closed = false
  private listeners: ListenerMap = {}
  private fallbackTimer: number | undefined = undefined

  constructor(options: MerchantSseClientOptions) {
    const baseUrl = options.baseUrl ?? ''
    const clientId = options.clientId ?? generateClientId()
    this.url = `${baseUrl}/api/merchant/sse/connect?token=${encodeURIComponent(options.token)}&clientId=${clientId}`
  }

  connect() {
    if (this.closed || typeof EventSource === 'undefined') {
      return
    }

    try {
      this.es = new EventSource(this.url)
      const self = this

      this.es.onopen = function () {
        self.errorCount = 0
        self.disableFallback()
      }

      this.es.onerror = function () {
        self.errorCount = self.errorCount + 1
        self.reconnect()
      }

      for (const eventType in self.listeners) {
        self.bindEventSourceListener(eventType)
      }
    } catch (e) {
      this.errorCount = this.errorCount + 1
      this.reconnect()
    }
  }

  close() {
    this.closed = true
    this.disableFallback()
    if (this.es) {
      this.es.close()
      this.es = null
    }
  }

  on(eventType: string, callback: SseEventCallback) {
    if (!this.listeners[eventType]) {
      this.listeners[eventType] = []
      this.bindEventSourceListener(eventType)
    }
    this.listeners[eventType].push(callback)
    const self = this
    return function () {
      self.off(eventType, callback)
    }
  }

  off(eventType: string, callback: SseEventCallback) {
    const arr = this.listeners[eventType]
    if (!arr) {
      return
    }
    const idx = arr.indexOf(callback)
    if (idx >= 0) {
      arr.splice(idx, 1)
    }
    if (arr.length === 0) {
      delete this.listeners[eventType]
    }
  }

  enableFallback(intervalMs: number, callback: () => void) {
    this.disableFallback()
    this.fallbackTimer = window.setInterval(callback, intervalMs)
  }

  disableFallback() {
    if (this.fallbackTimer !== undefined) {
      window.clearInterval(this.fallbackTimer)
      this.fallbackTimer = undefined
    }
  }

  private bindEventSourceListener(eventType: string) {
    if (!this.es) {
      return
    }
    const self = this
    this.es.addEventListener(eventType, function (event) {
      self.handleEvent(event as MessageEvent)
    })
  }

  private handleEvent(event: MessageEvent) {
    if (!event.data) {
      return
    }
    let data: any
    try {
      data = JSON.parse(event.data)
    } catch {
      data = event.data
    }
    const eventType = event.type
    const arr = this.listeners[eventType]
    if (!arr) {
      return
    }
    for (let i = 0; i < arr.length; i++) {
      try {
        arr[i](data)
      } catch (e) {
        console.error('SSE 事件处理异常:', e)
      }
    }
  }

  private reconnect() {
    if (this.es) {
      this.es.close()
      this.es = null
    }
    if (this.closed) {
      return
    }
    let delay = 1000
    let i = 0
    while (i < this.errorCount && delay < 30000) {
      delay = delay * 2
      i = i + 1
    }
    const self = this
    window.setTimeout(function () {
      if (!self.closed) {
        self.connect()
      }
    }, delay)
  }
}

function generateClientId(): string {
  return Date.now().toString() + '-' + Math.random().toString(36).slice(2, 10)
}
