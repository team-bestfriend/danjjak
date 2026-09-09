<template>
  <!-- 등록 수취인 송금 시작 확인 -->
  <div
    v-if="taskName === 'task-transfer'"
    class="flex h-full flex-col bg-[#FAFAF8]"
  >
    <SafeArea />

    <TopBar
      :title="(person?.relation || '가족') + '에게 송금'"
      :onBack="leaveTask"
    />

    <div class="flex flex-1 flex-col items-center justify-center gap-6 px-5">
      <div
        class="flex h-24 w-24 items-center justify-center rounded-full bg-[#FFBC00]"
      >
        <Ic name="Transfer" />
      </div>

      <div v-if="store.financeLoading" class="text-center text-[#6B7280]">
        등록 정보를 불러오고 있어요…
      </div>

      <div
        v-else-if="store.financeError"
        class="w-full space-y-3 rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-5 text-center"
      >
        <p class="text-[#991B1B]" role="alert">
          {{ store.financeError }}
        </p>

        <Btn variant="secondary" @click="store.loadFinancialData(true)">
          다시 시도
        </Btn>
      </div>

      <div v-else-if="person" class="text-center">
        <img
          v-if="profileImageForPerson(person)"
          :src="profileImageForPerson(person)"
          alt=""
          class="mx-auto mb-3 h-20 w-20 rounded-full border border-[#FFBC00] object-cover"
          aria-hidden="true"
        />
        <p class="text-[28px] font-bold text-[#111827]">
          {{ person.name }}
        </p>

        <p class="mt-1 text-[16px] text-[#6B7280]">
          {{
            [account?.accountAlias, account?.bankName, account?.masked]
              .filter(Boolean)
              .join(" · ")
          }}
        </p>
      </div>

      <div v-else class="w-full rounded-2xl bg-white p-5 text-center">
        <p class="font-bold text-[#111827]">연결된 수취인을 찾을 수 없어요.</p>

        <p class="mt-2 text-[#6B7280]">
          등록 정보를 확인한 뒤 다시 시작해 주세요.
        </p>
      </div>

      <div class="w-full space-y-3">
        <Btn :disabled="!person || !account" @click="beginPatternTransfer">
          시작하기
        </Btn>

        <Btn variant="secondary" @click="store.goBack"> 돌아가기 </Btn>
      </div>
    </div>
  </div>

  <!-- 잔액·거래·분류 조회 -->
  <div v-else-if="isInquiryTask" class="flex h-full flex-col bg-[#FAFAF8]">
    <SafeArea />

    <TopBar :title="inquiryTitle" :onBack="leaveTask" />

    <div class="min-h-0 flex-1 space-y-4 overflow-y-auto px-4 pb-6 pt-4">
      <!-- 조회 계좌 선택 -->
      <label v-if="store.ownedAccounts.length > 0" class="block space-y-2">
        <span class="font-bold text-[#374151]"> 조회할 본인 계좌 </span>

        <select
          :value="store.selectedInquiryAccountId || ''"
          class="min-h-[52px] w-full rounded-[14px] border-2 border-[#E5E7EB] bg-white px-4 text-[17px] font-bold text-[#111827]"
          @change="changeAccount"
        >
          <option
            v-for="owned in store.ownedAccounts"
            :key="owned.accountId"
            :value="owned.accountId"
          >
            {{ owned.accountAlias || owned.bankName }} ·
            {{ owned.masked }}
          </option>
        </select>
      </label>

      <!-- 시연 데이터 안내 -->
      <div
        class="rounded-[16px] border border-[#FDE68A] bg-[#FFFBEB] p-4 text-[15px] text-[#92650A]"
      >
        <p class="font-bold">
          시연용 거래 데이터 · 전체 기간{{
            taskConfig?.category ? ` · ${categoryLabel} 분류` : ""
          }}
        </p>

        <p class="mt-1">
          {{
            taskConfig?.category
              ? `선택한 계좌에서 ‘${categoryLabel}’ 분류의 거래만 모아 보여줘요. 계좌를 바꾸면 해당 계좌의 같은 분류를 확인해요.`
              : "선택한 계좌의 저장된 잔액과 거래 내역을 확인해요."
          }}
        </p>
      </div>

      <p
        v-if="store.financeWarning"
        class="rounded-xl border border-[#FDE68A] bg-[#FFFBEB] p-3 text-[#92400E]"
        role="status"
      >
        {{ store.financeWarning }}
      </p>

      <!-- 불러오는 중 -->
      <p
        v-if="store.financeLoading || store.inquiryLoading"
        class="rounded-2xl bg-white p-5 text-[#6B7280]"
      >
        금융 정보를 불러오고 있어요…
      </p>

      <!-- 오류 -->
      <div
        v-else-if="store.financeError || store.inquiryError"
        class="space-y-3 rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-5"
      >
        <p class="text-[#991B1B]" role="alert">
          {{ store.financeError || store.inquiryError }}
        </p>

        <Btn variant="secondary" @click="prepareInquiry(true)"> 다시 시도 </Btn>
      </div>

      <!-- 본인 계좌 없음 -->
      <div
        v-else-if="store.ownedAccounts.length === 0"
        class="space-y-2 rounded-2xl bg-white p-5 text-center"
      >
        <p class="text-[19px] font-bold text-[#111827]">
          조회할 본인 계좌가 없어요.
        </p>

        <p class="text-[#6B7280]">수취 계좌는 조회 계좌로 사용할 수 없어요.</p>
      </div>

      <template v-else>
        <!-- 계좌 잔액 카드 -->
        <Card v-if="showAccountBalance" class="p-5">
          <!-- 은행 로고와 계좌 정보 -->
          <div class="flex items-center gap-3">
            <BankLogo
              v-if="selectedInquiryAccount"
              :bank-name="selectedInquiryAccount.bankName"
              size="medium"
            />

            <div class="min-w-0 flex-1">
              <p class="text-[17px] font-bold text-[#374151]">
                {{
                  selectedInquiryAccount?.accountAlias ||
                  selectedInquiryAccount?.bankName
                }}
              </p>

              <p class="mt-1 text-[15px] text-[#6B7280]">
                {{ selectedInquiryAccount?.bankName }}

                <span v-if="selectedInquiryAccount?.masked">
                  · {{ selectedInquiryAccount.masked }}
                </span>
              </p>
            </div>
          </div>

          <!-- 잔액과 잔액 보기 버튼 -->
          <div class="mt-5 flex items-center gap-3">
            <p class="text-[32px] font-black text-[#111827]">
              {{
                balanceVisible
                  ? formatWon(store.inquiryBalance?.balance)
                  : "• • • • • •"
              }}
            </p>

            <button
              type="button"
              class="ml-auto min-h-[48px] shrink-0 rounded-xl bg-[#F3F4F6] px-4 font-bold text-[#374151]"
              @click="balanceVisible = !balanceVisible"
            >
              {{ balanceVisible ? "숨기기" : "잔액 보기" }}
            </button>
          </div>
        </Card>

        <!-- 거래내역 필터 -->
        <div v-if="taskName === 'task-5'" class="flex gap-2">
          <button
            v-for="filter in historyFilters"
            :key="filter.key"
            type="button"
            :class="[
              'min-h-[48px] flex-1 rounded-xl border font-bold',
              historyFilter === filter.key
                ? 'border-[#FFBC00] bg-[#FFBC00] text-[#111827]'
                : 'border-[#E5E7EB] bg-white text-[#374151]',
            ]"
            @click="historyFilter = filter.key"
          >
            {{ filter.label }}
          </button>
        </div>

        <!-- 거래내역 없음 -->
        <div
          v-if="filteredTransactions.length === 0"
          class="space-y-2 rounded-2xl bg-white p-5 text-center"
        >
          <p class="font-bold text-[#111827]">
            {{ emptyTitle }}
          </p>

          <p class="text-[#6B7280]">
            {{ emptyDescription }}
          </p>
        </div>

        <!-- 거래내역 목록 -->
        <Card v-else class="overflow-hidden">
          <article
            v-for="(transaction, index) in filteredTransactions"
            :key="transaction.transactionId"
            :class="[
              'space-y-2 px-5 py-4',
              index < filteredTransactions.length - 1
                ? 'border-b border-[#F3F4F6]'
                : '',
            ]"
          >
            <div class="flex items-start justify-between gap-4">
              <div>
                <p class="text-[18px] font-bold text-[#111827]">
                  {{ transaction.counterpartyName || transaction.description }}
                </p>

                <p class="text-[14px] text-[#6B7280]">
                  {{ typeLabel(transaction.transactionType) }} ·
                  {{ formatDate(transaction.transactionAt) }}
                </p>
              </div>

              <p
                :class="[
                  'text-[18px] font-black',
                  isDeposit(transaction) ? 'text-[#2563EB]' : 'text-[#374151]',
                ]"
              >
                {{ isDeposit(transaction) ? "+" : "-"
                }}{{ formatWon(transaction.amount) }}
              </p>
            </div>

            <div class="flex justify-between gap-4 text-[14px] text-[#6B7280]">
              <span>
                {{ transaction.description }}
              </span>

              <span> 거래 후 {{ formatWon(transaction.balanceAfter) }} </span>
            </div>
          </article>
        </Card>
      </template>
    </div>

    <VoiceGuideBar
      v-if="resultGuidance"
      :text="resultGuidance"
      :speed="store.currentUser?.settings?.voiceSpeed ?? 'NORMAL'"
      voice-mode="TTS"
    />
  </div>

  <!-- 고객센터 -->
  <div
    v-else-if="taskName === 'task-6'"
    class="flex h-full flex-col bg-[#FAFAF8]"
  >
    <SafeArea />

    <TopBar title="고객센터 연결" :onBack="leaveTask" />

    <div class="flex flex-1 flex-col space-y-4 overflow-y-auto px-4 pb-6 pt-5">
      <p class="text-[25px] font-bold text-[#111827]">도움이 필요하신가요?</p>

      <p
        v-if="store.supportLoading"
        class="rounded-2xl bg-white p-5 text-[#6B7280]"
      >
        고객센터 번호를 불러오고 있어요…
      </p>

      <div
        v-else-if="store.supportError"
        class="space-y-3 rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-5"
      >
        <p class="text-[#991B1B]">
          {{ store.supportError }}
        </p>

        <Btn variant="secondary" @click="store.loadSupport(true)">
          다시 시도
        </Btn>
      </div>

      <Card
        v-else-if="store.support?.customerCenterPhone"
        class="space-y-4 p-6 text-center"
      >
        <div
          class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-[#DBEAFE] text-[#2563EB]"
        >
          <Ic name="Phone" />
        </div>

        <div>
          <p class="text-[#6B7280]">단짝 고객센터</p>

          <p class="mt-1 text-[28px] font-black text-[#111827]">
            {{ store.support.customerCenterPhone }}
          </p>
        </div>

        <a
          :href="'tel:' + store.support.customerCenterPhone"
          class="flex min-h-[58px] w-full items-center justify-center rounded-[18px] bg-[#2563EB] px-5 text-[18px] font-bold text-white"
          @click="completeSupportTask"
        >
          전화 연결하기
        </a>

        <p class="text-[14px] text-[#6B7280]">
          컴퓨터에서는 전화번호를 확인한 뒤 휴대전화로 걸어 주세요.
        </p>
      </Card>

      <div v-else class="space-y-3 rounded-2xl bg-white p-5 text-center">
        <p class="font-bold text-[#111827]">고객센터 번호가 없어요.</p>

        <p class="mt-2 text-[#6B7280]">잠시 후 다시 조회해 주세요.</p>

        <Btn variant="secondary" @click="store.loadSupport(true)">
          번호 다시 불러오기
        </Btn>
      </div>
    </div>
  </div>

  <!-- MVP 제외 기능 -->
  <div v-else class="flex h-full flex-col bg-[#FAFAF8]">
    <SafeArea />

    <TopBar title="준비 중인 기능" :onBack="leaveTask" />

    <div
      class="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center"
    >
      <div
        class="flex h-20 w-20 items-center justify-center rounded-full bg-[#E5E7EB]"
      >
        <Ic name="Gear" />
      </div>

      <p class="text-[24px] font-bold text-[#111827]">
        이 기능은 아직 준비 중이에요.
      </p>

      <p class="text-[17px] text-[#6B7280]">
        현재 시연에서는 잔액, 거래내역, 연금, 관리비, 공과금 조회를 이용할 수
        있어요.
      </p>

      <Btn variant="secondary" @click="store.goBack"> 돌아가기 </Btn>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useAppStore } from "../stores/appStore";
