# Guided Steps, TTS, Family Voice, and Voice Commands

## Requirements

| ID | Requirement | Required behavior |
| --- | --- | --- |
| FR-018 | Step highlight | Visually highlight the control the user should operate in the current step. |
| FR-019 | Instruction editing | Allow the pre-start description and current instruction text for each pattern step to be changed. |
| FR-020 | Default wording | Use the template's default instruction when no custom instruction is supplied. |
| FR-021 | Caption guidance | Display the current instruction as large, readable on-screen text. |
| FR-022 | TTS guidance | Generate and play the current instruction at the selected guidance speed. |
| FR-023 | Family-voice guidance | In family mode, play the saved recording for the current step when available. |
| FR-024 | Family-voice recording | Record, upload, and play back audio for an individual step. |
| FR-025 | Family-voice replacement | Replace the current step recording with a newly uploaded recording. |
| FR-026 | TTS fallback | Use TTS with the same instruction when family audio is absent or cannot play. |
| FR-027 | Simple voice command | Match a predefined normalized phrase to one shortcut and highlight it. |

## Instruction Source and Priority

1. Use the pattern step's persisted current `instructionText` as the caption source.
2. When the user has not customized a step, use the instruction copied from the selected template.
3. Caption and TTS input must use the same current text.
4. A family recording is supplemental playback linked to a step. It never hides or replaces the visible text.
5. A changed instruction becomes active only after a successful persisted update.

## Step Highlight

- When a step has `targetElementId`, map it to exactly one actionable element on the current screen.
- Highlight may use border, background, scale, or animation, but it must preserve readability and click behavior.
- The highlighted area and actual click target must refer to the same control.
- If the target is not rendered, do not highlight an unrelated fallback. Keep the caption visible and record a diagnostic error that contains no sensitive input.
- When the user activates a valid non-target control during a measurable guided step, increment `wrongTouchCount` and remain on the current step when doing so is safe.
- Never include the entered PIN or full account number in highlight metadata, captions, logs, or analytics.

## Caption Behavior

- Keep the current instruction visible throughout the step, including while audio is loading, playing, paused, or failed.
- Use the current accessibility text-size setting without clipping the primary action.
- Provide a clear replay action near the caption when audio guidance is available.
- Changing steps cancels obsolete audio and updates the caption before or with the next playback request.
- A caption failure caused by missing data must show a safe generic next-action message and record the configuration problem; it must not invent a financial result.

## TTS Playback

- Attempt one automatic playback when entering a step.
- If browser autoplay policy prevents playback, expose an enabled play control instead of repeatedly retrying automatically.
- Provide replay and pause/stop behavior appropriate to the current browser capability.
- Map `SLOW`, `NORMAL`, and `FAST` accessibility settings to the server TTS request.
- Cancel the previous request or audio instance when navigation makes it obsolete so speech from an old screen does not continue over a new step.
- TTS generation failure keeps the caption and financial action usable, explains that audio is unavailable, and allows retry.
- Repeated requests for identical text and speed may be cached, but cache behavior must not replay obsolete content.

## Family Recording Lifecycle

- Explain the recording purpose before asking for microphone permission.
- Distinguish permission denial, unsupported browser, capture failure, upload failure, and playback failure.
- After capture and before upload, provide preview and rerecord actions.
- Show a saved state only after the upload response succeeds.
- A successful new upload replaces the step's prior file path; revision history is outside MVP scope.
- Store file path/identifier and content type in the database, not the audio binary.
- Do not store or log a transcript of the family recording.
- If a recording is missing or family playback fails, generate or play TTS for the same current instruction and briefly disclose the fallback.
- Family audio is never evidence of recipient identity, transaction approval, PIN verification, or guardian consent.
- Define upload, lookup, playback metadata, and replacement in OpenAPI before implementing them.

## Playback Selection

| Voice mode | Recording state | Required playback |
| --- | --- | --- |
| `TTS` | Any | TTS for current instruction |
| `FAMILY` | Available and playable | Family recording while caption remains visible |
| `FAMILY` | Missing | TTS fallback with a short notice |
| `FAMILY` | Playback failure | Stop failed media, then offer or start TTS fallback without blocking the task |

## Voice-command Matching

- Use only predefined normalized phrases and deterministic keyword rules.
- Normalize supported spacing and ordinary recognition variation without introducing an LLM, RAG, embeddings, or open-ended interpretation.
- Example: a supported equivalent of “send money to my son” identifies the active son-transfer shortcut.
- When exactly one active shortcut matches, highlight it and present confirmation; do not execute it.
- When no shortcut or multiple shortcuts match, ask the user to retry speech or choose on screen.
- Do not store recognized text or captured command audio in the database.
- Microphone denial or recognition failure leaves all manual shortcut interactions available.

## Completion Criteria

- FR-018 through FR-021: Every representative step shows the correct target and persisted text; a successful edit is reflected in later execution.
- FR-022: Actual generated audio plays at the selected speed, while caption and retry remain available on failure.
- FR-023 through FR-026: Recording, upload, playback, replacement, and TTS fallback are reproducible with a real browser audio file.
- FR-027: Each supported demo phrase highlights only the matching shortcut and never starts a financial task automatically.
