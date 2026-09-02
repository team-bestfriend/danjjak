---
name: danjjak-db
description: Work on the Danjjak MySQL schema, Flyway migrations, mock seed data, database queries, or MyBatis persistence while preserving the agreed MVP data model.
---

# Danjjak Database

Before database work, read the documents that match the task:

- Schema, queries, persistence, or seed data: read [references/schema-contract.md](references/schema-contract.md).
- Flyway naming, version allocation, or migration recovery: also read [../../../db/README.md](../../../db/README.md).
- Docker or local MySQL operation: also read [../../../infra/README.md](../../../infra/README.md).

## Rules

- Reuse the current schema when it can represent the requested behavior.
- Change the schema only when the requested behavior requires persisted data that the current model cannot represent.
- Before a schema change, state why a query, application rule, or existing column is insufficient.
- Keep the model focused on the runnable mock-finance demo; do not introduce production banking infrastructure.
- Never store a raw account PIN, audio binary, OAuth token, or secret in a migration.
- Treat a shared Flyway migration as immutable. Add a new migration for later changes.
- Validate schema changes against a clean MySQL 8.4 database through Flyway.
