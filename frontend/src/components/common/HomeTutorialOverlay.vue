<template>
  <div>
    <!-- 홈 화면을 어둡게 만드는 배경 -->
    <div class="absolute inset-0 z-40 bg-black/55" aria-hidden="true" />

    <!-- 설명 상자 -->
    <section
      ref="guideBox"
      class="fixed z-[70] rounded-[18px] border-[3px] border-[#FFBC00] bg-white px-5 py-4 shadow-[0_12px_30px_rgba(0,0,0,0.25)]"
      :style="guideStyle"
      role="dialog"
      aria-modal="true"
      aria-labelledby="home-tutorial-description"
    >
      <!-- 설명 상자의 화살표 -->
      <div
        v-if="resolvedTarget"
        class="absolute h-4 w-4 rotate-45 border-[#FFBC00] bg-white"
        :class="
          placement === 'below'
            ? '-top-[10px] border-l-[3px] border-t-[3px]'
            : '-bottom-[10px] border-b-[3px] border-r-[3px]'
        "
        :style="{ left: `${arrowLeft}px` }"
        aria-hidden="true"
      />

      <p
        id="home-tutorial-description"
        class="break-keep text-[17px] font-bold leading-relaxed text-[#111827]"
      >
        {{ currentStep.description }}
      </p>

      <div class="mt-4 flex items-center justify-between gap-4">
        <p class="text-[14px] font-semibold text-[#9CA3AF]">
          {{ stepIndex + 1 }} / {{ steps.length }}
        </p>

        <button
          type="button"
          class="min-h-12 rounded-[14px] bg-[#FFBC00] px-5 text-[16px] font-bold text-[#111827] transition active:scale-[0.97]"
          @click="$emit('next')"
        >
          {{ isLastStep ? "학습 완료" : "다음" }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";

const props = defineProps({
  stepIndex: {
    type: Number,
    required: true,
  },

  target: {
    type: Object,
    default: null,
  },
});

defineEmits(["next"]);

const steps = [
  {
    description:
      "말로 선택하기를 누르면 원하는 금융 업무를 목소리로 찾을 수 있어요.",
  },
  {
    description:
      "단축번호 카드를 꾹 누르면 번호의 위치와 순서를 바꿀 수 있어요.",
  },
  {
    description:
      "단짝이 버튼을 누르면 궁금한 금융 업무를 채팅으로 물어볼 수 있어요.",
  },
];

const guideBox = ref(null);
const resolvedTarget = ref(null);
const placement = ref("below");
const arrowLeft = ref(28);

const guideStyle = ref({
  visibility: "hidden",
});

let chatButtonObserver = null;
let highlightedChatButton = null;
let highlightedChatLayer = null;

const currentStep = computed(() => {
  return steps[props.stepIndex] ?? steps[0];
});

const isLastStep = computed(() => {
  return props.stepIndex === steps.length - 1;
});

/*
 * App.vue에 있는 기존 챗봇 버튼의 하이라이트를 제거합니다.
 */
function clearChatHighlight() {
  if (highlightedChatButton) {
    highlightedChatButton.classList.remove("guide-glow");
    highlightedChatButton.style.pointerEvents = "";
  }

  if (highlightedChatLayer) {
    highlightedChatLayer.style.zIndex = "20";
  }

  highlightedChatButton = null;
  highlightedChatLayer = null;
}

/*
 * 3단계에서는 HomeView의 target이 없더라도
 * App.vue의 기존 .chat-fab 버튼을 직접 찾습니다.
 */
function findChatButton() {
  const chatButton = document.querySelector(".chat-fab");

  if (!chatButton) {
    return null;
  }

  const chatLayer = chatButton.parentElement;

  clearChatHighlight();

  highlightedChatButton = chatButton;
  highlightedChatLayer = chatLayer;

  chatButton.classList.add("guide-glow");
  chatButton.style.pointerEvents = "none";

  if (chatLayer) {
    chatLayer.style.zIndex = "60";
  }

  return chatButton;
}

/*
 * 현재 단계의 실제 하이라이트 대상을 결정합니다.
 */
function resolveCurrentTarget() {
  clearChatHighlight();

  if (props.stepIndex === 2) {
    resolvedTarget.value = props.target ?? findChatButton();

    return;
  }

  resolvedTarget.value = props.target;
}

/*
 * 챗봇 버튼은 App.vue에서 스플래시 종료 후 만들어질 수 있으므로
 * 버튼이 늦게 생성되는 경우를 감시합니다.
 */
function startWatchingChatButton() {
  chatButtonObserver?.disconnect();
  chatButtonObserver = null;

  if (props.stepIndex !== 2 || resolvedTarget.value) {
    return;
  }

  chatButtonObserver = new MutationObserver(() => {
    const chatButton = findChatButton();

    if (!chatButton) {
      return;
    }

    resolvedTarget.value = chatButton;

    chatButtonObserver?.disconnect();
    chatButtonObserver = null;

    updatePosition();
  });

  chatButtonObserver.observe(document.body, {
    childList: true,
    subtree: true,
  });
}

async function updatePosition() {
  await nextTick();

  /*
   * 전달받은 대상이 없으면 3단계에서 챗봇 버튼을 다시 찾습니다.
   */
  if (!resolvedTarget.value && props.stepIndex === 2) {
    resolvedTarget.value = findChatButton();
  }

  const targetElement = resolvedTarget.value;

  /*
   * 대상을 아직 못 찾은 짧은 순간에도 설명창 자체는
   * 아이폰 화면 가운데 표시되도록 처리합니다.
   */
  const appShell = document.querySelector(".app-shell");

  const boundaryRect = appShell?.getBoundingClientRect() ?? {
    left: 0,
    top: 0,
    right: window.innerWidth,
    bottom: window.innerHeight,
    width: window.innerWidth,
    height: window.innerHeight,
  };

  const screenPadding = 16;
  const gap = 14;

  const availableWidth = Math.max(boundaryRect.width - screenPadding * 2, 0);

  const fallbackWidth = Math.min(360, availableWidth);

  if (!targetElement) {
    const fallbackLeft =
      boundaryRect.left +
      Math.max((boundaryRect.width - fallbackWidth) / 2, screenPadding);

    guideStyle.value = {
      visibility: "visible",
      left: `${fallbackLeft}px`,
      top: `${boundaryRect.top + boundaryRect.height / 2 - 90}px`,
      width: `${fallbackWidth}px`,
      maxWidth: `${availableWidth}px`,
    };

    placement.value = "below";
    arrowLeft.value = 28;

    startWatchingChatButton();

    return;
  }

  const targetRect = targetElement.getBoundingClientRect();

  /*
   * 설명창의 너비를 아이폰 화면 안으로 제한합니다.
   */
  const width = Math.min(Math.max(targetRect.width, 300), availableWidth);

  const minimumLeft = boundaryRect.left + screenPadding;
  const maximumLeft = boundaryRect.right - screenPadding - width;

  /*
   * 3단계는 오른쪽 아래의 챗봇 버튼을 가리키므로
   * 설명창을 아이폰 화면 왼쪽에 맞춥니다.
   */
  const left =
    props.stepIndex === 2
      ? minimumLeft
      : Math.min(Math.max(targetRect.left, minimumLeft), maximumLeft);

  const guideHeight = guideBox.value?.getBoundingClientRect().height ?? 150;

  const minimumTop = boundaryRect.top + screenPadding;
  const maximumTop = boundaryRect.bottom - screenPadding - guideHeight;

  const hasSpaceBelow =
    targetRect.bottom + gap + guideHeight <=
    boundaryRect.bottom - screenPadding;

  placement.value = hasSpaceBelow ? "below" : "above";

  const calculatedTop = hasSpaceBelow
    ? targetRect.bottom + gap
    : targetRect.top - guideHeight - gap;

  const top = Math.min(
    Math.max(calculatedTop, minimumTop),
    Math.max(minimumTop, maximumTop),
  );

  guideStyle.value = {
    visibility: "visible",
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    maxWidth: `${availableWidth}px`,
  };

  /*
   * 화살표가 하이라이트 대상의 중앙을 가리키게 합니다.
   */
  const targetCenter = targetRect.left + targetRect.width / 2;

  arrowLeft.value = Math.min(
    Math.max(targetCenter - left - 8, 24),
    Math.max(width - 40, 24),
  );
}

watch(
  () => [props.stepIndex, props.target],
  async () => {
    chatButtonObserver?.disconnect();
    chatButtonObserver = null;

    resolveCurrentTarget();
    await updatePosition();

    if (props.stepIndex === 2 && !resolvedTarget.value) {
      startWatchingChatButton();
    }
  },
  {
    immediate: true,
  },
);

onMounted(async () => {
  resolveCurrentTarget();
  await updatePosition();

  if (props.stepIndex === 2 && !resolvedTarget.value) {
    startWatchingChatButton();
  }

  window.addEventListener("resize", updatePosition);
  window.addEventListener("scroll", updatePosition, true);
});

onBeforeUnmount(() => {
  chatButtonObserver?.disconnect();
  chatButtonObserver = null;

  clearChatHighlight();

  window.removeEventListener("resize", updatePosition);
  window.removeEventListener("scroll", updatePosition, true);
});
</script>
