# Mock Transfer

- Use mock accounts, balances, recipients, and transactions.
- Keep the main flow: source account, recipient, amount, confirmation, PIN, completion.
- Support one normal transfer and one anomalous transfer for the demo.
- Evaluate FDS immediately before the mock transfer is confirmed.
- Do not connect to a real bank or payment API.
- Keep balance updates and transaction creation in one Spring transaction.
