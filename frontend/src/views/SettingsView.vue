<template>
  <div className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div className="bg-white px-5 border-b border-[#EEEEED] flex-shrink-0" style="padding-top: 18px; padding-bottom: 16px;">
      <h1 className="font-bold text-[#111827]" style="font-size: 26px;">설정</h1>
    </div>
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-5">
      <!-- 사용자 프로필 -->
      <Card className="p-5">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 rounded-full flex items-center justify-center flex-shrink-0" style="background: #FFF3CC; border: 2px solid #FFBC00; font-size: 32px;">👵</div>
          <div className="flex-1">
            <p className="font-bold text-[#111827]" style="font-size: 23px;">김순자</p>
            <p className="text-[#6B7280] mt-0.5" style="font-size: 15px;">KB국민은행 · 010-****-1234</p>
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
              :options="[{ key: 'small', label: '작게' }, { key: 'medium', label: '보통' }, { key: 'large', label: '크게' }]"
              v-model:value="fontSize"
            />
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
              :options="[{ key: 'slow', label: '느리게' }, { key: 'normal', label: '보통' }, { key: 'fast', label: '빠르게' }]"
              v-model:value="guideSpeed"
            />
          </div>
          <div className="border-t border-[#F3F4F6] pt-5">
            <p className="font-semibold text-[#111827] mb-3" style="font-size: 19px;">음성 안내 방식</p>
            <SegControl
              :options="[{ key: 'tts', label: '자동 음성(TTS)' }, { key: 'family', label: '가족 음성' }]"
              v-model:value="voiceMode"
            />
            <p className="text-[#9CA3AF] mt-2 px-1" style="font-size: 13px;">
              {{ voiceMode === 'tts' ? '등록 정보를 기반으로 자동 생성된 음성이 재생됩니다.' : '가족이 직접 녹음한 음성이 재생됩니다.' }}
            </p>
          </div>
        </Card>
      </div>

      <!-- 보호자 설정 -->
      <div>
        <p className="font-bold text-[#9CA3AF] uppercase tracking-wide mb-3 px-1" style="font-size: 13px;">보호자 설정</p>
        <Card className="p-5 space-y-3">
          <div>
            <p className="font-semibold text-[#111827] mb-1" style="font-size: 19px;">보호자 전화번호 등록</p>
            <p className="text-[#6B7280] mb-3" style="font-size: 15px;">이상 거래 감지 시 카카오톡 알림을 보내드려요.</p>
            <div className="flex gap-2 w-full">
              <input
                v-model="guardianPhone"
                type="tel"
                placeholder="010-0000-0000"
                inputMode="tel"
                className="flex-1 min-w-0 rounded-[14px] border-2 border-[#E5E7EB] focus:border-[#FFBC00] outline-none px-4 font-bold text-[#111827] placeholder:text-[#D1D5DB]"
                style="min-height: 54px; font-size: 17px;"
              />
              <button className="rounded-[14px] bg-[#FFBC00] text-[#111827] font-semibold flex-shrink-0 px-5" style="height: 54px; font-size: 17px;">저장</button>
            </div>
          </div>
        </Card>
      </div>

      <!-- 계좌/사람 관리 -->
      <div>
        <p className="font-bold text-[#9CA3AF] uppercase tracking-wide mb-3 px-1" style="font-size: 13px;">계좌 관리</p>
        <Card className="overflow-hidden">
          <div
            className="flex items-center justify-between px-5 py-4 cursor-pointer active:bg-[#F9FAFB]"
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
          </div>
        </Card>
      </div>

      <!-- 서비스 -->
      <div>
        <p className="font-bold text-[#9CA3AF] uppercase tracking-wide mb-3 px-1" style="font-size: 13px;">서비스</p>
        <Card className="overflow-hidden">
          <div
            v-for="(item, i) in serviceItems"
            :key="item.label"
            :class="['flex items-center justify-between px-5 py-4 cursor-pointer active:bg-[#F9FAFB]', i > 0 ? 'border-t border-[#F3F4F6]' : '']"
          >
            <div className="flex items-center gap-3">
              <span style="font-size: 22px;">{{ item.icon }}</span>
              <span className="font-normal text-[#111827]" style="font-size: 18px;">{{ item.label }}</span>
            </div>
            <Ic name="ChevR" />
          </div>
        </Card>
      </div>

      <!-- 로그아웃 -->
      <button className="w-full rounded-[18px] border-2 border-[#E5E7EB] font-medium text-[#6B7280]" style="padding: 18px; font-size: 17px;">
        로그아웃
      </button>
    </div>

    <NavBar active="settings" :onSelect="store.navTo" />
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import Card from '../components/common/Card.vue';
import Ic from '../components/common/Ic.vue';
import NavBar from '../components/common/NavBar.vue';
import SegControl from '../components/common/SegControl.vue';

const store = useAppStore();

const fontSize = ref("medium");
const guideSpeed = ref("normal");
const voiceMode = ref("tts");
const guardianPhone = ref("");

const serviceItems = [
  { label: "서비스 이용방법", icon: "📖" },
  { label: "고객센터 연결", icon: "📞" },
  { label: "개인정보 처리방침", icon: "🔒" }
];
</script>
