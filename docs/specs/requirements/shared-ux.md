# Shared User Experience

These requirements apply to every relevant screen and implementation Issue.

## Requirements

| ID | Requirement | Behavior-based acceptance criteria |
| --- | --- | --- |
| UX-001 | Single routing authority | Vue Router controls rendered routes and URL navigation. Do not maintain a second screen-name switch that duplicates routing state. |
| UX-002 | Direct entry and refresh | Public entry screens and authenticated core screens do not become blank or incorrect after direct URL entry or refresh. Required data is fetched again. |
| UX-003 | Back navigation | In-app and browser back actions return to the actual previous step. Forms account for possible loss of entered transfer or configuration data. |
| UX-004 | Loading feedback | A pending request shows progress in the affected area or control and prevents an identical concurrent submission. |
| UX-005 | Error recovery | A failed inquiry explains the problem and offers retry. A failed save or transfer retains safe user input when possible. |
| UX-006 | Empty state | Empty accounts, people, patterns, transactions, or analytics show the reason and an available next action instead of an unexplained blank area. |
| UX-007 | Input validation | Required values, formats, and ranges are validated, with plain Korean guidance near the invalid field. |
| UX-008 | Duplicate submission prevention | Login, save, transfer, anomaly decision, and notification controls are disabled while their request is pending. |
| UX-009 | State reset | Transfer completion, cancellation, or starting a new transfer clears the previous recipient, amount, PIN, risk result, and transient input. |
| UX-010 | Modal and bottom-sheet behavior | Outside click, close, cancel, and browser back behavior is consistent. Background controls cannot be activated accidentally while an overlay is open. |
| UX-011 | Operability | Primary controls and inputs normally provide at least a 48 by 48 CSS-pixel touch target and sufficient spacing. Selected, disabled, and error states are not conveyed by color alone. |
| UX-012 | Persistent guidance | Captions and the primary action remain usable when audio is disabled or playback fails. |
| UX-013 | No false affordances | An unimplemented call, save, edit, logout, or other primary action is not presented as an active control; disable it or label it as unavailable. |
| UX-014 | Server state wins | After a successful save or transfer, use identifiers, state, and balance from the server response instead of displaying assumed success values. |

## Required Screen States

Every data-backed screen distinguishes the states that apply to it.

| State | Required presentation |
| --- | --- |
| Initial loading | Visible progress and duplicate-request prevention |
| Success | Data from the server or an explicitly documented mock fallback |
| Empty result | Reason for the empty state and a registration, navigation, or retry action when available |
| Validation failure | Field-level explanation and correction guidance |
| Server failure | Understandable message and retry or safe exit |
| Missing session or authorization | Redirect to login or explain how to restore the demo session |
| Submission in progress | Locked submit control and visible progress |
| Submission success | Result confirmation followed by a valid next route |

## Screen and Route Coverage

Route names may evolve, but one feature must not be controlled by both Vue Router and a parallel screen-state router.

| Area | Required screen | Entry condition | Primary exits |
| --- | --- | --- | --- |
| Public | Service introduction | Unauthenticated initial entry | Kakao login |
| Public | Kakao login | Unauthenticated user | OAuth or introduction |
| Initial setup | Consent choices | Authenticated, choices incomplete | Home |
| Shared | Shortcut home | Authenticated, choices complete | Pattern, analysis, settings, direct transfer |
| Pattern | List and detail | Bottom navigation or home | Create, edit, home |
| Pattern | Create and edit | Empty slot or pattern detail | Detail, list, home |
| Transfer | Source account | Direct transfer or transfer-pattern start | Recipient choice or cancel |
| Transfer | Registered recipient | Registered-recipient path | Amount or back |
| Transfer | Direct recipient form | New-account path | Amount or back |
| Transfer | Amount | Recipient confirmed | Review or back |
| Transfer | Review | Amount confirmed | PIN or edit any value |
| Transfer | PIN | Review confirmed | Submit or back |
| Transfer | Anomaly warning | Server returns `REQUIRES_REVIEW` | Recheck, continue, cancel, and HIGH notification |
| Transfer | Completion or cancellation result | Server decision completed | Home or transaction history |
| Inquiry | Balance, history, or category result | Shortcut execution | Complete or home |
| Settings | Accessibility and guardian | Bottom navigation | Home or contact management |
| Analysis | Usage analysis | Bottom navigation | Instruction improvement or settings |

## Authentication and Refresh

- Opening an authenticated route without a session redirects to login.
- Opening the login route with a valid session checks the current user and consent state before redirecting to the correct screen.
- Refresh during transfer never restores a PIN. If safe transient transfer restoration is unavailable, return to transfer start with a clear explanation.
- A completion URL opened directly must not fabricate a successful transaction.
- An unknown route shows a not-found state with a safe navigation action instead of silently hiding the problem by redirecting unconditionally.

## Common Error Behavior

| Error type | UI behavior | Data behavior |
| --- | --- | --- |
| Invalid or missing request field | Identify the field and correction | Do not enter saved or completed state |
| Missing resource | Explain deletion, deactivation, or stale state; refresh or navigate safely | Remove stale local detail data |
| Shortcut collision or already-processed anomaly | Explain the conflict and offer refresh, reselection, or existing result | Do not overwrite or repeat the operation |
| PIN mismatch or insufficient balance | Distinguish the cause and return to PIN or amount editing | Clear PIN; do not change balance or transactions |
| Missing session | Explain expiration and move to Kakao login | Clear PIN and transient transfer state |
| TTS provider failure | Keep captions and offer retry or silent continuation | Do not fail the financial task |
| Kakao delivery failure | Distinguish actual failure from mock fallback | Do not block continue or cancel decisions |
| Network loss | Explain connectivity and offer retry | Do not confirm an unverified server mutation as successful |
