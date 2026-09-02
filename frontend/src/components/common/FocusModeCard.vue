<template>
  <div
    className="bg-white rounded-[28px] mx-5 overflow-hidden w-full max-w-[340px]"
    style="box-shadow: 0 8px 40px rgba(0,0,0,0.22);"
    @click.stop
  >
    <!-- Header: KB Yellow, dark text -->
    <div className="flex flex-col items-center justify-center gap-2 px-8 pt-10 pb-8" style="background: #FFBC00;">
      <span className="font-black text-[#111827] leading-none" style="font-size: 76px;">{{ pat.num }}</span>
      <span className="font-black text-[#111827] text-center" style="font-size: 22px; margin-top: 4px;">{{ pat.label }}</span>
      <span v-if="person" className="text-[15px] font-bold" style="color: rgba(0,0,0,0.55);">
        {{ person.emoji }} {{ person.name }} · {{ person.relation }}
      </span>
    </div>

    <div className="p-5 space-y-4">
      <div>
        <p className="text-[13px] font-bold text-[#9CA3AF] uppercase tracking-wide mb-2">업무 안내 · 자동 TTS</p>
        <div className="flex items-center gap-3">
          <button
            @click="togglePlaying"
            className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 text-[14px] font-bold"
            style="background: #FFF3CC; color: #92650A;"
          >
            {{ playing ? '⏸' : '▶' }}
          </button>
          <div className="flex-1">
            <p className="text-[14px] font-bold text-[#111827] mb-1.5">"{{ quote }}"</p>
            <div className="flex items-end gap-px h-5">
              <div
                v-for="(_, i) in 28"
                :key="i"
                :class="['flex-1 rounded-sm transition-colors']"
                :style="{
                  height: `${Math.max(3, Math.abs(Math.sin(i * 0.9)) * 12 + 3)}px`,
                  backgroundColor: (i / 28) < (prog / 100) ? '#FFBC00' : '#F3F4F6'
                }"
              />
            </div>
          </div>
        </div>
      </div>

      <div className="flex gap-2 pt-1">
        <button
          @click="$emit('cancel')"
          className="flex-1 rounded-[14px] border-2 border-[#E5E7EB] font-bold text-[#374151]"
          style="height: 56px; font-size: 17px;"
        >
          취소하기
        </button>
        <button
          @click="$emit('start')"
          className="flex-1 rounded-[14px] font-black text-[#111827]"
          style="height: 56px; font-size: 17px; background: #FFBC00;"
        >
          시작하기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useAppStore } from '../../stores/appStore';
import { generatePatternDesc } from '../../constants/data';

const props = defineProps({
  pat: { type: Object, required: true }
});

defineEmits(['cancel', 'start']);

const store = useAppStore();

const playing = ref(false);
const prog = ref(0);
let timer = null;
let initTimer = null;

const person = computed(() => {
  return props.pat.personId ? store.people.find((p) => p.id === props.pat.personId) : null;
});

const quote = computed(() => {
  return generatePatternDesc(props.pat, store.people, store.accountsByPerson);
});

function togglePlaying() {
  playing.value = !playing.value;
  if (playing.value && prog.value >= 100) prog.value = 0;
}

watch([playing, prog], ([newPlaying, newProg]) => {
  if (timer) clearTimeout(timer);
  if (newPlaying && newProg < 100) {
    timer = setTimeout(() => {
      prog.value = Math.min(prog.value + 3, 100);
    }, 150);
  } else if (newProg >= 100) {
    playing.value = false;
    prog.value = 0;
  }
});

onMounted(() => {
  initTimer = setTimeout(() => {
    playing.value = true;
  }, 600);
});

onUnmounted(() => {
  if (timer) clearTimeout(timer);
  if (initTimer) clearTimeout(initTimer);
});
</script>
