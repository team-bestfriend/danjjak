<template>
  <div class="flex flex-col h-full bg-[#FAFAF8]">
    <SafeArea />
    <TopBar title="사람 및 계좌 관리" :onBack="store.goBack" />
    <div class="flex-1 overflow-y-auto px-4 pt-4 pb-6 space-y-4">
      <p class="text-[#6B7280] text-[16px]">송금할 사람과 여러 개의 받는 계좌를 관리해요.</p>

      <p v-if="store.financeLoading" class="rounded-2xl bg-white p-5 text-[#6B7280]">등록 정보를 불러오고 있어요…</p>
      <div v-else-if="store.financeError" class="rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-5 space-y-3">
        <p class="text-[#991B1B]">{{ store.financeError }}</p>
        <Btn variant="secondary" @click="store.loadFinancialData(true)">다시 시도</Btn>
      </div>
      <div v-else-if="formattedPeople.length === 0" class="rounded-2xl bg-white p-5 text-center space-y-3">
        <p class="font-bold text-[#111827] text-[19px]">등록된 사람이 없어요.</p>
        <p class="text-[#6B7280]">자주 송금하는 사람과 계좌를 등록해 보세요.</p>
        <Btn @click="openCreate">사람 추가</Btn>
      </div>
      <template v-else>
        <Card v-for="person in formattedPeople" :key="person.id" class="overflow-hidden">
          <div class="p-5 flex items-center gap-4">
            <div class="w-14 h-14 rounded-full bg-[#FFF3CC] border border-[#FFBC00] flex items-center justify-center text-[28px]">
              {{ person.emoji }}
            </div>
            <div class="flex-1">
              <p class="font-bold text-[#111827] text-[20px]">{{ person.name }}</p>
              <p class="text-[#6B7280] text-[14px]">{{ person.relation }} · 등록 계좌 {{ person.accounts.length }}개</p>
            </div>
            <button
              @click="openEdit(person.id)"
              class="min-h-[48px] rounded-xl border border-[#D1D5DB] px-4 font-bold text-[#374151]"
            >수정</button>
          </div>
          <div v-if="person.accounts.length" class="border-t border-[#F3F4F6] divide-y divide-[#F3F4F6]">
            <div v-for="account in person.accounts" :key="account.accountId" class="px-5 py-4 flex items-center gap-3">
              <div class="w-10 h-10 rounded-[10px] flex items-center justify-center font-black bg-[#FFBC00] text-[#111827] text-[11px]">
                {{ account.bankName.slice(0, 2) }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-bold text-[#374151] text-[15px]">{{ account.bankName }}<span v-if="account.accountAlias"> · {{ account.accountAlias }}</span></p>
                <p class="font-mono text-[#9CA3AF] text-[13px]">{{ account.masked }}</p>
              </div>
              <button
                @click="openAccountEdit(person.id, account.accountId)"
                class="min-h-[48px] rounded-xl border border-[#D1D5DB] px-3 font-bold text-[#374151]"
              >계좌 수정</button>
            </div>
          </div>
          <p v-else class="border-t border-[#F3F4F6] px-5 py-4 text-[#6B7280]">등록된 수취 계좌가 없어요.</p>
          <div class="border-t border-[#F3F4F6] p-4">
            <button
              @click="openAccountCreate(person.id)"
              class="w-full min-h-[48px] rounded-xl bg-[#FFF3CC] px-4 font-bold text-[#76520A] flex items-center justify-center gap-2"
            ><Ic name="Plus" /> 계좌 추가</button>
          </div>
        </Card>

        <Btn @click="openCreate"><Ic name="Plus" />사람 추가</Btn>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import Card from '../components/common/Card.vue';
import Btn from '../components/common/Btn.vue';
import Ic from '../components/common/Ic.vue';

const store = useAppStore();
const formattedPeople = computed(() => store.people.map((person) => ({
  ...person,
  accounts: store.accountsByPerson[person.id] ?? [],
})));

onMounted(() => store.loadFinancialData());

function openCreate() {
  store.editingPersonId = null;
  store.accountFormPersonId = null;
  store.editingRecipientAccountId = null;
  store.navigate('add-person');
}

function openEdit(personId) {
  store.editingPersonId = personId;
  store.accountFormPersonId = null;
  store.editingRecipientAccountId = null;
  store.navigate('add-person');
}

function openAccountCreate(personId) {
  store.editingPersonId = null;
  store.accountFormPersonId = personId;
  store.editingRecipientAccountId = null;
  store.navigate('add-person');
}

function openAccountEdit(personId, accountId) {
  store.editingPersonId = null;
  store.accountFormPersonId = personId;
  store.editingRecipientAccountId = accountId;
  store.navigate('add-person');
}
</script>
