<template>
  <div
    class="danjjak-assistant"
    :class="{ 'assistant-guiding': showVoiceMessage }"
  >
    <div
      class="voice-command-bubble"
      :class="{ 'voice-command-bubble-visible': showVoiceMessage }"
    >
      <button
        v-if="showVoiceMessage"
        type="button"
        class="voice-command-close"
        aria-label="음성 안내 닫기"
        @click="dismissVoiceMessage"
      >
        <span aria-hidden="true">
          <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
            <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </span>
      </button>
      <p
        id="voice-command-status"
        class="voice-command-text"
        :tabindex="showVoiceMessage ? 0 : undefined"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        {{ showVoiceMessage ? voiceMessage : "" }}
      </p>
    </div>

    <Transition v-if="!voiceMessage" name="chat-hint">
      <div
        v-if="autoHint || hovered || focused"
        class="chat-hint absolute right-[84px] top-1/2 w-max -translate-y-1/2 whitespace-nowrap rounded-[18px] border-2 border-[#F1C232] bg-[#FFFDF5] px-3 py-2.5 text-[15px] font-semibold leading-none text-[#4B3A08] shadow-lg"
        role="status"
      >
        <span class="chat-hint-full">궁금한 게 있으면 저한테 물어보세요!</span>
        <span class="chat-hint-compact">단짝이에게 물어보세요!</span>
      </div>
    </Transition>

    <button
      ref="chatButton"
      class="chat-fab flex h-16 w-16 items-center justify-center rounded-full bg-[#FFCA3A] text-[#111827] shadow-lg"
      aria-label="단짝에게 물어보기"
      @mouseenter="hovered = true"
      @mouseleave="hovered = false"
      @focus="focused = true"
      @blur="focused = false"
      @click="$emit('open-chat')"
    >
      <img :src="chatIcon" alt="" class="h-[52px] w-[52px] rounded-full object-contain" />
    </button>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import chatIcon from "../../assets/danjjakee.png";

const props = defineProps({
  voiceMessage: { type: String, default: "" },
  autoHint: { type: Boolean, default: false },
});
defineEmits(["open-chat"]);

const hovered = ref(false);
const focused = ref(false);
const chatButton = ref(null);
const voiceMessageDismissed = ref(false);
const showVoiceMessage = computed(() => props.voiceMessage && !voiceMessageDismissed.value);

watch(() => props.voiceMessage, () => { voiceMessageDismissed.value = false; });

function dismissVoiceMessage() {
  // 안내만 닫고, 진행 중인 음성 인식과 다음 상태 안내는 유지한다.
  voiceMessageDismissed.value = true;
  chatButton.value?.focus();
}
</script>

<style scoped>
.danjjak-assistant {
  position: relative;
  z-index: 20;
  width: 64px;
  height: 64px;
}

.assistant-guiding {
  width: 88px;
  height: 88px;
}

.chat-fab {
  flex-shrink: 0;
  animation:
    chat-fab-arrive 0.8s ease-out both,
    chat-fab-float 4.5s ease-in-out 1s infinite;
}

.assistant-guiding .chat-fab {
  width: 88px;
  height: 88px;
  animation: none;
}

.assistant-guiding .chat-fab img {
  width: 76px;
  height: 76px;
}

/* 빈 상태에서도 라이브 영역을 유지해 첫 음성 안내부터 읽어 준다. */
.voice-command-bubble {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
}

.voice-command-bubble-visible {
  position: absolute;
  right: 104px;
  bottom: 0;
  width: min(280px, calc(100vw - 148px));
  height: auto;
  overflow: visible;
  clip-path: none;
  padding: 14px;
  border: 2px solid #f1c232;
  border-radius: 18px;
  background: #fffdf5;
  color: #4b3a08;
  font-size: calc(16px * var(--text-scale, 1));
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.chat-hint::after,
.voice-command-bubble-visible::after {
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

/* 긴 실패 안내도 화면 밖으로 넘치지 않고 읽을 수 있게 한다. */
.voice-command-text {
  max-height: 28dvh;
  overflow-y: auto;
}

.voice-command-bubble-visible::after {
  top: auto;
  bottom: 36px;
  border-color: #f1c232;
  background: #fffdf5;
  transform: rotate(45deg);
}

.voice-command-close {
  position: absolute;
  top: -2px;
  right: -2px;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  transform: translate(50%, -50%);
}

.voice-command-close span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f59a23;
  color: #fff;
  box-shadow: 0 2px 4px rgb(137 62 25 / 12%);
}

.chat-hint-compact {
  display: none;
}

.chat-hint-enter-active,
.chat-hint-leave-active {
  transition: opacity 0.45s ease, transform 0.45s ease;
}

.chat-hint-enter-from,
.chat-hint-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

@media (max-width: 390px) {
  .chat-hint-full { display: none; }
  .chat-hint-compact { display: inline; }
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
  .chat-fab { animation: none; }
  .chat-hint-enter-active,
  .chat-hint-leave-active { transition: none; }
}
</style>
