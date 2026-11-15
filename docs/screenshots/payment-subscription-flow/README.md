# Subscription and payment: step by step

Platform staff (SUPER_ADMIN / SAAS_STAFF) flow. Screenshots use sample data (Dhaka Garments Ltd, Business plan 2,500 BDT/month).

**PDF version:** [subscription-payment-guide.pdf](subscription-payment-guide.pdf) (cover + one page per step).

| # | Screen | What to do / what happens |
|---|--------|---------------------------|
| 01 | Subscription Plans | Plans with price, billing cycle, limits and features. A YEARLY plan is charged price / 12 per month. |
| 02 | Organizations | The client organization. Pick it in the top-bar organization picker. |
| 03 | Subscriptions (empty) | No subscription yet: click **Add subscription**. |
| 04 | Add subscription | Choose plan (Business), status (ACTIVE), start date. Save. |
| 05 | Subscription created | The row has a **Collect payment** button (hidden for CANCELLED). |
| 06 | Collect payment: Cash | Amount and the month it covers come from the server (`payment-quote`). |
| 07 | Collect payment: Card | Card approval / reference is optional. |
| 08 | Collect payment: bKash | **Receive** stays disabled until the bKash TrxID is entered. |
| 09 | bKash filled in | TrxID + optional note, then **Receive 2,500.00 BDT**. |
| 10 | Payment received | Paid invoice `SUB-INV-…`, receipt `RCPT-…`, next renewal date. |
| 11 | Subscriptions | Renewal date moved one month forward (2026-12-15). |
| 12 | History | The payment is recorded in the subscription history. |
| 13 | Billing → Invoices | The invoice, status PAID. |
| 14 | Billing → Payments | CONFIRMED payment, **Method** = bKash (`payments.channel`), TrxID, receipt Issued. |
| 15 | Billing → Record payment | Manual payments can also set a **Method** (Cash / bKash / Card). |
