<template>
  <!-- Direct Transfer Method Selection -->
  <div v-if="flowStep === 'direct-transfer'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="직접 송금하기" :onBack="store.goBack" />
    <div className="flex-1 flex flex-col px-5 pt-8 pb-6 gap-5">
      <p className="font-bold text-[#111827]" style="font-size: 28px;">누구에게 보내시겠어요?</p>

      <button
        @click="selectFamily"
        className="guide-glow w-full rounded-[24px] bg-white border border-[#FFBC00] p-6 flex flex-col items-center gap-3 active:scale-[0.97] transition-all"
        style="box-shadow: 0 1px 4px rgba(0,0,0,0.06);"
      >
        <div className="w-16 h-16 rounded-full bg-[#FFBC00] flex items-center justify-center text-[32px]">👨‍👩‍👧</div>
        <p className="font-bold text-[#111827]" style="font-size: 21px;">등록된 가족에게 보내기</p>
        <p className="text-[#6B7280] text-center" style="font-size: 15px;">미리 등록해 둔 가족 계좌로 보내요.</p>
      </button>

      <button
        @click="store.navigate('direct-newaccount')"
        className="w-full rounded-[24px] bg-white border border-[#E5E7EB] p-6 flex flex-col items-center gap-3 active:scale-[0.97] transition-all"
        style="box-shadow: 0 1px 4px rgba(0,0,0,0.06);"
      >
        <div className="w-16 h-16 rounded-full bg-[#374151] flex items-center justify-center text-white"><Ic name="Transfer" /></div>
        <p className="font-bold text-[#111827]" style="font-size: 21px;">새 계좌로 보내기</p>
        <p className="text-[#6B7280] text-center" style="font-size: 15px;">계좌 번호를 직접 입력해서 보내요.</p>
      </button>
    </div>
  </div>

  <!-- Direct New Account -->
  <div v-else-if="flowStep === 'direct-newaccount'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="새 계좌로 송금" :onBack="store.goBack" rightLabel="취소" :onRight="() => store.navigate('home')" />
    <StepBar :current="2" :total="6" />
    <div className="flex-1 overflow-y-auto px-4 pt-5 pb-4 space-y-5">
      <p className="font-bold text-[#111827]" style="font-size: 26px;">받는 계좌를 입력해주세요.</p>

      <div className="space-y-2">
        <p className="font-bold text-[#374151] px-1" style="font-size: 17px;">은행 선택</p>
        <button
          @click="showBanks = !showBanks"
          :class="[
            'w-full rounded-[16px] border-2 px-4 text-left flex items-center justify-between transition-all',
            bank ? 'border-[#FFBC00] text-[#111827]' : 'border-[#E5E7EB] text-[#9CA3AF]'
          ]"
          style="min-height: 58px; font-size: 18px; font-weight: 700;"
        >
          <span>{{ bank || '은행 선택' }}</span>
          <span :class="['transition-transform', showBanks ? 'rotate-180' : '']"><Ic name="ChevR" /></span>
        </button>
        <div v-if="showBanks" className="grid grid-cols-2 gap-2 pt-1">
          <button
            v-for="b in banks"
            :key="b"
            @click="bank = b; showBanks = false;"
            :class="[
              'rounded-[12px] border-2 font-bold transition-all',
              bank === b ? 'border-[#FFBC00] bg-[#FFFBEB] text-[#92650A]' : 'border-[#E5E7EB] text-[#374151]'
            ]"
            style="height: 52px; font-size: 16px;"
          >
            {{ b }}
          </button>
        </div>
      </div>

      <div className="space-y-2">
        <p className="font-bold text-[#374151] px-1" style="font-size: 17px;">계좌 번호</p>
        <input
          type="tel"
          v-model="acctNum"
          @input="acctNum = $event.target.value.replace(/[^0-9-]/g, '')"
          placeholder="000-00-000000"
          inputMode="numeric"
          className="w-full rounded-[16px] border-2 border-[#E5E7EB] focus:border-[#FFBC00] outline-none px-4 text-[#111827] placeholder:text-[#D1D5DB]"
          style="min-height: 58px; font-size: 20px; font-weight: 700;"
        />
      </div>

      <div className="bg-[#FFF7ED] border border-[#FED7AA] rounded-[16px] p-4 flex items-start gap-2">
        <Ic name="Warning" />
        <p className="text-[#92400E] flex-1" style="font-size: 15px;">처음 보내는 계좌예요. 보이스피싱 주의 — 가족이 요청한 경우만 보내세요.</p>
      </div>

      <Btn :disabled="!bank || acctNum.length < 8" @click="proceedNewAccount">다음</Btn>
    </div>
  </div>

  <!-- Guide Person -->
  <div v-else-if="flowStep === 'guide-person'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="받는 사람 선택" :onBack="store.goBack" rightLabel="취소" :onRight="() => store.navigate('home')" />
    <StepBar :current="2" :total="7" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
      <p className="font-bold text-[#111827]" style="font-size: 26px;">보낼 사람을 선택해주세요.</p>

      <div className="space-y-3">
        <div v-for="(person, i) in store.people" :key="person.id"
          :class="['rounded-[20px]', i === 0 ? 'guide-glow' : '']"
          @click="handleSelectFamilyPerson(person.id)">
          <Card className="p-5">
            <div className="flex items-center gap-4">
              <div className="w-14 h-14 rounded-full bg-[#FFF3CC] border border-[#FFBC00] flex items-center justify-center flex-shrink-0" style="font-size: 28px;">
                {{ person.emoji }}
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-bold text-[#111827]" style="font-size: 21px;">{{ person.name }}</p>
                <p className="text-[#6B7280]" style="font-size: 15px;">
                  {{ person.relation }} · 계좌 {{ getAccCount(person.id) }}개
                </p>
              </div>
              <Ic name="ChevR" />
            </div>
          </Card>
        </div>
      </div>
    </div>
  </div>

  <!-- Guide Account -->
  <div v-else-if="flowStep === 'guide-account'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="계좌 선택" :onBack="store.goBack" rightLabel="취소" :onRight="() => store.navigate('home')" />
    <StepBar :current="3" :total="7" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
      <p className="font-bold text-[#111827]" style="font-size: 26px;">보낼 계좌를 선택해주세요.</p>
      <div className="space-y-3">
        <div v-for="acc in personAccs" :key="acc.masked"
          :class="['rounded-[20px]', acc.primary ? 'guide-glow' : '']"
          @click="handleSelectAccount(acc.masked)">
          <Card className="p-5">
            <div className="flex items-center gap-4">
              <div
                className="w-12 h-12 rounded-[14px] flex items-center justify-center font-black flex-shrink-0"
                style="font-size: 12px; background: #FFBC00; color: #111827;"
              >
                {{ acc.bank ? acc.bank.slice(0, 2) : '' }}
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <p className="font-bold text-[#111827]" style="font-size: 18px;">{{ acc.bank }}</p>
                  <span v-if="acc.primary" className="font-bold bg-[#FFF3CC] text-[#92650A] border border-[#FFBC00] px-2 py-0.5 rounded-full" style="font-size: 12px;">자주 사용</span>
                </div>
                <p className="font-mono text-[#374151]" style="font-size: 14px;">{{ acc.masked }}</p>
              </div>
              <Ic name="ChevR" />
            </div>
          </Card>
        </div>
      </div>
    </div>
  </div>

  <!-- Amount Input -->
  <div v-else-if="flowStep === 'amount-input'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="얼마를 보낼까요?" :onBack="store.goBack" rightLabel="취소" :onRight="() => store.navigate('home')" />
    <StepBar :current="4" :total="7" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4">
      <AmountKeypad @complete="handleAmountComplete" />
    </div>
  </div>

  <!-- Pin Entry -->
  <div v-else-if="flowStep === 'pin-entry'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="비밀번호를 입력해주세요" :onBack="store.goBack" rightLabel="취소" :onRight="() => store.navigate('home')" />
    <StepBar :current="5" :total="7" />
    <div className="flex-1 overflow-y-auto px-4 pt-5 pb-4 space-y-5">
      <p className="text-[#374151] text-center" style="font-size: 17px;">계좌 비밀번호 4자리를<br />직접 입력해주세요.</p>
      <div className="bg-[#FFFBEB] border border-[#FFBC00] rounded-2xl px-4 py-3 flex items-center gap-2 text-[#92650A]">
        <Ic name="Shield" />
        <p className="font-bold" style="font-size: 15px;">비밀번호는 본인이 직접 입력해주세요.</p>
      </div>
      <PinEntry @complete="handlePinComplete" />
    </div>
  </div>

  <!-- Fraud Warning -->
  <div v-else-if="flowStep === 'fraud-warning'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="잠깐, 확인이 필요해요" :onBack="store.goBack" rightLabel="취소" :onRight="() => store.navigate('home')" />
    <StepBar :current="6" :total="7" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
      <div className="flex flex-col items-center gap-3 py-2">
        <div className="rounded-full bg-[#FFF0F0] border-2 border-[#FECACA] flex items-center justify-center text-[#EF4444]" style="width: 72px; height: 72px;">
          <Ic name="Warning" />
        </div>
        <p className="text-[#6B7280] text-center" style="font-size: 17px;">평소 이용하던 방식과 달라요.</p>
      </div>
      <div className="bg-[#FFF7ED] border border-[#FED7AA] rounded-[20px] p-4 space-y-2">
        <div v-for="r in fraudReasons" :key="r" className="flex items-center gap-2">
          <div className="w-2 h-2 rounded-full bg-[#F97316]" />
          <span className="text-[#9A3412]" style="font-size: 16px;">{{ r }}</span>
        </div>
      </div>
      <AudioCard quote="처음 보는 계좌면 나한테 먼저 전화해." />
      <div className="space-y-3 pt-1">
        <Btn variant="danger"><Ic name="Phone" />&nbsp;가족에게 전화하기</Btn>
        <Btn variant="secondary" @click="store.goBack">거래 정보 다시 확인</Btn>
        <button @click="store.navigate('final-confirm')" className="w-full py-4 text-center text-[#9CA3AF] font-medium" style="font-size: 16px;">그래도 계속 진행하기</button>
      </div>
    </div>
  </div>

  <!-- Final Confirm -->
  <div v-else-if="flowStep === 'final-confirm'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="보내기 전에 확인해주세요" :onBack="store.goBack" rightLabel="취소" :onRight="() => store.navigate('home')" />
    <StepBar :current="7" :total="7" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
      <Card className="overflow-hidden">
        <div
          v-for="(r, i) in finalRows"
          :key="i"
          :class="['flex items-center justify-between px-5 py-4', i < finalRows.length - 1 ? 'border-b border-[#F3F4F6]' : '']"
        >
          <span className="text-[#6B7280]" style="font-size: 16px;">{{ r.l }}</span>
          <span :class="['font-bold', r.a ? 'text-[#111827]' : 'text-[#111827]']" :style="r.a ? 'font-size:28px;font-weight:900;' : 'font-size:17px;'">{{ r.v }}</span>
        </div>
      </Card>
      <Btn @click="store.navigate('complete')">최종 송금하기</Btn>
      <Btn variant="secondary" @click="store.goBack">다시 확인</Btn>
    </div>
  </div>

  <!-- Complete -->
  <div v-else-if="flowStep === 'complete'" className="flex flex-col h-full items-center justify-center px-6 gap-6" style="background: #FAFAF8;">
    <SafeArea />
    <div className="w-28 h-28 rounded-full flex items-center justify-center" style="background: #22C55E;">
      <Ic name="Check" />
    </div>
    <div className="text-center space-y-3">
      <p className="font-bold text-[#111827]" style="font-size: 28px;">송금이 완료됐어요!</p>
      <p className="text-[#374151]" style="font-size: 18px;">
        {{ targetPerson.name }}님에게<br />
        <span className="font-bold text-[#111827]" style="font-size: 32px;">{{ Number(store.transferAmount || 50000).toLocaleString('ko-KR') }}원</span>을 보냈어요.
      </p>
    </div>
    <Btn @click="store.navigate('home')">홈으로 돌아가기</Btn>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import StepBar from '../components/common/StepBar.vue';
import Card from '../components/common/Card.vue';
import Btn from '../components/common/Btn.vue';
import Ic from '../components/common/Ic.vue';
import AudioCard from '../components/common/AudioCard.vue';
import AmountKeypad from '../components/common/AmountKeypad.vue';
import PinEntry from '../components/common/PinEntry.vue';

const props = defineProps({
  flowStep: { type: String, required: true }
});

const store = useAppStore();

const bank = ref('');
const acctNum = ref('');
const showBanks = ref(false);
const banks = ["KB국민은행", "신한은행", "우리은행", "하나은행", "NH농협", "카카오뱅크", "케이뱅크", "토스뱅크"];

const pid = computed(() => store.selectedPersonId || store.activePattern?.personId || store.people[0]?.id || 1);
const primaryPerson = computed(() => store.people.find((p) => p.id === pid.value) || null);

const personAccs = computed(() => store.accountsByPerson[pid.value] || store.accountsByPerson[store.people[0]?.id] || []);
const primaryAcc = computed(() => {
  if (store.selectedAccountMasked) return personAccs.value.find((a) => a.masked === store.selectedAccountMasked) || personAccs.value[0] || {};
  return personAccs.value.find((a) => a.masked === store.activePattern?.accountMasked) || personAccs.value[0] || {};
});

const targetPerson = computed(() => store.people.find((p) => p.id === pid.value) || store.people[0] || {});

const shouldShowFraud = computed(() => {
  return store.isNewAccountFlow || parseInt(store.transferAmount || '0') >= 10000000;
});

const fraudReasons = computed(() => {
  const isLarge = parseInt(store.transferAmount || '0') >= 10000000;
  const list = [];
  if (store.isNewAccountFlow) list.push("처음 송금하는 계좌예요");
  if (isLarge) list.push("1천만원 이상의 큰 금액이에요");
  return list;
});

const finalRows = computed(() => [
  { l: "받는 사람", v: `${targetPerson.value.emoji} ${targetPerson.value.name} · ${targetPerson.value.relation}` },
  { l: "은행", v: primaryAcc.value.bank || "국민은행" },
  { l: "계좌", v: primaryAcc.value.masked || "123-45-****-90" },
  { l: "금액", v: `${Number(store.transferAmount || 50000).toLocaleString('ko-KR')}원`, a: true },
  { l: "수수료", v: "0원" }
]);

function selectFamily() {
  store.isNewAccountFlow = false;
  store.navigate('guide-person');
}

function proceedNewAccount() {
  store.isNewAccountFlow = true;
  store.navigate('amount-input');
}

function getAccCount(personId) {
  return personId ? (store.accountsByPerson[personId]?.length || 1) : 1;
}

function handleSelectFamilyPerson(personId) {
  store.selectedPersonId = personId;
  store.selectedAccountMasked = null;
  const accs = store.accountsByPerson[personId] || [];
  if (accs.length > 1) {
    store.navigate('guide-account');
  } else {
    store.navigate('amount-input');
  }
}

function handleSelectAccount(masked) {
  store.selectedAccountMasked = masked;
  store.navigate('amount-input');
}

function handleAmountComplete(amt) {
  store.transferAmount = amt;
  store.navigate('pin-entry');
}

function handlePinComplete() {
  store.navigate(shouldShowFraud.value ? 'fraud-warning' : 'final-confirm');
}
</script>
