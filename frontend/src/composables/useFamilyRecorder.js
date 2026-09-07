import { onUnmounted, ref } from 'vue';

export function useFamilyRecorder(onRecorded) {
  const state = ref('idle');
  const seconds = ref(0);
  const error = ref('');
  let recorder;
  let stream;
  let timer;
  let disposed = false;

  function releaseTracks() {
    clearInterval(timer);
    stream?.getTracks().forEach((track) => track.stop());
    stream = null;
  }

  function stop() {
    if (recorder?.state === 'recording') {
      state.value = 'stopping';
      recorder.stop();
    }
    releaseTracks();
  }

  async function start(text) {
    if (state.value !== 'idle') return;
    error.value = '';
    if (!globalThis.isSecureContext || !navigator.mediaDevices?.getUserMedia || !globalThis.MediaRecorder) {
      error.value = '이 환경에서는 녹음할 수 없어요. HTTPS 또는 localhost에서 지원 브라우저로 열어 주세요.';
      return;
    }
    state.value = 'requesting';
    seconds.value = 0;
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      if (disposed) { releaseTracks(); return; }
      const mimeType = ['audio/webm;codecs=opus', 'audio/mp4', 'audio/ogg;codecs=opus']
        .find((type) => MediaRecorder.isTypeSupported(type));
      if (!mimeType) throw new Error('unsupported');
      recorder = new MediaRecorder(stream, { mimeType });
      const parts = [];
      let failed = false;
      let bytes = 0;
      recorder.ondataavailable = (event) => {
        bytes += event.data.size;
        if (bytes > 10 * 1024 * 1024) {
          failed = true;
          error.value = '녹음이 10MB를 넘었어요. 조금 더 짧게 녹음해 주세요.';
          stop();
        } else if (event.data.size) parts.push(event.data);
      };
      recorder.onerror = () => {
        failed = true;
        error.value = '마이크 녹음 중 문제가 생겼어요. 마이크 연결을 확인하고 다시 시도해 주세요.';
        stop();
        state.value = 'idle';
      };
      recorder.onstop = () => {
        releaseTracks();
        state.value = 'idle';
        if (disposed || failed) return;
        const blob = new Blob(parts, { type: recorder.mimeType });
        if (blob.size) onRecorded({ blob, text });
        else error.value = '녹음된 소리가 없어요. 다시 녹음해 주세요.';
      };
      recorder.start(1000);
      state.value = 'recording';
      timer = setInterval(() => {
        seconds.value++;
        if (seconds.value >= 90) stop();
      }, 1000);
    } catch (cause) {
      releaseTracks();
      state.value = 'idle';
      error.value = cause?.name === 'NotAllowedError' ? '마이크 권한이 거절됐어요. 브라우저에서 마이크를 허용한 뒤 다시 시도해 주세요.'
        : cause?.name === 'NotFoundError' ? '사용할 마이크를 찾지 못했어요. 마이크를 연결해 주세요.'
          : cause?.message === 'unsupported' ? '이 브라우저의 녹음 형식을 지원하지 않아요. 다른 브라우저에서 시도해 주세요.'
            : '마이크를 열지 못했어요. 다른 앱에서 사용 중인지 확인해 주세요.';
    }
  }

  onUnmounted(() => { disposed = true; stop(); });
  return { state, seconds, error, start, stop };
}
