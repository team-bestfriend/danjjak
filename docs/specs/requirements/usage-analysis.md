# Execution Logging, Usage Analysis, and Instruction Improvement

## Requirements

| ID | Requirement | Required behavior |
| --- | --- | --- |
| FR-044 | Execution record | Record pattern-execution start, end, and final status. |
| FR-045 | Step visit | Create a distinct visit whenever a step is entered, including re-entry. |
| FR-046 | Step actions | Aggregate retry, back, wrong-touch, and route-deviation behavior per visit. |
| FR-047 | Step duration | Derive visit duration from start and end timestamps. |
| FR-048 | Execution counts | Aggregate and display completed executions by financial task. |
| FR-049 | Difficult step | Select a relatively difficult step using the deterministic approved rule. |
| FR-050 | Wording suggestion | Provide a simpler guidance suggestion for a difficult step. |
| FR-051 | Apply wording | Compare current and suggested text, then explicitly apply the selected suggestion. |
| FR-052 | Rerecord path | After wording changes, offer family-voice rerecording for the affected step. |

## Consent Gate

- Create new execution and step-action records only when usage recording is accepted.
- Opening pre-execution confirmation does not create an execution.
- When consent choices are incomplete, analysis links to consent choice completion.
- When usage recording is declined, analysis explains that no records are collected and links to settings.
- When recording is accepted but no records exist, show a genuine no-data state.
- Never present fixed preview analytics as the current user's recorded behavior.

## Execution Lifecycle

- Create a `STARTED` execution when the user explicitly starts a confirmed pattern.
- Finish it as `COMPLETED` only after the financial task completes successfully.
- Finish it as `CANCELLED` when the user explicitly cancels the task.
- Finish it as `FAILED` only for an unrecoverable task failure, not for a retryable validation or provider error.
- Persist start and end timestamps. Do not fabricate an end time for an active execution.
- A transfer that requires anomaly review remains unfinished until continue or cancel completes.

## Step Visits and Actions

- Entering a step creates a visit with the next `visitNumber` for that execution and step.
- Going back and re-entering the same step creates a new visit instead of overwriting the previous one.
- Persist visit start and end timestamps; calculate duration from them rather than storing a second conflicting duration value.
- Aggregate retry, back, and wrong-touch counts within one visit.
- Record route deviation as a boolean indicating whether it happened at least once in the visit.
- Close the current visit before opening the next one whenever navigation permits a definitive transition.
- Do not store raw click streams, touch coordinates, PINs, recognized commands, or audio transcripts.
- Help-request count and independent-completion status remain unavailable until requirements define reliable frontend events.

## Analysis Rules

Use this deterministic error-action score:

```text
error action score
= retry count
+ back count
+ wrong-touch count
+ route-deviation count
```

- Count completed executions by financial task within the selected analysis period.
- Sum the error-action score across visits for each pattern step.
- Select the step with the highest summed score as the difficult step.
- If scores tie, select the step with the longer average visit duration.
- If still tied, select the earlier step order to keep the result deterministic.
- If there is no analyzable record, return no difficult step instead of manufacturing one.
- Compute summaries and suggestions at read time; do not persist a second analytics-result table for the MVP.
- Describe observed interaction records without medical, cognitive, or diagnostic claims.

## Report Presentation

- Display completed task counts with a period and task label.
- Show the selected difficult step, its pattern/task context, and the deterministic evidence used by the rule.
- Empty, declined-consent, incomplete-consent, loading, and server-error states remain distinct.
- Do not infer user intent or impairment from elapsed time or mistakes.
- The displayed totals must match the server aggregation for the same period and user.

## Instruction Suggestion and Application

- When a difficult step exists, show the current instruction and suggested instruction together.
- The first implementation may use a deterministic template keyed by step code.
- Do not change the persisted current instruction until the user explicitly applies the suggestion.
- After successful application, pattern detail and the next execution use the new text.
- On application failure, keep the old persisted text and leave the suggestion available for retry.
- If the step has family audio, explain that recording and text may no longer match and provide a rerecord action.
- Keeping the old recording does not mean it has been approved as the spoken version of the new wording.
- Define analytics, suggestion, and application endpoints in OpenAPI before implementation.

## Completion Criteria

- FR-044 through FR-047: A representative run produces the expected final status, separate visits, action totals, and timestamp-derived durations.
- FR-048: The UI task-count total matches the server aggregation.
- FR-049: Identical input records always produce the same difficult-step result.
- FR-050 through FR-051: Current and suggested text are comparable, and only explicit successful application changes later execution.
- FR-052: A changed step with family audio can enter the actual step-specific rerecording flow.
