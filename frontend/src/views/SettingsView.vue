<template>
  <div className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div className="bg-white px-5 border-b border-[#EEEEED] flex-shrink-0" style="padding-top: 18px; padding-bottom: 16px;">
      <h1 className="font-bold text-[#111827]" style="font-size: 26px;">설정</h1>
    </div>
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-5">
      <button
        v-if="store.anomaly"
        type="button"
        @click="store.goBack"
        className="w-full min-h-[52px] rounded-[16px] border-2 border-[#FFBC00] bg-[#FFFBEB] font-bold text-[#92650A]"
      >이상거래 확인 화면으로 돌아가기</button>

      <!-- 사용자 프로필 -->
      <Card className="p-5">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 rounded-full flex items-center justify-center flex-shrink-0" style="background: #FFF3CC; border: 2px solid #FFBC00; font-size: 32px;">👵</div>
          <div className="flex-1">
            <p className="font-bold text-[#111827]" style="font-size: 23px;">{{ store.userName }}</p>
            <p className="text-[#6B7280] mt-0.5" style="font-size: 15px;">단짝 시연 사용자</p>
          </div>
        </div>
      </Card>

      <!-- 화면 설정 -->
      <div>
        <p className="font-bold text-[#9CA3AF] uppercase tracking-wide mb-3 px-1" style="font-size: 13px;">화면 설정</p>
        <Card className="p-5 space-y-5">
          <div>
            <p className="font-semibold text-[#111827] mb-3" style="font-size: 19px;">글씨 크기</p>
            <SegControl
              label="글씨 크기"
              :disabled="settingsSaving"
              :options="[{ key: 'small', label: '작게' }, { key: 'normal', label: '보통' }, { key: 'large', label: '크게' }]"
              v-model:value="fontSize"
            />
            <p class="mt-3 rounded-[14px] bg-[#FFFBEB] p-4 text-[#374151]" :style="{ fontSize: `calc(18px * ${{ small: 0.95, normal: 1, large: 1.15 }[fontSize]})` }">이 크기로 안내를 읽어요.<br>버튼 크기는 그대로 유지해요.</p>
          </div>
        </Card>
      </div>

      <!-- 음성 안내 설정 -->
      <div>
        <p className="font-bold text-[#9CA3AF] uppercase tracking-wide mb-3 px-1" style="font-size: 13px;">음성 안내</p>
        <Card className="p-5 space-y-5">
          <div>
            <p className="font-semibold text-[#111827] mb-3" style="font-size: 19px;">안내 속도</p>
            <SegControl
              label="안내 속도"
              :disabled="settingsSaving"
              :options="[{ key: 'slow', label: '느리게' }, { key: 'normal', label: '보통' }, { key: 'fast', label: '빠르게' }]"
              v-model:value="guideSpeed"
            />
            <p class="mt-3 text-[15px] text-[#6B7280]">자동 음성과 저장된 가족 음성 모두에 적용해요.</p>
            <button type="button" class="mt-3 min-h-12 w-full rounded-[14px] bg-[#FFF3CC] px-3 text-[17px] font-bold text-[#92650A]" :disabled="previewLoading" @click="previewVoice">{{ previewLoading ? '음성 준비 중…' : previewPlaying ? '미리듣기 멈추기' : '이 속도로 자동 음성 들어보기' }}</button>
            <p class="mt-2 text-[14px] text-[#6B7280]">Marin · AI가 생성한 한국어 안내 음성이에요.</p>
            <p v-if="previewError" class="mt-2 text-[15px] text-[#B91C1C]" role="alert">{{ previewError }} 설정은 계속 저장할 수 있어요.</p>
          </div>
          <div className="border-t border-[#F3F4F6] pt-5">
            <p className="font-semibold text-[#111827] mb-3" style="font-size: 19px;">음성 안내 방식</p>
            <SegControl
              label="기본 음성 안내 방식"
              :disabled="settingsSaving"
              :options="[{ key: 'tts', label: '자동 음성(TTS)' }, { key: 'family', label: '가족 음성' }]"
              v-model:value="voiceMode"
            />
            <p className="text-[#9CA3AF] mt-2 px-1" style="font-size: 13px;">
              {{ voiceMode === 'tts' ? '현재 안내 문구를 자동 음성으로 읽어요.' : '저장된 가족 녹음이 없거나 재생되지 않으면 같은 문구를 자동 음성으로 읽어요.' }} 패턴에서 따로 선택한 음성은 유지돼요.
            </p>
          </div>
          <button
            type="button"
            className="w-full rounded-[16px] bg-[#FFBC00] font-semibold text-[#111827] disabled:opacity-50"
            style="min-height: 56px; font-size: 17px;"
            :disabled="settingsSaving"
            @click="saveSettings"
          >{{ settingsSaving ? '저장 중…' : '화면·음성 설정 저장' }}</button>
          <p v-if="settingsError" className="text-[#B91C1C]" role="alert">{{ settingsError }}</p>
          <p v-if="settingsSaved" className="text-[#166534]" role="status">설정을 저장했어요.</p>
        </Card>
      </div>

      <button
        type="button"
        className="w-full rounded-[18px] border-2 border-[#E5E7EB] bg-white p-4 text-left"
        @click="store.navigate('consent', { query: { edit: '1' } })"
      >
        <span className="font-semibold text-[#111827]" style="font-size: 18px;">선택 동의 관리</span>
        <span className="mt-1 block text-[#6B7280]" style="font-size: 14px;">이용 기록과 카카오 나에게 보내기 동의를 변경해요.</span>
      </button>

      <!-- 보호자 설정 -->
      <div>
        <p className="font-bold text-[#9CA3AF] uppercase tracking-wide mb-3 px-1" style="font-size: 13px;">보호자 설정</p>
        <Card className="p-5 space-y-3">
          <div>
            <p className="font-semibold text-[#111827] mb-1" style="font-size: 19px;">보호자 전화번호 등록</p>
            <p className="text-[#6B7280] mb-3" style="font-size: 15px;">이상 거래 화면의 보호자 전화 연결에 사용해요.</p>
            <p v-if="store.supportLoading" className="text-[#6B7280] mb-3">보호자 번호를 불러오고 있어요…</p>
            <p v-else-if="!store.supportError && !guardianPhone" className="text-[#6B7280] mb-3">등록된 보호자 번호가 없어요. 아래에 번호를 입력해 주세요.</p>
            <div className="flex gap-2 w-full">
              <input
                v-model="guardianPhone"
                type="tel"
                aria-label="보호자 전화번호"
                placeholder="010-0000-0000"
                inputMode="tel"
                className="flex-1 min-w-0 rounded-[14px] border-2 border-[#E5E7EB] focus:border-[#FFBC00] outline-none px-4 font-bold text-[#111827] placeholder:text-[#D1D5DB]"
                style="min-height: 54px; font-size: 17px;"
              />
              <button
                :disabled="store.guardianSaving || store.supportLoading"
                @click="saveGuardian"
                className="rounded-[14px] bg-[#FFBC00] text-[#111827] font-semibold flex-shrink-0 px-5 disabled:opacity-50"
                style="height: 54px; font-size: 17px;"
              >{{ store.guardianSaving ? '저장 중…' : '저장' }}</button>
            </div>
            <p v-if="guardianError || store.supportError" className="text-[#B91C1C] mt-2" role="alert">{{ guardianError || store.supportError }}</p>
            <p v-if="guardianSaved" className="text-[#166534] mt-2">저장한 번호를 보호자 전화 연결에 반영했어요.</p>
          </div>
        </Card>
      </div>

      <!-- 계좌/사람 관리 -->
      <div>
        <p className="font-bold text-[#9CA3AF] uppercase tracking-wide mb-3 px-1" style="font-size: 13px;">계좌 관리</p>
        <Card className="overflow-hidden">
          <button
            type="button"
            className="w-full flex items-center justify-between px-5 py-4 text-left active:bg-[#F9FAFB]"
            @click="store.navigate('account-import', { query: { manage: '1' } })"
          >
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-full bg-[#E8F3FF] border border-[#93C5FD] flex items-center justify-center font-bold text-[#1D4ED8]" style="font-size: 15px;">내 계좌</div>
              <div>
                <p className="font-semibold text-[#111827]" style="font-size: 19px;">내 계좌 관리</p>
                <p className="text-[#9CA3AF]" style="font-size: 14px;">모의 본인 계좌 확인·추가</p>
              </div>
            </div>
            <Ic name="ChevR" />
          </button>
          <button
            type="button"
            className="w-full flex items-center justify-between border-t border-[#F3F4F6] px-5 py-4 text-left active:bg-[#F9FAFB]"
            @click="store.navigate('contact-manage')"
          >
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-full bg-[#FFF3CC] border border-[#FFBC00] flex items-center justify-center" style="font-size: 22px;">👨‍👩‍👧</div>
              <div>
                <p className="font-semibold text-[#111827]" style="font-size: 19px;">사람 및 계좌 관리</p>
                <p className="text-[#9CA3AF]" style="font-size: 14px;">가족 {{ store.people.length }}명 · 송금 계좌 관리</p>
              </div>
            </div>
            <Ic name="ChevR" />
          </button>
        </Card>
      </div>

      <!-- 서비스 -->
      <div>
        <p className="font-bold text-[#9CA3AF] uppercase tracking-wide mb-3 px-1" style="font-size: 13px;">서비스</p>
        <Card className="overflow-hidden">
          <button
            v-for="(item, i) in serviceItems"
            :key="item.label"
            :disabled="!item.screen"
            @click="item.screen && store.navigate(item.screen)"
            :class="['w-full flex items-center justify-between px-5 py-4 text-left active:bg-[#F9FAFB] disabled:cursor-not-allowed disabled:opacity-60', i > 0 ? 'border-t border-[#F3F4F6]' : '']"
          >
            <div className="flex items-center gap-3">
              <span style="font-size: 22px;">{{ item.icon }}</span>
              <span className="font-normal text-[#111827]" style="font-size: 18px;">{{ item.label }}{{ item.screen ? '' : ' (준비 중)' }}</span>
            </div>
            <Ic name="ChevR" />
          </button>
        </Card>
      </div>

      <!-- 로그아웃 -->
      <button
        type="button"
        :disabled="store.logoutPending"
        className="w-full rounded-[18px] border-2 border-[#E5E7EB] bg-white font-medium text-[#6B7280] disabled:opacity-50"
        style="padding: 18px; font-size: 17px;"
        @click="store.logout"
      >
        {{ store.logoutPending ? '로그아웃 중…' : '로그아웃' }}
      </button>
    </div>

    <NavBar active="settings" :onSelect="store.navTo" />
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';
import { useTtsAudio } from '../composables/useTtsAudio.js';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import Card from '../components/common/Card.vue';
import Ic from '../components/common/Ic.vue';
import NavBar from '../components/common/NavBar.vue';
import SegControl from '../components/common/SegControl.vue';

const store = useAppStore();

const fontSize = ref("normal");
const guideSpeed = ref("normal");
const voiceMode = ref("tts");
const previewEnabled = ref(false);
const { playing: previewPlaying, loading: previewLoading, error: previewError, toggle: togglePreview, cleanup: stopPreview } = useTtsAudio(
  '안녕하세요. 단짝과 함께 천천히 확인해요. 받는 분과 금액이 맞으면 다음 버튼을 눌러 주세요.',
  { speed: () => guideSpeed.value.toUpperCase(), enabled: previewEnabled, autoplay: true },
);
watch(guideSpeed, () => { previewEnabled.value = false; stopPreview(); }, { flush: 'sync' });
watch([fontSize, guideSpeed, voiceMode], () => { settingsSaved.value = false; });
function previewVoice() {
  if (!previewEnabled.value) previewEnabled.value = true;
  else void togglePreview();
}
const settingsSaving = ref(false);
const settingsError = ref("");
const settingsSaved = ref(false);
const guardianPhone = ref("");
const guardianError = ref("");
const guardianSaved = ref(false);

const serviceItems = [
  { label: "서비스 이용방법", icon: "📖", screen: "service-guide" },
  { label: "고객센터 연결", icon: "📞", screen: "task-6" },
  { label: "개인정보 처리방침", icon: "🔒", screen: null }
];

onMounted(async () => {
  const settings = store.currentUser?.settings;
  fontSize.value = String(settings?.fontSize ?? 'NORMAL').toLowerCase();
  guideSpeed.value = String(settings?.voiceSpeed ?? 'NORMAL').toLowerCase();
  voiceMode.value = String(settings?.guideVoiceType ?? 'TTS').toLowerCase();
  await Promise.all([store.loadSupport(), store.loadFinancialData()]);
  guardianPhone.value = store.support?.guardian?.phoneNumber ?? '';
});

async function saveSettings() {
  if (settingsSaving.value) return;
  settingsSaving.value = true;
  settingsError.value = '';
  settingsSaved.value = false;
  try {
    await store.saveAccessibilitySettings({
      fontSize: fontSize.value.toUpperCase(),
      voiceSpeed: guideSpeed.value.toUpperCase(),
      guideVoiceType: voiceMode.value.toUpperCase(),
    });
    settingsSaved.value = true;
  } catch (error) {
    settingsError.value = error?.message ?? '설정을 저장하지 못했습니다. 다시 시도해 주세요.';
  } finally {
    settingsSaving.value = false;
  }
}

async function saveGuardian() {
  guardianError.value = '';
  guardianSaved.value = false;
  if (!/^[0-9-]{8,20}$/.test(guardianPhone.value)) {
    guardianError.value = '전화번호는 숫자와 하이픈을 포함해 8~20자로 입력해 주세요.';
    return;
  }
  try {
    const saved = await store.saveGuardian(guardianPhone.value);
    guardianPhone.value = saved.phoneNumber;
    guardianSaved.value = true;
  } catch {
    // 저장소가 서버 오류를 화면용 문구로 보관한다.
  }
}
</script>
