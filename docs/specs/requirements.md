# Danjjak MVP Requirements

## Document Status

| Field | Value |
| --- | --- |
| Status | Draft for review |
| Baseline date | 2026-09-04 |
| Audience | Product, frontend, backend, API, database, test, and coding agents |
| Purpose | Define product behavior and acceptance criteria for Epic and implementation Issue planning |
| Product boundary | Runnable hackathon MVP using deterministic mock financial data, not a production banking system |

## Source Authority

- These requirement documents define product intent, user flows, business rules, scope, and behavior-based completion criteria.
- `contracts/openapi.yaml` is authoritative for HTTP paths, methods, fields, schemas, and status codes.
- Flyway migrations under `db/migration` are authoritative for tables, columns, constraints, and seed data.
- The latest approved Figma design is the initial visual reference. Approved product behavior and current user instructions take precedence over stale Figma output.
- A current explicit user decision takes precedence over these documents. Persist an accepted decision by updating the affected requirement and validation scenario.
- When sources disagree, do not silently choose one. Align the requirement, OpenAPI contract, schema, and implementation in the same change, or record the conflict and dependency in the Issue.

## Reading Route

Read this index first, then read only the files required by the current task.

| Task area | Required document | Requirement IDs |
| --- | --- | --- |
| Product scope, roles, or terminology | [Product Scope](requirements/product-scope.md) | Product-wide |
| Shared UI, routing, accessibility, loading, errors, or navigation | [Shared UX](requirements/shared-ux.md) | UX-001 through UX-014 |
| Login, consent, current user, or accessibility settings | [Authentication and Settings](requirements/auth-settings.md) | FR-001 through FR-004, FR-053 |
| People, owned accounts, recipient accounts, or guardian contact | [People and Accounts](requirements/people-accounts.md) | FR-005 through FR-009 |
| Shortcut home, templates, pattern creation, editing, ordering, or execution start | [Shortcuts and Patterns](requirements/shortcuts-patterns.md) | FR-010 through FR-017 |
| Step highlight, captions, TTS, family voice, recording, or voice command | [Guidance and Voice](requirements/guidance-voice.md) | FR-018 through FR-027 |
| Balance, transactions, pension, fees, utilities, or customer-center calls | [Financial Inquiries](requirements/financial-inquiries.md) | FR-032 through FR-034 |
| Registered-recipient or direct mock transfer | [Mock Transfer](requirements/mock-transfer.md) | FR-028 through FR-031 |
| FDS, anomaly review, guardian notification, or guardian call | [FDS and Guardian Response](requirements/fds-guardian.md) | FR-035 through FR-043 |
| Execution logging, metrics, difficult-step analysis, or wording improvement | [Usage Analysis](requirements/usage-analysis.md) | FR-044 through FR-052 |
| API/DB boundaries, non-functional constraints, known gaps, or Issue design | [Delivery Constraints](requirements/delivery-constraints.md) | NFR-001 through NFR-012 |
| End-to-end validation or cross-feature Issue planning | [Validation Scenarios](requirements/validation-scenarios.md) | SC-001 through SC-012 |

### Cross-cutting Tasks

- For a user-visible feature, read its feature document and [Shared UX](requirements/shared-ux.md).
- For an API, persistence, or integration change, also read [Delivery Constraints](requirements/delivery-constraints.md).
- For an end-to-end test or whole-feature Epic, also read the matching scenarios in [Validation Scenarios](requirements/validation-scenarios.md).
- Read all requirement files only for whole-MVP planning, a full requirement audit, or a cross-domain integration review.

## Requirement Maintenance

- Preserve existing requirement and scenario IDs. Add a new ID instead of renumbering unrelated entries.
- Keep each rule and acceptance criterion in its owning feature document; link to it instead of copying it elsewhere.
- Update affected validation scenarios when a business rule or user flow changes.
- Update `contracts/openapi.yaml` when the HTTP contract changes.
- Add a new Flyway migration when persisted data changes; never edit an applied shared migration.
- Reference applicable requirement IDs in GitHub Epics, Issues, and pull requests.
