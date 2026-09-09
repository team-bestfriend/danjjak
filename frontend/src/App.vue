<template>
  <div
    class="fixed inset-0 flex items-start justify-center overflow-hidden bg-[#E5E7EB]"
  >
    <div
      class="app-shell relative flex flex-shrink-0 flex-col overflow-hidden bg-white"
      :style="appShellStyle"
    >
      <SplashScreen v-if="showSplash" @finished="showSplash = false" />

      <div
        ref="routeArea"
        :class="{ 'chat-entry': showChatFab(String(route.name)) }"
        class="min-h-0 flex-1 overflow-hidden"
        @click.capture="handleGuidanceClick"
      >
        <RouterView />
      </div>

      <div
        v-if="showChatFab(String(route.name)) && !showSplash"
        class="absolute right-5 h-16 w-16"
        style="bottom: 90px; z-index: 20;"
      >
        <Transition name="chat-hint">
          <div
            v-if="showChatHint"
            class="chat-hint absolute right-[84px] top-1/2 w-max -translate-y-1/2 whitespace-nowrap rounded-[18px] border-2 border-[#F1C232] bg-[#FFFDF5] px-3 py-2.5 text-[15px] font-semibold leading-none text-[#4B3A08] shadow-lg"
            role="status"
          >
            <span class="chat-hint-full">궁금한 게 있으면 저한테 물어보세요!</span>
            <span class="chat-hint-compact">단짝이에게 물어보세요!</span>
          </div>
        </Transition>

        <button
          class="chat-fab flex h-16 w-16 items-center justify-center rounded-full bg-[#FFCA3A] text-[#111827] shadow-lg"
          aria-label="단짝에게 물어보기"
          @mouseenter="fabHovered = true"
          @mouseleave="fabHovered = false"
          @focus="fabFocused = true"
          @blur="fabFocused = false"
          @click="openChat"
        >
          <img
            :src="chatIcon"
            alt=""
            class="h-[52px] w-[52px] rounded-full object-contain"
          />
        </button>
      </div>

      <VoiceGuideBar
        v-if="voiceText"
        ref="voiceGuideBar"
        :key="`${route.name}:${activeStep?.stepId ?? ''}`"
        :text="voiceText"
        :speed="voiceSpeed"
        :voice-mode="voiceMode"
        :family-audio-url="activeStep?.voiceFilePath ?? ''"
        :notice="guidanceNotice"
        :guided="Boolean(activeStep)"
      />

      <Toast
        v-if="store.toast"
        :key="store.toast.key"
        :message="store.toast.msg"
        :action="store.toast.action"
        :on-action="store.toast.cb"
        @done="store.clearToast"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterView, useRoute, useRouter } from "vue-router";
import Toast from "./components/common/Toast.vue";
import VoiceGuideBar from "./components/common/VoiceGuideBar.vue";
import { useAppStore } from "./stores/appStore";
import {
  stepInstruction,
  useStepGuidance,
} from "./composables/useStepGuidance.js";
import { showChatFab } from './features/chat/chatActions.js';
import chatIcon from './assets/danjjakee.png';
import SplashScreen from "./components/common/SplashScreen.vue";
import { RESULT_INQUIRY_CATEGORIES } from "./features/inquiry/resultGuidance.js";

const route = useRoute();
const router = useRouter();
const store = useAppStore();
const routeArea = ref(null);
const voiceGuideBar = ref(null);
const showSplash = ref(true);
const autoChatHintVisible = ref(false);
const fabHovered = ref(false);
const fabFocused = ref(false);
const showChatHint = computed(
  () => autoChatHintVisible.value || fabHovered.value || fabFocused.value,
);
const CHAT_HINT_SESSION_KEY = "danjjak-chat-hint-shown";
let chatHintTimer;
let chatHintShown = false;

