import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.directive('fp-spotlight', {
  mounted(el: HTMLElement) {
    const handler = (e: MouseEvent) => {
      const rect = el.getBoundingClientRect()
      const x = ((e.clientX - rect.left) / rect.width) * 100
      const y = ((e.clientY - rect.top) / rect.height) * 100
      el.style.setProperty('--mouse-x', `${x}%`)
      el.style.setProperty('--mouse-y', `${y}%`)
    }
    el.addEventListener('mousemove', handler)
    ;(el as any)._fpSpotlightHandler = handler
  },
  unmounted(el: HTMLElement) {
    const handler = (el as any)._fpSpotlightHandler
    if (handler) el.removeEventListener('mousemove', handler)
  }
})

app.directive('fp-magnetic', {
  mounted(el: HTMLElement) {
    const handler = (e: MouseEvent) => {
      const rect = el.getBoundingClientRect()
      const cx = rect.left + rect.width / 2
      const cy = rect.top + rect.height / 2
      const rx = ((e.clientY - cy) / (rect.height / 2)) * -2.5
      const ry = ((e.clientX - cx) / (rect.width / 2)) * 2.5
      el.style.setProperty('--rotate-x', `${rx}deg`)
      el.style.setProperty('--rotate-y', `${ry}deg`)
    }
    const leave = () => {
      el.style.setProperty('--rotate-x', '0deg')
      el.style.setProperty('--rotate-y', '0deg')
    }
    el.addEventListener('mousemove', handler)
    el.addEventListener('mouseleave', leave)
    ;(el as any)._fpMagneticHandlers = { handler, leave }
  },
  unmounted(el: HTMLElement) {
    const handlers = (el as any)._fpMagneticHandlers
    if (handlers) {
      el.removeEventListener('mousemove', handlers.handler)
      el.removeEventListener('mouseleave', handlers.leave)
    }
  }
})

app.directive('fp-ripple', {
  mounted(el: HTMLElement) {
    const handler = (e: MouseEvent) => {
      const rect = el.getBoundingClientRect()
      const circle = document.createElement('span')
      const diameter = Math.max(rect.width, rect.height)
      const radius = diameter / 2
      circle.style.width = circle.style.height = `${diameter}px`
      circle.style.left = `${e.clientX - rect.left - radius}px`
      circle.style.top = `${e.clientY - rect.top - radius}px`
      circle.classList.add('ripple')
      const existing = el.getElementsByClassName('ripple')[0]
      if (existing) existing.remove()
      el.appendChild(circle)
      setTimeout(() => circle.remove(), 650)
    }
    el.addEventListener('click', handler)
    ;(el as any)._fpRippleHandler = handler
  },
  unmounted(el: HTMLElement) {
    const handler = (el as any)._fpRippleHandler
    if (handler) el.removeEventListener('click', handler)
  }
})

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
