<template>
  <!-- ① SPLASH -->
  <div v-if="step === 'splash'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div className="flex-1 flex flex-col items-center justify-center px-8 gap-6">
      <div className="w-24 h-24 rounded-[24px] flex items-center justify-center shadow-lg" style="background: #FFBC00;">
        <DanjjakMark :size="52" />
      </div>
      <div className="text-center space-y-2">
        <p className="font-bold text-[#111827]" style="font-size: 38px;">단짝</p>
        <p className="font-normal text-[#6B7280]" style="font-size: 17px;">어르신을 위한 쉬운 금융 도우미</p>
      </div>
      <div className="bg-[#FFF3CC] border border-[#FFBC00] rounded-[18px] px-5 py-3 text-center">
        <p className="font-normal text-[#92650A]" style="font-size: 15px;">KB국민은행과 함께하는<br />안심 금융 서비스</p>
      </div>
    </div>
    <div className="px-6 pb-12 space-y-3">
      <button
        @click="step = 'intro'; introPg = 0"
        className="w-full rounded-[18px] bg-[#FFBC00] text-[#111827] font-semibold flex items-center justify-center gap-2"
        style="height: 66px; font-size: 20px;"
      >
        처음 시작하기
      </button>
      <button
        @click="store.navigate('home')"
        className="w-full rounded-[18px] bg-white border border-[#D1D5DB] text-[#374151] font-normal"
        style="height: 60px; font-size: 18px;"
      >
        다시 이용하기
      </button>
    </div>
  </div>

  <!-- ② INTRO 슬라이드 -->
  <div v-else-if="step === 'intro'" className="flex flex-col h-full bg-white">
    <SafeArea />
    <div className="flex justify-between items-center px-5 pt-3 flex-shrink-0">
      <div className="flex items-center gap-2">
        <DanjjakMark :size="24" />
        <span className="font-semibold text-[#111827]" style="font-size: 15px;">단짝</span>
      </div>
      <button @click="step = 'terms'" className="font-normal text-[#9CA3AF] px-2 py-1" style="font-size: 15px;">건너뛰기</button>
    </div>

    <div className="flex-1 flex flex-col items-center justify-center px-8 gap-8">
      <!-- 슬라이드별 비주얼 -->
      <div v-if="introPg === 0" className="flex gap-3 justify-center">
        <div
          v-for="s in [{n:'1',c:'#FF5E5E'},{n:'2',c:'#FF9943'},{n:'3',c:'#22C55E'}]"
          :key="s.n"
          className="w-[88px] h-[88px] rounded-[22px] flex items-center justify-center shadow-md"
          :style="{ background: s.c }"
        >
          <span className="font-bold text-white" style="font-size: 38px;">{{ s.n }}</span>
        </div>
      </div>

      <div v-else-if="introPg === 1" className="relative w-56 h-44 mx-auto">
        <div className="absolute inset-0 rounded-[20px] bg-white border border-[#EBEBEA] shadow-sm p-4 space-y-3">
          <div className="h-9 bg-[#F3F4F6] rounded-xl" />
          <div className="h-9 bg-[#F3F4F6] rounded-xl" />
          <div className="h-10 rounded-xl border-2 border-[#FFBC00] bg-[#FFFBEB] flex items-center px-3 gap-2" style="animation: guide-ring 1.5s ease-in-out infinite;">
            <div className="w-5 h-5 rounded-full bg-[#FFBC00] flex items-center justify-center flex-shrink-0" style="font-size: 10px;">✓</div>
            <span className="font-semibold text-[#111827]" style="font-size: 13px;">👨 김민수 (아들)</span>
          </div>
        </div>
      </div>

      <div v-else-if="introPg === 2" className="w-32 h-32 mx-auto rounded-full bg-[#EFF6FF] border-4 border-[#BFDBFE] flex items-center justify-center" style="font-size: 58px;">
        🎙️
      </div>

      <div v-else-if="introPg === 3" className="relative">
        <div className="w-32 h-32 mx-auto rounded-full bg-[#FFF0F0] border-4 border-[#FECACA] flex items-center justify-center" style="font-size: 56px;">
          🥷
        </div>
        <div className="absolute bottom-0 right-0 w-12 h-12 rounded-full bg-[#22C55E] border-2 border-white flex items-center justify-center" style="font-size: 22px;">🛡️</div>
      </div>

      <div className="text-center space-y-3">
        <h1 className="font-bold text-[#111827] whitespace-pre-line leading-snug" style="font-size: 28px;">{{ INTRO[introPg].title }}</h1>
        <p className="font-normal text-[#6B7280] whitespace-pre-line leading-relaxed" style="font-size: 16px;">{{ INTRO[introPg].desc }}</p>
      </div>
    </div>

    <div className="px-6 pb-12 flex-shrink-0 space-y-5">
      <div className="flex justify-center gap-2.5">
        <div
          v-for="(_, i) in INTRO"
          :key="i"
          className="transition-all"
          :style="i === introPg
            ? 'width:28px;height:10px;border-radius:5px;background:#FFBC00;'
            : 'width:10px;height:10px;border-radius:50%;background:#E5E7EB;'"
        />
      </div>
      <button
        v-if="introPg < 3"
        @click="introPg++"
        className="w-full rounded-[18px] bg-[#FFBC00] text-[#111827] font-semibold"
        style="height: 64px; font-size: 19px;"
      >
        다음
      </button>
      <button
        v-else
        @click="step = 'terms'"
        className="w-full rounded-[18px] bg-[#FFBC00] text-[#111827] font-semibold"
        style="height: 64px; font-size: 19px;"
      >
        시작하기
      </button>
    </div>
  </div>

  <!-- ③ 약관 동의 -->
  <div v-else-if="step === 'terms'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div className="bg-white px-5 border-b border-[#EEEEED] flex items-center justify-between flex-shrink-0" style="min-height: 56px;">
      <button @click="step = 'intro'" className="w-10 h-10 flex items-center justify-center text-[#374151]" style="font-size: 22px;">←</button>
      <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-full border border-[#D1D5DB] text-[#374151] font-normal" style="font-size: 14px;">
        🔊 음성으로 듣기
      </button>
    </div>

    <div className="flex-1 overflow-y-auto px-5 pt-7 pb-4 space-y-6">
      <h1 className="font-bold text-[#111827] leading-snug" style="font-size: 26px;">서비스 이용을 위해<br />동의해주세요.</h1>

      <!-- 전체 동의 -->
      <button
        @click="toggleAll"
        className="w-full rounded-[18px] border-2 flex items-center gap-4 px-5 transition-all"
        style="min-height: 66px;"
        :style="allChecked ? 'border-color:#FFBC00; background:#FFFBEB;' : 'border-color:#E5E7EB; background:white;'"
      >
        <div
          className="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 transition-all"
          :style="allChecked ? 'background:#FFBC00;' : 'border: 2px solid #D1D5DB; background: white;'"
        >
          <span v-if="allChecked" style="color: #111827; font-size: 15px; font-weight: 700;">✓</span>
        </div>
        <span className="font-semibold text-[#111827]" style="font-size: 18px;">전체 동의 (필수)</span>
      </button>

      <!-- 개별 약관 -->
      <div className="bg-white rounded-[18px] border border-[#E5E7EB] overflow-hidden">
        <div
          v-for="(t, i) in terms"
          :key="i"
          :class="['flex items-center justify-between px-5 py-4', i < terms.length - 1 ? 'border-b border-[#F3F4F6]' : '']"
        >
          <div className="flex items-center gap-3">
            <button
              @click="t.checked = !t.checked"
              className="w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 transition-all"
              :style="t.checked ? 'background:#FFBC00;' : 'border: 2px solid #D1D5DB; background: white;'"
            >
              <span v-if="t.checked" style="font-size: 12px; color: #111827; font-weight: 700;">✓</span>
            </button>
            <span className="font-normal text-[#374151]" style="font-size: 16px;">{{ t.label }}</span>
          </div>
          <button className="font-normal text-[#9CA3AF] px-2 py-1" style="font-size: 15px;">자세히</button>
        </div>
      </div>
    </div>

    <div className="px-5 pb-8 pt-4 bg-white border-t border-[#F3F4F6] flex-shrink-0">
      <button
        @click="step = 'kakao'"
        :disabled="!allChecked"
        className="w-full rounded-[18px] font-semibold transition-all"
        :style="allChecked ? 'height:64px;font-size:19px;background:#FFBC00;color:#111827;' : 'height:64px;font-size:19px;background:#F3F4F6;color:#9CA3AF;'"
      >
        다음
      </button>
    </div>
  </div>

  <!-- ④ 카카오 로그인 -->
  <div v-else-if="step === 'kakao'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div className="bg-white px-5 border-b border-[#EEEEED] flex items-center flex-shrink-0" style="min-height: 56px;">
      <button @click="step = 'terms'" className="w-10 h-10 flex items-center justify-center text-[#374151]" style="font-size: 22px;">←</button>
    </div>

    <div className="flex-1 flex flex-col items-center justify-center px-6 gap-8">
      <div className="text-center space-y-4">
        <div className="w-24 h-24 rounded-full bg-[#FFF3CC] border-4 border-[#FFBC00] flex items-center justify-center mx-auto" style="font-size: 48px;">👵</div>
        <h1 className="font-bold text-[#111827]" style="font-size: 26px;">로그인 방법을<br />선택해주세요.</h1>
        <p className="font-normal text-[#6B7280]" style="font-size: 16px;">간편하게 단짝을 시작하세요.</p>
      </div>

      <button
        @click="step = 'name'"
        className="w-full rounded-[18px] flex items-center justify-center gap-3 font-semibold text-[#111827]"
        style="height: 64px; font-size: 19px; background: #FEE500;"
      >
        <span style="font-size: 24px;">💬</span>
        카카오톡으로 시작하기
      </button>
    </div>
  </div>

  <!-- ⑦ 이름 확인 -->
  <div v-else-if="step === 'name'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div className="bg-white px-5 border-b border-[#EEEEED] flex items-center flex-shrink-0" style="min-height: 56px;">
      <button @click="step = 'kakao'" className="w-10 h-10 flex items-center justify-center text-[#374151]" style="font-size: 22px;">←</button>
    </div>

    <div className="flex-1 flex flex-col px-6 pt-8 pb-6">
      <div className="flex-1 flex flex-col gap-6">
        <!-- 어르신 아바타 -->
        <div className="w-24 h-24 rounded-full bg-[#FFF3CC] border-4 border-[#FFBC00] flex items-center justify-center" style="font-size: 46px;">👵</div>

        <div className="space-y-2">
          <h1 className="font-bold text-[#111827] leading-snug" style="font-size: 26px;">
            어떻게 불러드릴까요? 😊
          </h1>
          <p className="font-normal text-[#6B7280]" style="font-size: 16px;">편하게 부르실 이름을 입력해주세요.</p>
        </div>

        <!-- 이름 표시 / 입력 -->
        <div v-if="!editingName">
          <div className="rounded-[18px] border-2 border-[#FFBC00] bg-[#FFFBEB] px-5 flex items-center" style="height: 66px;">
            <span className="font-bold text-[#111827]" style="font-size: 22px;">{{ userName }}님 😊</span>
          </div>
        </div>
        <div v-else>
          <input
            v-model="userName"
            placeholder="이름을 입력해주세요"
            className="w-full rounded-[18px] border-2 border-[#FFBC00] outline-none px-5 font-bold text-[#111827] placeholder:text-[#D1D5DB] placeholder:font-normal"
            style="height: 66px; font-size: 20px;"
          />
        </div>
      </div>

      <div className="space-y-3">
        <button
          @click="editingName = !editingName"
          className="w-full rounded-[18px] bg-white border border-[#D1D5DB] text-[#374151] font-normal"
          style="height: 58px; font-size: 18px;"
        >
          다른 이름 입력
        </button>
        <button
          @click="store.userName = userName; step = 'guardian'"
          className="w-full rounded-[18px] bg-[#FFBC00] text-[#111827] font-semibold"
          style="height: 64px; font-size: 19px;"
        >
          좋아요
        </button>
      </div>
    </div>
  </div>

  <!-- ⑧ 가족 연결 / 보호자 등록 -->
  <div v-else-if="step === 'guardian'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div className="bg-white px-5 border-b border-[#EEEEED] flex items-center flex-shrink-0" style="min-height: 56px;">
      <button @click="step = 'name'" className="w-10 h-10 flex items-center justify-center text-[#374151]" style="font-size: 22px;">←</button>
    </div>

    <div className="flex-1 overflow-y-auto px-5 pt-6 pb-4 space-y-5">
      <!-- 도둑/보호 일러스트 -->
      <div className="relative flex items-center justify-center" style="height: 130px;">
        <div className="w-32 h-32 rounded-full bg-[#FEF2F2] border-4 border-[#FECACA] flex items-center justify-center" style="font-size: 60px;">
          🥷
        </div>
        <div className="absolute bottom-1 right-24 w-11 h-11 rounded-full bg-[#22C55E] border-2 border-white flex items-center justify-center shadow-md" style="font-size: 22px;">🛡️</div>
        <div className="absolute top-1 left-24 w-9 h-9 rounded-full bg-[#FFF3CC] border-2 border-[#FFBC00] flex items-center justify-center" style="font-size: 18px;">⚠️</div>
      </div>

      <div className="space-y-2">
        <h1 className="font-bold text-[#111827]" style="font-size: 24px;">가족과 함께<br />안전을 지켜요</h1>
        <p className="font-normal text-[#6B7280] leading-relaxed" style="font-size: 15px;">
          보이스피싱이 의심되는 거래가 감지되면 등록하신 보호자에게 즉시 문자를 보내드려요.
        </p>
      </div>

      <div className="bg-[#FFF7ED] border border-[#FED7AA] rounded-[16px] p-4 flex items-start gap-3">
        <span style="font-size: 20px;">💡</span>
        <p className="font-normal text-[#92400E]" style="font-size: 14px; line-height: 1.5;">
          보호자는 이상 거래 알림을 받고 언제든 전화로 도와줄 수 있어요.
        </p>
      </div>

      <div className="space-y-2">
        <p className="font-semibold text-[#111827] px-1" style="font-size: 17px;">보호자 전화번호</p>
        <input
          v-model="guardianPhone"
          type="tel"
          placeholder="010-0000-0000"
          inputMode="tel"
          @input="guardianPhone = fmtPhone($event.target.value)"
          className="w-full rounded-[16px] border-2 border-[#E5E7EB] focus:border-[#FFBC00] outline-none px-5 font-bold text-[#111827] placeholder:text-[#D1D5DB] placeholder:font-normal"
          style="height: 60px; font-size: 19px;"
        />
      </div>
    </div>

    <div className="px-5 pb-8 pt-3 bg-white border-t border-[#F3F4F6] flex-shrink-0 space-y-2">
      <button
        @click="step = 'complete'"
        className="w-full rounded-[18px] font-semibold transition-all"
        :style="guardianPhone.length >= 12
          ? 'height:64px;font-size:19px;background:#FFBC00;color:#111827;'
          : 'height:64px;font-size:19px;background:#F3F4F6;color:#9CA3AF;'"
      >
        보호자 전화번호 등록하기
      </button>
      <button @click="step = 'complete'" className="w-full py-3 font-normal text-[#9CA3AF]" style="font-size: 16px;">
        나중에 등록할게요
      </button>
    </div>
  </div>

  <!-- ⑨ 설정 완료 -->
  <div v-else-if="step === 'complete'" className="relative flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div className="flex-1 flex flex-col items-center justify-center px-8 gap-6">
      <div className="w-32 h-32 rounded-full flex items-center justify-center shadow-lg" style="background: #22C55E;">
        <span style="font-size: 56px; color: white;">✓</span>
      </div>
      <div className="text-center space-y-3">
        <p className="font-bold text-[#111827]" style="font-size: 28px;">모든 준비가<br />완료됐어요! 🎉</p>
        <p className="font-normal text-[#6B7280]" style="font-size: 17px;">{{ userName }}님, 지금 바로<br />단짝을 시작해 보세요.</p>
      </div>
      <!-- 완료 항목 -->
      <div className="w-full bg-white rounded-[18px] border border-[#E5E7EB] overflow-hidden">
        <div
          v-for="item in completeItems"
          :key="item.label"
          className="flex items-center gap-3 px-5 py-4 border-b border-[#F3F4F6] last:border-0"
        >
          <div className="w-7 h-7 rounded-full bg-[#DCFCE7] flex items-center justify-center flex-shrink-0" style="font-size: 14px;">✓</div>
          <span className="font-normal text-[#374151]" style="font-size: 16px;">{{ item.label }}</span>
        </div>
      </div>
    </div>

    <div className="px-6 pb-12 flex-shrink-0">
      <button
        @click="showVolumePopup = true"
        className="w-full rounded-[18px] bg-[#FFBC00] text-[#111827] font-semibold"
        style="height: 66px; font-size: 20px;"
      >
        서비스 시작하기
      </button>
    </div>

    <!-- 볼륨 안내 팝업 -->
    <div
      v-if="showVolumePopup"
      className="absolute inset-0 z-50 flex items-center justify-center px-6"
      style="background: rgba(0,0,0,0.55);"
    >
      <div className="bg-white rounded-[28px] w-full overflow-hidden" style="box-shadow: 0 20px 60px rgba(0,0,0,0.25);">
        <!-- 상단 강조 영역 -->
        <div className="flex flex-col items-center pt-8 pb-6 px-6" style="background: linear-gradient(160deg, #FFFBEB 0%, #FFF3CC 100%);">
          <div
            className="w-24 h-24 rounded-full flex items-center justify-center mb-4"
            style="background: #FFBC00; box-shadow: 0 0 0 8px rgba(255,188,0,0.2);"
          >
            <span style="font-size: 46px;">🔊</span>
          </div>
          <p className="font-bold text-[#111827] text-center" style="font-size: 22px; line-height: 1.4;">
            소리를 켜주세요!
          </p>
          <p className="font-normal text-[#92650A] text-center mt-1" style="font-size: 15px;">
            단짝은 음성 안내가 함께해요
          </p>
        </div>

        <!-- 안내 내용 -->
        <div className="px-6 py-5 space-y-3">
          <div className="flex items-center gap-3 bg-[#F9FAFB] rounded-[14px] p-4">
            <span style="font-size: 24px;">📱</span>
            <p className="font-normal text-[#374151]" style="font-size: 15px; line-height: 1.4;">
              휴대폰 옆면의 <span className="font-semibold text-[#111827]">볼륨 버튼</span>을 올려<br />소리를 크게 해주세요.
            </p>
          </div>
          <div className="flex items-center gap-3 bg-[#F9FAFB] rounded-[14px] p-4">
            <span style="font-size: 24px;">🔕</span>
            <p className="font-normal text-[#374151]" style="font-size: 15px; line-height: 1.4;">
              <span className="font-semibold text-[#111827]">무음 모드</span>가 켜져 있다면<br />해제해주세요.
            </p>
          </div>
        </div>

        <!-- 버튼 -->
        <div className="px-6 pb-7 space-y-2">
          <button
            @click="store.navigate('home')"
            className="w-full rounded-[18px] bg-[#FFBC00] text-[#111827] font-semibold"
            style="height: 62px; font-size: 19px;"
          >
            소리를 켰어요! 시작할게요 🎵
          </button>
          <button
            @click="store.navigate('home')"
            className="w-full py-3 font-normal text-[#9CA3AF]"
            style="font-size: 15px;"
          >
            소리 없이 시작할게요
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import DanjjakMark from '../components/common/DanjjakMark.vue';

const store = useAppStore();

const step = ref('splash');
const introPg = ref(0);
const showVolumePopup = ref(false);

const INTRO = [
  { title: '금융 업무,\n이제 번호 하나로!', desc: '자주 쓰는 금융 업무를\n단축번호로 쉽게 이용해요.' },
  { title: '반짝이는 곳만\n따라 눌러요', desc: '어디를 눌러야 할지\n화면이 직접 알려줘요.' },
  { title: '가족 목소리가\n함께 안내해요', desc: '가족이 등록한 안내를\n필요한 순간에 들을 수 있어요.' },
  { title: '이상한 거래는\n다시 확인해요', desc: '평소와 다른 거래를 발견하면\n안전하게 한 번 더 확인해요.' },
];

// 약관
const terms = ref([
  { label: '서비스 이용약관 (필수)', checked: false },
  { label: '개인정보 수집 및 이용 (필수)', checked: false },
  { label: '고유식별정보 처리 (필수)', checked: false },
]);
const allChecked = computed(() => terms.value.every(t => t.checked));
function toggleAll() {
  const next = !allChecked.value;
  terms.value.forEach(t => { t.checked = next; });
}

// 인증 코드
const codeDigits = ref([]);
const countdown = ref(180);
let countdownTimer = null;
const countdownStr = computed(() => {
  const m = Math.floor(countdown.value / 60);
  const s = countdown.value % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
});

function startCountdown() {
  if (countdownTimer) clearInterval(countdownTimer);
  countdown.value = 180;
  codeDigits.value = [];
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--;
    } else {
      clearInterval(countdownTimer);
    }
  }, 1000);
}

function pressKey(k) {
  if (k === 'del') {
    codeDigits.value = codeDigits.value.slice(0, -1);
  } else if (codeDigits.value.length < 6) {
    codeDigits.value = [...codeDigits.value, k];
    if (codeDigits.value.length === 6) {
      setTimeout(() => { step.value = 'kakao'; }, 350);
    }
  }
}

// 이름
const userName = ref('순자');
const editingName = ref(false);

// 보호자
const guardianPhone = ref('');
function fmtPhone(v) {
  const d = v.replace(/\D/g, '').slice(0, 11);
  if (d.length <= 3) return d;
  if (d.length <= 7) return `${d.slice(0, 3)}-${d.slice(3)}`;
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`;
}

// 완료 항목
const completeItems = [
  { label: '약관 동의 완료' },
  { label: '본인 인증 완료' },
  { label: '카카오 로그인 연결' },
  { label: '이름 설정 완료' },
];

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer);
});
</script>
