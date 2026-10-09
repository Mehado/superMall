<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { buildGroups } from '@/lib/marquee-images'

/**
 * 横向流动图片墙（多行「跑马灯」，无限无缝循环，纯 CSS 动画）。
 *
 * 用在首页字标**后面**：图片左右滚动，字标压在图片**之上**（z-index 更高），字永远不会被盖住。
 *
 * 三层动效（参考 shop.app 的 hero 卡片）：
 *   ① 轨道层：横向无限循环（`.lane-track`，纯 CSS keyframes）
 *   ② 卡片层：鼠标 3D 倾斜（`.flow-card`，读容器上的 --mx/--my）+ 4s 错峰待机浮动（`.card-inner`）
 *   ③ 图片层：反向视差（`img` 微微位移）
 * 三层各管各的 transform，互不打架；`prefers-reduced-motion` 或 `:tilt="false"` 时全部退化。
 *
 * 无缝循环的关键：每组 n 张要重复到比容器宽，再把轨道渲染两遍，
 * 位移 = 半条轨道宽 + 半个间距。一组够不够宽取决于容器宽度，所以用 ResizeObserver 量一次。
 */
const props = defineProps({
  images: { type: Array, default: () => [] },
  /** 行数（同时可见的图片行）；2 行卡片能大一些 */
  lanes: { type: Number, default: 2 },
  /** 每组张数 */
  perGroup: { type: Number, default: 5 },
  /** 基准一圈时长（秒）；每行按倍率错开 */
  speed: { type: Number, default: 34 },
  /** 卡片间距（px） */
  gap: { type: Number, default: 14 },
  /** 是否开启鼠标倾斜 / 视差 / 待机浮动（嫌费性能就关掉） */
  tilt: { type: Boolean, default: true }
})

/** 每行的时长倍率与方向：速度不一致、方向交错，避免「一条传送带」的僵硬感 */
const RATE = [1, 1.28, 1.12, 1.42, 1.06, 1.33, 1.18]

const rootRef = ref(null)
const copies = ref(3)
let ro = null

/* ---------------- 鼠标倾斜（--mx / --my ∈ [-1, 1]） ---------------- */
const mx = ref(0)
const my = ref(0)
let raf = 0
let enabled = false

function onMove(e) {
  if (raf) return
  raf = requestAnimationFrame(() => {
    raf = 0
    const el = rootRef.value
    if (!el) return
    const r = el.getBoundingClientRect()
    if (!r.width || !r.height) return
    const nx = (e.clientX - (r.left + r.width / 2)) / (r.width / 2)
    const ny = (e.clientY - (r.top + r.height / 2)) / (r.height / 2)
    mx.value = Math.max(-1, Math.min(1, Number(nx.toFixed(3))))
    my.value = Math.max(-1, Math.min(1, Number(ny.toFixed(3))))
  })
}
function reset() {
  mx.value = 0
  my.value = 0
}

const groups = computed(() =>
  buildGroups(props.images, { columns: props.lanes, perGroup: props.perGroup })
)

const lanes = computed(() =>
  groups.value.map((group, i) => {
    const half = []
    for (let c = 0; c < copies.value; c++) half.push(...group)
    return {
      key: i,
      items: [...half, ...half], // 两半 → 无缝
      dur: `${(props.speed * (RATE[i % RATE.length] || 1)).toFixed(2)}s`,
      delay: `-${(i * 4.3).toFixed(1)}s`,
      reverse: i % 2 === 1
    }
  })
)

/** 按实际卡片宽度算「一组」多宽，得出需要几份才能盖满容器 */
function measure() {
  const root = rootRef.value
  if (!root) return
  const card = root.querySelector('.flow-card')
  if (!card) return
  const cardW = card.getBoundingClientRect().width
  if (!cardW) return
  const groupW = props.perGroup * cardW + (props.perGroup - 1) * props.gap
  const need = Math.max(1, Math.ceil((root.clientWidth + props.gap) / (groupW + props.gap)))
  if (need !== copies.value) copies.value = need
}

onMounted(() => {
  measure()
  if (typeof ResizeObserver !== 'undefined' && rootRef.value) {
    ro = new ResizeObserver(() => measure())
    ro.observe(rootRef.value)
  }
  // 倾斜只在「有真鼠标 + 没开减少动效」时启用
  const hover = window.matchMedia?.('(hover: hover)').matches ?? true
  const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  enabled = props.tilt && hover && !reduce
  if (enabled) {
    window.addEventListener('mousemove', onMove, { passive: true })
    document.addEventListener('mouseleave', reset)
  }
})

