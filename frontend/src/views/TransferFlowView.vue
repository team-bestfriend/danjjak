<template>
  <!-- 보낼 계좌 선택 -->
  <div
    v-if="flowStep === 'transfer-source'"
    class="flex h-full flex-col bg-[#FAFAF8]"
  >
    <SafeArea />

    <TopBar
      title="보낼 계좌 선택"
      :onBack="store.goBack"
      rightLabel="취소"
      :onRight="store.cancelTransfer"
    />

    <StepBar v-bind="stepBar('transfer-source')" />

    <div class="flex-1 space-y-4 overflow-y-auto px-4 pb-6 pt-4">
      <p class="text-[26px] font-bold text-[#111827]">
        어느 계좌에서 보낼까요?
      </p>

      <p
        v-if="store.transferError"
        class="rounded-xl bg-[#FEF2F2] p-3 text-[#991B1B]"
        role="alert"
      >
        {{ store.transferError }}
      </p>

      <p
        v-if="store.financeWarning"
        class="rounded-xl border border-[#FDE68A] bg-[#FFFBEB] p-3 text-[#92400E]"
        role="status"
      >
        {{ store.financeWarning }}
      </p>

      <p
        v-if="store.financeLoading"
        class="rounded-2xl bg-white p-5 text-[#6B7280]"
      >
        계좌를 불러오고 있어요…
      </p>

      <div
        v-else-if="store.financeError"
        class="space-y-3 rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-5"
      >
        <p class="text-[#991B1B]">
          {{ store.financeError }}
        </p>

        <Btn variant="secondary" @click="store.loadFinancialData(true)">
          다시 시도
        </Btn>
      </div>

      <div
        v-else-if="store.ownedAccounts.length === 0"
        class="space-y-2 rounded-2xl bg-white p-5 text-center"
      >
        <p class="text-[19px] font-bold text-[#111827]">
          사용할 수 있는 본인 계좌가 없어요.
        </p>

        <p class="text-[#6B7280]">계좌가 준비된 뒤 송금을 시작해 주세요.</p>
      </div>

      <div v-else class="space-y-3">
        <button
          v-for="account in store.ownedAccounts"
          :key="account.accountId"
          type="button"
          :data-guide-target="
            store.selectedSourceAccountId === account.accountId
              ? 'source-account-list'
              : null
          "
          data-guide-exempt
          :class="[
            'min-h-[128px] w-full rounded-[20px] border-2 bg-white p-5 text-left transition active:scale-[0.99]',
            store.selectedSourceAccountId === account.accountId
              ? 'border-[#FFBC00]'
              : 'border-[#E5E7EB]',
          ]"
          :aria-pressed="store.selectedSourceAccountId === account.accountId"
          :disabled="store.financeLoading || Boolean(store.financeError) || patternTargetMissing"
          @click="handleSelectSourceAccount(account.accountId)"
        >
          <div class="flex items-center justify-between gap-3">
            <!-- 은행 로고와 계좌 정보 -->
            <div class="flex min-w-0 items-center gap-3">
              <BankLogo :bank-name="account.bankName" size="medium" />

              <div class="min-w-0">
                <p class="truncate text-[19px] font-bold text-[#111827]">
                  {{ account.accountAlias || account.bankName }}
                </p>

                <p class="mt-1 truncate text-[#6B7280]">
                  {{ account.bankName }} · {{ account.masked }}
                </p>
              </div>
            </div>

            <span
              v-if="account.primary"
              class="shrink-0 rounded-full bg-[#FFF3CC] px-3 py-1 text-sm font-bold text-[#92650A]"
            >
              기본
            </span>
          </div>

          <p class="mt-4 break-keep text-[23px] font-black leading-relaxed text-[#111827]">
            잔액 {{ formatWonWithKorean(account.balance) }}
          </p>
        </button>
      </div>

      <div
        v-if="patternTargetMissing"
        class="space-y-3 rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-4"
      >
        <p class="font-bold text-[#991B1B]">
          단축번호에 연결된 받는 사람이나 계좌를 찾을 수 없어요.
        </p>

        <p class="text-[#991B1B]">
          사람 및 계좌 관리에서 연결 정보를 먼저 확인해 주세요.
        </p>

        <Btn
          data-guide-exempt
          variant="secondary"
          @click="store.navigate('contact-manage')"
        >
          사람 및 계좌 관리
        </Btn>
      </div>

    </div>
  </div>

  <!-- 수취 방식 선택 -->
  <div
    v-else-if="flowStep === 'direct-transfer'"
    class="flex flex-col h-full bg-[#FAFAF8]"
  >
    <SafeArea />
    <TopBar
      title="받는 방법 선택"
      :onBack="store.goBack"
      rightLabel="취소"
      :onRight="store.cancelTransfer"
    />
    <StepBar v-bind="stepBar('direct-transfer')" />
    <div class="flex-1 flex flex-col overflow-y-auto px-5 pt-8 pb-6 gap-5">
      <p class="font-bold text-[#111827] text-[28px]">누구에게 보내시겠어요?</p>
      <div class="rounded-[28px] p-2 flex flex-col gap-3 bg-white">
        <button
          @click="selectFamily"
          class="w-full rounded-[20px] bg-white border border-[#FFBC00] p-6 flex flex-col items-center gap-3 active:scale-[0.97] transition-all"
        >
          <img :src="familyImage" alt="" class="h-16 w-16 object-contain" />
          <p class="font-bold text-[#111827] text-[21px]">
            등록된 가족에게 보내기
          </p>
          <p class="text-[#6B7280] text-center text-[15px]">
            미리 등록해 둔 가족 계좌로 보내요.
          </p>
        </button>
        <button
          @click="store.navigate('direct-newaccount')"
          class="w-full rounded-[20px] bg-white border border-[#E5E7EB] p-6 flex flex-col items-center gap-3 active:scale-[0.97] transition-all"
        >
          <img :src="piggyBankImage" alt="" class="h-16 w-16 object-contain" />
          <p class="font-bold text-[#111827] text-[21px]">새 계좌로 보내기</p>
          <p class="text-[#6B7280] text-center text-[15px]">
            계좌 번호를 직접 입력해서 보내요.
          </p>
        </button>
      </div>
    </div>
  </div>

  <!-- 직접 수취 계좌 입력 -->
  <div
    v-else-if="flowStep === 'direct-newaccount'"
    class="flex flex-col h-full bg-[#FAFAF8]"
  >
    <SafeArea />
    <TopBar
      title="새 계좌로 송금"
      :onBack="store.goBack"
      rightLabel="취소"
      :onRight="store.cancelTransfer"
    />
    <StepBar v-bind="stepBar('direct-newaccount')" />
    <div class="flex-1 overflow-y-auto px-4 pt-5 pb-6 space-y-5">
      <p class="font-bold text-[#111827] text-[26px]">
        받는 계좌를 입력해 주세요.
      </p>
      <p class="rounded-[14px] bg-[#FFFBEB] p-4 text-[15px] text-[#92650A]">
        시연용 송금이에요. 계좌번호 형식을 확인한 뒤 시연 거래로 기록해요. 실제
        은행의 계좌 존재 여부는 확인하지 않아요.
      </p>
      <label class="block space-y-2">
        <span class="font-bold text-[#374151] text-[17px]">받는 분 이름</span>
        <input
          id="direct-recipient-name"
          v-model.trim="recipientName"
          :aria-invalid="Boolean(directFieldErrors.name)"
          aria-describedby="direct-recipient-name-error"
          @blur="directTouched.name = true"
          maxlength="100"
          placeholder="예: 박친구"
          class="w-full min-h-[58px] rounded-[16px] border-2 border-[#E5E7EB] focus:border-[#FFBC00] outline-none px-4 text-[18px] font-bold"
        />
        <p
          v-if="directFieldErrors.name"
          id="direct-recipient-name-error"
          class="text-[14px] text-[#B91C1C]"
          role="alert"
        >
          {{ directFieldErrors.name }}
        </p>
      </label>
      <div class="space-y-2">
        <p class="font-bold text-[#374151] text-[17px]">은행 선택</p>
        <button
          :aria-invalid="Boolean(directFieldErrors.bank)"
          aria-describedby="direct-recipient-bank-error"
          @click="
            directTouched.bank = true;
            showBanks = !showBanks;
          "
          :class="[
            'w-full rounded-[16px] border-2 px-4 text-left flex items-center justify-between min-h-[58px] text-[18px] font-bold',
            selectedBank
              ? 'border-[#FFBC00] text-[#111827]'
              : 'border-[#E5E7EB] text-[#9CA3AF]',
          ]"
        >
          <span>{{ selectedBank?.name || "은행 선택" }}</span>
          <Ic name="ChevR" />
        </button>
        <p
          v-if="directFieldErrors.bank"
          id="direct-recipient-bank-error"
          class="text-[14px] text-[#B91C1C]"
          role="alert"
        >
          {{ directFieldErrors.bank }}
        </p>
        <div v-if="showBanks" class="grid grid-cols-2 gap-2 pt-1">
          <button
            v-for="bankOption in BANKS"
            :key="bankOption.code"
            @click="
              bankCode = bankOption.code;
              showBanks = false;
            "
            :class="[
              'rounded-[12px] border-2 font-bold h-[52px]',
              bankCode === bankOption.code
                ? 'border-[#FFBC00] bg-[#FFFBEB] text-[#92650A]'
                : 'border-[#E5E7EB] text-[#374151]',
            ]"
          >
            {{ bankOption.name }}
          </button>
        </div>
      </div>
      <label class="block space-y-2">
        <span class="font-bold text-[#374151] text-[17px]">계좌 번호</span>
        <input
          id="direct-recipient-account"
          type="tel"
          :value="accountNumber"
          :aria-invalid="Boolean(directFieldErrors.account)"
          aria-describedby="direct-recipient-account-help direct-recipient-account-error"
          @input="accountNumber = $event.target.value.replace(/[^0-9-]/g, '')"
          @blur="directTouched.account = true"
          maxlength="50"
          placeholder="000-00-000000"
          inputmode="numeric"
          class="w-full min-h-[58px] rounded-[16px] border-2 border-[#E5E7EB] focus:border-[#FFBC00] outline-none px-4 text-[20px] font-bold"
        />
        <p
          id="direct-recipient-account-help"
          class="text-[14px] text-[#6B7280]"
        >
          숫자 8~20자리를 입력해 주세요. 숫자 사이에 하이픈(-)을 넣을 수 있어요.
        </p>
        <p
          v-if="directFieldErrors.account"
          id="direct-recipient-account-error"
          class="text-[14px] text-[#B91C1C]"
          role="alert"
        >
          {{ directFieldErrors.account }}
        </p>
      </label>
      <p
        v-if="directInputError"
        class="rounded-xl bg-[#FEF2F2] p-3 text-[#991B1B]"
      >
        {{ directInputError }}
      </p>
      <p
        v-else-if="store.transferError"
        class="rounded-xl bg-[#FEF2F2] p-3 text-[#991B1B]"
        role="alert"
      >
        {{ store.transferError }}
      </p>
      <div
        class="bg-[#FFF7ED] border border-[#FED7AA] rounded-[16px] p-4 flex items-start gap-2"
      >
        <Ic name="Warning" />
        <p class="text-[#92400E] flex-1 text-[15px]">
          입력한 계좌는 이번 송금에만 사용되고 등록 목록에는 저장되지 않아요.
        </p>
      </div>
      <Btn :disabled="!canProceedDirect" @click="proceedNewAccount">다음</Btn>
    </div>
  </div>

  <!-- 등록 수취인 선택 -->
  <div
    v-else-if="flowStep === 'guide-person'"
    class="flex flex-col h-full bg-[#FAFAF8]"
  >
    <SafeArea />
    <TopBar
      title="받는 사람 선택"
      :onBack="store.goBack"
      rightLabel="취소"
      :onRight="store.cancelTransfer"
    />
    <StepBar v-bind="stepBar('guide-person')" />
    <div class="flex-1 overflow-y-auto px-4 pt-4 pb-6 space-y-4">
      <p class="font-bold text-[#111827] text-[26px]">
        받는 사람을 선택해 주세요.
      </p>
      <p
        v-if="store.transferError"
        class="rounded-xl bg-[#FEF2F2] p-3 text-[#991B1B]"
        role="alert"
      >
        {{ store.transferError }}
      </p>
      <p
        v-if="store.financeLoading"
        class="rounded-2xl bg-white p-5 text-[#6B7280]"
      >
        등록된 사람을 불러오고 있어요…
      </p>
      <div
        v-else-if="store.financeError"
        class="rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-5 space-y-3"
      >
        <p class="text-[#991B1B]">{{ store.financeError }}</p>
        <Btn variant="secondary" @click="store.loadFinancialData(true)"
          >다시 시도</Btn
        >
      </div>
      <div
        v-else-if="store.people.length === 0"
        class="rounded-2xl bg-white p-5 text-center space-y-3"
      >
        <p class="font-bold text-[#111827]">등록된 사람이 없어요.</p>
      </div>
      <div v-else role="radiogroup" aria-label="받는 사람" class="space-y-3 rounded-[24px] p-2 bg-white">
        <label
          v-for="person in store.people"
          :key="person.id"
          :data-guide-target="
            person.id === store.activePattern?.personId
              ? 'registered-person-list'
              : null
          "
          class="block w-full cursor-pointer rounded-[20px] text-left focus-within:outline-2 focus-within:outline-[#2563EB]"
        >
          <Card class="p-5" :highlighted="store.selectedPersonId === person.id">
            <div class="flex items-center gap-4">
              <div
                class="w-14 h-14 shrink-0 overflow-hidden rounded-full border border-[#FFBC00] bg-white flex items-center justify-center text-[28px]"
              >
                <img
                  v-if="profileImageForPerson(person)"
                  :src="profileImageForPerson(person)"
                  alt=""
                  class="h-full w-full object-cover"
                  aria-hidden="true"
                />
                <span v-else>{{ person.emoji }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-bold text-[#111827] text-[21px]">
                  {{ person.name }}
                </p>
                <p class="text-[#6B7280] text-[15px]">
                  {{ person.relation }} · 수취 계좌
                  {{ getAccCount(person.id) }}개
                </p>
              </div>
              <input
                type="radio"
                name="transfer-person"
                :value="person.id"
                :checked="store.selectedPersonId === person.id"
                class="selection-radio"
                @click="handleSelectFamilyPerson(person.id)"
              />
            </div>
          </Card>
        </label>
      </div>
      <Btn
        v-if="!store.isPatternTransfer"
        variant="secondary"
        @click="store.navigate('direct-newaccount')"
      >새 계좌 직접 입력</Btn>
    </div>
  </div>

  <!-- 등록 수취 계좌 선택 -->
  <div
    v-else-if="flowStep === 'guide-account'"
    class="flex flex-col h-full bg-[#FAFAF8]"
  >
    <SafeArea />
    <TopBar
      title="받는 계좌 선택"
      :onBack="store.goBack"
      rightLabel="취소"
      :onRight="store.cancelTransfer"
    />
    <StepBar v-bind="stepBar('guide-account')" />
    <div class="flex-1 overflow-y-auto px-4 pt-4 pb-6 space-y-4">
      <p class="font-bold text-[#111827] text-[26px]">
        받는 계좌를 선택해 주세요.
      </p>
      <div
        v-if="personAccs.length"
        role="radiogroup"
        aria-label="받는 계좌"
        class="space-y-3 rounded-[24px] p-2 bg-white"
      >
        <label
          v-for="account in personAccs"
          :key="account.accountId"
          :data-guide-target="
            account.accountId === store.activePattern?.recipientAccountId
              ? 'recipient-account-list'
              : null
          "
          class="block w-full cursor-pointer rounded-[20px] text-left focus-within:outline-2 focus-within:outline-[#2563EB]"
        >
          <Card
            className="p-5"
            :highlighted="store.selectedRecipientAccountId === account.accountId"
          >
            <div class="flex items-center gap-4">
              <BankLogo
                :bank-name="account.bankName"
                size="medium"
              />
              <div class="min-w-0 flex-1 break-words">
                <p class="font-bold text-[#111827] text-[18px]">
                  {{ account.accountAlias || account.bankName }}
                </p>
                <p class="font-mono text-[#374151] text-[14px]">
                  {{ account.bankName }} · {{ account.masked }}
                </p>
              </div>
              <input
                type="radio"
                name="transfer-recipient-account"
                :value="account.accountId"
                :checked="store.selectedRecipientAccountId === account.accountId"
                class="selection-radio"
                @click="handleSelectAccount(account)"
              />
            </div>
          </Card>
        </label>
      </div>
      <div v-else class="rounded-2xl bg-white p-5 text-center space-y-3">
        <p class="font-bold text-[#111827]">등록된 수취 계좌가 없어요.</p>
        <Btn variant="secondary" @click="store.navigate('contact-manage')"
          >계좌 관리로 이동</Btn
        >
      </div>
    </div>
  </div>

  <!-- 금액 입력 -->
  <div
    v-else-if="flowStep === 'amount-input'"
    class="relative flex flex-col h-full bg-[#FAFAF8]"
  >
    <SafeArea />
    <TopBar
      title="얼마를 보낼까요?"
      :onBack="store.goBack"
      rightLabel="취소"
      :onRight="store.cancelTransfer"
    />
    <StepBar v-bind="stepBar('amount-input')" />
    <div class="flex-1 overflow-y-auto px-4 pt-4 pb-6 space-y-3">
      <p
        v-if="store.transferError"
        class="rounded-xl bg-[#FEF2F2] p-3 text-[#991B1B]"
        role="alert"
      >
        {{ store.transferError }}
      </p>
      <AmountKeypad
        data-guide-target="amount-keypad"
        role="group"
        aria-label="송금 금액 입력"
        :initialValue="store.transferAmount"
        @complete="handleAmountComplete"
      />
    </div>
  </div>

  <!-- 송금 내용 확인 -->
  <div
    v-else-if="flowStep === 'final-confirm'"
    class="flex flex-col h-full bg-[#FAFAF8]"
  >
    <SafeArea />
    <TopBar
      title="보내기 전에 확인해 주세요"
      :onBack="store.goBack"
      rightLabel="취소"
      :onRight="store.cancelTransfer"
    />
    <StepBar v-bind="stepBar('final-confirm')" />
    <div class="flex-1 overflow-y-auto px-4 pt-4 pb-6 space-y-4">
      <Card class="overflow-hidden">
        <div
          v-for="(row, index) in reviewRows"
          :key="row.label"
          :class="[
            'flex items-center justify-between gap-4 px-5 py-4',
            index < reviewRows.length - 1 ? 'border-b border-[#F3F4F6]' : '',
          ]"
        >
          <span class="shrink-0 whitespace-nowrap text-[#6B7280] text-[16px]">{{ row.label }}</span>
          <span
            v-if="row.account"
            class="flex min-w-0 flex-wrap justify-end gap-x-1 text-right text-[17px] font-bold text-[#111827]"
          >
            <span class="whitespace-nowrap">{{ row.account.nameAndBank }}</span>
            <span class="whitespace-nowrap">{{ row.account.number }}</span>
          </span>
          <span
            v-else
            :class="[
              'font-bold text-right text-[#111827]',
              row.emphasis ? 'text-[26px]' : 'text-[17px]',
            ]"
            >{{ row.value }}</span
          >
        </div>
      </Card>
      <Btn v-if="store.anomaly" @click="returnToWarning"
        >경고 화면으로 돌아가기</Btn
      >
      <Btn
        v-else
        data-guide-target="transfer-summary"
        @click="store.navigate('pin-entry')"
        >확인했어요</Btn
      >
      <Btn data-guide-exempt variant="secondary" @click="store.goBack"
        >내용 수정하기</Btn
      >
    </div>
  </div>

  <!-- PIN 입력 및 서버 송금 요청 -->
  <div
    v-else-if="flowStep === 'pin-entry'"
    class="relative flex flex-col h-full bg-[#FAFAF8]"
  >
    <SafeArea />
    <TopBar
      title="계좌 비밀번호 입력"
      :onBack="goBackFromPin"
      :backDisabled="store.transferSubmitting"
      rightLabel="취소"
      :onRight="store.cancelTransfer"
      :rightDisabled="store.transferSubmitting"
    />
    <StepBar v-bind="stepBar('pin-entry')" />
    <div class="flex-1 overflow-y-auto px-4 pt-5 pb-6 space-y-5">
      <h2 class="font-bold text-[#111827] text-[26px]">
        계좌 비밀번호를 입력해주세요.
      </h2>
      <div
        v-if="!store.transferError && !store.transferSubmitting"
        class="bg-[#FFFBEB] border border-[#FFBC00] rounded-2xl px-4 py-3 flex items-center gap-2 text-[#92650A]"
      >
        <Ic name="Shield" />
        <p class="font-bold text-[17px]">비밀번호는 저장되지 않아요.</p>
      </div>
      <div
        v-else-if="store.transferError"
        class="flex items-center gap-3 rounded-xl border border-[#FCA5A5] bg-[#FEF2F2] p-4 text-[#991B1B]"
        role="alert"
      >
        <img
          :src="warningIcon"
          alt=""
          class="h-10 w-10 flex-shrink-0"
          aria-hidden="true"
        />
        <p class="text-[18px] font-bold">계좌 비밀번호를 다시 입력해주세요.</p>
      </div>
      <p
        v-if="store.transferSubmitting"
        class="text-center font-bold text-[#6B7280]"
      >
        안전하게 확인하고 있어요…
      </p>
      <PinEntry
        data-guide-target="pin-keypad"
        role="group"
        aria-label="계좌 비밀번호 입력"
        :disabled="store.transferSubmitting"
        @complete="handlePinComplete"
      />
    </div>
  </div>

  <!-- 송금 전 확인과 가족 도움 요청 -->
  <div
    v-else-if="flowStep === 'fraud-warning'"
    class="flex flex-col h-full bg-[#FAFAF8] break-keep"
  >
    <SafeArea />
    <TopBar
      title="송금 전 확인"
      rightLabel="취소"
      :onRight="cancelAnomaly"
      :rightDisabled="store.anomalyResolving"
    />
    <div class="flex-1 overflow-y-auto px-4 pt-4 pb-6 space-y-4">
      <template v-if="store.anomaly">
        <section
          aria-labelledby="fraud-warning-title"
          class="rounded-[28px] border border-[#EF9A9A] bg-[#FDE8E7] px-4 py-6"
        >
          <div class="flex flex-col items-center gap-4">
            <img
              :src="warningIcon"
              alt=""
              aria-hidden="true"
              class="h-32 w-32 object-contain"
            />
            <span
              class="rounded-full bg-[#7F1D1D] px-5 py-2 text-[20px] text-white font-black"
              >{{ riskLabel }}</span
            >
            <h1
              id="fraud-warning-title"
              class="text-[#111827] text-center text-[26px] font-bold leading-snug"
            >
              돈을 보내기 전에<br />한 번 더 확인해 주세요.
            </h1>
          </div>
        </section>
        <div class="relative pb-20">
          <div class="fraud-reason-bubble relative mr-4 rounded-[24px] border border-[#FED7AA] bg-[#FFF7ED] p-5 space-y-3">
            <h2 class="font-bold text-[#9A3412] text-[22px]">누군가 돈을 보내라고 했나요?</h2>
            <p class="text-[#9A3412] text-[20px] leading-relaxed">
              전화나 문자로 돈을 보내라고 하거나, 빨리 보내라고 재촉했다면 사기일 수 있어요.
            </p>
            <p class="font-bold text-[#9A3412] text-[20px] leading-relaxed">
              잠깐 멈추고 가족과 함께 확인해 주세요.
            </p>
          </div>
          <img
            :src="warningDanjjak"
            alt=""
            aria-hidden="true"
            class="absolute bottom-0 right-0 h-24 w-24 object-contain"
          />
        </div>
        <Card class="p-5 space-y-2">
          <section class="mb-4 border-b border-[#E5E7EB] pb-4 space-y-2">
            <h2 class="font-bold text-[#374151] text-[18px]">이번 송금에서 확인할 점</h2>
            <ul class="list-disc pl-5 space-y-2 text-[#4B5563] text-[18px] leading-relaxed">
              <li v-for="reason in fraudReasons" :key="reason">{{ reason }}</li>
            </ul>
          </section>
          <h2 class="font-bold text-[#374151] text-[20px]">받는 분과 보낼 금액</h2>
          <p class="font-bold text-[#111827] text-[20px]">
            {{ store.anomaly?.recipient?.name }}
          </p>
          <p class="text-[#6B7280] text-[18px] break-words">
            {{ store.anomaly?.recipient?.bankName }} ·
            {{ store.anomaly?.recipient?.masked }}
          </p>
          <p class="break-keep font-black text-[#111827] text-[28px]">
            {{ formatWonWithKorean(store.anomaly?.amount) }}
          </p>
        </Card>
        <p
          v-if="store.transferError"
          class="rounded-xl bg-[#FEF2F2] p-3 text-[#991B1B]"
          role="alert"
        >
          {{ store.transferError }}
        </p>
        <section class="rounded-[20px] border border-[#E5E7EB] bg-white p-4 space-y-3">
          <h2 class="font-bold text-[#111827] text-[20px]">걱정되면 가족과 확인해 보세요</h2>
          <Btn
            v-if="store.anomaly?.riskLevel === 'HIGH' && guardianShareAgreed"
            variant="danger"
            style="background-color: #fde8e7; color: #7f1d1d;"
            :class="{
              'guardian-notification-glow': !store.notificationSending && !store.anomalyResolving && !store.notificationResult,
            }"
            :disabled="
              store.notificationSending || store.anomalyResolving || Boolean(store.notificationResult)
            "
            @click="notifyGuardian"
            >{{ notificationButtonLabel }}</Btn
          >
          <div
            v-else-if="store.anomaly?.riskLevel === 'HIGH'"
            class="rounded-2xl border border-[#FCD34D] bg-[#FFFBEB] p-4 space-y-3"
          >
            <p class="font-bold text-[#92400E] text-[18px]">
              알림을 보내려면 먼저 가족에게 정보를 알려주는 데 동의해 주세요.
            </p>
            <Btn
              variant="secondary"
              @click="store.navigate('consent', { query: { edit: '1' } })"
            >
              동의 설정하기
            </Btn>
          </div>
          <div
            v-if="store.notificationResult"
            class="rounded-2xl bg-[#EFF6FF] p-4 text-[#1E3A8A] text-[18px]"
            role="status"
          >
            <p class="font-bold">{{ notificationTitle }}</p>
            <p v-if="store.notificationResult.result !== 'SENT'" class="mt-2">
              도움이 필요하면 가족에게 전화해 주세요.
            </p>
            <p
              v-if="store.notificationResult.result === 'SENT' && store.notificationResult.sentAt"
              class="mt-2"
            >
              보낸 시간 {{ formatDate(store.notificationResult.sentAt) }}
            </p>
          </div>
          <p
            v-if="store.supportLoading"
            class="rounded-2xl bg-white p-4 text-center text-[#6B7280]"
          >
            가족 전화번호를 불러오고 있어요…
          </p>
          <div
            v-else-if="store.supportError"
            class="rounded-2xl border border-[#FCA5A5] bg-[#FEF2F2] p-4 space-y-3"
          >
            <p class="text-[#991B1B]" role="alert">{{ store.supportError }}</p>
            <Btn variant="secondary" @click="store.loadSupport(true)"
              >번호 다시 불러오기</Btn
            >
          </div>
          <a
            v-else-if="guardianPhone"
            :href="'tel:' + guardianPhone"
            class="flex min-h-[60px] w-full flex-col items-center justify-center gap-1 rounded-[18px] border border-[#D1D5DB] px-3 py-3 text-[18px] font-bold text-[#374151]"
          >
            <span>가족에게 전화하기</span>
            <span>{{ guardianPhone }}</span>
          </a>
          <div
            v-else
            class="rounded-2xl border border-[#E5E7EB] bg-white p-4 space-y-3 text-center"
          >
            <p class="font-bold text-[#6B7280]">
              가족 전화번호가 등록되지 않았어요.
            </p>
            <Btn variant="secondary" @click="store.navigate('settings')"
              >가족 전화번호 등록하기</Btn
            >
          </div>
        </section>
        <section class="border-t border-[#E5E7EB] pt-5 space-y-3">
          <h2 class="font-bold text-[#111827] text-[20px]">어떻게 하시겠어요?</h2>
          <Btn
            variant="secondary"
            :disabled="store.anomalyResolving"
            @click="recheckTransfer"
            >다시 확인하기</Btn
          >
          <Btn :disabled="store.anomalyResolving" @click="continueTransfer">
            {{ store.anomalyResolving ? "처리 중…" : "계속 보내기" }}
          </Btn>
          <Btn
            variant="secondary"
            :disabled="store.anomalyResolving"
            @click="cancelAnomaly"
          >
            보내지 않기
          </Btn>
        </section>
      </template>
      <template v-else>
        <div class="rounded-2xl bg-white p-5 text-center space-y-3">
          <p class="font-bold text-[#111827] text-[22px]">
            확인할 송금 정보가 없어요.
          </p>
          <p class="text-[#6B7280]">
            새 송금을 시작하거나 거래내역을 확인해 주세요.
          </p>
          <Btn @click="finishToHome">홈으로 돌아가기</Btn>
        </div>
      </template>
    </div>
  </div>

  <!-- 완료 -->
  <div
    v-else-if="flowStep === 'complete'"
    class="flex flex-col h-full items-center justify-center px-6 gap-5 bg-[#FAFAF8]"
  >
    <SafeArea />
    <template v-if="store.transferResult">
      <div
        class="w-28 h-28 rounded-full flex items-center justify-center bg-[#22C55E]"
      >
        <Ic name="Check" />
      </div>
      <div class="text-center space-y-2">
        <p class="font-bold text-[#111827] text-[28px]">송금이 완료됐어요!</p>
        <p class="text-[#374151] text-[18px]">
          {{ store.transferResult.recipientName }}님에게
        </p>
        <p class="break-keep font-black leading-relaxed text-[#111827] text-[32px]">
          {{ formatWonWithKorean(store.transferResult.amount) }}
        </p>
      </div>
      <Card class="w-full p-5 space-y-2">

        <div class="flex justify-between gap-3">
          <span class="shrink-0 whitespace-nowrap text-[#6B7280]">송금 후 잔액</span
          ><strong class="whitespace-nowrap text-right">{{ formatWonWithKorean(store.transferResult.balanceAfter) }}</strong>
        </div>
      </Card>
      <div
        v-if="store.postTransferSyncError"
        class="w-full rounded-2xl border border-[#FDE68A] bg-[#FFFBEB] p-4 space-y-3"
      >
        <p class="text-[#92400E]">{{ store.postTransferSyncError }}</p>
        <Btn variant="secondary" @click="store.refreshAfterTransfer"
          >최신 정보 다시 불러오기</Btn
        >
      </div>
      <Btn @click="goToHistory">거래내역에서 확인</Btn>
      <Btn variant="secondary" @click="finishToHome">홈으로 돌아가기</Btn>
    </template>
    <template v-else>
      <p class="font-bold text-[#111827] text-[25px] text-center">
        확인할 송금 완료 정보가 없어요.
      </p>
      <p class="text-[#6B7280] text-center">
        새 송금을 시작하거나 거래내역을 조회해 주세요.
      </p>
      <Btn @click="finishToHome">홈으로 돌아가기</Btn>
    </template>
  </div>

  <!-- 취소 결과 -->
  <div
    v-else-if="flowStep === 'cancelled'"
    class="flex flex-col h-full items-center justify-center px-6 gap-6 bg-[#FAFAF8]"
  >
    <SafeArea />
    <template v-if="store.transferCancelled">
      <div
        class="w-24 h-24 rounded-full flex items-center justify-center bg-[#E5E7EB]"
      >
        <Ic name="Check" />
      </div>
      <div class="text-center space-y-2">
        <p class="font-bold text-[#111827] text-[27px]">송금을 취소했어요.</p>
        <p class="text-[#6B7280] text-[17px]">
          잔액과 거래내역은 바뀌지 않았어요.
        </p>
      </div>
    </template>
    <template v-else>
      <p class="font-bold text-[#111827] text-[25px] text-center">
        확인할 송금 취소 정보가 없어요.
      </p>
      <p class="text-[#6B7280] text-center">
        새 송금을 시작하거나 홈으로 돌아가 주세요.
      </p>
    </template>
    <Btn @click="finishToHome">홈으로 돌아가기</Btn>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useAppStore } from "../stores/appStore";
