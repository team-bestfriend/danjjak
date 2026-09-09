<template>
  <div class="flex h-full flex-col bg-[#FAFAF8]">
    <SafeArea />

    <TopBar title="사람 및 계좌 관리" :onBack="store.goBack" />

    <div class="flex-1 space-y-4 overflow-y-auto px-4 pb-6 pt-4">
      <p class="text-[16px] text-[#6B7280]">
        송금할 사람과 여러 개의 받는 계좌를 관리해요.
      </p>

      <!-- 불러오는 중 -->
      <p
        v-if="store.financeLoading"
        class="rounded-2xl bg-white p-5 text-[#6B7280]"
      >
        등록 정보를 불러오고 있어요…
      </p>

      <!-- 오류 -->
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

      <!-- 등록된 사람이 없는 경우 -->
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

      <!-- 등록된 사람 목록 -->
      <template v-else>
        <Card
          v-for="person in formattedPeople"
          :key="person.id"
          class="overflow-hidden"
        >
          <!-- 사람 정보 -->
          <div class="flex items-center gap-4 p-5">
            <div
              class="flex h-14 w-14 shrink-0 items-center justify-center rounded-full border border-[#FFBC00] bg-[#FFF3CC] text-[28px]"
            >
              {{ person.emoji }}
            </div>

            <div class="min-w-0 flex-1">
              <p class="text-[20px] font-bold text-[#111827]">
                {{ person.name }}
              </p>

              <p class="text-[14px] text-[#6B7280]">
                {{ person.relation }} · 등록 계좌 {{ person.accounts.length }}개
              </p>
            </div>

            <button
              type="button"
              class="min-h-[48px] shrink-0 rounded-xl border border-[#D1D5DB] px-4 font-bold text-[#374151]"
              @click="openEdit(person.id)"
            >
              수정
            </button>
          </div>

          <!-- 계좌 목록 -->
          <div
            v-if="person.accounts.length"
            class="divide-y divide-[#F3F4F6] border-t border-[#F3F4F6]"
          >
            <div
              v-for="account in person.accounts"
              :key="account.accountId"
              class="flex items-center gap-3 px-5 py-4"
            >
              <!-- 은행별 로고 -->
              <BankLogo :bank-name="account.bankName" size="medium" />

              <!-- 계좌 정보 -->
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

          <!-- 계좌가 없는 경우 -->
          <p v-else class="border-t border-[#F3F4F6] px-5 py-4 text-[#6B7280]">
            등록된 수취 계좌가 없어요.
          </p>

          <!-- 계좌 추가 -->
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
  </div>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { useAppStore } from "../stores/appStore";
import SafeArea from "../components/common/SafeArea.vue";
import TopBar from "../components/common/TopBar.vue";
import Card from "../components/common/Card.vue";
import Btn from "../components/common/Btn.vue";
import Ic from "../components/common/Ic.vue";
import BankLogo from "../components/common/BankLogo.vue";

const store = useAppStore();

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
</script>
