import { createRouter, createWebHistory } from "vue-router";
import { useAppStore } from "../stores/appStore.js";

import AccountImportView from "../views/AccountImportView.vue";
import AddPersonView from "../views/AddPersonView.vue";
import AnalysisView from "../views/AnalysisView.vue";
import AuthCallbackView from "../views/AuthCallbackView.vue";
import ConsentView from "../views/ConsentView.vue";
import ContactManageView from "../views/ContactManageView.vue";
import FeatureIntroView from "../views/FeatureIntroView.vue";
import GuardianSetupView from "../views/GuardianSetupView.vue";
import HomeView from "../views/HomeView.vue";
import InstructionImprovementView from "../views/InstructionImprovementView.vue";
import LoginView from "../views/LoginView.vue";
import NicknameSetupView from "../views/NicknameSetupView.vue";
import NotFoundView from "../views/NotFoundView.vue";
import OnboardingView from "../views/OnboardingView.vue";
import PatternListView from "../views/PatternListView.vue";
import PatternRegisterView from "../views/PatternRegisterView.vue";
import PrivacyConsentView from "../views/PrivacyConsentView.vue";
import ServiceGuideView from "../views/ServiceGuideView.vue";
import SettingsView from "../views/SettingsView.vue";
import StepVoiceEditView from "../views/StepVoiceEditView.vue";
import TaskView from "../views/TaskView.vue";
import TransferFlowView from "../views/TransferFlowView.vue";
import VoiceEditView from "../views/VoiceEditView.vue";

/*
 * 로그인 및 기존 동의를 완료해야 접근할 수 있는 화면입니다.
 */
const consentMeta = {
  requiresAuth: true,
  requiresConsent: true,
};

/*
 * 로그인, 기존 동의, 계좌 연결까지 완료해야 접근할 수 있는 화면입니다.
 */
const protectedMeta = {
  ...consentMeta,
  requiresAccount: true,
};

/*
 * 업무 화면 라우트
 */
const taskRoutes = [
  ["task-transfer", "/tasks/transfer"],
  ["task-2", "/tasks/pension"],
  ["task-3", "/tasks/maintenance-fee"],
  ["task-4", "/tasks/balance"],
  ["task-5", "/tasks/transactions"],
  ["task-6", "/tasks/customer-center"],
  ["task-8", "/tasks/utilities"],
  ["task-9", "/tasks/automatic-transfers"],
  ["task-10", "/tasks/card-usage"],
  ["task-11", "/tasks/deposit-maturity"],
  ["task-12", "/tasks/exchange-rate"],
  ["pension-history", "/tasks/pension/history"],
].map(([name, path]) => ({
  path,
  name,
  component: TaskView,
  props: {
    taskName: name,
  },
  meta: protectedMeta,
}));

/*
 * 송금 과정 라우트
 */
const transferRoutes = [
  ["transfer-source", "/transfer/source"],
  ["direct-transfer", "/transfer/recipient"],
  ["direct-newaccount", "/transfer/new-account"],
  ["guide-person", "/transfer/person"],
  ["guide-account", "/transfer/account"],
  ["amount-input", "/transfer/amount"],
  ["pin-entry", "/transfer/pin"],
  ["fraud-warning", "/transfer/review"],
  ["final-confirm", "/transfer/confirm"],
  ["complete", "/transfer/complete"],
  ["cancelled", "/transfer/cancelled"],
].map(([name, path]) => ({
  path,
  name,
  component: TransferFlowView,
  props: {
    flowStep: name,
  },
  meta: protectedMeta,
}));

