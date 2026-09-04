# Delivery Constraints

Read this document for API, database, integration, build, cross-domain, or Issue-planning work.

## Frontend and API Integration

- Use feature API modules instead of repeating `fetch` calls in presentation components.
- Use `VITE_API_BASE_URL` for the backend origin.
- Preserve the names and meanings defined by `contracts/openapi.yaml` for request and response models.
- Separate display transformations from server DTOs without calculating the same business decision independently in both layers.
- Read shared API error codes and messages, then connect them to feature-specific recovery actions.
- Refresh affected list and detail state after successful create, update, reorder, or deactivate operations.
- Invalidate or refetch owned-account balance and transaction history after a completed transfer.
- Treat the successful guardian-contact or accessibility-setting response as the new UI baseline.
- Do not leave local state in a successful state after a failed server save.
- Use hardcoded mock constants only in an explicitly documented fallback or preview path; do not mix them with API data on the same live screen.

## Current Contract Coverage

The current OpenAPI contract covers:

- owned accounts, balances, and transaction history;
- registered people and recipient-account inquiry, creation, and update;
- mock transfer and anomaly continue/cancel decisions;
- guardian and customer-center contacts and HIGH-risk Kakao notification;
- TTS MP3 generation;
- current user, optional consent, and accessibility settings;
- pattern templates and pattern list, detail, creation, update, reorder, and deactivation.

## Contract Work Required Before Implementation

Define or extend the OpenAPI contract before implementing:

- Kakao OAuth start, callback, logout, and session-state behavior;
- persisted step-instruction updates if the existing pattern update contract cannot represent them;
- family-voice upload, lookup, playback metadata, and replacement;
- pattern-execution start/end and step-visit/action logging;
- task-count and difficult-step analytics;
- instruction suggestion and application.

## Non-functional Requirements

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| NFR-001 | Technology | Frontend uses Vue 3, Vite, Pinia, and Vue Router. Backend uses Java 17, Spring Framework 5.x, and MyBatis. |
| NFR-002 | Database | Use MySQL and introduce schema or seed changes only through new Flyway migrations. |
| NFR-003 | Local execution | The documented sequence can start MySQL, apply Flyway, run the backend, and run the frontend locally. |
| NFR-004 | API contract | OpenAPI is the only authoritative HTTP field contract shared by frontend and backend. |
| NFR-005 | Accessibility | Primary copy, targets, contrast, focus, and feedback prioritize older-user operability. |
| NFR-006 | Sensitive data | Do not persist raw PINs, OAuth tokens, audio binaries, or real banking credentials in application tables. |
| NFR-007 | Demo repeatability | Core mock flows remain repeatable in a documented demo environment when an external provider is unavailable. |
| NFR-008 | Simple implementation | Do not add frameworks, abstractions, infrastructure, or expansion features that are unnecessary for the current demo. |
| NFR-009 | Build verification | Relevant changes pass frontend build, backend test/WAR, and OpenAPI lint as applicable. |
| NFR-010 | Integration isolation | TTS or Kakao failure does not corrupt account, transaction, or anomaly state. |
| NFR-011 | Log protection | Do not log PINs, OAuth tokens, full account numbers, recognized speech, or recording content. |
| NFR-012 | Demo viewport | Representative flows work without clipped or overlapping content in the approved demo viewport. Broad responsive coverage is optional. |

## Explicitly Out of Scope

- Real bank, card, payment, or settlement integration
- Phone-number, SMS, password, or ordinary registration login
- A login PIN
- Separate guardian account, app, paired device, or remote approval
- Guaranteed delivery to a guardian's Kakao account
- Consent-document versioning and consent-change history
- Registered-person deletion
- Automatic saving of directly entered recipients or accounts
- FDS rules based on new recipients, route deviation, elapsed time, weighted scoring, ML, or LLMs
- Automatic anomaly blocking or added identity verification
- A full Kakao notification-history table
- Treating TTS or family voice as transaction approval or authentication
- Instruction-text or audio-file revision history
- MySQL BLOB storage for audio
- LLM, RAG, embeddings, or open-ended conversation for voice commands
- Raw click/touch coordinates or complete event streams
- Help-request, independent-completion, medical, or cognitive evaluation metrics
- Functional card-history, automatic-transfer, deposit-maturity, or exchange-rate inquiries
- User-customizable shortcut icons
- Production-grade settlement, audit, concurrency, idempotency, deployment, or monitoring

## Feature Definition of Done

An implementation Issue is complete only when:

- it references the applicable requirement IDs and validation scenario;
- the primary user action works end to end;
- applicable loading, success, empty, validation-error, and server-error states work;
- displayed values match the server response or a documented mock fallback;
- no nonfunctional primary control remains deceptively active;
- an API change is aligned across OpenAPI, backend, and frontend consumers;
- new persisted data is introduced with a new Flyway migration rather than an applied-migration edit;
- the smallest relevant frontend, backend, and API verification passes; and
- the representative scenario does not regress an already completed core flow.

## Issue Hierarchy and Boundaries

```text
Top-level MVP Epic
└── Feature Epic
    └── Implementation Issue
```

- The top-level Epic tracks the MVP outcome and dependencies between feature Epics.
- A feature Epic tracks one user value or cohesive domain flow.
- An implementation Issue must represent one independently verifiable behavior that reasonably fits one branch and pull request.
- Do not split a cohesive flow only because it spans multiple files.
- If multiple Issues must edit the same shared file, create or finish the common foundation Issue first.
- When an API is missing, order work as contract, backend, frontend integration, then integration validation.
- A visual mock and its functional integration are not the same completion state.
- Normal transfer, MEDIUM review, and HIGH review may be separate implementation or validation Issues when their completion criteria differ.
- Do not hide an unimplemented expansion feature as an optional checkbox inside a required Issue.

Each implementation Issue should state:

- goal;
- related requirement IDs;
- implementation checklist;
- prerequisites and follow-up dependencies;
- owned files or allowed change area;
- exclusions;
- expected success, error, and empty states;
- behavior-based completion criteria; and
- minimum verification.

## Known Contract and Implementation Gaps

Record these as prerequisites until they are resolved:

1. Kakao OAuth and logout do not yet have an OpenAPI contract.
2. The current user response does not contain a phone number; profile UI must not invent one.
3. Optional-consent API semantics differ from the current frontend's required-terms onboarding UI.
4. The registered-person API returns one recipient account, while the current frontend presents multiple-account UI.
5. Pattern-step instruction and family-voice updates do not have a complete API contract.
6. Family-voice upload, playback metadata, and replacement do not have an API contract.
7. Execution logging, step logging, analytics, and instruction suggestion do not have API contracts.
8. Some OpenAPI pattern types are outside the MVP; template `available` values and frontend disabled states must agree.
9. Current frontend FDS behavior based on new accounts or route deviation conflicts with the approved rules and must be replaced by server results.
10. Current frontend routing and any parallel screen-state navigation must be consolidated under Vue Router.