const VOICE_TEXTS = {
  "transfer-source": "송금할 본인 계좌를 선택해 주세요.",
  "direct-transfer":
    "누구에게 보내실지 선택해 주세요. 등록된 가족에게 보내기를 눌러보세요.",
  "direct-newaccount":
    "보낼 계좌 정보를 입력해 주세요. 은행을 먼저 선택하고 계좌 번호를 입력하세요.",
  "guide-person": "보낼 사람 이름을 눌러 주세요.",
  "guide-account": "보낼 계좌를 눌러 선택해 주세요.",
  "amount-input": "보내실 금액을 입력해 주세요.",
  "pin-entry": "계좌 비밀번호를 입력해주세요.",
  "fraud-warning": "누군가 돈을 보내라고 했나요? 전화나 문자로 돈을 보내라고 하거나, 빨리 보내라고 재촉했다면 사기일 수 있어요. 잠깐 멈추고 가족과 함께 확인해 주세요.",
  "final-confirm": "출금 계좌와 받는 분, 금액이 맞는지 확인해 주세요.",
  complete: "송금이 모두 완료됐어요. 정말 잘 하셨어요!",
  cancelled: "송금을 취소했어요. 잔액과 거래 내역은 바뀌지 않았어요.",
  "task-transfer":
    "아들 김민수님에게 송금하는 업무입니다. 시작하려면 시작하기를 눌러주세요.",
  "task-4": "내 계좌 잔액을 확인하는 화면이에요. 잔액 보기를 눌러 확인하세요.",
  "task-5": "거래 내역 화면이에요. 입금, 출금을 선택해서 확인할 수 있어요.",
  "task-6": "고객센터 화면이에요. 도움이 필요하면 전화 연결하기를 눌러 주세요.",
  "task-9": "자동이체 내역이에요. 매달 나가는 금액을 확인해 보세요.",
  "task-10": "카드 이용 내역이에요. 이번 달 쓴 금액을 확인해 보세요.",
  "task-11": "예금 만기 일정이에요. 만기일을 꼭 확인해 두세요.",
  "task-12": "오늘의 환율 정보예요. 천천히 살펴보세요.",
};

const activeStep = computed(
  () =>
    store.activePatternDetail?.steps?.find(
      (step) => step.screenCode === String(route.name),
    ) ?? null,
);

const voiceText = computed(() => {
  // 거래 결과 안내는 조회 화면에서 데이터가 준비된 뒤 TTS로 재생한다.
  if (RESULT_INQUIRY_CATEGORIES[String(route.name)]) return null;
  return activeStep.value
    ? stepInstruction(activeStep.value)
    : (VOICE_TEXTS[String(route.name)] ?? null);
});

const guidanceReady = computed(
  () => !store.financeLoading && !store.inquiryLoading && !store.supportLoading,
);

const { notice: guidanceNotice, handleClick: handleGuidanceClick } =
  useStepGuidance(routeArea, activeStep, guidanceReady, () => {
    void voiceGuideBar.value?.replayWhenIdle();
    if (store.currentStepVisit?.stepId === activeStep.value?.stepId) {
      store.recordPatternAction("wrongTouch");
    }
  });

const voiceSpeed = computed(
  () => store.currentUser?.settings?.voiceSpeed ?? "NORMAL",
);

const voiceMode = computed(
  () => store.currentUser?.settings?.guideVoiceType ?? "TTS",
);

/*
 * 화면 전체를 확대·축소하지 않고 글씨 크기에만 적용합니다.
 * 이렇게 해야 LARGE 설정에서도 내부 레이아웃 폭이 좁아지지 않습니다.
 */
const textScale = computed(
  () =>
    ({
      SMALL: 0.94,
      NORMAL: 1,
      LARGE: 1.08,
    })[store.currentUser?.settings?.fontSize ?? "NORMAL"] ?? 1,
);

/*
 * iPhone 12 Pro Max의 CSS viewport 폭인 428px을 기준으로 합니다.
 * 428px보다 작은 기기에서는 화면 폭에 맞춰 자동으로 줄어듭니다.
 */
