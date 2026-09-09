import { computed, onMounted, onUnmounted, ref, toValue, watch } from 'vue';
import { useTtsAudio } from './useTtsAudio.js';

export function useGuidanceAudio(text, { voiceMode = 'TTS', familyAudioUrl = '', speed = 'NORMAL' } = {}) {
  const failed = ref(false);
  const familyPlaying = ref(false);
  const familyLoading = ref(false);
  let audio;
  let replayPending = false;
  const useTts = computed(() => toValue(voiceMode) !== 'FAMILY' || !toValue(familyAudioUrl) || failed.value);
  const tts = useTtsAudio(text, { speed, enabled: useTts, autoplay: true });
  const playing = computed(() => useTts.value ? tts.playing.value : familyPlaying.value);
  const loading = computed(() => useTts.value ? tts.loading.value : familyLoading.value);
  const error = computed(() => useTts.value ? tts.error.value : '');
  const notice = computed(() => {
    if (toValue(voiceMode) !== 'FAMILY') return '';
    if (!toValue(familyAudioUrl)) return '저장된 가족 음성이 없어 AI 음성으로 안내해요.';
    if (failed.value) return '가족 음성을 재생하지 못해 AI 음성으로 안내해요.';
    return '저장된 가족 음성으로 안내해요.';
  });

  function release() {
    const previous = audio;
    audio = null;
    if (previous) {
      previous.onplaying = previous.onpause = previous.onended = previous.oncanplay = previous.onerror = null;
      previous.pause();
      previous.removeAttribute('src');
      previous.load();
    }
    familyPlaying.value = familyLoading.value = false;
  }

  async function playFamily() {
    const current = audio;
    if (!current) return;
    try { await current.play(); }
    catch (cause) {
      if (audio !== current) return;
      familyLoading.value = false;
      if (cause?.name !== 'NotAllowedError') { release(); failed.value = true; }
    }
  }

  function load() {
    release();
    failed.value = false;
    if (toValue(voiceMode) !== 'FAMILY' || !toValue(familyAudioUrl)) return;
    const current = new Audio();
    audio = current;
    current.crossOrigin = 'use-credentials';
    current.playbackRate = { SLOW: 0.8, NORMAL: 1, FAST: 1.2 }[toValue(speed)] ?? 1;
    current.onplaying = () => { familyLoading.value = false; familyPlaying.value = true; };
    current.onpause = current.onended = () => { familyPlaying.value = false; };
    current.oncanplay = () => { familyLoading.value = false; };
    current.onerror = () => { if (audio === current) { release(); failed.value = true; } };
    familyLoading.value = true;
    current.src = toValue(familyAudioUrl);
    void playFamily();
  }

  async function toggle() {
    if (useTts.value) return tts.toggle();
    if (!audio) { load(); return; }
    if (audio.paused) {
      if (audio.ended) audio.currentTime = 0;
      await playFamily();
    } else audio.pause();
  }

  async function replay() {
    if (useTts.value) return tts.replay();
    if (!audio) { load(); return; }
    audio.currentTime = 0;
    await playFamily();
  }

  async function replayWhenIdle() {
    // 연속 오조작으로 재생 중인 안내나 재생 시작 요청이 끊기지 않도록 한다.
    if (playing.value || loading.value || replayPending) return;
    replayPending = true;
    try { await replay(); }
    finally { replayPending = false; }
  }

  watch([() => toValue(text), () => toValue(voiceMode), () => toValue(familyAudioUrl), () => toValue(speed)], load);
  onMounted(load);
  onUnmounted(release);
  return { playing, loading, error, notice, toggle, replay, replayWhenIdle };
}