import SafeArea from "../components/common/SafeArea.vue";
import TopBar from "../components/common/TopBar.vue";
import Card from "../components/common/Card.vue";
import Btn from "../components/common/Btn.vue";
import Ic from "../components/common/Ic.vue";
import VoiceGuideBar from "../components/common/VoiceGuideBar.vue";
import BankLogo from "../components/common/BankLogo.vue";
import {
  inquiryResultText,
  RESULT_INQUIRY_CATEGORIES,
} from "../features/inquiry/resultGuidance.js";
import { profileImageForPerson } from "../constants/profileImages.js";

const props = defineProps({
  taskName: {
    type: String,
    required: true,
  },
});

const store = useAppStore();
const balanceVisible = ref(false);
const preparingInquiry = ref(true);
const historyFilter = ref("ALL");

const historyFilters = [
  {
    key: "ALL",
    label: "전체",
  },
  {
    key: "DEPOSIT",
    label: "입금",
  },
  {
    key: "OUTGOING",
    label: "출금",
  },
];

const taskConfig = computed(
  () =>
    ({
      "task-2": {
        title: "연금 입금 내역",
        category: "PENSION",
      },
      "pension-history": {
        title: "연금 입금 내역",
        category: "PENSION",
      },
      "task-3": {
        title: "관리비 내역",
        category: "MANAGEMENT_FEE",
      },
      "task-4": {
        title: "내 계좌 잔액",
        category: null,
      },
      "task-5": {
        title: "거래내역",
        category: null,
      },
      "task-8": {
        title: "공과금 내역",
        category: "UTILITY_BILL",
      },
    })[props.taskName] ?? null,
);