const appShellStyle = computed(() => ({
  width: "100%",
  maxWidth: "428px",
  height: "100dvh",
  "--text-scale": textScale.value,
}));

watch(
  () => route.name,
  (routeName) => {
    void store.syncPatternStep(String(routeName));
  },
  { immediate: true },
);

watch(
  () => store.currentUser?.settings?.fontSize,
  (fontSize) => {
    if (typeof document === "undefined") return;

    document.documentElement.dataset.fontSize = String(
      fontSize ?? "NORMAL",
    ).toLowerCase();
  },
  { immediate: true },
);

watch(
  [showSplash, () => route.name],
  ([splashVisible, routeName]) => {
    if (splashVisible || !showChatFab(String(routeName))) return;
    showMobileChatHintOnce();
  },
  { immediate: true },
);

function showMobileChatHintOnce() {
  if (typeof window === "undefined" || window.matchMedia("(hover: hover)").matches) return;
  if (chatHintShown) return;
  try {
    if (window.sessionStorage.getItem(CHAT_HINT_SESSION_KEY)) {
      chatHintShown = true;
      return;
    }
    window.sessionStorage.setItem(CHAT_HINT_SESSION_KEY, "true");
  } catch {
    // 저장소를 사용할 수 없어도 현재 앱 실행 중에는 한 번만 보여준다.
  }
  chatHintShown = true;
  autoChatHintVisible.value = true;
  chatHintTimer = window.setTimeout(() => {
    autoChatHintVisible.value = false;
    chatHintTimer = undefined;
  }, 3500);
}

function openChat() {
  autoChatHintVisible.value = false;
  void router.push({ name: 'chat' });
}

function handleSessionExpired() {
  store.clearSession("로그인이 만료되었습니다. 다시 로그인해 주세요.");

  if (route.name !== "login") {
    void router.replace({ name: "login" });
  }
}

onMounted(() => {
  window.addEventListener("danjjak:session-expired", handleSessionExpired);
});

onBeforeUnmount(() => {
  window.removeEventListener("danjjak:session-expired", handleSessionExpired);
  window.clearTimeout(chatHintTimer);
});
</script>

<style>
/* 마지막 카드도 도우미 버튼 위로 올려서 누를 수 있게 여백을 둔다. */
.chat-entry .overflow-y-auto {
  padding-bottom: 96px !important;
}

.chat-fab {
  animation:
    chat-fab-arrive 0.8s ease-out both,
    chat-fab-float 4.5s ease-in-out 1s infinite;
}

.chat-hint::after {
  position: absolute;
  right: -8px;
  top: 50%;
  width: 14px;
  height: 14px;
  border-top: 2px solid #f1c232;
  border-right: 2px solid #f1c232;
  background: #fffdf5;
  content: "";
  transform: translateY(-50%) rotate(45deg);
}

.chat-hint-compact {
  display: none;
}

.chat-hint-enter-active,
.chat-hint-leave-active {
  transition:
    opacity 0.45s ease,
    transform 0.45s ease;
}

.chat-hint-enter-from,
.chat-hint-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

@media (max-width: 390px) {
  .chat-hint-full {
    display: none;
  }

  .chat-hint-compact {
    display: inline;
  }
}

@keyframes chat-fab-arrive {
  from { opacity: 0; transform: scale(0.82); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes chat-fab-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

@media (prefers-reduced-motion: reduce) {
  .chat-fab {
    animation: none;
  }

  .chat-hint-enter-active,
  .chat-hint-leave-active {
    transition: none;
  }
}

.vbar-enter-active {
  transition:
    opacity 0.3s ease,
    transform 0.3s ease;
}

.vbar-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.vbar-enter-from {
  opacity: 0;
  transform: translateY(16px);
}

.vbar-leave-to {
  opacity: 0;
  transform: translateY(16px);
}
</style>
