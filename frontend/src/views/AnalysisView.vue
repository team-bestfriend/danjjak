<template>
  <div className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div className="bg-white px-5 border-b border-[#EEEEED] flex-shrink-0" style="padding-top: 18px; padding-bottom: 16px;">
      <h1 className="font-bold text-[#111827]" style="font-size: 26px;">어머님 이용 분석</h1>
      <p className="font-normal text-[#9CA3AF] mt-1" style="font-size: 15px;">최근 7일</p>
    </div>
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
<!-- 최근 7일 Summary -->
<div
  className="rounded-[20px] border border-[#FFBC00] flex items-center justify-between px-6"
  style="background: #FFFBEB; min-height: 92px;"
>
  <div className="flex flex-col gap-1">
    <p
      className="font-semibold"
      style="font-size: 16px; color: #92650A;"
    >
      금융 업무 실행
    </p>
  </div>

  <div className="flex items-baseline gap-1">
    <span
      className="font-bold text-[#111827]"
      style="font-size: 38px; line-height: 1;"
    >
      12
    </span>
    <span
      className="font-semibold"
      style="font-size: 17px; color: #92650A;"
    >
      회
    </span>
  </div>
</div>

      <!-- 패턴별 이용 현황 -->
      <Card className="p-4">
        <p className="font-semibold text-[#111827] mb-4" style="font-size: 19px;">패턴별 이용 현황</p>
        <div className="space-y-4">
          <div v-for="u in usageData" :key="u.id">
            <div className="flex items-center justify-between mb-2">
              <span className="flex items-center gap-2 font-normal text-[#374151]" style="font-size: 16px;">
                <span
                  v-if="u.num !== undefined"
                  className="font-bold px-2 py-0.5 rounded-md text-white"
                  :style="{ fontSize: '12px', background: u.color }"
                >
                  {{ u.num }}
                </span>
                {{ u.label }}
              </span>
              <span className="font-bold text-[#111827]" style="font-size: 15px;">{{ u.count }}회</span>
            </div>
            <div className="h-3 bg-[#F3F4F6] rounded-full overflow-hidden">
              <div
                className="h-full rounded-full transition-all duration-700"
                :style="{ width: `${(u.count / maxCount) * 100}%`, background: u.color }"
              />
            </div>
          </div>
        </div>
      </Card>

      <!-- 민수의 한마디 -->
      <Card className="p-5 border-2 border-[#FFBC00]" style="background: #FFFDF5;">
        <div className="flex items-center gap-3 mb-4">
          <div className="w-11 h-11 rounded-full bg-[#FFF3CC] flex items-center justify-center flex-shrink-0" style="font-size: 22px;">
            {{ minsuEmoji }}
          </div>
          <div>
            <p className="font-semibold text-[#111827]" style="font-size: 17px;">
              {{ minsuName }} ({{ minsuRelation }})
            </p>
            <p className="font-semibold" style="font-size: 17px; color: #92650A;">
              민수의 한마디 💬
            </p>
          </div>
        </div>

        <div className="bg-[#FFF3CC] border border-[#FFBC00] rounded-[18px] p-4 mb-3">
          <p className="font-normal text-[#111827] leading-snug" style="font-size: 17px;">
            어머니, 이번 달 금융 업무를 <span className="font-bold" style="font-size: 19px; color: #92650A;">총 84번</span> 하셨어요! 대부분 혼자서 잘 하고 계세요 👍
          </p>
        </div>

        <div className="bg-[#FFF3CC] border border-[#FFBC00] rounded-[18px] p-4 mb-3 space-y-2">
          <p className="font-semibold" style="font-size: 16px; color: #92650A;">
            💡 이런 부분이 좀 어려우신 것 같아요
          </p>
          <p className="font-normal text-[#374151] leading-relaxed" style="font-size: 16px;">
            <span className="font-semibold text-[#111827]">7번 (딸에게 돈 보내기)</span>에서 평균 63초가 걸리고 2번 도움이 필요하셨어요. 해당 단계 음성 안내를 더 자세하게 바꿔볼까요?
          </p>
        </div>

        <div className="bg-[#F0FDF4] border border-[#86EFAC] rounded-[18px] p-4 space-y-2">
          <p className="font-semibold text-[#15803D]" style="font-size: 16px;">
            ⭐ 제일 잘 하시는 업무
          </p>
          <p className="font-normal text-[#374151] leading-relaxed" style="font-size: 16px;">
            <span className="font-semibold text-[#111827]">4번 (내 잔액 확인하기)</span>은 이번 달 28번이나 하셨네요! 이제 완전히 익숙하신 것 같아요 😊
          </p>
        </div>
      </Card>
    </div>
    <NavBar active="analysis" :onSelect="store.navTo" />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import Card from '../components/common/Card.vue';
import NavBar from '../components/common/NavBar.vue';

const store = useAppStore();


const rawUsage = [
  { id: "p4", count: 4 }, { id: "p1", count: 3 }, { id: "p2", count: 2 },
  { id: "p3", count: 2 }, { id: "p5", count: 1 }
];

const usageData = computed(() => {
  return rawUsage.map((u) => {
    const pat = store.patterns.find((p) => p.id === u.id);
    return { ...u, label: pat?.label || u.id, color: pat?.color || "#9CA3AF", num: pat?.num };
  });
});

const maxCount = computed(() => Math.max(...usageData.value.map((u) => u.count)));

const minsu = computed(() => store.people[0]);
const minsuEmoji = computed(() => minsu.value?.emoji || "👨");
const minsuName = computed(() => minsu.value?.name || "민수");
const minsuRelation = computed(() => minsu.value?.relation || "아들");
</script>
