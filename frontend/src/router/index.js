import { createRouter, createWebHistory } from 'vue-router';
import OnboardingView from '../views/OnboardingView.vue';
import LoginView from '../views/LoginView.vue';
import SignupView from '../views/SignupView.vue';
import HomeView from '../views/HomeView.vue';
import TaskView from '../views/TaskView.vue';
import TransferFlowView from '../views/TransferFlowView.vue';
import PatternListView from '../views/PatternListView.vue';
import PatternRegisterView from '../views/PatternRegisterView.vue';
import VoiceEditView from '../views/VoiceEditView.vue';
import AnalysisView from '../views/AnalysisView.vue';
import SettingsView from '../views/SettingsView.vue';
import ContactManageView from '../views/ContactManageView.vue';

const routes = [
  { path: '/', redirect: '/onboarding' },
  { path: '/onboarding', name: 'onboarding', component: OnboardingView },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/signup', name: 'signup', component: SignupView },
  { path: '/home', name: 'home', component: HomeView },

  // Task screens
  { path: '/task-transfer', name: 'task-transfer', component: TaskView, props: { taskName: 'task-transfer' } },
  { path: '/task-2', name: 'task-2', component: TaskView, props: { taskName: 'task-2' } },
  { path: '/task-3', name: 'task-3', component: TaskView, props: { taskName: 'task-3' } },
  { path: '/task-4', name: 'task-4', component: TaskView, props: { taskName: 'task-4' } },
  { path: '/task-5', name: 'task-5', component: TaskView, props: { taskName: 'task-5' } },
  { path: '/task-6', name: 'task-6', component: TaskView, props: { taskName: 'task-6' } },
  { path: '/task-8', name: 'task-8', component: TaskView, props: { taskName: 'task-8' } },
  { path: '/task-9', name: 'task-9', component: TaskView, props: { taskName: 'task-9' } },
  { path: '/task-10', name: 'task-10', component: TaskView, props: { taskName: 'task-10' } },
  { path: '/task-11', name: 'task-11', component: TaskView, props: { taskName: 'task-11' } },
  { path: '/task-12', name: 'task-12', component: TaskView, props: { taskName: 'task-12' } },
  { path: '/pension-history', name: 'pension-history', component: TaskView, props: { taskName: 'pension-history' } },

  // Transfer flow screens
  { path: '/direct-transfer', name: 'direct-transfer', component: TransferFlowView, props: { flowStep: 'direct-transfer' } },
  { path: '/direct-newaccount', name: 'direct-newaccount', component: TransferFlowView, props: { flowStep: 'direct-newaccount' } },
  { path: '/guide-person', name: 'guide-person', component: TransferFlowView, props: { flowStep: 'guide-person' } },
  { path: '/guide-account', name: 'guide-account', component: TransferFlowView, props: { flowStep: 'guide-account' } },
  { path: '/amount-input', name: 'amount-input', component: TransferFlowView, props: { flowStep: 'amount-input' } },
  { path: '/pin-entry', name: 'pin-entry', component: TransferFlowView, props: { flowStep: 'pin-entry' } },
  { path: '/fraud-warning', name: 'fraud-warning', component: TransferFlowView, props: { flowStep: 'fraud-warning' } },
  { path: '/final-confirm', name: 'final-confirm', component: TransferFlowView, props: { flowStep: 'final-confirm' } },
  { path: '/complete', name: 'complete', component: TransferFlowView, props: { flowStep: 'complete' } },

  // Patterns
  { path: '/patterns', name: 'patterns', component: PatternListView, props: { viewMode: 'patterns' } },
  { path: '/pattern-detail', name: 'pattern-detail', component: PatternListView, props: { viewMode: 'pattern-detail' } },
  { path: '/pattern-register', name: 'pattern-register', component: PatternRegisterView },
  { path: '/voice-edit', name: 'voice-edit', component: VoiceEditView },

  // Other tabs
  { path: '/analysis', name: 'analysis', component: AnalysisView },
  { path: '/settings', name: 'settings', component: SettingsView },
  { path: '/contact-manage', name: 'contact-manage', component: ContactManageView },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