onBeforeUnmount(() => {
  ro?.disconnect()
  if (enabled) {
    window.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseleave', reset)
  }
  if (raf) cancelAnimationFrame(raf)
})
</script>

<template>
  <div
    v-if="lanes.length"
    ref="rootRef"
    class="flow"
    :class="{ 'is-tilt': tilt }"
    :style="{ '--flow-gap': `${gap}px`, '--mx': mx, '--my': my }"
    aria-hidden="true"
  >
    <div
      v-for="lane in lanes"
      :key="lane.key"
      class="flow-lane"
      :class="{ 'is-reverse': lane.reverse }"
    >
      <div class="lane-track" :style="{ '--dur': lane.dur, '--delay': lane.delay }">
        <span
          v-for="(src, i) in lane.items"
          :key="`${lane.key}-${i}`"
          class="flow-card"
          :style="{ '--float-delay': `-${((i % 5) * 0.74 + lane.key * 0.31).toFixed(2)}s` }"
        >
          <span class="card-inner">
            <img :src="src" alt="" loading="lazy" decoding="async" />
          </span>
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 容器：绝对定位由使用方给尺寸；这里只管裁切、左右渐隐、不拦鼠标 */
.flow {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--flow-gap);
  overflow: hidden;
  pointer-events: none;
  contain: paint;
  -webkit-mask-image: linear-gradient(90deg, transparent 0, #000 7%, #000 93%, transparent 100%);
  mask-image: linear-gradient(90deg, transparent 0, #000 7%, #000 93%, transparent 100%);
}

/* 每条轨道自己一个透视点：卡片倾斜才有立体感；overflow 只做横向裁切 */
.flow-lane {
  overflow: hidden;
  perspective: 1100px;
  /* 给「待机浮动」留出上下余地，否则浮起来会被裁掉一条边 */
  padding-block: 5px;
}

.lane-track {
  display: flex;
  gap: var(--flow-gap);
  width: max-content;
  animation: flow-x var(--dur) linear infinite;
  animation-delay: var(--delay);
  will-change: transform;
}
.flow-lane.is-reverse .lane-track {
  animation-direction: reverse;
}

/* 位移 = 半条轨道宽 + 半个间距（补掉两半之间那个 gap），接缝才看不出 */
@keyframes flow-x {
  from {
    transform: translate3d(0, 0, 0);
  }
  to {
    transform: translate3d(calc(-50% - var(--flow-gap) / 2), 0, 0);
  }
}

/* ②-a 卡片：跟着鼠标做 3D 倾斜（读容器上的 --mx/--my） */
.flow-card {
  flex: 0 0 auto;
  height: clamp(64px, 7.6vw, 112px);
  aspect-ratio: 4 / 5;
  transform: rotateX(calc(var(--my, 0) * -5deg)) rotateY(calc(var(--mx, 0) * 7deg));
  transition: transform 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}
.flow:not(.is-tilt) .flow-card {
  transform: none;
}

/* ②-b 卡片内层：4s 错峰待机浮动（和上面的倾斜是两个独立 transform，互不覆盖） */
.card-inner {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 14px 30px -22px rgba(22, 19, 15, 0.7);
  animation: card-float 4.4s ease-in-out infinite alternate;
  animation-delay: var(--float-delay);
  will-change: transform;
}

@keyframes card-float {
  from {
    transform: translate3d(0, -3px, 0);
  }
  to {
    transform: translate3d(0, 3px, 0);
  }
}

/* ③ 图片：反向视差（卡片往右倒时，图稍微往左挪） */
.card-inner img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: translate3d(calc(var(--mx, 0) * 6px), calc(var(--my, 0) * 5px), 0) scale(1.08);
  transition: transform 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 晕动症：停掉所有动效（保留静态一排图） */
@media (prefers-reduced-motion: reduce) {
  .lane-track,
  .card-inner {
    animation: none;
  }
  .flow-card,
  .card-inner img {
    transform: none;
    transition: none;
  }
}

/* 触屏没有 hover：不做倾斜与视差 */
@media (hover: none) {
  .flow-card,
  .card-inner img {
    transform: none;
  }
}

/* 窄屏：卡片小一点 */
@media (max-width: 900px) {
  .flow-card {
    height: clamp(56px, 15vw, 84px);
  }
}
</style>