const isInquiryTask = computed(() => {
  return Boolean(taskConfig.value);
});

const inquiryTitle = computed(() => {
  return taskConfig.value?.title ?? "금융 조회";
});

const categoryLabel = computed(() => {
  const labels = {
    PENSION: "연금",
    MANAGEMENT_FEE: "관리비",
    UTILITY_BILL: "공과금",
  };

  return labels[taskConfig.value?.category] ?? "전체";
});

const resultGuidance = computed(() => {
  const category = RESULT_INQUIRY_CATEGORIES[props.taskName];

  if (
    !category ||
    preparingInquiry.value ||
    store.financeLoading ||
    store.inquiryLoading
  ) {
    return "";
  }

  if (store.financeError || store.inquiryError) {
    return `${categoryLabel.value} 내역을 불러오지 못했어요. 다시 시도해 주세요.`;
  }

  if (store.ownedAccounts.length === 0) {
    return "조회할 본인 계좌가 없어요. 계좌를 먼저 불러와 주세요.";
  }

  return inquiryResultText(category, store.inquiryTransactions);
});

const showAccountBalance = computed(() => {
  return ["task-4", "task-5"].includes(props.taskName);
});

const selectedInquiryAccount = computed(() => {
  return (
    store.ownedAccounts.find(
      (owned) => owned.accountId === store.selectedInquiryAccountId,
    ) ?? null
  );
});

