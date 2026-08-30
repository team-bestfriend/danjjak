---
name: danjjak-frontend
description: Implement or update the Danjjak Vue frontend, including converting Figma Make TSX screens to Vue components.
---

# Danjjak Frontend

## Stack

- Vue 3
- Vite
- Vue Single-File Components with `<script setup>`

## Structure

Create directories only when they are needed.

```text
frontend/src/
|-- api/          HTTP clients
|-- components/   shared UI
|-- features/     feature-specific UI and logic
|-- pages/        route-level screens
|-- router/       route definitions
|-- App.vue
`-- main.js
```

## Rules

- Name components with `PascalCase.vue`.
- Name variables and functions with `camelCase`.
- Keep HTTP calls outside presentation components.
- Use `VITE_API_BASE_URL` for the backend origin.
- Use the latest approved Figma design as the initial UI reference.
- Follow current user instructions and approved project changes over existing Figma output.
- Preserve the intended user flow; pixel-perfect reproduction is optional unless requested.
- Do not revert intentional UI changes only because they differ from Figma.
- Reuse shared components only when duplication is real.
- Treat visual fidelity and the complete demo flow as required.
- Preserve readable text, large touch targets, and clear interaction feedback for older users.
- Optimize for the agreed demo viewport. Broader responsive support is optional unless requested.
- Do not build a full design system unless the current UI requires it.

## Verification

```powershell
cd frontend
npm run build
```
