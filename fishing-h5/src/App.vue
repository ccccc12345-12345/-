<script setup lang="ts">
import { useRoute } from 'vue-router'
import TabBar from '@/components/TabBar.vue'

const route = useRoute()
</script>

<template>
  <div class="app-wrapper">
    <router-view />
    <TabBar v-if="!route.path.startsWith('/login') && !route.path.startsWith('/draw') && !route.path.startsWith('/admin')" />
  </div>
</template>

<style scoped>
.app-wrapper {
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--bg);
  position: relative;
}
</style>
