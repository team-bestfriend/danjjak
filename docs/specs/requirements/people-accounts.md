# People, Accounts, and Guardian Contact

## Requirements

| ID | Requirement | Required behavior |
| --- | --- | --- |
| FR-005 | Registered-person inquiry | Retrieve each saved person's name, relationship, and recipient account. |
| FR-006 | Registered-person management | Create and update a registered person and recipient account. |
| FR-007 | Mock-account inquiry | Retrieve owned accounts separately from registered people's recipient accounts. |
| FR-008 | Default source account | Select the default owned account as the initial source account in a transfer. |
| FR-009 | Guardian-contact management | Retrieve and update one guardian phone number per user. |

## Seed and Data Rules

- Initial seed data includes two owned accounts and registered son and daughter recipients with one recipient account each.
- An owned account exposes an identifier, bank, masked account number, alias, balance, and default-account flag.
- A recipient account does not expose or manage an owned balance or transfer PIN.
- A registered-person card shows name, relationship, bank, masked account number, and selectable state.
- Use the OpenAPI representation for ordinary account display. Full account input may be accepted for creation or update but must not be logged.
- The current contract returns one recipient account per registered person. Extending the UI to manage multiple recipient accounts requires an OpenAPI change first.
- Registered-person deletion is outside MVP scope. Do not expose an active delete action.

## Registered-person Create and Update

- Name, relationship, bank, and account number are required.
- The frontend maps the selected bank to the contract's bank code; do not ask the user to type an internal bank code.
- Validate supported account-number characters and length before submission without pretending to validate against a real bank.
- After successful creation or update, refresh the person list and use the server-returned identifiers and display values.
- On failure, keep safe entered values and do not add or change a local person as if the server succeeded.
- An empty person list explains that no recipients are registered and provides a registration action when management is available.

## Owned-account Behavior

- Never mix owned accounts and recipient accounts in a selector without a clear role label.
- The default owned account is initially selected for inquiry and transfer, but the user may choose another owned account.
- A recipient account cannot be selected as a transfer source.
- If no default account exists, use an explicit deterministic fallback from the returned owned-account order and record the data problem; do not treat a recipient account as the fallback.
- If there is no owned account, transfer entry is blocked with a clear empty-state explanation.

## Guardian-contact Behavior

- A user has at most one guardian phone number in the MVP.
- When no contact exists, show an unregistered state and an action to add one.
- Validate the supported phone-number format before saving.
- After a successful update, later inquiry and call links use the same server-returned number.
- A guardian call uses a `tel:` link after the user confirms the displayed number.
- A desktop environment that cannot place a call must still display the number clearly.
- The guardian phone number is not a Kakao recipient identifier and does not establish a guardian account or approval channel.

## Completion Criteria

- FR-005: Names, relationships, and accounts match the server response, and the empty state works.
- FR-006: A create or update is visible after refetch with no frontend-only phantom record.
- FR-007: UI and state never confuse owned accounts with recipient accounts.
- FR-008: Transfer starts with the default owned account while still allowing another owned account to be chosen.
- FR-009: A saved guardian number is returned by inquiry and used by the call action.
