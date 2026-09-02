<template>
  <div className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="사람 및 계좌 관리" :onBack="store.goBack" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-3">
      <p className="text-[#6B7280] px-1" style="font-size: 16px;">송금할 수 있는 사람과 계좌를 관리해요.</p>

      <Card v-for="person in formattedPeople" :key="person.id" className="overflow-hidden">
        <div className="p-5 flex items-center gap-4">
          <div className="w-14 h-14 rounded-full bg-[#FFF3CC] border border-[#FFBC00] flex items-center justify-center flex-shrink-0" style="font-size: 28px;">
            {{ person.emoji }}
          </div>
          <div className="flex-1">
            <p className="font-bold text-[#111827]" style="font-size: 20px;">{{ person.name }}</p>
            <p className="text-[#6B7280]" style="font-size: 14px;">{{ person.relation }} · 계좌 {{ person.accs.length }}개</p>
          </div>
          <button
            @click="sel = person; sheet = true;"
            className="w-10 h-10 flex items-center justify-center text-[#9CA3AF] font-bold"
            style="font-size: 22px;"
          >
            ⋮
          </button>
        </div>
        <div className="border-t border-[#F3F4F6]">
          <div
            v-for="(acc, i) in person.accs"
            :key="acc.masked"
            :class="['flex items-center gap-3 px-5 py-4', i < person.accs.length - 1 ? 'border-b border-[#F3F4F6]' : '']"
          >
            <div
              className="w-10 h-10 rounded-[10px] flex items-center justify-center font-black flex-shrink-0"
              style="font-size: 11px; background: #FFBC00; color: #111827;"
            >
              {{ acc.bank.slice(0, 2) }}
            </div>
            <div className="flex-1 min-w-0">
              <p className="font-bold text-[#374151]" style="font-size: 15px;">{{ acc.bank }}</p>
              <p className="font-mono text-[#9CA3AF]" style="font-size: 13px;">{{ acc.masked }}</p>
            </div>
            <span v-if="acc.primary" className="font-bold text-[#92650A] bg-[#FFF3CC] px-2 py-0.5 rounded-full border border-[#FFBC00]" style="font-size: 11px;">
              주 계좌
            </span>
          </div>
          <button className="w-full flex items-center gap-2 px-5 py-4 font-bold border-t border-[#F3F4F6]" style="color: #92650A; font-size: 15px;">
            <Ic name="Plus" /><span>계좌 추가</span>
          </button>
        </div>
      </Card>

      <button
        @click="store.navigate('add-person')"
        className="w-full border-2 border-dashed border-[#FFBC00] rounded-[20px] font-bold text-[#92650A] flex items-center justify-center gap-2 active:bg-[#FFF3CC] transition-all"
        style="min-height: 60px; font-size: 17px;"
      >
        <Ic name="Plus" />새로운 사람 추가
      </button>
    </div>

    <BottomSheet :open="sheet" @close="sheet = false">
      <SheetRow label="정보 수정" :sub="sel?.name" @click="sheet = false" />
      <SheetRow label="삭제" color="#EF4444" :sub="sel?.name" @click="deletePerson" />
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import Card from '../components/common/Card.vue';
import Ic from '../components/common/Ic.vue';
import BottomSheet from '../components/common/BottomSheet.vue';
import SheetRow from '../components/common/SheetRow.vue';

const store = useAppStore();

const sheet = ref(false);
const sel = ref(null);

const formattedPeople = computed(() => {
  return store.people.map((p) => ({
    ...p,
    accs: store.accountsByPerson[p.id] || []
  }));
});

function deletePerson() {
  if (sel.value) {
    store.removePerson(sel.value.id);
  }
  sheet.value = false;
}
</script>
