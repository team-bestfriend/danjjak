const FIRST_START_FLOW_KEY = "danjjakPrivacyConsent";

/**
 * 현재 사용자가 '처음 시작하기' 흐름을 진행 중인지 확인합니다.
 */
export function isFirstStartFlow() {
  if (typeof window === "undefined") {
    return false;
  }

  return sessionStorage.getItem(FIRST_START_FLOW_KEY) !== null;
}

/**
 * 최초 시작 흐름을 종료합니다.
 *
 * 이 값을 제거해야 다음 로그인부터는
 * 닉네임 설정, 카카오 알림 동의, 보호자 설정 화면을
 * 다시 거치지 않고 기존 사용자 흐름으로 이동합니다.
 */
export function finishFirstStartFlow() {
  if (typeof window === "undefined") {
    return;
  }

  sessionStorage.removeItem(FIRST_START_FLOW_KEY);
}
