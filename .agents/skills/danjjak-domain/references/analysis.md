# Usage Analysis

- Record only events that the frontend can emit consistently.
- Derive duration from start and end timestamps.
- Store one row per step visit and aggregate retry, back, wrong-touch, and deviation data for that visit.
- Show financial-task execution counts in the current summary UI.
- Do not add solo-completion or help-request metrics unless a new requirement defines the underlying event.
- Prefer deterministic summaries for the first demo implementation.
- Add an LLM-generated comment only when the feature is requested and the input data is defined.
- Do not present mock analysis as a validated behavioral assessment.
