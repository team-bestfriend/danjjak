<template>
  <div class="flex h-full flex-col bg-[#FAFAF8]">
    <SafeArea />

    <TopBar title="사람 및 계좌 관리" :onBack="store.goBack" />

    <div class="flex-1 space-y-4 overflow-y-auto px-4 pb-6 pt-4">
      <p class="text-[16px] text-[#6B7280]">
        송금할 사람과 여러 개의 받는 계좌를 관리해요.
      </p>

      <p
        v-if="store.financeLoading"
        class="rounded-2xl bg-white p-5 text-[#6B7280]"
      >
        등록 정보를 불러오고 있어요…
      </p>

      <div
        v-else-if="store.financeError"
        class="space-y-3 rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-5"
      >
        <p class="text-[#991B1B]">
          {{ store.financeError }}
        </p>

        <Btn variant="secondary" @click="store.loadFinancialData(true)">
          다시 시도
        </Btn>
      </div>

      <div
        v-else-if="formattedPeople.length === 0"
        class="space-y-3 rounded-2xl bg-white p-5 text-center"
      >
        <p class="text-[19px] font-bold text-[#111827]">
          등록된 사람이 없어요.
        </p>

        <p class="text-[#6B7280]">자주 송금하는 사람과 계좌를 등록해 보세요.</p>

        <Btn @click="openCreate"> 사람 추가 </Btn>
      </div>

      <template v-else>
        <Card
          v-for="person in formattedPeople"
          :key="person.id"
          class="overflow-hidden"
        >
          <div class="flex items-center gap-4 p-5">
            <div
              class="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-[#FFE08A] bg-white text-[28px]"
            >
              <img
                v-if="profileImageForPerson(person)"
                :src="profileImageForPerson(person)"
                alt=""
                class="h-full w-full object-cover"
                aria-hidden="true"
              />
              <span v-else>{{ person.emoji }}</span>
            </div>

            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2">
                <p class="text-[20px] font-bold text-[#111827]">
                  {{ person.name }}
                </p>
                <span
                  class="shrink-0 rounded-full bg-[#FFF3CC] px-2 py-0.5 text-[12px] font-bold text-[#B45309]"
                >
                  {{ person.relation }}
                </span>
              </div>

              <p class="mt-1 text-[14px] text-[#6B7280]">
                등록 계좌 {{ person.accounts.length }}개
              </p>
            </div>

            <div class="flex shrink-0 gap-2">
              <button
                type="button"
                class="min-h-[48px] rounded-xl border border-[#D1D5DB] px-4 font-bold text-[#374151]"
                @click="openEdit(person.id)"
              >
                수정
              </button>
              <button
                type="button"
                :disabled="store.registeredPersonDeletingId === person.id"
                class="min-h-[48px] rounded-xl border border-[#FCA5A5] px-4 font-bold text-[#B91C1C] disabled:opacity-50"
                @click="deletePerson(person)"
              >
                {{
                  store.registeredPersonDeletingId === person.id
                    ? "삭제 중"
                    : "삭제"
                }}
              </button>
            </div>
          </div>

          <div
            v-if="person.accounts.length && !isAccountsCollapsed(person.id)"
            class="divide-y divide-[#F3F4F6] border-t border-[#F3F4F6]"
          >
            <div
              v-for="account in person.accounts"
              :key="account.accountId"
              class="flex items-center gap-3 px-5 py-4"
            >
              <BankLogo :bank-name="account.bankName" size="medium" />

              <div class="min-w-0 flex-1">
                <p class="text-[15px] font-bold text-[#374151]">
                  {{ account.bankName }}

                  <span v-if="account.accountAlias">
                    · {{ account.accountAlias }}
                  </span>
                </p>

                <p class="font-mono text-[13px] text-[#9CA3AF]">
                  {{ account.masked }}
                </p>
              </div>

              <button
                type="button"
                class="min-h-[48px] shrink-0 rounded-xl border border-[#D1D5DB] px-3 font-bold text-[#374151]"
                @click="openAccountEdit(person.id, account.accountId)"
              >
                계좌 수정
              </button>
            </div>
          </div>

          <button
            v-if="person.accounts.length > 1"
            type="button"
            :aria-expanded="!isAccountsCollapsed(person.id)"
            class="w-full border-t border-[#F3F4F6] px-5 py-3 text-[14px] font-bold text-[#76520A]"
            @click="toggleAccounts(person.id)"
          >
            {{
              isAccountsCollapsed(person.id)
                ? `계좌 ${person.accounts.length}개 펼치기`
                : "계좌 접기"
            }}
          </button>

          <p v-if="person.accounts.length === 0" class="border-t border-[#F3F4F6] px-5 py-4 text-[#6B7280]">
            등록된 수취 계좌가 없어요.
          </p>

          <div class="border-t border-[#F3F4F6] p-4">
            <button
              type="button"
              class="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-xl bg-[#FFF3CC] px-4 font-bold text-[#76520A]"
              @click="openAccountCreate(person.id)"
            >
              <Ic name="Plus" />
              계좌 추가
            </button>
          </div>
        </Card>

        <Btn @click="openCreate">
          <Ic name="Plus" />
          사람 추가
        </Btn>
      </template>
    </div>

    <div
      v-if="deleteDialog"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-6"
      role="presentation"
      @click.self="closeDeleteDialog"
    >
      <div
        class="w-full max-w-[320px] rounded-[24px] bg-white p-5 text-center shadow-xl"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="deleteDialog.type === 'error' ? 'delete-error-title' : 'delete-confirm-title'"
      >
        <h2
          :id="deleteDialog.type === 'error' ? 'delete-error-title' : 'delete-confirm-title'"
          class="text-[21px] font-bold text-[#111827]"
        >
          {{ deleteDialog.title }}
        </h2>
        <p class="mt-3 text-[16px] leading-relaxed text-[#6B7280]">
          {{ deleteDialog.message }}
        </p>
        <div class="mt-5 flex gap-2">
          <template v-if="deleteDialog.type === 'confirm'">
            <button
              type="button"
              class="min-h-[52px] flex-1 rounded-[14px] border border-[#D1D5DB] font-bold text-[#374151]"
              :disabled="store.registeredPersonDeletingId !== null"
              @click="closeDeleteDialog"
            >
              취소
            </button>
            <button
              type="button"
              class="min-h-[52px] flex-1 rounded-[14px] bg-[#EF4444] font-bold text-white disabled:opacity-50"
              :disabled="store.registeredPersonDeletingId !== null"
              @click="confirmDeletePerson"
            >
              {{
                store.registeredPersonDeletingId !== null
                  ? "삭제 중"
                  : "삭제"
              }}
            </button>
          </template>
          <button
            v-else
            type="button"
            class="min-h-[52px] flex-1 rounded-[14px] bg-[#FFBC00] font-bold text-[#111827]"
            @click="closeDeleteDialog"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ApiError } from "../api/httpClient.js";
