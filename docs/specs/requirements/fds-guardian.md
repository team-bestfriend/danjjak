# Rule-based FDS and Guardian Response

## Requirements

| ID | Requirement | Required behavior |
| --- | --- | --- |
| FR-035 | FDS coverage | Apply server FDS to both shortcut-based and direct transfers. |
| FR-036 | High-amount detection | Treat an amount of KRW 10,000,000 or more as high value. |
| FR-037 | Repetition detection | Treat the current attempt as repeated when at least two completed outgoing transfers exist in the previous ten minutes. |
| FR-038 | Risk level | No rule is `NORMAL`, one rule is `MEDIUM`, and both rules are `HIGH`. |
| FR-039 | Warning presentation | Clearly show risk, every triggered reason, recipient, and amount. |
| FR-040 | User decision | Allow recheck, continue, or cancel; offer guardian notification only for HIGH. |
| FR-041 | Anomaly record | Persist exactly one anomaly record per anomalous transfer attempt. |
| FR-042 | Kakao notification | Attempt Kakao notification only after an explicit choice on HIGH and show actual or mock result. |
| FR-043 | Guardian call | Open a call link to the saved guardian number from the warning flow. |

## Server Decision Rules

- The frontend never calculates or upgrades risk. It renders the server's `riskLevel`, `reasons`, and `recentTransferCount`.
- `HIGH_AMOUNT` triggers when amount is greater than or equal to KRW 10,000,000; the boundary value is included.
- `REPEATED_TRANSFER` triggers when two completed `TRANSFER_OUT` transactions exist in the preceding ten-minute window, excluding the current attempt.
- Failed, cancelled, or unresolved anomaly attempts do not count toward repetition.
- A new recipient, recipient registration state, route deviation, elapsed UI time, wrong touches, or analysis score is not an FDS condition.
- No triggered rule means `NORMAL` and creates no anomaly record.
- One triggered rule means `MEDIUM`; both triggered rules mean `HIGH`.
- Even when both rules trigger, create one anomaly record containing both reasons.

## Transfer-response Behavior

- A `NORMAL` result completes through the mock-transfer transaction and returns `COMPLETED`.
- `MEDIUM` or `HIGH` returns `REQUIRES_REVIEW` before the transfer is committed.
- The response contains an anomaly ID stable across recheck, continue, cancel, and notification requests.
- Repeating the initial transfer submission while a review already exists must not create another anomaly for the same active attempt.

## Warning Screen

- Distinguish MEDIUM and HIGH through explicit text or badges, not color alone.
- Translate every server reason into plain Korean while preserving its server meaning.
- Show recipient name, bank/account summary, and amount so the user knows which transaction is under review.
- Provide `Review transfer details`, `Continue transfer`, and `Cancel transfer` actions.
- Show `Notify guardian` only for HIGH.
- Family guidance may play, but it cannot hide system risk reasons or represent transaction approval.
- Disable each mutation control while its request is pending.

## Recheck, Continue, and Cancel

- Recheck returns to transfer review while preserving the anomaly ID and safe transfer values.
- Continue or cancel uses the same anomaly ID and reports whether the user rechecked details.
- Successful continue commits the transfer, returns a transaction ID and updated balance, records the final action, and opens completion.
- Successful cancel creates no transaction, changes no balance, records the final action, and shows cancellation or a route home.
- A second decision on an already processed anomaly does not repeat the mutation; show the stored outcome.
- A failed decision request retains the anomaly ID and safe input so the user can retry.

## Guardian Notification

- Notification is permitted only for a HIGH anomaly and only after explicit user selection.
- The real demo path sends a Kakao “message to self” using the authenticated demo account, not a guaranteed message to the guardian's account.
- Return and display one of `SENT`, `MOCKED_NO_TOKEN`, or `MOCKED_AFTER_ACTUAL_FAILURE`.
- Clearly distinguish actual send success from a mock fallback.
- Record a notification timestamp only for actual `SENT` success.
- Mock fallback never blocks the user's later continue or cancel decision.
- Notification is not transaction approval or proof of guardian consent.
- If guardian-sharing consent affects this notification path, enforce the agreed rule in the API contract and server, not only in UI.

## Guardian Call

- Retrieve the saved guardian number before enabling the call action.
- Display and confirm the number, then open a `tel:` link.
- If the contact is missing, show registration guidance instead of a working-looking call button.
- The phone number is not used as a Kakao recipient identifier.
- A call does not alter the anomaly decision state automatically.

## Completion Criteria

- FR-035 through FR-038: Normal, high-only, repetition-only, and both-rule cases return the exact expected risk and reasons.
- FR-039: Warning values match the transfer request and server response.
- FR-040 through FR-041: Recheck, continue, and cancel produce one consistent final anomaly action and the correct transaction outcome.
- FR-042: Non-HIGH requests cannot send notification, and actual versus mock outcomes are visible.
- FR-043: When a guardian number exists, the warning screen opens a `tel:` link with that retrieved number.
