# Mock Transfer

## Requirements

| ID | Requirement | Required behavior |
| --- | --- | --- |
| FR-028 | Transfer input | Select a source account and recipient account, enter an amount, and review the full transfer. |
| FR-029 | Direct transfer | Transfer to an unsaved recipient using name, bank, bank code, and account number. |
| FR-030 | Transfer PIN | Enter the selected owned account's mock PIN before submitting a transfer. |
| FR-031 | Atomic mock transfer | A normal transfer deducts balance and creates a transaction as one server transaction. |

## Required Flow

```text
Transfer start
→ Source account
→ Recipient method
→ Registered recipient/account or direct-recipient input
→ Amount
→ Transfer review
→ PIN
→ Submit
→ Normal completion or anomaly review
```

## Shared Transfer State

- Direct transfer from home and transfer-pattern execution use the same transfer state model and server operation.
- Starting a new transfer clears all prior recipient, amount, PIN, anomaly, transaction, and transient completion state.
- Back navigation preserves safe non-sensitive values so the user can correct them.
- Completion, cancellation, logout, or session loss clears the full transient transfer state.
- Never restore a PIN after navigation reload or error.

## Source Account

- Initially select the default owned account and show bank, masked number, alias, and current balance.
- Allow another owned account to be selected before review.
- A recipient account cannot become a source account.
- If the selected account becomes missing or unavailable, block submission and return to account selection after refresh.

## Registered Recipient

- In direct-transfer entry, selecting a registered person selects that person's recipient account.
- In a transfer pattern, preselect the person and account linked by the pattern.
- If the linked account is missing or unavailable, stop and direct the user to repair the pattern recipient instead of silently choosing another recipient.
- Show recipient name, relationship, bank, and masked account number during selection and review.

## Direct Recipient

- Require recipient name, bank, and account number. Map the selected bank to the request's bank code.
- Validate supported characters and length without performing a real-bank account check.
- Keep the same direct-recipient values through amount, review, and submission.
- Do not automatically add the direct recipient or account to registered people after completion.

## Amount and Review

- Amount is an integer of at least KRW 1.
- Do not submit zero, negative, nonnumeric, or contract-overflow values.
- The UI may warn from the currently displayed balance, but the server makes the final insufficient-balance decision.
- Review shows source account, recipient name, recipient bank/account, amount, and fee.
- The user can return from review to correct source, recipient, or amount without losing unrelated safe input.
- Review values and the eventual request must be identical.

## PIN and Submission

- The PIN belongs to the selected mock owned account and is not a login PIN.
- Do not persist the raw PIN, keep it longer than the active submission, or include it in logs, error messages, URLs, or analytics.
- After PIN entry, send one transfer request and disable duplicate submission until a response or recoverable failure occurs.
- Distinguish PIN mismatch, insufficient balance, missing account, invalid recipient, and invalid request.
- After any rejected request, clear PIN. Recipient and amount may remain for correction when safe.

## Server Response Handling

- `COMPLETED`: store the returned transaction ID and updated balance in transient result state, then show completion.
- `REQUIRES_REVIEW`: do not show completion; show the anomaly screen using returned anomaly ID, risk level, reasons, and recent-transfer count.
- Request error: do not show completion; offer retry or correction based on the error.
- Completion displays the confirmed recipient, amount, transaction ID, and server-returned post-transfer balance.
- Refetched balance and transaction history must match the completed response.

## Atomicity

- On a normal transfer, balance deduction and outgoing-transaction creation succeed or fail together in one Spring transaction.
- A PIN failure, balance failure, validation failure, or cancelled anomaly creates no completed transfer and changes no balance.
- FDS evaluation occurs on the server immediately before committing the transfer result.
- Do not connect to a real bank or payment provider.

## Completion Criteria

- FR-028: Source, recipient, amount, and review remain consistent through forward and backward navigation.
- FR-029: The completed direct-recipient transaction contains the entered recipient data and does not change the registered-person list.
- FR-030: Only the correct mock PIN permits processing, and no raw PIN is persisted or exposed.
- FR-031: Successful balance and transaction mutations occur together; every failed path changes neither.