import { useAppStore } from "../stores/appStore";
import SafeArea from "../components/common/SafeArea.vue";
import TopBar from "../components/common/TopBar.vue";
import Card from "../components/common/Card.vue";
import Btn from "../components/common/Btn.vue";
import Ic from "../components/common/Ic.vue";
import BankLogo from "../components/common/BankLogo.vue";
import { profileImageForPerson } from "../constants/profileImages.js";

const store = useAppStore();
const collapsedPersonIds = ref(new Set());
const deleteDialog = ref(null);
const formattedPeople = computed(() =>
  store.people.map((person) => ({
    ...person,
    accounts: store.accountsByPerson[person.id] ?? [],
  })),
);

onMounted(() => {
  store.loadFinancialData();
});

function openCreate() {
  store.editingPersonId = null;
  store.accountFormPersonId = null;
  store.editingRecipientAccountId = null;
  store.navigate("add-person");
}

function openEdit(personId) {
  store.editingPersonId = personId;
  store.accountFormPersonId = null;
  store.editingRecipientAccountId = null;
  store.navigate("add-person");
}

function openAccountCreate(personId) {
  store.editingPersonId = null;
  store.accountFormPersonId = personId;
  store.editingRecipientAccountId = null;
  store.navigate("add-person");
}

function openAccountEdit(personId, accountId) {
  store.editingPersonId = null;
  store.accountFormPersonId = personId;
  store.editingRecipientAccountId = accountId;
  store.navigate("add-person");
}

function isAccountsCollapsed(personId) {
  return collapsedPersonIds.value.has(personId);
}

function toggleAccounts(personId) {
  const nextIds = new Set(collapsedPersonIds.value);
  if (nextIds.has(personId)) {
    nextIds.delete(personId);
  } else {
    nextIds.add(personId);
  }
  collapsedPersonIds.value = nextIds;
}

function deletePerson(person) {
  deleteDialog.value = {
    type: "confirm",
    person,
    title: `${person.name}님을 삭제할까요?`,
    message: "삭제하면 등록된 수취 계좌도 함께 삭제돼요.",
  };
}

async function confirmDeletePerson() {
  if (deleteDialog.value?.type !== "confirm") return;
  const person = deleteDialog.value.person;
  try {
    await store.deleteRegisteredPerson(person.id);
    deleteDialog.value = null;
  } catch (error) {
    deleteDialog.value = {
      type: "error",
      title: "삭제할 수 없어요.",
      message:
        error instanceof ApiError
          ? error.message
          : "삭제 요청에 실패했습니다. 다시 시도해 주세요.",
    };
  }
}

function closeDeleteDialog() {
  if (store.registeredPersonDeletingId !== null) return;
  deleteDialog.value = null;
}
</script>
