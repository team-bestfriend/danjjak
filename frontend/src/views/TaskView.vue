<template>
  <!-- Task Transfer -->
  <div v-if="taskName === 'task-transfer'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar :title="`${person.relation}에게 송금`" :onBack="store.goBack" />
    <div className="flex-1 flex flex-col items-center justify-center px-5 gap-6">
      <div className="w-24 h-24 rounded-full flex items-center justify-center" style="background: #FFBC00;">
        <Ic name="Transfer" />
      </div>
      <div className="text-center">
        <p className="font-bold text-[#111827]" style="font-size: 28px;">{{ person.emoji }} {{ person.name }}</p>
        <p className="text-[#6B7280] mt-1" style="font-size: 16px;">{{ acc.bank }} · {{ acc.masked }}</p>
      </div>
      <div className="w-full space-y-3">
        <Btn @click="store.navigate('guide-person')">시작하기</Btn>
        <Btn variant="secondary" @click="store.goBack">돌아가기</Btn>
      </div>
    </div>
  </div>

  <!-- Task 2 (Pension) -->
  <div v-else-if="taskName === 'task-2'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="연금 입금 확인" :onBack="store.goBack" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
      <div className="bg-[#F0FDF4] border border-[#86EFAC] rounded-[20px] p-5 flex items-center gap-4">
        <div className="w-14 h-14 rounded-full bg-[#DCFCE7] border-2 border-[#4ADE80] flex items-center justify-center text-[#16A34A]">
          <Ic name="Check" />
        </div>
        <div>
          <p className="font-bold text-[#16A34A]" style="font-size: 14px;">입금 완료</p>
          <p className="font-bold text-[#111827]" style="font-size: 21px;">이번 달 연금이 들어왔어요.</p>
        </div>
      </div>
      <Card className="overflow-hidden">
        <div
          v-for="(r, i) in pensionRows"
          :key="i"
          :class="['flex items-center justify-between px-5 py-4', i < pensionRows.length - 1 ? 'border-b border-[#F3F4F6]' : '']"
        >
          <span className="text-[#6B7280]" style="font-size: 16px;">{{ r.l }}</span>
          <span :class="['font-bold', r.a ? 'text-[#111827]' : 'text-[#111827]']" :style="r.a ? 'font-size:22px;font-weight:900;' : 'font-size:17px;'">{{ r.v }}</span>
        </div>
      </Card>
      <Btn variant="info" @click="store.navigate('pension-history')">최근 연금 내역 보기</Btn>
    </div>
  </div>

  <!-- Task 3 (Building) -->
  <div v-else-if="taskName === 'task-3'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="이번 달 관리비" :onBack="store.goBack" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
      <Card className="p-5">
        <p className="text-[#6B7280]" style="font-size: 15px;">행복아파트 · 2026년 8월</p>
        <div className="mt-3 flex items-end justify-between">
          <div>
            <p className="text-[#6B7280]" style="font-size: 14px;">납부 총액</p>
            <p className="font-bold text-[#111827]" style="font-size: 36px;">182,400<span style="font-size:18px;">원</span></p>
          </div>
          <span className="bg-[#FFF0F0] text-[#EF4444] font-bold px-3 py-1.5 rounded-full" style="font-size: 14px;">납부 전</span>
        </div>
        <p className="text-[#EF4444] font-medium mt-2" style="font-size: 14px;">납부 기한: 2026.08.31</p>
      </Card>
      <Card className="overflow-hidden">
        <div
          v-for="(r, i) in buildingRows"
          :key="i"
          :class="['flex items-center justify-between px-5 py-4', i < buildingRows.length - 1 ? 'border-b border-[#F3F4F6]' : '']"
        >
          <span className="text-[#374151]" style="font-size: 17px;">{{ r.l }}</span>
          <span className="font-bold text-[#111827]" style="font-size: 17px;">{{ r.v }}</span>
        </div>
      </Card>
      <Btn>관리비 납부하기</Btn>
    </div>
  </div>

  <!-- Task 4 (Balance) -->
  <div v-else-if="taskName === 'task-4'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="내 계좌 잔액" :onBack="store.goBack" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
      <Card className="p-5">
        <p className="text-[#6B7280]" style="font-size: 15px;">KB국민은행 · 생활비 계좌</p>
        <div className="mt-4 flex items-center gap-3">
          <p className="font-bold text-[#111827]" style="font-size: 34px;">{{ showBalance ? '2,458,300원' : '• • • • • •' }}</p>
          <button
            @click="showBalance = !showBalance"
            className="ml-auto font-bold text-[#374151] bg-[#F3F4F6] px-3 py-1.5 rounded-lg"
            style="font-size: 14px;"
          >
            {{ showBalance ? '숨기기' : '잔액 보기' }}
          </button>
        </div>
      </Card>
      <div
        v-for="(t, i) in TRANSACTIONS"
        :key="i"
        className="flex items-center justify-between py-4 border-b border-[#F3F4F6] last:border-0"
      >
        <div>
          <p className="font-bold text-[#111827]" style="font-size: 17px;">{{ t.desc }}</p>
          <p className="text-[#9CA3AF]" style="font-size: 13px;">{{ t.date }}</p>
        </div>
        <span :class="['font-bold', t.income ? 'text-[#2563EB]' : 'text-[#374151]']" style="font-size: 17px;">{{ t.amount }}원</span>
      </div>
    </div>
  </div>

  <!-- Task 5 (History) -->
  <div v-else-if="taskName === 'task-5'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="거래내역" :onBack="store.goBack" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-3">
      <div className="flex gap-2">
        <button
          v-for="f in ['전체', '입금', '출금']"
          :key="f"
          @click="historyFilter = f"
          :class="['flex-1 rounded-xl font-bold border', historyFilter === f ? 'bg-[#FFBC00] border-[#FFBC00] text-[#111827]' : 'bg-white border-[#EBEBEA] text-[#374151]']"
          style="height: 48px; font-size: 16px;"
        >
          {{ f }}
        </button>
      </div>
      <Card className="overflow-hidden">
        <div
          v-for="(t, i) in filteredTransactions"
          :key="i"
          :class="['flex items-center justify-between px-5 py-4', i < filteredTransactions.length - 1 ? 'border-b border-[#F3F4F6]' : '']"
        >
          <div>
            <p className="font-bold text-[#111827]" style="font-size: 17px;">{{ t.desc }}</p>
            <p className="text-[#9CA3AF]" style="font-size: 13px;">2026.{{ t.date }}</p>
          </div>
          <span :class="['font-bold', t.income ? 'text-[#2563EB]' : 'text-[#374151]']" style="font-size: 17px;">{{ t.amount }}원</span>
        </div>
      </Card>
    </div>
  </div>

  <!-- Task 6 (Support) -->
  <div v-else-if="taskName === 'task-6'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="고객센터" :onBack="store.goBack" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-3">
      <p className="font-bold text-[#111827]" style="font-size: 24px;">어떤 도움이 필요하세요?</p>
      <Card
        v-for="item in supportItems"
        :key="item.l"
        className="px-5 py-4 flex items-center gap-4"
      >
        <div className="text-[#9CA3AF]"><Ic :name="item.I" /></div>
        <span className="flex-1 font-bold text-[#111827]" style="font-size: 18px;">{{ item.l }}</span>
        <Ic name="ChevR" />
      </Card>
      <div className="bg-white border border-[#E5E7EB] rounded-[20px] p-5 text-center space-y-3">
        <p className="font-bold text-[#374151]" style="font-size: 18px;">KB 고객센터 1588-9999</p>
        <Btn @click="supportModal = true">전화 연결하기</Btn>
      </div>
    </div>
    <BottomSheet :open="supportModal" @close="supportModal = false" title="전화 연결할까요?">
      <SheetRow label="전화 연결하기" color="#2563EB" @click="supportModal = false" />
      <SheetRow label="취소" @click="supportModal = false" />
    </BottomSheet>
  </div>

  <!-- Pension History -->
  <div v-else-if="taskName === 'pension-history'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="연금 내역" :onBack="store.goBack" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-3">
      <Card v-for="m in ['2026년 8월', '2026년 7월', '2026년 6월']" :key="m" className="p-5 flex items-center justify-between">
        <div>
          <p className="font-bold text-[#111827]" style="font-size: 18px;">국민연금</p>
          <p className="text-[#6B7280]" style="font-size: 14px;">{{ m }}</p>
        </div>
        <span className="font-bold text-[#2563EB]" style="font-size: 20px;">+650,000원</span>
      </Card>
    </div>
  </div>

  <!-- Simple Task -->
  <div v-else className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar :title="simpleTaskConfig.title" :onBack="store.goBack" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
      <div className="flex flex-col items-center gap-3 py-4">
        <div className="w-20 h-20 rounded-full flex items-center justify-center" style="background: #FFBC00;">
          <Ic :name="simpleTaskConfig.icon" />
        </div>
        <p className="font-bold text-[#111827]" style="font-size: 25px;">{{ simpleTaskConfig.title }}</p>
      </div>
      <Card className="overflow-hidden">
        <div
          v-for="(r, i) in simpleTaskConfig.rows"
          :key="i"
          :class="['flex items-center justify-between px-5 py-4', i < simpleTaskConfig.rows.length - 1 ? 'border-b border-[#F3F4F6]' : '']"
        >
          <span className="text-[#6B7280]" style="font-size: 16px;">{{ r.l }}</span>
          <span className="font-bold text-[#111827]" style="font-size: 17px;">{{ r.v }}</span>
        </div>
      </Card>
      <Btn variant="secondary" @click="store.goBack">홈으로</Btn>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useAppStore } from '../stores/appStore';