import { BANKS } from "../constants/banks";
import { directAccountPattern } from "../features/directRecipient.js";
import SafeArea from "../components/common/SafeArea.vue";
import TopBar from "../components/common/TopBar.vue";
import StepBar from "../components/common/StepBar.vue";
import { transferStepBar as stepBar } from "../features/transferSteps.js";
import Card from "../components/common/Card.vue";
import Btn from "../components/common/Btn.vue";
import Ic from "../components/common/Ic.vue";
import AmountKeypad from "../components/common/AmountKeypad.vue";
import PinEntry from "../components/common/PinEntry.vue";
import BankLogo from "../components/common/BankLogo.vue";
import warningIcon from "../assets/icons/warning.png";
import { formatWonWithKorean } from "../utils/money.js";
import warningDanjjak from "../assets/danjjakee_warning.png";
import familyImage from "../assets/family.png";
import piggyBankImage from "../assets/piggy_bank.png";
import { profileImageForPerson } from "../constants/profileImages.js";

const props = defineProps({
  flowStep: { type: String, required: true },
});

const store = useAppStore();
const recipientName = ref(store.directRecipient?.name ?? "");
const bankCode = ref(store.directRecipient?.bankCode ?? "");
const accountNumber = ref(store.directRecipient?.accountNumber ?? "");
const showBanks = ref(false);
const directInputError = ref("");
const directTouched = ref({ name: false, bank: false, account: false });