const filteredTransactions = computed(() => {
  if (historyFilter.value === "DEPOSIT") {
    return store.inquiryTransactions.filter(
      (transaction) => transaction.transactionType === "DEPOSIT",
    );
  }

  if (historyFilter.value === "OUTGOING") {
    return store.inquiryTransactions.filter(
      (transaction) => transaction.transactionType !== "DEPOSIT",
    );
  }

  return store.inquiryTransactions;
});

const emptyTitle = computed(() => {
  if (taskConfig.value?.category) {
    return "이 분류의 거래가 없어요.";
  }

  if (historyFilter.value !== "ALL") {
    return "선택한 입출금 내역이 없어요.";
  }

  return "거래내역이 없어요.";
});

const emptyDescription = computed(() => {
  return taskConfig.value?.category
    ? `선택한 계좌의 ${inquiryTitle.value}을 확인했어요.`
    : "선택한 계좌의 전체 기간을 확인했어요.";
});

const person = computed(() => {
  return (
    store.people.find(
      (item) =>
        item.id === (store.activePattern?.personId ?? store.selectedPersonId),
    ) ?? null
  );
});

/*
 * 저장된 송금 패턴은 연결 계좌가 사라져도
 * 다른 계좌로 임의 대체하지 않습니다.
 */
const account = computed(() => {
  const accounts = store.accountsByPerson[person.value?.id] ?? [];

  const linkedAccountId = store.activePattern?.recipientAccountId ?? null;

  if (store.activePattern?.patternType === "TRANSFER") {
    return linkedAccountId
      ? (accounts.find((item) => item.accountId === linkedAccountId) ?? null)
      : null;
  }

  return accounts[0] ?? null;
});

