# Danjjak Agent Guide

## Working Style

- State assumptions when they materially affect the implementation.
- Ask only when ambiguity would significantly change the result.
- Implement only the requested scope.
- Avoid speculative features, abstractions, and configuration.
- Touch only files required by the task and preserve the existing style.
- Remove only unused code introduced by the current change.
- Mention unrelated problems without modifying them.
- Run the smallest relevant verification before finishing.
- Use a short plan only for non-trivial tasks.
- Add packages or dependencies when they directly support the requested feature.
- Avoid duplicate libraries and unnecessary stack changes.

## Comments

- Write code comments and Javadoc in Korean.
- Explain business rules, non-obvious behavior, and important implementation reasons.
- Do not describe code that is already clear from its name and structure.
- Keep comments concise, usually within one to three lines.
- Update comments when the related behavior changes.
- Use Javadoc only when a public class or method has a contract that is not clear from its name and signature.
- Do not require Javadoc for every class, method, getter, or setter.

## Project

- Build a runnable hackathon MVP for demonstrating financial task flows for older users.
- Use mock users, accounts, balances, and transactions.
- Do not treat this as a production financial system.
- Prefer the simplest implementation that supports the agreed demo.

## Routing

- Frontend work: read `.agents/skills/danjjak-frontend/SKILL.md`.
- Backend work: read `.agents/skills/danjjak-backend/SKILL.md`.
- API contract work: read `.agents/skills/danjjak-api/SKILL.md`.
- Shortcut, transfer, FDS, or usage-analysis work: read `.agents/skills/danjjak-domain/SKILL.md` and only the relevant reference.
- Read multiple skills only when the task spans multiple areas.

## Shared Rules

- Keep changes small and demo-focused.
- Do not add production-grade security, concurrency, idempotency, or deployment work unless requested.
- Follow `CONTRIBUTING.md` for commit and code conventions.
- Do not commit secrets, build output, IDE metadata, or local database data.
