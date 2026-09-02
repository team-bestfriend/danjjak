import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
  INITIAL_PATTERNS,
  INITIAL_PEOPLE,
  INITIAL_ACCOUNTS_BY_PERSON,
  reorderPatterns,
} from '../constants/data';

export const useAppStore = defineStore('app', () => {
  // Navigation & Screen management
  const screen = ref('onboarding');
  const history = ref([]);
  
  // Data State
  const patterns = ref([...INITIAL_PATTERNS]);
  const activePattern = ref(null);
  const editingId = ref(null);
  const homePage = ref(1);
  const toast = ref(null);
  
  // Flow state
  const userName = ref('순자');
  const transferAmount = ref('0');
  const isNewAccountFlow = ref(false);
  const selectedPersonId = ref(null);
  const selectedAccountMasked = ref(null);

  // People & Accounts state
  const people = ref([...INITIAL_PEOPLE]);
  const accountsByPerson = ref({ ...INITIAL_ACCOUNTS_BY_PERSON });

  function navigate(s) {
    history.value.push(screen.value);
    screen.value = s;
  }

  function goBack() {
    if (history.value.length > 0) {
      screen.value = history.value.pop();
    }
  }

  function navTo(tab) {
    const s = tab === 'home' ? 'home' : tab === 'patterns' ? 'patterns' : tab === 'analysis' ? 'analysis' : 'settings';
    history.value = [];
    screen.value = s;
  }

  function startTransfer() {
    isNewAccountFlow.value = false;
    transferAmount.value = '0';
    selectedPersonId.value = null;
    selectedAccountMasked.value = null;
  }

  function showToast(msg, action, cb) {
    toast.value = { msg, action, cb, key: Date.now() };
  }

  function clearToast() {
    toast.value = null;
  }

  function setPatterns(newPatterns) {
    patterns.value = typeof newPatterns === 'function' ? newPatterns(patterns.value) : newPatterns;
  }

  function reorder(sourceNum, targetNum) {
    const prev = [...patterns.value];
    const dragged = patterns.value.find((p) => p.num === sourceNum);
    const draggedLabel = dragged ? dragged.label : '패턴';
    patterns.value = reorderPatterns(patterns.value, sourceNum, targetNum);
    showToast(`${draggedLabel}이(가) ${targetNum}번으로 변경됐어요.`, '되돌리기', () => {
      patterns.value = prev;
    });
  }

  function addPerson(name, relation, emoji) {
    const id = Date.now();
    people.value.push({ id, name, emoji, relation, lastUsed: '방금', accounts: 0 });
    accountsByPerson.value[id] = [];
    return id;
  }

  function addAccount(personId, acc) {
    if (!accountsByPerson.value[personId]) {
      accountsByPerson.value[personId] = [];
    }
    accountsByPerson.value[personId].push(acc);
    const personIndex = people.value.findIndex((p) => p.id === personId);
    if (personIndex !== -1) {
      people.value[personIndex].accounts = accountsByPerson.value[personId].length;
    }
  }

  function removePerson(personId) {
    people.value = people.value.filter((p) => p.id !== personId);
    delete accountsByPerson.value[personId];
  }

  return {
    screen,
    history,
    userName,
    patterns,
    activePattern,
    editingId,
    homePage,
    toast,
    transferAmount,
    isNewAccountFlow,
    selectedPersonId,
    selectedAccountMasked,
    people,
    accountsByPerson,
    navigate,
    goBack,
    navTo,
    startTransfer,
    showToast,
    clearToast,
    setPatterns,
    reorder,
    addPerson,
    addAccount,
    removePerson,
  };
});