const selectedBank = computed(
  () => BANKS.find((bank) => bank.code === bankCode.value) ?? null,
);
const personAccs = computed(
  () => store.accountsByPerson[store.selectedPersonId] ?? [],
);
const patternTargetMissing = computed(
  () =>
    store.isPatternTransfer &&
    store.financeLoaded &&
    (!store.selectedPerson || !store.selectedRecipientAccount),
);
const canProceedDirect = computed(
  () =>
    recipientName.value.length > 0 &&
    Boolean(selectedBank.value) &&
    directAccountPattern.test(accountNumber.value),
);
const directFieldErrors = computed(() => ({
  name:
    directTouched.value.name && recipientName.value.length === 0
      ? "받는 분 이름을 입력해 주세요."
      : "",
  bank:
    directTouched.value.bank && !selectedBank.value
      ? "은행을 선택해 주세요."
      : "",
  account:
    directTouched.value.account &&
    !directAccountPattern.test(accountNumber.value)
      ? "숫자 8~20자리인지 확인해 주세요. 하이픈은 숫자 사이에 하나씩 넣어 주세요."
      : "",
}));
const guardianPhone = computed(
  () => store.support?.guardian?.phoneNumber ?? "",
);
const guardianShareAgreed = computed(() =>
  Boolean(store.currentUser?.consents?.guardianShareAgreed),
);
const riskLabel = computed(() =>
  store.anomaly?.riskLevel === "HIGH" ? "높은 주의" : "주의",
);
const notificationTitle = computed(() => {
  const titles = {
    SENT: "카카오톡 알림을 보냈어요.",
    MOCKED_NO_TOKEN: "카카오톡 알림을 보내지 못했어요.",
    MOCKED_AFTER_ACTUAL_FAILURE: "카카오톡 알림을 보내지 못했어요.",
  };
  return titles[store.notificationResult?.result] ?? "알림을 보냈는지 확인하지 못했어요.";
});
const notificationButtonLabel = computed(() => {
  if (store.notificationSending) return "알림을 보내고 있어요…";
  return "가족에게 알림 보내기";
});
const fraudReasons = computed(() =>
  (store.anomaly?.reasons ?? [])
    .map((reason) => {
      if (reason === "HIGH_AMOUNT") return "1,000만원 이상 보내려고 해요.";
      if (reason === "REPEATED_TRANSFER") {
        return (
          "최근 10분 동안 " +
          store.anomaly.recentTransferCount +
          "번 보냈어요."
        );
      }
      return null;
    })
    .filter(Boolean),
);
const reviewRows = computed(() => {
  const recipient = store.isNewAccountFlow
    ? store.directRecipient
    : {
        name: store.selectedPerson?.name,
        accountAlias: store.selectedRecipientAccount?.accountAlias,
        bankName: store.selectedRecipientAccount?.bankName,
        masked: store.selectedRecipientAccount?.masked,
      };
  return [
    {
      label: "출금 계좌",
      value:
        store.selectedSourceAccount?.accountAlias ||
        store.selectedSourceAccount?.bankName ||
        "-",
    },
    { label: "받는 사람", value: recipient?.name || "-" },
    {
      label: "받는 계좌",
      account: {
        nameAndBank:
          [recipient?.accountAlias, recipient?.bankName]
            .filter(Boolean)
            .join(" · ") || "-",
        number: recipient?.masked || store.selectedAccountMasked || "-",
      },
    },
    {
      label: "금액",
      value: formatWonWithKorean(Number(store.transferAmount)),
      emphasis: true,
    },
    { label: "수수료", value: "0원" },
  ];
});