onMounted(async () => {
  if (props.taskName === "task-6") {
    await store.loadSupport();
    return;
  }

  if (props.taskName === "task-transfer") {
    await store.loadFinancialData();
    return;
  }

  if (isInquiryTask.value) {
    await prepareInquiry();
  }
});

watch(
  () => props.taskName,
  async () => {
    historyFilter.value = "ALL";

    if (isInquiryTask.value) {
      await prepareInquiry();
    }
  },
);

async function prepareInquiry(force = false) {
  preparingInquiry.value = true;

  try {
    const loaded = await store.loadFinancialData(force);

    if (!loaded) return;

    const accountId =
      store.selectedInquiryAccountId ?? store.defaultOwnedAccount?.accountId;

    if (accountId) {
      await store.loadInquiry(accountId, taskConfig.value?.category ?? null);
    }
  } finally {
    preparingInquiry.value = false;
  }

  if (
    store.ownedAccounts.length &&
    !store.inquiryError &&
    store.activePatternDetail
  ) {
    await store.finishPatternExecution("COMPLETED");
  }
}

async function changeAccount(event) {
  const accountId = Number(event.target.value);

  await store.loadInquiry(accountId, taskConfig.value?.category ?? null);
}

async function beginPatternTransfer() {
  await store.loadFinancialData();

  if (!person.value || !account.value) {
    return;
  }

  store.startTransfer({
    pattern: true,
    personId: person.value.id,
    recipientAccountId: account.value.accountId,
  });

  store.selectRecipientAccount(account.value);
  store.navigate("transfer-source");
}

async function completeSupportTask() {
  if (store.activePatternDetail) {
    await store.finishPatternExecution("COMPLETED");
  }
}

async function leaveTask() {
  if (store.activePatternDetail) {
    store.recordPatternAction("back");

    await store.finishPatternExecution("CANCELLED");

    /*
     * 시작 화면을 보존해 홈에서 왔으면 홈으로,
     * 챗봇에서 왔으면 채팅으로 돌아갑니다.
     */
    store.goBack();

    return;
  }

  store.goBack();
}

function isDeposit(transaction) {
  return transaction.transactionType === "DEPOSIT";
}

function formatWon(value) {
  return `${Number(value ?? 0).toLocaleString("ko-KR")}원`;
}

function formatDate(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "시각 확인 불가";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function typeLabel(type) {
  const labels = {
    DEPOSIT: "입금",
    WITHDRAWAL: "출금",
    TRANSFER_OUT: "송금",
    PAYMENT: "납부",
  };

  return labels[type] ?? type;
}
</script>
