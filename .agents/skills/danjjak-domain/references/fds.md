# Rule-Based FDS

- Apply FDS to both shortcut-based and direct transfer flows.
- Evaluate the rules immediately before the mock transfer is confirmed.
- Detect a high amount at KRW 10,000,000 or more.
- Detect repetition when two completed outgoing transfers exist in the previous ten minutes, making the current attempt the third.
- Set risk to `MEDIUM` when one rule triggers and `HIGH` when both trigger.
- Create one anomaly event per anomalous attempt and return the triggered rules so the UI can explain the warning.
- Create no anomaly event when neither rule triggers.
- Do not add new-recipient, route-deviation, weighted-score, or machine-learning rules.
- The warning UI may play family-recorded guidance, but it must keep the system-generated risk reason visible and must not treat the recording as transaction approval or authentication.
- Treat guardian notification as a demo integration, not a guaranteed delivery system.