onMounted(async () => {
  if (
    ["transfer-source", "guide-person", "guide-account"].includes(
      props.flowStep,
    )
  ) {
    await store.loadFinancialData();
    if (
      props.flowStep === "transfer-source" &&
      !store.selectedSourceAccountId
    ) {
      store.selectedSourceAccountId =
        store.defaultOwnedAccount?.accountId ?? null;
    }
  }
  if (props.flowStep === "fraud-warning") await store.loadSupport();
});

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "시각 확인 불가";
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function handleSelectSourceAccount(accountId) {
  if (store.financeLoading || store.financeError || patternTargetMissing.value) return;
  if (!store.ownedAccounts.some((account) => account.accountId === accountId)) return;
  store.selectedSourceAccountId = accountId;
  store.transferError = "";
  return store.navigate("guide-person");
}

function selectFamily() {
  store.isNewAccountFlow = false;
  store.navigate("guide-person");
}

function proceedNewAccount() {
  directInputError.value = "";
  store.transferError = "";
  if (!canProceedDirect.value) {
    directInputError.value = "이름, 은행, 계좌번호를 모두 확인해 주세요.";
    store.recordPatternAction("wrongTouch");
    return;
  }
  store.setDirectRecipient({
    name: recipientName.value,
    bankCode: selectedBank.value.code,
    bankName: selectedBank.value.name,
    accountNumber: accountNumber.value,
  });
  store.navigate("amount-input");
}