import { TRANSACTIONS } from '../constants/data';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import Card from '../components/common/Card.vue';
import Btn from '../components/common/Btn.vue';
import Ic from '../components/common/Ic.vue';
import BottomSheet from '../components/common/BottomSheet.vue';
import SheetRow from '../components/common/SheetRow.vue';

const props = defineProps({
  taskName: { type: String, required: true }
});

const store = useAppStore();

const showBalance = ref(false);
const historyFilter = ref('전체');
const supportModal = ref(false);

const pid = computed(() => store.activePattern?.personId ?? 1);
const person = computed(() => store.people.find((p) => p.id === pid.value) || store.people[0]);
const accounts = computed(() => store.accountsByPerson[pid.value] || store.accountsByPerson[1] || []);
const acc = computed(() => accounts.value.find((a) => a.masked === store.activePattern?.accountMasked) || accounts.value[0] || {});

const pensionRows = [
  { l: "기간", v: "2026년 8월" },
  { l: "종류", v: "국민연금" },
  { l: "입금 금액", v: "650,000원", a: true },
  { l: "입금일", v: "2026.08.25" },
  { l: "입금 계좌", v: "KB국민은행 생활비계좌" }
];

const buildingRows = [
  { l: "전기요금", v: "42,300원" },
  { l: "수도요금", v: "18,100원" },
  { l: "공용관리비", v: "122,000원" }
];

