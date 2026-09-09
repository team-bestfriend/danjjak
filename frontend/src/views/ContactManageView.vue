<template>
  <div class="flex flex-col h-full bg-[#FAFAF8]">
    <SafeArea />
    <TopBar title="사람 및 계좌 관리" :onBack="store.goBack" />
    <div class="flex-1 overflow-y-auto px-4 pt-4 pb-6 space-y-4">
      <p class="text-[#6B7280] text-[16px]">
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
        class="rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-5 space-y-3"
      >
        <p class="text-[#991B1B]">{{ store.financeError }}</p>
        <Btn variant="secondary" @click="store.loadFinancialData(true)"
          >다시 시도</Btn
        >
      </div>
      <div
        v-else-if="formattedPeople.length === 0"
        class="rounded-2xl bg-white p-5 text-center space-y-3"
      >
        <p class="font-bold text-[#111827] text-[19px]">
          등록된 사람이 없어요.
        </p>
        <p class="text-[#6B7280]">자주 송금하는 사람과 계좌를 등록해 보세요.</p>
        <Btn @click="openCreate">사람 추가</Btn>
      </div>
      <template v-else>
        <Card
          v-for="person in formattedPeople"
          :key="person.id"
          class="overflow-hidden"
        >
          <div class="p-5 flex items-center gap-4">
            <div
              class="w-16 h-16 rounded-full bg-white border-2 border-[#FFE08A] overflow-hidden flex items-center justify-center"
            >
              <img
                v-if="PROFILE_IMAGES[person.profileImageKey]"
                :src="PROFILE_IMAGES[person.profileImageKey]"
                alt=""
                class="h-full w-full object-cover"
                aria-hidden="true"
              />
              <span v-else class="text-[28px]">{{ person.emoji }}</span>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <p class="font-bold text-[#111827] text-[20px]">
                  {{ person.name }}
                </p>
                <span
                  class="shrink-0 rounded-full bg-[#FFF3CC] px-2 py-0.5 text-[12px] font-bold text-[#B45309]"
                >
                  {{ person.relation }}
                </span>
              </div>
              <p class="mt-1 text-[#6B7280] text-[14px]">
                등록 계좌 {{ person.accounts.length }}개
              </p>
            </div>
            <div class="flex gap-2">
              <button
                @click="openEdit(person.id)"
                class="min-h-[48px] rounded-xl border border-[#D1D5DB] px-4 font-bold text-[#374151]"
              >
                수정
              </button>
              <button
                :disabled="store.registeredPersonDeletingId === person.id"
                @click="deletePerson(person)"
                class="min-h-[48px] rounded-xl border border-[#FCA5A5] px-4 font-bold text-[#B91C1C] disabled:opacity-50"
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
            class="border-t border-[#F3F4F6] divide-y divide-[#F3F4F6]"
          >
            <div
              v-for="account in person.accounts"
              :key="account.accountId"
              class="px-5 py-4 flex items-center gap-3"
            >
              <div
                class="w-10 h-10 rounded-[10px] flex items-center justify-center font-black bg-[#FFBC00] text-[#111827] text-[11px]"
              >
                {{ account.bankName.slice(0, 2) }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-bold text-[#374151] text-[15px]">
                  {{ account.bankName
                  }}<span v-if="account.accountAlias">
                    · {{ account.accountAlias }}</span
                  >
                </p>
                <p class="font-mono text-[#9CA3AF] text-[13px]">
                  {{ account.masked }}
                </p>
              </div>
              <button
                @click="openAccountEdit(person.id, account.accountId)"
                class="min-h-[48px] rounded-xl border border-[#D1D5DB] px-3 font-bold text-[#374151]"
              >
                계좌 수정
              </button>
            </div>
          </div>
          <button
            v-if="person.accounts.length > 1"
            type="button"
            :aria-expanded="!isAccountsCollapsed(person.id)"
            @click="toggleAccounts(person.id)"
            class="w-full border-t border-[#F3F4F6] px-5 py-3 text-[14px] font-bold text-[#76520A]"
          >
            {{
              isAccountsCollapsed(person.id)
                ? `계좌 ${person.accounts.length}개 펼치기`
                : "계좌 접기"
            }}
          </button>
          <p
            v-if="person.accounts.length === 0"
            class="border-t border-[#F3F4F6] px-5 py-4 text-[#6B7280]"
          >
            등록된 수취 계좌가 없어요.
          </p>
          <div class="border-t border-[#F3F4F6] p-4">
            <button
              @click="openAccountCreate(person.id)"
              class="w-full min-h-[48px] rounded-xl bg-[#FFF3CC] px-4 font-bold text-[#76520A] flex items-center justify-center gap-2"
            >
              <Ic name="Plus" /> 계좌 추가
            </button>
          </div>
        </Card>

        <Btn @click="openCreate"><Ic name="Plus" />사람 추가</Btn>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useAppStore } from "../stores/appStore";
import SafeArea from "../components/common/SafeArea.vue";
import TopBar from "../components/common/TopBar.vue";
import Card from "../components/common/Card.vue";
import Btn from "../components/common/Btn.vue";
import Ic from "../components/common/Ic.vue";
import baby from "../assets/icons/profile/baby.png";
import youthBoy from "../assets/icons/profile/youth_boy.png";
import youthWoman from "../assets/icons/profile/youth_woman.png";
import adultMan from "../assets/icons/profile/adult_man.png";
import adultWoman from "../assets/icons/profile/adult_woman.png";
import middleAgedMan from "../assets/icons/profile/middle_aged_man.png";
import middleAgedWoman from "../assets/icons/profile/middle_aged_woman.png";
import oldAgeMan from "../assets/icons/profile/old_age_man.png";
import oldAgeWoman from "../assets/icons/profile/old_age_woman.png";

const PROFILE_IMAGES = {
  baby,
  youth_boy: youthBoy,
  youth_woman: youthWoman,
  adult_man: adultMan,
  adult_woman: adultWoman,
  middle_aged_man: middleAgedMan,
  middle_aged_woman: middleAgedWoman,
  old_age_man: oldAgeMan,
  old_age_woman: oldAgeWoman,
};

const store = useAppStore();
const collapsedPersonIds = ref(new Set());
const formattedPeople = computed(() =>
  store.people.map((person) => ({
    ...person,
    accounts: store.accountsByPerson[person.id] ?? [],
  })),
);

onMounted(() => store.loadFinancialData());

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

async function deletePerson(person) {
  if (!window.confirm(`${person.name}님을 삭제할까요?`)) return;
  await store.deleteRegisteredPerson(person.id);
}
</script>