function getAccCount(personId) {
  return store.accountsByPerson[personId]?.length ?? 0;
}

function handleSelectFamilyPerson(personId) {
  store.transferError = "";
  store.isNewAccountFlow = false;
  // 같은 사람을 다시 누르면 이전에 고른 받는 계좌를 유지한다.
  if (store.selectedPersonId !== personId) store.selectPerson(personId);
  // 받는 계좌가 하나뿐이어도 확인 화면을 생략하지 않는다.
  store.navigate("guide-account");
}

function handleSelectAccount(account) {
  store.selectRecipientAccount(account);
  store.navigate("amount-input");
}

function handleAmountComplete(amount) {
  store.transferError = "";
  store.transferAmount = amount;
  store.navigate("final-confirm");
}

async function handlePinComplete(pin) {
  try {
    const response = await store.submitTransfer(pin);
    if (response?.status === "COMPLETED") store.navigate("complete");
    if (response?.status === "REQUIRES_REVIEW") store.navigate("fraud-warning");
  } catch (error) {
    if (error?.code === "INSUFFICIENT_BALANCE")
      replaceTransferStep("amount-input");
    if (error?.code === "SOURCE_ACCOUNT_NOT_FOUND") {
      await store.loadFinancialData(true);
      replaceTransferStep("transfer-source");
    }
    if (error?.code === "RECIPIENT_ACCOUNT_NOT_FOUND") {
      await store.loadFinancialData(true);
      replaceTransferStep("guide-person");
    }
    if (error?.code === "INVALID_RECIPIENT") {
      replaceTransferStep(
        store.isNewAccountFlow ? "direct-newaccount" : "guide-person",
      );
    }
  }
}

