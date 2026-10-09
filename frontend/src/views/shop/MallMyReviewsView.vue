<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import Icon from '@/components/Icon.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MStars from '@/components/mall/MStars.vue'
import MModal from '@/components/mall/MModal.vue'
import MUploader from '@/components/mall/MUploader.vue'
import MPager from '@/components/mall/MPager.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallCenterApi, mallReviewApi } from '@/api/mall'
import { shortTime } from '@/utils/format'
import { ok } from '@/lib/toast'

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 8
const loading = ref(true)
const followOpen = ref(false)
const followTarget = ref(null)
const followForm = reactive({ content: '', images: [] })

async function load() {
  loading.value = true
  try {
    const data = await mallReviewApi.mine({ page: page.value, pageSize })
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

async function upload(file) {
  const res = await mallCenterApi.upload(file)
  return typeof res === 'string' ? res : res?.url || res?.data || ''
}

function openFollow(review) {
  followTarget.value = review
  followForm.content = ''
  followForm.images = []
  followOpen.value = true
}

async function submitFollow() {
  await mallReviewApi.follow(followTarget.value.id, {
    content: followForm.content,
    images: followForm.images
  })
  followOpen.value = false
  ok('追评已提交（追评只有一次，且不改评分）')
  load()
}

onMounted(load)
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">My reviews</span>
          <h1 class="m-h1 m-mt-2">我的评价</h1>
        </div>
        <RouterLink to="/shop/me" class="m-btn m-btn-soft m-btn-sm">个人中心</RouterLink>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 300px" />

      <div v-else-if="list.length" class="m-col" style="gap: 16px">
        <div v-for="r in list" :key="r.id" class="m-panel">
          <div class="m-between">
            <div class="m-row" style="gap: 12px">
              <MStars :value="r.rating || 0" :show-value="false" />
              <span v-if="Number(r.anonymous) === 1" class="m-badge m-badge-soft">匿名</span>
            </div>
            <span class="m-dim">{{ shortTime(r.createTime) }}</span>
          </div>

          <RouterLink
            v-if="r.productId"
            :to="`/shop/products/${r.productId}`"
            class="m-row"
            style="gap: 14px; margin-top: 16px"
          >
            <img v-if="r.productImage || r.image" class="m-thumb-sm" :src="r.productImage || r.image" :alt="r.productName" />
            <span v-else class="m-thumb-sm" style="display: grid; place-items: center">
              <Icon name="image" :size="16" />
            </span>
            <div class="m-grow">
              <p class="m-h4">{{ r.productName || r.name }}</p>
              <p class="m-dim">{{ r.skuName || '' }}</p>
            </div>
            <Icon name="right" :size="15" class="m-muted" />
          </RouterLink>

          <p class="m-mt-3" style="white-space: pre-wrap; font-size: 14px">
            {{ r.content || '（未填写文字评价）' }}
          </p>

          <div v-if="r.images?.length" class="m-row m-wrapflex m-mt-3" style="gap: 8px">
            <img
              v-for="(img, i) in r.images"
              :key="img + i"
              :src="img"
              :alt="`图${i}`"
              style="width: 82px; height: 82px; border-radius: 12px; object-fit: cover"
            />
          </div>

          <div v-if="r.followContent" class="m-soft m-mt-3" style="padding: 14px 16px">
            <p class="m-dim">我的追评 · {{ shortTime(r.followTime) }}</p>
            <p class="m-mt-1">{{ r.followContent }}</p>
          </div>

          <div class="m-row m-mt-3">
            <button v-if="!r.followContent" class="m-btn m-btn-outline m-btn-sm" type="button" @click="openFollow(r)">
              追加评价
            </button>
            <span v-else class="m-dim">每条评价只能追评一次</span>
          </div>
        </div>

        <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />
      </div>

      <MEmpty image="empty-review" v-else icon="star" title="还没有发过评价" desc="完成订单后可在订单详情里点「去评价」">
        <RouterLink to="/shop/orders" class="m-btn m-btn-primary m-mt-2">查看订单</RouterLink>
      </MEmpty>
    </div>

    <MModal :open="followOpen" title="追加评价" @close="followOpen = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-field">
          <label class="m-eyebrow">追评内容</label>
          <textarea v-model="followForm.content" class="m-textarea" placeholder="用了一段时间后再来说说" />
        </div>
        <div class="m-field">
          <span class="m-eyebrow">晒图</span>
          <MUploader v-model="followForm.images" :uploader="upload" :max="9" />
        </div>
      </div>
      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="followOpen = false">取消</button>
        <button class="m-btn m-btn-primary" type="button" @click="submitFollow">提交追评</button>
      </template>
    </MModal>
  </ShopLayout>
</template>