const filteredTransactions = computed(() => {
  if (historyFilter.value === '입금') return TRANSACTIONS.filter((t) => t.income);
  if (historyFilter.value === '출금') return TRANSACTIONS.filter((t) => !t.income);
  return TRANSACTIONS;
});

const supportItems = [
  { I: "Transfer", l: "금융 상담" },
  { I: "Warning", l: "보이스피싱 신고" },
  { I: "Wallet", l: "카드 분실" },
  { I: "Gear", l: "앱 사용 도움" }
];

const simpleTaskConfigs = {
  'task-8': {
    title: "공과금 확인", icon: "Building",
    rows: [{ l: "전기요금", v: "24,800원" }, { l: "수도요금", v: "9,200원" }, { l: "가스요금", v: "31,500원" }, { l: "납부 기한", v: "2026.08.31" }]
  },
  'task-9': {
    title: "자동이체 확인", icon: "Repeat",
    rows: [{ l: "KB국민→이정훈", v: "월 200,000원" }, { l: "보험료", v: "월 85,000원" }, { l: "다음 이체일", v: "2026.09.01" }]
  },
  'task-10': {
    title: "카드 이용내역", icon: "CreditCard",
    rows: [{ l: "이번 달 사용액", v: "342,000원" }, { l: "결제일", v: "2026.09.15" }, { l: "최근 결제", v: "OO마트 32,000원" }]
  },
  'task-11': {
    title: "예금 만기 확인", icon: "Safe",
    rows: [{ l: "상품명", v: "KB정기예금" }, { l: "만기일", v: "2026.12.01" }, { l: "금액", v: "5,000,000원" }, { l: "금리", v: "연 3.5%" }]
  },
  'task-12': {
    title: "환율 확인", icon: "Globe",
    rows: [{ l: "USD/KRW", v: "1,385원" }, { l: "EUR/KRW", v: "1,498원" }, { l: "JPY/KRW", v: "9.21원" }, { l: "기준 시각", v: "오늘 09:00" }]
  }
};

const simpleTaskConfig = computed(() => simpleTaskConfigs[props.taskName] || { title: "상세 확인", icon: "Building", rows: [] });
</script>
