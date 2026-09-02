<template>
  <div class="flex flex-col h-full bg-white">
    <SafeArea />
    <TopBar title="새로운 사람 추가" :onBack="store.goBack" />
    <div class="flex-1 overflow-y-auto px-4 pt-4 pb-6">
      <AddPersonForm @saved="onSaved" @cancel="store.goBack" />
    </div>
  </div>
</template>

<script setup>
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import AddPersonForm from '../components/common/AddPersonForm.vue';

const store = useAppStore();

function onSaved() {
  store.goBack();
}
</script>
