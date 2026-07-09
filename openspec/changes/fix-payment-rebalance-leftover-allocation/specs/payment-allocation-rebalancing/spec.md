## ADDED Requirements

### Requirement: Rebalance released payment across wrapped eligible line order
When a bill line item becomes overallocated after its net total decreases, the system SHALL trim the changed line item to its current net total and redistribute the released payment amount across eligible recipient line items in deterministic wrapped bill order.

Eligible recipients SHALL be non-voided line items whose payment status is not `EXEMPTED`, `CANCELLED`, or `ADJUSTED`. The changed line item SHALL NOT receive its own released amount during the same rebalance.

#### Scenario: Later line partially consumes released amount and remainder wraps to earlier line
- **WHEN** a changed line releases 450.00 from an active payment, the next eligible later line has 400.00 remaining, and an earlier eligible line has at least 50.00 remaining
- **THEN** the system allocates 400.00 of the released amount to the later line and 50.00 to the earlier line

#### Scenario: Later lines consume all released amount
- **WHEN** a changed line releases payment amount and later eligible line items have enough remaining balance to consume all released amount
- **THEN** the system allocates the released amount only to later eligible line items and does not allocate to earlier line items

#### Scenario: No eligible recipient can consume remaining released amount
- **WHEN** a changed line releases payment amount and all other line items are voided, terminal, or already fully allocated
- **THEN** the system leaves the unconsumed released amount unallocated for that payment

### Requirement: Rebalance order is deterministic
The system SHALL evaluate rebalance recipients in ascending bill line order, using persisted line item order with a stable persisted fallback when necessary, so that redistribution does not depend on collection iteration order.

#### Scenario: Voided historical lines do not affect visible active order
- **WHEN** a bill contains voided historical lines and active line items with sparse line item order values
- **THEN** the system evaluates only active non-voided recipients in ascending bill line order

### Requirement: Rebalance synchronizes affected settlement statuses
After redistributing released payment allocation, the system SHALL synchronize payment status for the changed line item, every recipient line item that received allocation, and the owning bill.

#### Scenario: Earlier line receives leftover allocation
- **WHEN** an earlier `PENDING` line item receives leftover released allocation during wrap-back redistribution
- **THEN** the system updates that line item status according to its allocated amount and net total, including `POSTED` when it is partially allocated
