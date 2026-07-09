## Why

Payment allocation rebalancing can leave part of an active payment unallocated when a discounted line releases more value than later line items can absorb. This causes the bill-level tendered amount and line-item statuses to diverge, leaving earlier eligible unpaid items marked `PENDING` even though leftover payment value should have wrapped back to them.

## What Changes

- Make payment allocation rebalance consume released payment chunks across all eligible non-voided line items, wrapping from later line items back to earlier line items in deterministic bill order.
- Preserve the existing rule that the changed line is trimmed to its new net total before released value is redistributed.
- Keep terminal line item statuses (`EXEMPTED`, `CANCELLED`, `ADJUSTED`) and voided line items excluded from rebalance recipients.
- Ensure any affected recipient line item has its payment status synchronized after receiving redistributed allocation.
- Add regression coverage for the observed sequence: discounting a paid registration line releases payment, fills a later registration line, and allocates the leftover amount to an earlier dental line.

## Capabilities

### New Capabilities
- `payment-allocation-rebalancing`: Defines deterministic redistribution of released payment allocation across eligible bill line items after line-item net totals decrease.

### Modified Capabilities
- None.

## Impact

- Backend rebalance logic in `PaymentAllocationRebalanceServiceImpl`.
- Unit tests around bill payment allocation rebalance behavior.
- No REST API contract changes, database schema changes, or frontend changes expected.
