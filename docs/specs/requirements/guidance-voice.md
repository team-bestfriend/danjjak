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
| FR-054 | Pre-start voice configuration | During pattern editing, choose TTS or record family voice for the pre-start description, preview it, and persist the selection. |
| FR-055 | Per-step voice editor | Open an ordered step list and a dedicated editor for each step's text, voice mode, and recording. |
| FR-056 | Shared script editor | Edit, reset, preview, and save the text used by TTS, visible guidance, and the family recording script for either guidance target. |

## Guidance Targets and Editing Flow

- Distinguish the pattern's pre-start guidance from its executable steps. Pre-start guidance uses the pattern description; a step uses its current `instructionText`.
- Pattern editing includes pre-start voice configuration, an ordered step-voice list, and a final review. Pattern detail also provides entry to these editors after saving.
- The pre-start editor offers `TTS` and `FAMILY`, current text, preview, explicit save, and a change-method action. `FAMILY` offers actual microphone recording rather than simulated elapsed time.
- The step list comes from persisted pattern detail, including actual step identifiers, order, names, current scripts, effective voice modes, and recording availability. Reference screenshots do not fix the number or names of executable steps.
- Selecting a step opens an editor for that specific pattern and step with the same text, mode, preview, recording, and save actions as the pre-start editor. Saving one target must not overwrite another target.
- Direct entry or refresh reloads the selected pattern and step. Missing, inactive, or invalid targets show a recovery action instead of editing the first step or stale local data.
- Returning from a step editor preserves the parent pattern draft. Skipping voice configuration keeps existing saved values during editing and uses template text plus the user's default mode for a newly created pattern.
- Editing, previewing, and recording never create financial-task execution or step-action logs.

## Script Editing and Save Behavior

- Both TTS and family-recording screens expose an editable text field. Pre-start text updates the pattern description; step text updates that step's `instructionText`. Do not introduce separate TTS-only and family-only scripts.
- Show the same current draft under the text editor, send it to TTS preview at the selected speed, and display it as the script the family should read. Editing text stops obsolete preview audio.
- `Reset to default` restores the corresponding template description or step instruction in the draft. The default must be available from the server contract; do not reconstruct it from hardcoded screen text.
- Validate nonblank text within the existing 500-character limit before preview or save. Explain invalid input near the field and keep the draft available for correction.
- Finishing text editing, changing voice method, previewing, or resetting changes the draft only. Mark the draft as unsaved until the applicable explicit save succeeds.
- Within the pattern wizard, `Save this voice` accepts the target draft for final review; final pattern save persists it. In a standalone editor for an existing pattern or step, explicit save persists that target and returns the server-confirmed state to the list.
- Final review includes pre-start and step voice choices and pending recordings. Prevent duplicate saves; show success only after the required text, mode, and audio operations succeed. On partial failure, explain what was saved, reload server state, and retain remaining drafts for retry instead of reporting full success.
- Cancelling discards only unsaved changes. Leaving with an edited script or unsaved recording offers keep-editing or discard; navigation alone must not silently save or replace a recording.
- Saving or refetching must preserve the selected target's script and mode. The next pre-start confirmation or step execution uses those saved values.
- If text changes while a family recording exists, explain that the recording has not changed and offer rerecording or switching to TTS. A new recording captures the edited script only through actual recording; do not fabricate updated speech or claim that old audio matches the new text. Keeping old audio remains possible with the mismatch notice; no transcription or version-history feature is required.

## Instruction Source and Priority

1. Use the pattern step's persisted current `instructionText` as the caption source.
2. When the user has not customized a step, use the instruction copied from the selected template.
3. Caption and TTS input must use the same current text.
4. A family recording is supplemental playback linked to its pre-start or step target. It never hides or replaces the visible text or the script the family should read.
5. A changed instruction becomes active only after a successful persisted update.
6. Apply the same text-source rules to the pre-start description. Preview uses the draft; actual task guidance uses persisted text.

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

- Apply this lifecycle separately to the pre-start target and every executable step; a pre-start recording must not be stored as a fictitious executable step.
- Explain the recording purpose before asking for microphone permission.
- Distinguish permission denial, unsupported browser, capture failure, upload failure, and playback failure.
- After capture and before upload, provide preview and rerecord actions.
- Distinguish a locally captured draft from an uploaded and saved recording; follow the explicit save boundaries above.
- A successful new upload replaces the selected target's prior file path; revision history is outside MVP scope. Switching to TTS does not delete the saved family recording.
- Store file path/identifier and content type in the database, not the audio binary.
- Persist the user-authored guidance script as pattern text, but do not transcribe recordings or store/log a recording transcript.
- If a recording is missing or family playback fails, generate or play TTS for the same current instruction and briefly disclose the fallback.
- Family audio is never evidence of recipient identity, transaction approval, PIN verification, or guardian consent.
- Define upload, lookup, playback metadata, and replacement in OpenAPI before implementing them.

## Playback Selection

Resolve the voice mode for each target before applying the table:

1. Use that pre-start target's or step's explicitly saved `TTS`/`FAMILY` choice when present.
2. Otherwise use the user's global voice-guidance setting; use its existing default `TTS` when unset.
3. Pre-start selection does not implicitly override step choices. A global setting change affects targets without an explicit choice and preserves explicit target choices and recordings.

The table applies to both pre-start and step guidance. Missing family audio is a valid saved configuration with visible TTS fallback. Pre-start playback never requires the user to finish listening before starting or cancelling the task.

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
- FR-054: Pattern editing persists pre-start TTS/family selection and actual recorded audio; confirmation uses the saved description and effective voice mode after refresh.
- FR-055: Each real step is independently editable through list and direct entry; editing, skipping, cancelling, and refetching preserve the correct target and unrelated values.
- FR-056: Edited and reset text is consistent across caption, TTS preview, and family script; only successful save changes later guidance, and existing audio is never silently presented as newly recorded text. Verify these behaviors in SC-011.
