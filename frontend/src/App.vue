<script setup>
import { onMounted, ref } from 'vue'

const apiStatus = ref('확인 중')
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

async function checkApi() {
  apiStatus.value = '확인 중'

  try {
    const response = await fetch(`${apiBaseUrl}/api/health`)

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const data = await response.json()
    apiStatus.value = data.status === 'UP' ? '정상' : '확인 필요'
  } catch {
    apiStatus.value = '연결 안 됨'
  }
}

onMounted(checkApi)
</script>

<template>
  <main class="app-shell">
    <section class="status-card" aria-labelledby="service-title">
      <p class="eyebrow">KB IT's Your Life Hackathon</p>
      <h1 id="service-title">금융 단짝</h1>
      <p class="description">고령 사용자의 안전한 금융 생활을 돕는 서비스를 준비하고 있습니다.</p>

      <div class="api-status" aria-live="polite">
        <span>백엔드 연결</span>
        <strong>{{ apiStatus }}</strong>
      </div>

      <button type="button" @click="checkApi">다시 확인</button>
    </section>
  </main>
</template>

