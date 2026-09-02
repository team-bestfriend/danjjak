<template>
  <div class="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div class="flex-1 flex flex-col px-6 pt-10 pb-6">
      <!-- 로고 -->
      <div class="flex flex-col items-center gap-3 mb-12">
        <DanjjakMark :size="64" />
        <div class="text-center">
          <p class="font-bold text-[#111827]" style="font-size: 32px;">단짝</p>
          <p class="text-[#6B7280] mt-1" style="font-size: 16px;">어르신을 위한 쉬운 금융 도우미</p>
        </div>
      </div>

      <!-- 환영 -->
      <p class="font-bold text-[#111827] mb-7" style="font-size: 28px;">다시 만나서 반가워요 👋</p>

      <!-- 입력 폼 -->
      <div class="space-y-5">
        <div class="space-y-2">
          <p class="font-bold text-[#374151] px-1" style="font-size: 17px;">전화번호</p>
          <input
            type="tel"
            v-model="phone"
            @input="phone = formatPhone($event.target.value)"
            placeholder="010-0000-0000"
            inputMode="tel"
            class="w-full rounded-[18px] border-2 border-[#E5E7EB] focus:border-[#FFBC00] outline-none px-5 text-[#111827] placeholder:text-[#D1D5DB] placeholder:font-normal"
            style="min-height: 62px; font-size: 20px; font-weight: 700;"
          />
        </div>
        <div class="space-y-2">
          <p class="font-bold text-[#374151] px-1" style="font-size: 17px;">비밀번호</p>
          <div class="relative">
            <input
              :type="pwVisible ? 'text' : 'password'"
              v-model="pw"
              placeholder="비밀번호 입력"
              class="w-full rounded-[18px] border-2 border-[#E5E7EB] focus:border-[#FFBC00] outline-none px-5 pr-16 text-[#111827] placeholder:text-[#D1D5DB] placeholder:font-normal"
              style="min-height: 62px; font-size: 20px; font-weight: 700;"
            />
            <button
              @click="pwVisible = !pwVisible"
              class="absolute right-4 top-1/2 -translate-y-1/2 font-bold text-[#9CA3AF] px-2 py-1"
              style="font-size: 14px;"
            >
              {{ pwVisible ? '숨기기' : '보기' }}
            </button>
          </div>
        </div>
      </div>

      <div class="mt-7">
        <Btn @click="store.navigate('home')" :disabled="phone.length < 12 || pw.length < 4">
          로그인
        </Btn>
      </div>

      <p class="text-center mt-6 text-[#6B7280]" style="font-size: 16px;">
        처음 이용하시나요?
        <button @click="store.navigate('signup')" class="font-bold" style="color: #92650A;">회원가입</button>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import DanjjakMark from '../components/common/DanjjakMark.vue';
import Btn from '../components/common/Btn.vue';

const store = useAppStore();
const phone = ref('');
const pw = ref('');
const pwVisible = ref(false);

function formatPhone(v) {
  const d = v.replace(/\D/g, "").slice(0, 11);
  if (d.length <= 3) return d;
  if (d.length <= 7) return `${d.slice(0, 3)}-${d.slice(3)}`;
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`;
}
</script>