function goBackFromPin() {
  store.transferError = '';
  store.goBack();
}

function replaceTransferStep(step) {
  store.navigate(step, { replace: true });
}

function recheckTransfer() {
  store.anomalyRechecked = true;
  store.navigate("final-confirm");
}

function returnToWarning() {
  store.navigate("fraud-warning");
}

async function continueTransfer() {
  try {
    const response = await store.resolveAnomaly("CONTINUE");
    if (response?.action === "CONTINUE") store.navigate("complete");
    if (response?.action === "CANCEL") store.navigate("cancelled");
  } catch {
    // 처리 실패 시 같은 이상거래 ID를 보존해 재시도할 수 있다.
  }
}

async function cancelAnomaly() {
  try {
    const response = await store.resolveAnomaly("CANCEL");
    if (response?.action === "CONTINUE") store.navigate("complete");
    if (response?.action === "CANCEL") store.navigate("cancelled");
  } catch {
    // 처리 실패 시 경고 화면에 머문다.
  }
}

async function notifyGuardian() {
  try {
    await store.sendGuardianNotification();
  } catch {
    // 알림 실패는 사용자의 계속·취소 결정을 막지 않는다.
  }
}

async function goToHistory() {
  store.startTransfer();
  // 거래내역에서 뒤로 가면 끝난 송금 단계 대신 홈으로 돌아가도록 한다.
  await store.navigate("home", { replace: true });
  await store.navigate("task-5");
}

function finishToHome() {
  store.cancelTransfer();
}
</script>

<style scoped>
.selection-radio {
  appearance: none;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border: 2px solid #9ca3af;
  border-radius: 50%;
  background-color: white;
  cursor: pointer;
}

.selection-radio:checked {
  border-color: #ffbc00;
  background: radial-gradient(circle, white 0 5px, #ffbc00 6px);
}

.guardian-notification-glow {
  animation: guardian-notification-ring 1.5s ease-in-out infinite;
  border-radius: 24px;
}

@keyframes guardian-notification-ring {
  0%, 100% {
    box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.75), 0 0 0 4px rgba(220, 38, 38, 0.3);
  }
  50% {
    box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.75), 0 0 0 10px rgba(220, 38, 38, 0), 0 0 22px 6px rgba(220, 38, 38, 0.35);
  }
}

.fraud-reason-bubble::after {
  content: "";
  position: absolute;
  right: 36px;
  bottom: -10px;
  width: 18px;
  height: 18px;
  background: #fff7ed;
  border-right: 1px solid #fed7aa;
  border-bottom: 1px solid #fed7aa;
  transform: rotate(45deg);
}
</style>