const routes = [
  /*
   * 최초 진입 화면
   */
  {
    path: '/chat',
    name: 'chat',
    component: () => import('../views/ChatView.vue'),
    meta: consentMeta,
  },
  {
    path: "/",
    redirect: {
      name: "onboarding",
    },
  },
  {
    path: "/onboarding",
    name: "onboarding",
    component: OnboardingView,
  },

  /*
   * 처음 시작하기 전용 화면
   */
  {
    path: "/intro",
    name: "feature-intro",
    component: FeatureIntroView,
  },
  {
    path: "/privacy-consent",
    name: "privacy-consent",
    component: PrivacyConsentView,
  },

  /*
   * 로그인 화면
   */
  {
    path: "/login",
    name: "login",
    component: LoginView,
  },
  {
    path: "/auth/callback",
    name: "auth-callback",
    component: AuthCallbackView,
  },

  /*
   * 로그인 이후 최초 설정 화면
   */
  {
    path: "/nickname-setup",
    name: "nickname-setup",
    component: NicknameSetupView,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: "/consent",
    name: "consent",
    component: ConsentView,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: "/guardian-setup",
    name: "guardian-setup",
    component: GuardianSetupView,
    meta: consentMeta,
  },

  /*
   * 계좌 연결과 홈
   */
  {
    path: "/account-import",
    name: "account-import",
    component: AccountImportView,
    meta: consentMeta,
  },
  {
    path: "/home",
    name: "home",
    component: HomeView,
    meta: protectedMeta,
  },

  /*
   * 업무 화면
   */
  ...taskRoutes,

  /*
   * 송금 화면
   */
  ...transferRoutes,

  /*
   * 패턴 관리
   */
  {
    path: "/patterns",
    name: "patterns",
    component: PatternListView,
    props: {
      viewMode: "patterns",
    },
    meta: protectedMeta,
  },
  {
    path: "/patterns/:patternId",
    name: "pattern-detail",
    component: PatternListView,
    props: {
      viewMode: "pattern-detail",
    },
    meta: protectedMeta,
  },
  {
    path: "/patterns/new",
    name: "pattern-register",
    component: PatternRegisterView,
    meta: protectedMeta,
  },
  {
    path: "/patterns/:patternId/voice",
    name: "voice-edit",
    component: VoiceEditView,
    meta: protectedMeta,
  },
  {
    path: "/patterns/:patternId/steps/voice",
    name: "step-voice-list",
    component: StepVoiceEditView,
    meta: protectedMeta,
  },
  {
    path: "/patterns/:patternId/steps/:stepOrder/voice",
    name: "step-voice-edit",
    component: StepVoiceEditView,
    meta: protectedMeta,
  },

  /*
   * 이용 분석
   */
  {
    path: "/analysis",
    name: "analysis",
    component: AnalysisView,
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: "/analysis/improvement",
    name: "instruction-improvement",
    component: InstructionImprovementView,
    meta: {
      requiresAuth: true,
    },
  },

  /*
   * 설정
   */
  {
    path: "/settings",
    name: "settings",
    component: SettingsView,
    meta: consentMeta,
  },
  {
    path: "/settings/guide",
    name: "service-guide",
    component: ServiceGuideView,
    meta: protectedMeta,
  },
  {
    path: "/settings/people",
    name: "contact-manage",
    component: ContactManageView,
    meta: consentMeta,
  },
  {
    path: "/settings/people/edit",
    name: "add-person",
    component: AddPersonView,
    meta: consentMeta,
  },

  /*
   * 존재하지 않는 주소
   */
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: NotFoundView,
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

/*
 * 송금 상태가 준비되어야 하는 화면입니다.
 */
const TRANSFER_CONTEXT_ROUTES = new Set([
  "direct-transfer",
  "direct-newaccount",
  "guide-person",
  "guide-account",
  "amount-input",
  "pin-entry",
  "fraud-warning",
  "final-confirm",
]);

/*
 * 로그인 전 진입 화면입니다.
 *
 * 이미 로그인된 사용자가 이 화면에 접근하면 현재 가입 상태에 따라
 * 기존 동의, 계좌 연결 또는 홈으로 이동합니다.
 */
const PUBLIC_ENTRY_ROUTES = new Set([
  "onboarding",
  "feature-intro",
  "privacy-consent",
  "login",
]);

export function installRouterGuards(pinia) {
  router.beforeEach(async (to) => {
    const store = useAppStore(pinia);

    const authenticated = await store.checkSession();
    const consentCompleted = Boolean(store.currentUser?.consents?.completed);
    const accountReady = Boolean(store.currentUser?.accountReady);
    const firstStartFlow =
      typeof window !== "undefined" &&
      sessionStorage.getItem("danjjakPrivacyConsent") !== null;

    /*
     * 인증이 필요한 화면에 비로그인 사용자가 접근한 경우
     */
    if (to.meta.requiresAuth && !authenticated) {
      return {
        name: "login",
        query: {
          redirect: to.fullPath,
        },
      };
    }

    /*
     * 로그인된 사용자가 온보딩, 기능 소개, 개인정보 동의,
     * 로그인 화면에 접근한 경우
     */
    if (authenticated && PUBLIC_ENTRY_ROUTES.has(String(to.name))) {
      /*
       * 처음 시작하기 흐름이면 기존 로그인 세션이 남아 있어도
       * 홈으로 보내지 않고 닉네임 설정부터 진행합니다.
       */
      if (firstStartFlow) {
        return {
          name: "nickname-setup",
        };
      }

      /*
       * 다시 이용하기 흐름은 기존 가입 상태에 따라 바로 이동합니다.
       */
      return {
        name: consentCompleted
          ? accountReady
            ? "home"
            : "account-import"
          : "consent",
      };
    }

    /*
     * 기존 동의를 완료한 사용자가 ConsentView에 접근한 경우
     *
     * 다음 두 경우에는 동의 화면 접근을 허용합니다.
     * 1. 설정에서 동의를 수정하는 경우: edit=1
     * 2. 처음 시작하기 흐름인 경우: onboarding=1
     */
    if (
      to.name === "consent" &&
      consentCompleted &&
      to.query.edit !== "1" &&
      to.query.onboarding !== "1"
    ) {
      return {
        name: accountReady ? "home" : "account-import",
      };
    }

    /*
     * 기존 동의를 완료하지 않은 사용자가 보호 화면에 접근한 경우
     */
    if (to.meta.requiresConsent && !consentCompleted) {
      return {
        name: "consent",
      };
    }

    /*
     * 이미 계좌 준비가 완료된 사용자가 계좌 연결 화면에 접근한 경우
     *
     * 설정에서 계좌를 관리하기 위해 들어온 경우에는 접근을 허용합니다.
     */
    if (
      to.name === "account-import" &&
      accountReady &&
      to.query.manage !== "1"
    ) {
      return {
        name: "home",
      };
    }

    /*
     * 계좌 준비가 완료되지 않은 사용자가 금융 화면에 접근한 경우
     */
    if (to.meta.requiresAccount && !accountReady) {
      return {
        name: "account-import",
      };
    }

    /*
     * 송금 과정에 필요한 출금 계좌가 선택되지 않은 경우
     */
    if (
      TRANSFER_CONTEXT_ROUTES.has(String(to.name)) &&
      !store.selectedSourceAccountId
    ) {
      store.startTransfer();

      return {
        name: "transfer-source",
      };
    }

    /*
     * 완료된 송금 결과 없이 완료 화면에 접근한 경우
     */
    if (to.name === "complete" && !store.transferResult?.transactionId) {
      store.startTransfer();

      return {
        name: "transfer-source",
      };
    }

    /*
     * 실제 취소 상태가 아닌데 취소 화면에 접근한 경우
     */
    if (to.name === "cancelled" && !store.transferCancelled) {
      return {
        name: "transfer-source",
      };
    }

    return true;
  });

  router.afterEach((to, from) => {
    const store = useAppStore(pinia);

    store.recordPatternNavigation(
      String(from.name ?? ""),
      String(to.name ?? ""),
    );
  });
}

export default router;
