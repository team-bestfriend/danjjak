<template>
  <div class="flex flex-col h-full bg-white">
    <SafeArea />
    <TopBar :title="pageTitle" :onBack="closeForm" />
    <div class="flex-1 overflow-y-auto px-4 pt-4 pb-6">
      <p v-if="store.financeLoading" class="rounded-2xl bg-[#FAFAF8] p-5 text-[#6B7280]">등록 정보를 불러오고 있어요…</p>
      <div v-else-if="missingTarget" class="rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-5 space-y-3">
        <p class="text-[#991B1B]">수정할 등록 정보를 찾을 수 없어요.</p>
        <Btn variant="secondary" @click="closeForm">목록으로 돌아가기</Btn>
      </div>
      <RecipientAccountForm
        v-else-if="accountPerson"
        :registeredPersonId="accountPerson.id"
        :personName="accountPerson.name"
        :existingAccount="existingAccount"
        @saved="onSaved"
        @cancel="closeForm"
      />
      <AddPersonForm v-else :existingPerson="existingPerson" @saved="onSaved" @cancel="closeForm" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import AddPersonForm from '../components/common/AddPersonForm.vue';
import Btn from '../components/common/Btn.vue';
import RecipientAccountForm from '../components/common/RecipientAccountForm.vue';

const store = useAppStore();
const existingPerson = computed(() => {
  const person = store.people.find((item) => item.id === store.editingPersonId);
  return person ?? null;
});
const accountPerson = computed(() => (
  store.people.find((item) => item.id === store.accountFormPersonId) ?? null
));
const existingAccount = computed(() => (
  (store.accountsByPerson[store.accountFormPersonId] ?? [])
    .find((account) => account.accountId === store.editingRecipientAccountId) ?? null
));
const pageTitle = computed(() => {
  if (store.accountFormPersonId) {
    return store.editingRecipientAccountId ? '받는 계좌 수정' : '받는 계좌 추가';
  }
  return existingPerson.value ? '등록 정보 수정' : '새로운 사람 등록';
});
const missingTarget = computed(() => (
  !store.financeLoading
  && ((store.editingPersonId && !existingPerson.value)
    || (store.accountFormPersonId && !accountPerson.value)
    || (store.editingRecipientAccountId && !existingAccount.value))
));

onMounted(() => store.loadFinancialData());

function onSaved() {
  closeForm();
}

function closeForm() {
  store.editingPersonId = null;
  store.accountFormPersonId = null;
  store.editingRecipientAccountId = null;
  store.goBack();
}
</script>
