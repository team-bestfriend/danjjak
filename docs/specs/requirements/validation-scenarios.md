# Validation Scenarios

Use only the scenarios relevant to the current end-to-end test, feature Epic, or cross-domain review.

## SC-001 First Login and Consent

1. Open the service introduction while unauthenticated.
2. Start Kakao login and complete OAuth successfully.
3. Verify that the Kakao identifier is associated with one seeded user.
4. Accept usage recording and decline guardian sharing.
5. Verify that choice completion and both values persist.
6. Verify that home shows the seeded name and applies the returned accessibility settings.

Expected result: the user reaches home without SMS, password, name entry, or mandatory acceptance of either optional choice.

## SC-002 Returning Login

1. Log in again with the same Kakao user.
2. Verify that the same seeded user is retrieved.
3. Verify that completed consent choices and accessibility settings remain unchanged.
4. Verify direct navigation to home without registration or repeated choice steps.

Expected result: no second seeded user association is created.

## SC-003 Shortcut Create, Swap, and Deactivate

1. Retrieve the eight default active shortcuts.
2. Register an available template in an empty number.
3. Change its title and at least one instruction.
4. Move it to an occupied number and explicitly confirm the number swap.
5. Deactivate the pattern and verify that its number becomes available.
6. Refresh and verify the server-confirmed state.

Expected result: creation, editing, swap, and deactivation persist without deleting execution history.

## SC-004 Pattern Execution and Guidance

1. Select a transfer shortcut on home.
2. Verify recipient and description on pre-execution confirmation.
3. Select `Start` and verify execution plus first step visit creation when recording consent is active.
4. Verify that caption and TTS use the same current instruction.
5. Activate a valid non-target control and verify wrong-touch aggregation while remaining on the step.
6. Go back and re-enter a step, then verify a new visit number.

Expected result: execution follows persisted pattern steps, and measurable guidance actions are recorded without sensitive values.

## SC-005 Normal Registered-recipient Transfer

1. Confirm the default source account and registered recipient account.
2. Enter an amount that triggers no FDS rule.
3. Review the transaction and enter the correct mock PIN.
4. Verify `COMPLETED` with `NORMAL` risk behavior.
5. Verify completion values and returned post-transfer balance.
6. Refetch transaction history and verify the matching outgoing transaction.

Expected result: balance deduction and transaction creation occur together exactly once.

## SC-006 Direct-recipient Transfer

1. Enter recipient name, bank, mapped bank code, and account number.
2. Verify identical values on review.
3. Complete a normal transfer.
4. Verify recipient and amount in transaction history.
5. Verify that registered people and recipient accounts did not grow automatically.

Expected result: direct input is used only by the transfer and its transaction record.

## SC-007 Transfer Error Recovery

1. Submit with an incorrect PIN.
2. Verify no balance or transaction change.
3. Verify PIN is cleared while safe recipient and amount values remain.
4. Submit an amount greater than balance and verify the same atomicity rule.
5. Correct the values and complete the transfer.

Expected result: failed attempts never show completion and can be corrected without an accidental duplicate transfer.

## SC-008 MEDIUM Anomaly

1. Submit a transfer that triggers only high amount or only repetition.
2. Verify `REQUIRES_REVIEW`, `MEDIUM`, and exactly the applicable reason.
3. Return to transfer review, then choose continue or cancel.
4. Verify that the anomaly final action matches transaction creation and balance behavior.

Expected result: one anomaly record is processed once, with no guardian-notification action.

## SC-009 HIGH Anomaly and Kakao Notification

1. Prepare two completed outgoing transfers inside ten minutes.
2. Submit a transfer of at least KRW 10,000,000.
3. Verify `HIGH` with both high-amount and repetition reasons.
4. Explicitly select guardian notification.
5. Verify that `SENT`, `MOCKED_NO_TOKEN`, or `MOCKED_AFTER_ACTUAL_FAILURE` is accurately distinguished.
6. Continue or cancel and verify consistent anomaly, balance, and transaction state.

Expected result: notification outcome does not approve, block, or duplicate the transfer decision.

## SC-010 Guardian and Customer-center Calls

1. Retrieve the support contacts.
2. Update the guardian number and retrieve it again.
3. From anomaly review, activate the guardian `tel:` link.
4. From customer-center inquiry, activate the support `tel:` link.

Expected result: each action uses its server-returned number, and both numbers remain visible on a non-calling desktop.

## SC-011 TTS and Family-voice Fallback

1. Save TTS mode and a selected speed.
2. Enter a step and verify current text playback at that speed.
3. Switch to family mode and record, preview, and upload one step recording.
4. Verify family playback for that step while the caption remains visible.
5. Enter a step without a recording or force playback failure.
6. Verify TTS fallback and continued task operability.

Expected result: actual recording lifecycle and fallback work without using audio as authentication.

## SC-012 Usage Analysis and Wording Improvement

1. With usage recording accepted, complete several pattern executions and step actions.
2. Verify completed counts by financial task.
3. Verify the difficult step and deterministic supporting metrics.
4. Compare current and suggested instruction text.
5. Apply the suggestion and verify it in the next execution.
6. For a step with family audio, verify the rerecord warning and route.

Expected result: analysis reflects recorded data, applies no medical interpretation, and changes wording only after explicit success.
