# Product Scope

## Service Definition

Danjjak helps older adults start frequent financial tasks from shortcuts numbered 1 through 12 and complete deterministic mock-finance flows with large captions, spoken guidance, and a highlighted next action.

## Product Goals

1. A senior user can identify and start a frequent financial task from a numbered home screen.
2. Every guided step makes the next action and its explanation clear.
3. Registered-recipient and direct-recipient mock transfers can be demonstrated from start to completion.
4. High-value and repeated transfers can demonstrate rule-based FDS review and an explicit user decision.
5. Recorded usage can identify a difficult step and support a visible instruction-wording improvement flow.

## Roles

| Role | Description | Allowed scope |
| --- | --- | --- |
| Senior user | Primary user who runs shortcuts and financial tasks | Login, inquiry, mock transfer, settings, and call links |
| Family configurator | Person using the senior user's device and session to configure assistance | Manage patterns, people, recipient accounts, instructions, and family recordings |
| Guardian contact | One saved phone contact available during anomaly review | No separate account, login, device, or remote approval |
| Demo operator | Developer or presenter who prepares seed data and external integration settings | Not a product UI role |

## Terms

| Term | Definition |
| --- | --- |
| Shortcut | One active association between a number from 1 through 12 and one financial pattern |
| Financial pattern | An executable unit containing a task type, title, pre-start description, optional linked account, and ordered steps |
| Pattern step | An ordered unit containing a screen code, instruction text, highlight target, and optional family recording |
| Registered person | A saved recipient such as a son, daughter, relative, or acquaintance |
| Owned account | A mock debit account with a balance and mock transfer PIN |
| Recipient account | A mock credit destination owned by a registered or directly entered recipient |
| Direct transfer | A transfer using a recipient name, bank, and account number not saved as a registered person |
| Anomalous transfer | A transfer attempt that triggers at least one high-amount or repetition rule |
| Step visit | One interval from entering a pattern step until leaving that step; re-entry creates another visit |

## Included MVP Scope

- Kakao OAuth login and deterministic association with a seeded user
- Current-user, optional-consent, and accessibility-setting retrieval and updates
- Owned-account, balance, and transaction-history inquiries
- Registered-person and recipient-account retrieval, creation, and updates
- Shortcut management for numbers 1 through 12 and financial-pattern execution
- Pattern title, description, linked account, and step-instruction updates
- Caption guidance, TTS, family-voice recording/playback/replacement, and TTS fallback
- Registered-recipient and direct-recipient mock transfers
- MEDIUM and HIGH FDS decisions based on amount and recent transfers
- Guardian and customer-center call links plus a HIGH-risk Kakao notification demonstration
- Pattern execution and step-visit logging, task counts, and difficult-step analysis
- Suggested instruction wording, explicit application, and a path to rerecord family voice

## Implementation Principles

- Do not call a real bank, card processor, or payment network.
- Accounts, balances, and transactions are mock data, but successful mutations must remain consistent in the database.
- Prefer one complete representative demo flow over additional incomplete screens or optional features.
- Every prominent interactive control must work or be visibly disabled with an explanation.
- The frontend must present server business decisions rather than independently recreating them.
- TTS or Kakao integration failure must not make the core mock-finance flow unusable.
- Do not add production banking security, settlement, audit, concurrency, idempotency, deployment, or monitoring unless explicitly requested.
