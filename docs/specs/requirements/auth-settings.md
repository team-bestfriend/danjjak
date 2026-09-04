# Authentication, Consent, and Accessibility Settings

## Requirements

| ID | Requirement | Required behavior |
| --- | --- | --- |
| FR-001 | Kakao login | A user can authenticate through Kakao OAuth. |
| FR-002 | Seed-user association | On first login, the Kakao user identifier is associated with one unlinked seeded demo user. |
| FR-003 | Current-user inquiry | The authenticated user's ID, name, consent-completion state, consent choices, and accessibility settings can be retrieved. |
| FR-004 | Accessibility settings | Text size, guidance speed, and voice-guidance mode can be retrieved and changed. |
| FR-053 | Optional consent | Usage-recording and guardian-sharing choices are independent; the user can complete the choice step even after declining both. |

## End-to-end Flow

1. An unauthenticated user views the service introduction and moves to Kakao login.
2. After Kakao login succeeds, the server looks up the user by Kakao identifier.
3. If there is no existing association, the server binds the identifier to one seeded user that has no Kakao identifier.
4. The client retrieves the current user and checks whether consent choices have been completed.
5. If incomplete, the user independently accepts or declines usage recording and guardian sharing, then submits the choices.
6. After choice completion, the client applies the returned accessibility settings and opens shortcut home.
7. A returning authenticated user with completed choices skips unnecessary registration and opens home.

## Login Rules

- Do not provide phone-number login, SMS verification, password login, ordinary registration, or a login PIN.
- The displayed name comes from seeded user data; do not require name entry as part of login.
- The current-user contract has no phone number, so profile UI must not invent or hardcode one.
- Keep the Kakao access token only in the server session for the current login and never persist it in an application table.
- Distinguish OAuth cancellation from provider or server failure. Return to login and allow another attempt.
- If OAuth start, callback, logout, or session behavior is missing from OpenAPI, define that contract before implementing the integration.
- Disable the login control while login initiation is pending to prevent duplicate attempts.

## Seed-user Association Rules

- The association is stable: the same Kakao identifier must resolve to the same seeded user on later logins.
- Never move an already-associated Kakao identifier to a different seeded user during ordinary login.
- If no unlinked seeded user is available, return an explicit demo-capacity error instead of fabricating a new production-style registration flow.
- Do not expose the Kakao identifier or token in ordinary UI, analytics events, or logs.

## Consent Rules

- `usage recording` and `guardian sharing` are independent optional booleans.
- The four combinations of accept/decline are all valid, including declining both.
- Persist choice completion separately from the two choice values so a deliberate double-decline is not mistaken for an incomplete form.
- When usage recording is declined, do not create new pattern-execution or step-action records.
- When usage recording is declined or has no data, analysis UI states the real reason and must not present preview data as the user's history.
- When guardian sharing is declined, do not run analysis-sharing or guidance-sharing behavior intended for a guardian.
- The relationship between guardian-sharing consent and HIGH-risk Kakao demo notification must be explicit in the API contract; do not infer it in only one client or server layer.
- A failed consent save keeps the user's choices visible as unsaved values and does not open home as if persistence succeeded.

## Accessibility Rules

- Text size is one of `SMALL`, `NORMAL`, or `LARGE`.
- Guidance speed is one of `SLOW`, `NORMAL`, or `FAST`.
- Voice-guidance mode is one of `TTS` or `FAMILY`.
- Apply retrieved settings before or during initial home rendering so the UI does not remain in a conflicting default mode.
- After a successful update, apply the returned values to the current screen immediately.
- On save failure, restore the previous saved values or visibly mark the new values as unsaved.
- A text-size change must not clip or overlap primary information and actions in the approved demo viewport.
- Guidance speed controls the TTS request mapping. Voice mode controls the family-recording-versus-TTS playback priority defined in the guidance requirements.

## Completion Criteria

- FR-001: OAuth success, user cancellation, and provider/server failure are visibly distinguishable and retryable.
- FR-002: Repeated login by the same Kakao user never selects another seeded user.
- FR-003: Displayed user identity, consent state, and settings match the current-user response rather than hardcoded frontend data.
- FR-004: Saving each setting survives refetch and changes actual UI or TTS behavior.
- FR-053: All four consent combinations persist, and declining both still marks the choice step complete.
