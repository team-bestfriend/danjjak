<template>
  <div class="flex h-full flex-col bg-[#FAFAF8]">
    <SafeArea />
    <TopBar
      v-if="manageMode"
      title="내 계좌 관리"
      :onBack="store.goBack"
      :backDisabled="importing"
    />

    <main class="flex-1 overflow-y-auto px-5 pb-6 pt-7">
      <h1 class="text-[28px] font-bold leading-snug text-[#111827]">
        {{ manageMode ? '내 모의 계좌를 관리해요' : '내 계좌를 불러올까요?' }}
      </h1>
      <p class="mt-3 text-[16px] leading-relaxed text-[#6B7280]">
        실제 은행에 연결하지 않는 연습용 계좌예요. 잔액과 거래내역도 모두 모의 자료입니다.
      </p>

      <p v-if="store.accountImportLoading" class="mt-7 rounded-[18px] bg-white p-5 text-[#6B7280]" role="status">
        모의 계좌를 불러오고 있어요…
      </p>

      <div v-else-if="store.accountImportError && options.length === 0" class="mt-7 rounded-[18px] border border-[#FCA5A5] bg-[#FEF2F2] p-5">
        <p class="text-[#991B1B]" role="alert">{{ store.accountImportError }}</p>
        <button type="button" class="mt-4 min-h-[52px] w-full rounded-[16px] border border-[#D1D5DB] bg-white font-bold text-[#374151]" @click="reload">
          다시 시도
        </button>
      </div>

      <template v-else>
        <section v-if="importedOptions.length" class="mt-7">
          <h2 class="text-[17px] font-bold text-[#374151]">불러온 계좌</h2>
          <div class="mt-3 space-y-3">
            <div v-for="account in importedOptions" :key="account.accountId" class="rounded-[18px] border border-[#D1FAE5] bg-white p-5">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="text-[19px] font-bold text-[#111827]">{{ account.bankName }}</p>
                  <p class="mt-1 text-[15px] text-[#6B7280]">{{ account.accountAlias || '내 모의 계좌' }}</p>
                  <p class="mt-1 font-mono text-[15px] text-[#6B7280]">{{ account.masked }}</p>
                </div>
                <span class="rounded-full bg-[#DCFCE7] px-3 py-1 text-[14px] font-bold text-[#166534]">불러옴</span>
              </div>
              <p class="mt-3 text-[16px] font-bold text-[#374151]">잔액 {{ formatWon(account.balance) }}</p>
            </div>
          </div>
        </section>

        <section class="mt-7">
          <h2 class="text-[17px] font-bold text-[#374151]">불러올 수 있는 계좌</h2>
          <div v-if="candidateOptions.length" class="mt-3 space-y-3">
            <button
              v-for="account in candidateOptions"
              :key="account.accountId"
              type="button"
              :disabled="importing"
              :aria-pressed="selectedAccountId === account.accountId"
              @click="selectedAccountId = account.accountId"
              :class="[
                'w-full min-h-[120px] rounded-[18px] border-2 bg-white p-5 text-left disabled:opacity-60',
                selectedAccountId === account.accountId ? 'border-[#FFBC00]' : 'border-[#E5E7EB]'
              ]"
            >
              <div class="flex items-start gap-3">
                <span :class="['mt-1 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full border-2 font-bold', selectedAccountId === account.accountId ? 'border-[#FFBC00] bg-[#FFBC00] text-[#111827]' : 'border-[#D1D5DB] text-transparent']">✓</span>
                <span class="flex-1">
                  <span class="flex items-center justify-between gap-3">
                    <span class="text-[19px] font-bold text-[#111827]">{{ account.bankName }}</span>
                    <span v-if="selectedAccountId === account.accountId" class="text-[14px] font-bold text-[#92650A]">선택됨</span>
                    <span v-else class="text-[14px] text-[#6B7280]">불러오기 전</span>
                  </span>
                  <span class="mt-1 block text-[15px] text-[#6B7280]">{{ account.accountAlias || '내 모의 계좌' }}</span>
                  <span class="mt-1 block font-mono text-[15px] text-[#6B7280]">{{ account.masked }}</span>
                  <span class="mt-3 block text-[16px] font-bold text-[#374151]">잔액 {{ formatWon(account.balance) }}</span>
                </span>
              </div>
            </button>
          </div>
          <div v-else class="mt-3 rounded-[18px] bg-white p-5 text-center">
            <p class="text-[18px] font-bold text-[#111827]">불러올 계좌가 없어요.</p>
            <p class="mt-2 text-[15px] text-[#6B7280]">준비된 모의 계좌를 모두 불러왔어요.</p>
          </div>
        </section>

        <p v-if="store.accountImportError" class="mt-5 rounded-[14px] bg-[#FEF2F2] p-4 text-[15px] text-[#B91C1C]" role="alert">
          {{ store.accountImportError }} 입력한 선택은 그대로 유지했어요.
        </p>
        <p v-if="savedMessage" class="mt-5 rounded-[14px] bg-[#F0FDF4] p-4 text-[15px] text-[#166534]" role="status">{{ savedMessage }}</p>
      </template>
    </main>

    <div v-if="!store.accountImportLoading" class="border-t border-[#EEEEED] bg-white px-5 pb-8 pt-4">
      <button
        type="button"
        class="min-h-[60px] w-full rounded-[18px] bg-[#FFBC00] text-[18px] font-bold text-[#111827] disabled:bg-[#E5E7EB] disabled:text-[#9CA3AF]"
        :disabled="!selectedAccountId || importing"
        @click="importSelected"
      >{{ importing ? '계좌 불러오는 중…' : '계좌 불러오기' }}</button>
      <div v-if="!manageMode" class="mt-3 grid grid-cols-2 gap-3">
        <button type="button" class="min-h-[48px] rounded-[14px] border border-[#D1D5DB] text-[15px] font-bold text-[#4B5563]" :disabled="importing" @click="store.navigate('settings')">설정</button>
        <button type="button" class="min-h-[48px] rounded-[14px] border border-[#D1D5DB] text-[15px] font-bold text-[#4B5563]" :disabled="importing || store.logoutPending" @click="store.logout">로그아웃</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import { useAppStore } from '../stores/appStore';

const route = useRoute();
const router = useRouter();
const store = useAppStore();
const selectedAccountId = ref(null);
const savedMessage = ref('');
const manageMode = computed(() => route.query.manage === '1');
const options = computed(() => store.mockAccountImportOptions);
const importedOptions = computed(() => options.value.filter((account) => account.imported));
const candidateOptions = computed(() => options.value.filter((account) => !account.imported));
const importing = computed(() => store.accountImportSavingId !== null);

onMounted(reload);

async function reload() {
  savedMessage.value = '';
  await Promise.all([
    store.loadFinancialData(true),
    store.loadMockAccountImportOptions(true),
  ]);
}

async function importSelected() {
  if (!selectedAccountId.value || importing.value) return;
  try {
    await store.importMockAccount(selectedAccountId.value);
    selectedAccountId.value = null;
    if (manageMode.value) {
      savedMessage.value = '선택한 모의 계좌를 불러왔어요.';
    } else {
      await router.replace({ name: 'home' });
    }
  } catch {
    // 저장소의 오류 문구와 사용자가 선택한 계좌를 화면에 그대로 유지한다.
  }
}

function formatWon(amount) {
  return `${Number(amount).toLocaleString('ko-KR')}원`;
}
</script>
