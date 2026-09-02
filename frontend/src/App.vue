<template>
  <div className="fixed inset-0 bg-[#E5E7EB] flex items-start justify-center overflow-hidden">
    <div className="relative bg-white overflow-hidden flex-shrink-0" style="width: 390px; height: 100%;">
      <!-- Render corresponding view dynamically -->
      <OnboardingView v-if="store.screen === 'onboarding'" />
      <LoginView v-else-if="store.screen === 'login'" />
      <SignupView v-else-if="store.screen === 'signup'" />
      <HomeView v-else-if="store.screen === 'home'" />

      <!-- Tasks -->
      <TaskView v-else-if="store.screen === 'task-transfer'" taskName="task-transfer" />
      <TaskView v-else-if="store.screen === 'task-2'" taskName="task-2" />
      <TaskView v-else-if="store.screen === 'task-3'" taskName="task-3" />
      <TaskView v-else-if="store.screen === 'task-4'" taskName="task-4" />
      <TaskView v-else-if="store.screen === 'task-5'" taskName="task-5" />
      <TaskView v-else-if="store.screen === 'task-6'" taskName="task-6" />
      <TaskView v-else-if="store.screen === 'task-8'" taskName="task-8" />
      <TaskView v-else-if="store.screen === 'task-9'" taskName="task-9" />
      <TaskView v-else-if="store.screen === 'task-10'" taskName="task-10" />
      <TaskView v-else-if="store.screen === 'task-11'" taskName="task-11" />
      <TaskView v-else-if="store.screen === 'task-12'" taskName="task-12" />
      <TaskView v-else-if="store.screen === 'pension-history'" taskName="pension-history" />

      <!-- Transfer flow -->
      <TransferFlowView v-else-if="store.screen === 'direct-transfer'" flowStep="direct-transfer" />
      <TransferFlowView v-else-if="store.screen === 'direct-newaccount'" flowStep="direct-newaccount" />
      <TransferFlowView v-else-if="store.screen === 'guide-person'" flowStep="guide-person" />
      <TransferFlowView v-else-if="store.screen === 'guide-account'" flowStep="guide-account" />
      <TransferFlowView v-else-if="store.screen === 'amount-input'" flowStep="amount-input" />
      <TransferFlowView v-else-if="store.screen === 'pin-entry'" flowStep="pin-entry" />
      <TransferFlowView v-else-if="store.screen === 'fraud-warning'" flowStep="fraud-warning" />
      <TransferFlowView v-else-if="store.screen === 'final-confirm'" flowStep="final-confirm" />
      <TransferFlowView v-else-if="store.screen === 'complete'" flowStep="complete" />

      <!-- Pattern screens -->
      <PatternListView v-else-if="store.screen === 'patterns'" viewMode="patterns" />
      <PatternListView v-else-if="store.screen === 'pattern-detail'" viewMode="pattern-detail" />
      <PatternRegisterView v-else-if="store.screen === 'pattern-register'" />
      <VoiceEditView v-else-if="store.screen === 'voice-edit'" />

      <!-- Tab views -->
      <AnalysisView v-else-if="store.screen === 'analysis'" />
      <SettingsView v-else-if="store.screen === 'settings'" />
      <ContactManageView v-else-if="store.screen === 'contact-manage'" />
      <AddPersonView v-else-if="store.screen === 'add-person'" />

      <!-- Fallback -->
      <HomeView v-else />

      <!-- 음성 안내 바 — 플로우/태스크 화면에만 표시 -->
      <Transition name="vbar">
        <VoiceGuideBar
          v-if="voiceText"
          :text="voiceText"
          :key="store.screen"
        />
      </Transition>

      <!-- Global Toast notification -->
      <Toast
        v-if="store.toast"
        :key="store.toast.key"
        :message="store.toast.msg"
        :action="store.toast.action"
        :onAction="store.toast.cb"
        @done="store.clearToast"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useAppStore } from './stores/appStore';

import OnboardingView from './views/OnboardingView.vue';
import LoginView from './views/LoginView.vue';
import SignupView from './views/SignupView.vue';
import HomeView from './views/HomeView.vue';
import TaskView from './views/TaskView.vue';
import TransferFlowView from './views/TransferFlowView.vue';
import PatternListView from './views/PatternListView.vue';
import PatternRegisterView from './views/PatternRegisterView.vue';
import VoiceEditView from './views/VoiceEditView.vue';
import AnalysisView from './views/AnalysisView.vue';
import SettingsView from './views/SettingsView.vue';
import ContactManageView from './views/ContactManageView.vue';
import AddPersonView from './views/AddPersonView.vue';
import Toast from './components/common/Toast.vue';
import VoiceGuideBar from './components/common/VoiceGuideBar.vue';

const store = useAppStore();

const VOICE_TEXTS = {
  'direct-transfer':   '누구에게 보내실지 선택해 주세요. 등록된 가족에게 보내기를 눌러보세요.',
  'direct-newaccount': '보낼 계좌 정보를 입력해 주세요. 은행을 먼저 선택하고 계좌 번호를 눌러 입력하세요.',
  'guide-person':      '보낼 사람 이름을 눌러 주세요.',
  'guide-account':     '보낼 계좌를 눌러 선택해 주세요.',
  'fraud-warning':     '처음 보내는 계좌예요. 가족인지 먼저 전화로 확인해 보세요.',
  'final-confirm':     '내용을 확인해 주세요. 맞으시면 최종 송금하기를 눌러 주세요.',
  'complete':          '송금이 모두 완료됐어요. 정말 잘 하셨어요!',
  'task-transfer':     '아들 김민수님에게 송금하는 업무입니다. 시작하려면 시작하기를 눌러주세요.',
  'task-2':            '이번 달 연금 입금 내역이에요. 천천히 확인해 보세요.',
  'task-3':            '이번 달 관리비예요. 납부하려면 관리비 납부하기를 눌러 주세요.',
  'task-4':            '내 계좌 잔액을 확인하는 화면이에요. 잔액 보기를 눌러 확인하세요.',
  'task-5':            '거래 내역 화면이에요. 입금, 출금을 선택해서 확인할 수 있어요.',
  'task-6':            '고객센터 화면이에요. 도움이 필요하면 전화 연결하기를 눌러 주세요.',
  'task-8':            '공과금 내역이에요. 전기, 수도, 가스 요금을 확인해 보세요.',
  'task-9':            '자동이체 내역이에요. 매달 나가는 금액을 확인해 보세요.',
  'task-10':           '카드 이용 내역이에요. 이번 달 쓴 금액을 확인해 보세요.',
  'task-11':           '예금 만기 일정이에요. 만기일을 꼭 확인해 두세요.',
  'task-12':           '오늘의 환율 정보예요. 천천히 살펴보세요.',
  'pension-history':   '연금 입금 내역이에요. 매달 들어온 금액을 확인해 보세요.',
};

const voiceText = computed(() => VOICE_TEXTS[store.screen] ?? null);
</script>

<style>
.vbar-enter-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.vbar-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.vbar-enter-from  { opacity: 0; transform: translateY(16px); }
.vbar-leave-to    { opacity: 0; transform: translateY(16px); }
</style>
