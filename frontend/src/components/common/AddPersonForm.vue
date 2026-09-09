<template>
  <form class="space-y-6" @submit.prevent="handleSave">
    <p class="text-[24px] font-extrabold leading-[1.35] text-[#111827]">
      {{
        existingPerson
          ? "등록 정보를 수정해주세요"
          : "새로운 사람을 등록해주세요"
      }}
    </p>

    <div class="space-y-3">
      <p class="text-[18px] pb-1 font-bold text-[#111827]">프로필</p>
      <div class="grid grid-cols-5 gap-3">
        <button
          v-for="profile in PROFILE_OPTIONS"
          :key="profile.key"
          type="button"
          :aria-label="profile.label"
          @click="profileImageKey = profile.key"
          :class="[
            'flex aspect-square min-h-[64px] items-center justify-center rounded-full border-[4px] bg-white overflow-hidden transition-colors',
            profileImageKey === profile.key
              ? 'border-[#F5B800] bg-[#FFFBEB]'
              : 'border-[#E5E7EB]',
          ]"
        >
          <img
            :src="profile.src"
            alt=""
            class="h-[64px] w-[64px] object-cover"
          />
        </button>
      </div>
    </div>

    <label class="block space-y-2">
      <span class="block pb-1 text-[18px] font-bold text-[#111827]">이름</span>
      <input
        id="registered-person-name"
        v-model.trim="name"
        :aria-invalid="Boolean(fieldErrors.name)"
        aria-describedby="registered-person-name-error"
        @blur="touched.name = true"
        maxlength="50"
        placeholder="예: 김민준"
        class="w-full min-h-[58px] rounded-[14px] border-2 border-[#E5E7EB] focus:border-[#F5B800] outline-none px-4 text-[18px] font-normal placeholder:text-[#9CA3AF] placeholder:font-normal"
      />
      <p
        v-if="fieldErrors.name"
        id="registered-person-name-error"
        class="text-[13px] text-[#B91C1C]"
        role="alert"
      >
        {{ fieldErrors.name }}
      </p>
    </label>

    <label class="block space-y-2">
      <span class="block pb-1 text-[17px] font-bold text-[#111827]">관계</span>
      <input
        id="registered-person-relationship"
        v-model.trim="relationship"
        :aria-invalid="Boolean(fieldErrors.relationship)"
        aria-describedby="registered-person-relationship-error"
        @blur="touched.relationship = true"
        maxlength="30"
        placeholder="예: 아들, 딸"
        class="w-full min-h-[58px] rounded-[14px] border-2 border-[#E5E7EB] focus:border-[#F5B800] outline-none px-4 text-[18px] font-normal placeholder:text-[#9CA3AF] placeholder:font-normal"
      />
      <p
        v-if="fieldErrors.relationship"
        id="registered-person-relationship-error"
        class="text-[13px] text-[#B91C1C]"
        role="alert"
      >
        {{ fieldErrors.relationship }}
      </p>
    </label>

    <p
      v-if="formError"
      class="rounded-xl bg-[#FEF2F2] p-3 text-[#991B1B]"
      role="alert"
    >
      {{ formError }}
    </p>

    <div class="flex gap-3 pt-2">
      <button
        type="button"
        :disabled="saving"
        @click="emit('cancel')"
        class="flex-1 min-h-[56px] rounded-[18px] border-2 border-[#E5E7EB] text-[16px] font-bold text-[#6B7280] disabled:opacity-50"
      >
        취소
      </button>
      <button
        type="submit"
        :disabled="!canSave || saving"
        :class="[
          'flex-1 min-h-[56px] rounded-[18px] text-[16px] font-bold',
          canSave && !saving
            ? 'bg-[#F5B800] text-[#111827]'
            : 'bg-[#E5E7EB] text-[#9CA3AF]',
        ]"
      >
        {{ saving ? "저장 중…" : "저장" }}
      </button>
    </div>
  </form>
</template>

<script setup>
import { computed, ref } from "vue";
import { ApiError } from "../../api/httpClient";
import { useAppStore } from "../../stores/appStore";
import baby from "../../assets/icons/profile/baby.png";
import youthBoy from "../../assets/icons/profile/youth_boy.png";
import youthWoman from "../../assets/icons/profile/youth_woman.png";
import adultMan from "../../assets/icons/profile/adult_man.png";
import adultWoman from "../../assets/icons/profile/adult_woman.png";
import middleAgedMan from "../../assets/icons/profile/middle_aged_man.png";
import middleAgedWoman from "../../assets/icons/profile/middle_aged_woman.png";
import oldAgeMan from "../../assets/icons/profile/old_age_man.png";
import oldAgeWoman from "../../assets/icons/profile/old_age_woman.png";

const PROFILE_OPTIONS = [
  { key: "baby", label: "아이", src: baby },
  { key: "youth_boy", label: "청년 남성", src: youthBoy },
  { key: "youth_woman", label: "청년 여성", src: youthWoman },
  { key: "adult_man", label: "남성", src: adultMan },
  { key: "adult_woman", label: "여성", src: adultWoman },
  { key: "middle_aged_man", label: "중년 남성", src: middleAgedMan },
  { key: "middle_aged_woman", label: "중년 여성", src: middleAgedWoman },
  { key: "old_age_man", label: "어르신 남성", src: oldAgeMan },
  { key: "old_age_woman", label: "어르신 여성", src: oldAgeWoman },
];

const props = defineProps({
  existingPerson: { type: Object, default: null },
});
const emit = defineEmits(["saved", "cancel"]);
const store = useAppStore();
const name = ref(props.existingPerson?.name ?? "");
const relationship = ref(props.existingPerson?.relation ?? "");
const profileImageKey = ref(
  props.existingPerson?.profileImageKey ?? "adult_man",
);
const saving = ref(false);
const formError = ref("");
const touched = ref({ name: false, relationship: false });
const canSave = computed(
  () => name.value.length > 0 && relationship.value.length > 0,
);
const fieldErrors = computed(() => ({
  name:
    touched.value.name && name.value.length === 0
      ? "이름을 입력해 주세요."
      : "",
  relationship:
    touched.value.relationship && relationship.value.length === 0
      ? "관계를 입력해 주세요."
      : "",
}));

async function handleSave() {
  if (!canSave.value || saving.value) {
    formError.value = "이름과 관계를 모두 확인해 주세요.";
    return;
  }
  saving.value = true;
  formError.value = "";
  try {
    const saved = await store.saveRegisteredPerson(
      {
        name: name.value,
        relationship: relationship.value,
        profileImageKey: profileImageKey.value,
      },
      props.existingPerson?.id ?? null,
    );
    emit("saved", saved.registeredPersonId);
  } catch (error) {
    formError.value =
      error instanceof ApiError
        ? error.message
        : "등록 정보를 저장하지 못했습니다. 다시 시도해 주세요.";
  } finally {
    saving.value = false;
  }
}
</script>
