# Shortcuts and Financial Patterns

## Requirements

| ID | Requirement | Required behavior |
| --- | --- | --- |
| FR-010 | Default shortcuts | A seeded demo user receives eight default active shortcuts. |
| FR-011 | Number limit | Active shortcuts use unique numbers from 1 through 12, with no more than 12 active patterns per user. |
| FR-012 | Pattern management | Add, update, deactivate, and reorder patterns. |
| FR-013 | Template selection | Create patterns only from predefined templates returned by the server. |
| FR-014 | Transfer-recipient link | A transfer pattern links to a registered person's recipient account. |
| FR-015 | Pattern information update | Update title, pre-start description, linked account, and step instructions. |
| FR-016 | Pre-execution confirmation | Selecting a shortcut shows task details and requires an explicit start action. |
| FR-017 | Ordered execution | Execute the ordered screens and instructions from the retrieved pattern detail. |

## Default Shortcuts

| Number | Task | Linked data |
| --- | --- | --- |
| 1 | Transfer to son | Son's seeded recipient account |
| 2 | Check pension deposit | Pension transaction in the default owned account |
| 3 | Check maintenance fee | Maintenance-fee transaction in the default owned account |
| 4 | Check balance | Default owned account |
| 5 | View transaction history | Default owned account |
| 6 | Call customer center | Server-provided support contact |
| 7 | Transfer to daughter | Daughter's seeded recipient account |
| 8 | Check utility payment | Utility transaction in the default owned account |

## Template Rules

- Required demo templates cover transfer, pension inquiry, maintenance-fee inquiry, balance inquiry, transaction history, customer-center call, and utility inquiry.
- A template returned with `available=false` remains visible only as disabled UI when useful and cannot be selected or registered.
- Do not expose card history, automatic transfer, deposit maturity, exchange rate, or another unspecified task as an active hardcoded feature.
- Do not provide free-form task types or an arbitrary step designer.
- Creation copies the selected template's current default steps and instructions into the new pattern according to the API contract.

## Registration Flow

1. Retrieve and select an available template.
2. Select an unused shortcut number from 1 through 12.
3. For a transfer template, select a registered person's recipient account.
4. Review and optionally edit the default title and pre-start description.
5. Review and optionally edit each copied step instruction.
6. Review a final summary containing the number, title, description, linked recipient when applicable, and step count.
7. Submit once, then update home and pattern list only after a successful response.

## Validation and Limits

- Reject a number outside 1 through 12.
- Reject a thirteenth active pattern.
- Reject duplicate active shortcut numbers except through an explicitly confirmed swap operation.
- Reject registration from an unavailable or stale template.
- Require a valid linked registered recipient for transfer patterns.
- Do not mark a pattern as registered before the server confirms the mutation.

## Edit, Reorder, and Deactivate

- Fetch persisted pattern detail before filling the edit form.
- Moving to an unused number changes only the current pattern's number.
- Moving to an occupied number requires an explicit confirmation to swap the two pattern numbers.
- Submit the complete active shortcut order in one reorder request.
- Deactivation changes the record to `is_active=false` and `shortcut_number=null`; it is not a row deletion.
- A deactivated number becomes available to another active pattern.
- Preserve execution and step logs after deactivation.
- On edit, reorder, swap, or deactivate failure, restore the last server-confirmed state and explain the failure.
- Editing step instructions requires a contract capable of persisting them. Do not keep a frontend-only edit that disappears on refetch.

## Home Behavior

- Home displays active patterns in shortcut-number order and makes the number visually dominant.
- Empty slots may support pattern registration but must not look like completed financial functions.
- Selecting an active shortcut opens pre-execution confirmation; it does not immediately perform an inquiry or transfer.
- A missing or deactivated pattern reached through stale UI is removed after refresh and cannot start.
- A predefined voice command may highlight a matching active shortcut, but it cannot start the task automatically.

## Execution Start

- Confirmation shows shortcut number, title, description, and recipient/account summary for a transfer pattern.
- Audio may read the description, but playback completion is not required to start.
- Create a pattern execution only when the user selects `Start`.
- Closing or cancelling confirmation leaves the user on home and creates no execution record.
- The first executable step comes from the retrieved ordered pattern detail, not a hardcoded screen list.
- Do not mark the pattern execution complete before the linked inquiry or transfer actually completes.

## Completion Criteria

- FR-010: A newly prepared seeded user sees exactly eight default active shortcuts.
- FR-011: A thirteenth active pattern and an unconfirmed duplicate number are rejected.
- FR-012: Create, update, move/swap, and deactivate changes survive refetch.
- FR-013: An unavailable template cannot be registered.
- FR-014: A transfer pattern detail identifies its linked person and recipient account.
- FR-015: Updated title, description, account link, and step instructions are used by later execution.
- FR-016: Shortcut selection alone never completes or starts the financial task.
- FR-017: Execution follows the ordered steps returned for the pattern.
