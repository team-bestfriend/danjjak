# Rule-Based FDS

- Apply FDS only to the main transfer flow.
- Use explicit rules as the primary decision method.
- Start with only the rules required by the demo, such as a new recipient or unusually large amount.
- Keep rule weights and thresholds visible in code or configuration.
- Return the triggered rules with the result so the UI can explain the warning.
- Treat guardian notification as a demo integration, not a guaranteed delivery system.
- Do not add ML unless its input schema matches the training data domain.
- If ML is added, use it only as a secondary score and document every input feature.
