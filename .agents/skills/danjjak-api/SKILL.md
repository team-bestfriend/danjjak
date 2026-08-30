---
name: danjjak-api
description: Create or update Danjjak HTTP endpoints and keep the OpenAPI contract synchronized with frontend and backend behavior.
---

# Danjjak API Contract

## Source of Truth

- Use `contracts/openapi.yaml` as the authoritative HTTP contract.
- Update the contract before or together with endpoint implementation.
- Do not maintain a second authoritative contract in Swagger annotations.

## Rules

- Keep paths, methods, request fields, response fields, and status codes aligned with the implementation.
- Add only schemas used by the current MVP.
- Keep error responses consistent after a shared error format is introduced.
- Do not add generated clients or servers unless requested.

## Verification

```powershell
npx --yes @redocly/cli@2.47.0 lint contracts/openapi.yaml
```
