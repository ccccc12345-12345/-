<script setup lang="ts">
import type { RestaurantOrderItem } from '@/api/restaurant'

const props = withDefaults(
  defineProps<{
    items: RestaurantOrderItem[]
    compact?: boolean
  }>(),
  { compact: false }
)

const money = (value?: number) => `¥${((value || 0) / 100).toFixed(2)}`
const subtotal = (item: RestaurantOrderItem) => (item.price || 0) * (item.quantity || 0)
</script>

<template>
  <div class="items-sheet" :class="{ compact }">
    <div class="items-head">
      <span class="col-name">菜品</span>
      <span class="col-price">单价</span>
      <span class="col-qty">数量</span>
      <span class="col-total">小计</span>
    </div>
    <div v-for="item in items" :key="item.id" class="item-row">
      <span class="col-name">
        <span class="item-name">{{ item.menuName }}</span>
      </span>
      <span class="col-price">{{ money(item.price) }}</span>
      <span class="col-qty">{{ item.quantity }}</span>
      <span class="col-total">{{ money(subtotal(item)) }}</span>
    </div>
  </div>
</template>

<style scoped>
.items-sheet {
  border-top: 1px solid var(--fp-border);
}

.items-head,
.item-row {
  display: grid;
  grid-template-columns: 1fr 100px 80px 100px;
  gap: var(--fp-space-4);
  padding: 14px 0;
  border-bottom: 1px solid var(--fp-border);
  align-items: center;
}

.items-head {
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--fp-muted);
  font-weight: 700;
}

.item-row {
  color: var(--fp-text);
  transition: background-color var(--fp-dur-fast) var(--fp-ease-out);
}

.item-row:hover {
  background: oklch(97% 0.01 100 / 0.6);
}

.item-name {
  font-weight: 600;
}

.col-price,
.col-qty,
.col-total {
  font-variant-numeric: tabular-nums;
}

.col-total {
  font-weight: 700;
  color: var(--fp-text);
}

.compact .items-head,
.compact .item-row {
  padding: 10px 0;
  grid-template-columns: 1fr 80px 60px 80px;
  gap: var(--fp-space-3);
}

@media (max-width: 560px) {
  .items-head,
  .item-row {
    grid-template-columns: 1fr 70px 50px 70px;
    gap: var(--fp-space-2);
  }

  .items-head {
    font-size: 10px;
  }
}
</style>
