const HOME_TUTORIAL_PENDING_KEY = "danjjak:home-tutorial-pending";

const HOME_TUTORIAL_COMPLETED_KEY = "danjjak:home-tutorial-completed";

export function markHomeTutorialPending() {
  if (typeof window === "undefined") return;

  localStorage.setItem(HOME_TUTORIAL_PENDING_KEY, "true");

  localStorage.removeItem(HOME_TUTORIAL_COMPLETED_KEY);
}

export function shouldShowHomeTutorial() {
  if (typeof window === "undefined") return false;

  const pending = localStorage.getItem(HOME_TUTORIAL_PENDING_KEY) === "true";

  const completed =
    localStorage.getItem(HOME_TUTORIAL_COMPLETED_KEY) === "true";

  return pending && !completed;
}

export function completeHomeTutorial() {
  if (typeof window === "undefined") return;

  localStorage.removeItem(HOME_TUTORIAL_PENDING_KEY);

  localStorage.setItem(HOME_TUTORIAL_COMPLETED_KEY, "true");
}
