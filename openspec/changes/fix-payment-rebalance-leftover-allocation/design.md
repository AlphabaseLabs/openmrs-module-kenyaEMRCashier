## Context

`PaymentAllocationRebalanceServiceImpl` corrects allocation state when an edited bill line item becomes overallocated after its net total decreases. The current algorithm trims the changed line, then attempts to redistribute the released payment chunks to later lines and then earlier lines.

The observed failure happened after a registration line received a PKR 450 discount. The rebalance released PKR 450 from that line, applied PKR 400 to the following registration line, and left PKR 50 as unallocated tendered payment instead of wrapping back to an earlier eligible dental line. The line-item statuses then diverged from bill-level tendered state: the bill showed PKR 500 tendered, but the dental line still had `allocated = 0` and `payment_status = PENDING`.

## Goals / Non-Goals

**Goals:**
- Keep the fix inside the existing payment allocation rebalance flow.
- Make recipient traversal deterministic by using active bill line order rather than collection iteration order.
- Redistribute each released chunk until it is exhausted or no eligible recipient has remaining balance.
- Preserve existing terminal-status and voided-line exclusions.
- Add focused regression coverage for partial absorption by a later line followed by wrap-back allocation to an earlier line.

**Non-Goals:**
- Do not introduce a new reconciliation job or broad bill payment repair routine.
- Do not change REST API payloads or response formats.
- Do not change database schema.
- Do not change frontend payment selection or payment submission behavior.

## Decisions

1. Sort active line items before rebalance traversal.

   Use deterministic bill order: non-null line items sorted by `lineItemOrder`, with a stable fallback such as persisted id or UUID when needed. This avoids relying on Hibernate collection order, which may not match the UI-visible line sequence after edits, voids, or reinsertions.

   Alternative considered: keep the current `bill.getLineItems()` iteration order. This preserves less code, but it is exactly where the algorithm can miss the intended wrap target in real edited bills.

2. Build one wrapped recipient sequence per changed line.

   Construct recipients as active eligible lines after the changed line, followed by active eligible lines before the changed line. Exclude the changed line itself from recipients after it has been trimmed to its new net total.

   Alternative considered: keep two independent `reallocateReleasedChunks` calls. This is close to the current implementation, but it makes the continuation behavior easier to break because the second pass depends on the same mutable chunk state and collection ordering assumptions.

3. Keep chunk-driven redistribution.

   The rebalance should only redistribute amounts released from the changed line. For each released chunk, allocate to recipients in wrapped order using `min(recipient.remainingAmount, chunk.remainingAmount)` until the chunk is exhausted or recipients are exhausted.

   Alternative considered: add a final global sweep over all active payments and unallocated tendered amounts. That would fix the symptom, but it expands the scope beyond the edited-line rebalance contract and risks changing behavior for unrelated historical underallocation.

4. Synchronize only affected lines plus the bill.

   Add every recipient that receives allocation to the affected set, keep the changed line in the affected set, and run existing status synchronization after redistribution.

   Alternative considered: synchronize every line on the bill after every rebalance. That is simpler, but it is broader than needed for a focused algorithmic fix.

## Risks / Trade-offs

- Deterministic sorting can expose existing tests that assumed insertion order. Mitigation: update tests to assert bill-order semantics explicitly.
- A released chunk may remain partially unallocated when no eligible recipient has remaining balance. Mitigation: preserve this as acceptable behavior and cover it with existing or targeted tests.
- Multiple allocations from the same payment to the same recipient can remain possible. Mitigation: keep behavior compatible with the existing allocation model; consolidation is outside this change.

## Migration Plan

No data migration is required. Deploying the backend change affects future line-item edits that trigger rebalance. Existing underallocated historical bills are not automatically repaired by this change.

Rollback is a normal code rollback; no schema or persisted format changes are introduced.

## Open Questions

None.
