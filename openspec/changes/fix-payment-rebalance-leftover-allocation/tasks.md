## 1. Regression Coverage

- [x] 1.1 Add a unit test in `BillServiceImplPaymentRebalanceTest` for a changed line that releases 450.00, a later line that can consume only 400.00, and an earlier eligible line that must receive the leftover 50.00.
- [x] 1.2 Add or adjust a test proving rebalance recipient order uses active bill line order rather than raw collection insertion order.
- [x] 1.3 Ensure existing tests still cover no-recipient behavior where released amount may remain unallocated because every other line is voided, terminal, or fully allocated.

## 2. Rebalance Algorithm

- [x] 2.1 Update `PaymentAllocationRebalanceServiceImpl` to derive active line items in deterministic bill order using `lineItemOrder` with a stable fallback.
- [x] 2.2 Replace the two-pass range-based recipient traversal with a wrapped recipient sequence: eligible lines after the changed line followed by eligible lines before the changed line.
- [x] 2.3 Keep the changed line excluded from recipient allocation after trimming it to its current net total.
- [x] 2.4 Allocate each released payment chunk across the wrapped recipient sequence until the chunk is exhausted or no recipient has remaining amount.

## 3. Settlement Synchronization

- [x] 3.1 Confirm every recipient that receives redistributed allocation is added to the affected-line set.
- [x] 3.2 Confirm the changed line, affected recipients, and owning bill have settlement statuses synchronized after redistribution.

## 4. Verification

- [x] 4.1 Run the focused API test class for payment allocation rebalance.
- [x] 4.2 Run the module API test target needed to verify no nearby cashier payment behavior regressed.

Note: `mvn -q -pl api test` was also attempted under JDK 11 with the OpenMRS app-data property set. The rebalance and nearby payment tests passed, but the full legacy API suite still fails in unrelated Spring TestContext and PowerMock runner bootstrap tests.
