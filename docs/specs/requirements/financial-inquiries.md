# Mock Financial Inquiries and Customer Center

## Requirements

| ID | Requirement | Required behavior |
| --- | --- | --- |
| FR-032 | Financial inquiry | Retrieve mock balance, transaction history, pension deposits, maintenance fees, and utility payments. |
| FR-033 | Transaction detail | Display direction/type, amount, counterparty, timestamp, and balance after the transaction. |
| FR-034 | Customer-center call | Retrieve the support number from the server and initiate a call link. |

## Account Selection

- Retrieve owned accounts and initially select the default account.
- Allow the user to switch to another owned account and refetch the relevant inquiry.
- Never present a registered person's recipient account as an inquiry source account.
- If there are no owned accounts, show an explicit empty state and do not display seeded balance constants as live data.

## Balance and Transaction History

- Display balance with a currency label and locale-appropriate thousands separators.
- Present transaction history newest first unless the API explicitly defines another order.
- Distinguish deposit and withdrawal using text or sign in addition to color.
- Each row or detail view shows transaction type/direction, amount, counterparty or recipient, timestamp, and post-transaction balance.
- A completed new transfer appears as an outgoing transaction after refetch and the displayed balance matches the server's updated balance.
- When there are no transactions, state that no transactions exist and show the active period or category filter.
- Never combine hardcoded preview transactions with server-returned live transactions in the same user history.

## Category Inquiries

- Pension, maintenance-fee, and utility screens use server transaction-category filters.
- A category result identifies the selected account and category.
- Category empty state does not imply that the account itself is missing.
- Completing a category inquiry completes the linked pattern execution only after the result is successfully shown.
- Card history, automatic transfers, deposit maturity, and exchange rates are not required MVP inquiries and must not appear as active completed features.

## Customer-center Call

- Retrieve the customer-center number from the server before displaying or using it.
- Display the number and require a user action before opening a `tel:` link.
- In a desktop environment that cannot place a call, keep the number readable and explain that call handling depends on the device.
- Do not replace an active call control with a modal that only closes.
- If contact retrieval fails, do not call a hardcoded number as if it came from the server; show retry or a documented demo fallback label.

## Completion Criteria

- FR-032: Every required inquiry screen displays server-backed mock data and handles account, category, and empty states.
- FR-033: Seed transactions and newly completed transfers expose all required transaction detail fields.
- FR-034: The call action uses the retrieved support number in an actual `tel:` link.
