# Internet Banking — Induction & Onboarding Guide

_Generated 2026-10-08T17:02:22.567151922Z by heuristic (GitHub Models disabled)._

This pack is derived from uploaded **Jira stories** and the **regression master**. It is the first-week reading list for engineers, testers, and automation developers.

## 1. Application overview

The application is an **internet banking** channel. Customers authenticate, view accounts, move money (own-account and beneficiary transfers), pay bills, manage payees, download statements, operate cards, observe limits, and receive alerts. The corpus below is the functional source of truth until a live Jira project is connected.

| Metric | Count |
| --- | ---: |
| Jira stories | 20 |
| Regression test cases | 50 |
| Stories with no linked tests | 0 |
| Tests with no linked story | 0 |

### Capability map (from stories)

- **IB-101** — Retail customer login with user ID and password (2 regression cases)
- **IB-102** — Step-up MFA with SMS OTP on login from a new device (4 regression cases)
- **IB-103** — Idle session timeout and explicit logout (2 regression cases)
- **IB-104** — Account summary dashboard (4 regression cases)
- **IB-105** — Account details and recent transactions (2 regression cases)
- **IB-106** — Own-account funds transfer (2 regression cases)
- **IB-107** — Transfer to an approved beneficiary (IMPS/NEFT) (2 regression cases)
- **IB-108** — Per-transaction and daily transfer limits (5 regression cases)
- **IB-109** — Pay a registered biller (3 regression cases)
- **IB-110** — Register a new biller (2 regression cases)
- **IB-111** — Manage transfer beneficiaries with cool-off (5 regression cases)
- **IB-112** — Account statements view, download, and email (5 regression cases)
- **IB-113** — View debit and credit cards linked to the customer (1 regression cases)
- **IB-114** — Card controls: block, unblock, PIN set, international usage (4 regression cases)
- **IB-115** — Customer-managed channel limits (3 regression cases)
- **IB-116** — Transaction and login alerts (2 regression cases)
- **IB-117** — Lock internet banking after repeated failed logins (1 regression cases)
- **IB-118** — Insufficient funds and duplicate-transfer protection (2 regression cases)
- **IB-119** — Self-service password reset (1 regression cases)
- **IB-120** — Future-dated own and beneficiary transfers (1 regression cases)

## 2. Jira story walkthrough

### IB-101 — Retail customer login with user ID and password

Customers sign in to internet banking with a unique user ID and password. Successful authentication lands on the account summary. Failed attempts must not disclose whether the user ID exists.

Acceptance criteria:
- Valid credentials open the account summary
- Invalid credentials show a generic error
- Password field is masked

**Linked regression cases**

- `TC-01` Successful login with valid user ID and password
- `TC-02` Login rejected for invalid password

### IB-102 — Step-up MFA with SMS OTP on login from a new device

After password validation, a new or untrusted device requires a 6-digit SMS OTP. Trusted devices skip OTP for 90 days unless the customer revokes them.

Acceptance criteria:
- OTP is required on first login from a new browser
- Expired or wrong OTP is rejected without completing login
- Successful OTP marks the device as trusted

**Linked regression cases**

- `TC-04` Login MFA succeeds with valid SMS OTP
- `TC-05` Login MFA rejected for expired OTP
- `TC-06` Login MFA rejected for invalid OTP
- `TC-09` New device always challenges MFA even with correct password

### IB-103 — Idle session timeout and explicit logout

Internet banking sessions expire after 10 minutes of inactivity. Customers can also logout from the header. Both paths invalidate the server session and require a fresh login.

Acceptance criteria:
- Idle timeout returns the customer to login with a session-expired message
- Logout clears cookies and blocks Back-button reuse of cached pages

**Linked regression cases**

- `TC-07` Idle session times out after 10 minutes
- `TC-08` Explicit logout invalidates the session

### IB-104 — Account summary dashboard

After login the customer sees every eligible retail account: product type, masked account number, available balance, and product nickname. A hide-balance control is available for shoulder-surfing protection.

Acceptance criteria:
- All active CASA and term-deposit accounts appear
- Available balance matches the core banking snapshot used by the channel
- Hide/show balance toggles amounts without a page reload error

**Linked regression cases**

- `TC-11` Account summary lists all eligible retail accounts
- `TC-12` Savings and current available balances match the channel snapshot
- `TC-13` Hide and show balances on summary
- `TC-15` New customer with a single zero-balance account still sees summary

### IB-105 — Account details and recent transactions

From summary the customer opens an account to see ledger-style recent transactions, holds, and joint-account indicators.

Acceptance criteria:
- Last 10 transactions display with date, description, debit/credit, running balance
- Joint accounts show a joint indicator
- Pending authorizations are labelled

**Linked regression cases**

- `TC-14` Account details show recent posted transactions
- `TC-16` Joint account shows joint indicator on details

### IB-106 — Own-account funds transfer

Customers move money between their own savings and current accounts in real time. A confirmation step shows from, to, amount, and remarks before submit.

Acceptance criteria:
- Transfer posts to both accounts immediately when within limits and funds
- Confirmation screen is mandatory
- Receipt shows a channel reference number

**Linked regression cases**

- `TC-17` Own-account transfer from savings to current succeeds
- `TC-22` Own-account transfer requires a confirmation screen

### IB-107 — Transfer to an approved beneficiary (IMPS/NEFT)

Customers send funds to an active beneficiary using IMPS (low value, immediate) or NEFT (batch). High-value payments require OTP.

Acceptance criteria:
- Only active beneficiaries appear as destinations
- IMPS succeeds within the IMPS cap
- High-value transfer prompts OTP before commit

**Linked regression cases**

- `TC-18` IMPS transfer to an active beneficiary succeeds
- `TC-23` High-value beneficiary transfer requires OTP

### IB-108 — Per-transaction and daily transfer limits

The channel enforces bank-max and customer-set daily and per-transaction limits for third-party transfers. Own-account transfers use a higher internal cap.

Acceptance criteria:
- Breach of per-transaction limit is blocked before OTP
- Breach of remaining daily limit is blocked
- Customer can view remaining limit for the day

**Linked regression cases**

- `TC-20` Third-party transfer blocked when daily limit is exhausted
- `TC-21` Third-party transfer blocked when per-transaction limit is exceeded
- `TC-46` View bank max
- `TC-47` Increase daily transfer limit within bank maximum
- `TC-48` Reject daily limit above bank maximum

### IB-109 — Pay a registered biller

Customers pay utilities and card bills from a registered biller list. Where the biller supports fetch, the outstanding amount is retrieved before pay.

Acceptance criteria:
- Pay succeeds against a registered biller with a valid amount
- Fetch-and-pay uses the fetched outstanding as the default amount
- Receipt includes biller name and consumer number

**Linked regression cases**

- `TC-26` Pay a registered electricity biller
- `TC-29` Fetch outstanding and pay credit-card biller
- `TC-30` Schedule a future bill payment

### IB-110 — Register a new biller

Customers add a biller by category, biller name, and consumer number. Invalid consumer numbers returned by the biller network are rejected.

Acceptance criteria:
- Valid consumer number creates an active biller
- Invalid consumer number shows a biller-network error and does not save

**Linked regression cases**

- `TC-27` Register a new water biller with a valid consumer number
- `TC-28` Add biller rejected for invalid consumer number

### IB-111 — Manage transfer beneficiaries with cool-off

New beneficiaries require a cooling-off period (4 hours) before they can receive funds. Duplicate account numbers for the same customer are rejected. Nickname edits do not reset cool-off.

Acceptance criteria:
- Add beneficiary succeeds and status is Cooling-off until the period ends
- Transfer is blocked during cool-off
- Duplicate account number is rejected
- Delete removes the beneficiary from the payee list

**Linked regression cases**

- `TC-31` Add a new IMPS beneficiary with account number and IFSC
- `TC-32` Transfer to a beneficiary still in cool-off is blocked
- `TC-33` Delete an existing beneficiary
- `TC-34` Edit beneficiary nickname without resetting cool-off on an active payee
- `TC-35` Duplicate beneficiary account number is rejected

### IB-112 — Account statements view, download, and email

Customers view up to 12 months of statements, download PDF or CSV, and email a statement to the registered address. Date ranges must be valid and within the retention window.

Acceptance criteria:
- Default view is last 30 days of posted transactions
- PDF and CSV downloads succeed for a valid range
- End date before start date is rejected
- Email statement is sent to the registered email only

**Linked regression cases**

- `TC-36` View last 30 days of posted transactions
- `TC-37` Download statement as PDF
- `TC-38` Download statement as CSV
- `TC-39` Statement date range rejected when end is before start
- `TC-40` Email statement to registered email only

### IB-113 — View debit and credit cards linked to the customer

The cards hub lists debit and credit cards with masked PAN, product name, status, and linked account.

Acceptance criteria:
- All active and blocked cards for the CIF are listed
- PAN is masked except last 4

**Linked regression cases**

- `TC-41` Cards hub lists debit and credit cards with masked PAN

### IB-114 — Card controls: block, unblock, PIN set, international usage

Customers can temporarily block a debit card, unblock it, set/reset PIN via OTP, and toggle international usage.

Acceptance criteria:
- Block changes status to Blocked and declines subsequent POS in the simulator
- Unblock restores Active
- PIN set requires OTP
- International toggle persists

**Linked regression cases**

- `TC-42` Temporary block of debit card
- `TC-43` Unblock a previously blocked debit card
- `TC-44` Set debit-card PIN with OTP
- `TC-45` Toggle international usage on a debit card

### IB-115 — Customer-managed channel limits

Within the bank maximum, customers raise or lower daily third-party transfer limits. Changes above the current limit may require OTP.

Acceptance criteria:
- View shows bank max, current daily limit, remaining today
- Update within max succeeds
- Update above bank max is rejected

**Linked regression cases**

- `TC-46` View bank max
- `TC-47` Increase daily transfer limit within bank maximum
- `TC-48` Reject daily limit above bank maximum

### IB-116 — Transaction and login alerts

Customers opt in or out of SMS and email alerts for debits, credits, and successful logins.

Acceptance criteria:
- Enable SMS debit alerts persists and is shown as On
- Disable email login alerts persists
- Preferences survive logout

**Linked regression cases**

- `TC-49` Enable SMS alerts for debits
- `TC-50` Disable email alerts for successful login

### IB-117 — Lock internet banking after repeated failed logins

Three consecutive invalid password attempts lock the user ID for 30 minutes or until the customer unlocks via the contact centre / forgot-password flow.

Acceptance criteria:
- Third consecutive failure locks the ID
- A fourth attempt with the correct password is still rejected while locked
- Generic lock message does not confirm the password was correct

**Linked regression cases**

- `TC-03` User ID locked after three consecutive failed logins

### IB-118 — Insufficient funds and duplicate-transfer protection

Transfers and bill payments that exceed available balance are blocked. A second submit of the same amount/beneficiary within two minutes warns of a possible duplicate.

Acceptance criteria:
- Amount greater than available balance is rejected with insufficient-funds
- Duplicate warning is shown; customer can cancel or confirm

**Linked regression cases**

- `TC-19` Transfer blocked when amount exceeds available balance
- `TC-25` Duplicate transfer warning on repeat submit within two minutes

### IB-119 — Self-service password reset

Customers reset a forgotten password using registered mobile OTP and a debit-card PIN or security questions, then set a new password that meets complexity rules.

Acceptance criteria:
- OTP to registered mobile is required
- New password meeting policy is accepted and old password cannot be reused immediately
- After reset the customer can login with the new password

**Linked regression cases**

- `TC-10` Password reset with mobile OTP then login with new password

### IB-120 — Future-dated own and beneficiary transfers

Customers may schedule a one-time transfer for a future business date. The debit occurs on that date if funds and limits allow; otherwise the schedule fails and an alert is sent.

Acceptance criteria:
- Future date is accepted for the next 30 days excluding today
- Scheduled instruction appears in the activity list as Pending

**Linked regression cases**

- `TC-24` Future-dated beneficiary transfer is stored as pending

## 3. Regression landscape

| Area | Cases |
| --- | ---: |
| Login / auth | 25 |
| Cards | 2 |
| Beneficiaries | 6 |
| Statements | 5 |
| Bill pay | 5 |
| Funds transfer | 2 |
| Limits | 4 |
| Account summary | 4 |

### Case catalogue

#### TC-01 — Successful login with valid user ID and password

Story: IB-101
Steps:
1. Open internet banking login. 2. Enter valid retail user ID and password. 3. Submit.
Expected: Customer is authenticated and the account summary is displayed. No OTP is required on a trusted device.

#### TC-02 — Login rejected for invalid password

Story: IB-101
Steps:
1. Enter a valid user ID. 2. Enter an incorrect password. 3. Submit.
Expected: Generic invalid-credentials message is shown. Account summary is not displayed. Remaining attempts are not disclosed beyond bank policy.

#### TC-03 — User ID locked after three consecutive failed logins

Story: IB-117
Steps:
1. Enter a valid user ID with a wrong password three times. 2. On the fourth try enter the correct password.
Expected: After the third failure the ID is locked. The fourth attempt is rejected with a lock message even if the password is correct.

#### TC-04 — Login MFA succeeds with valid SMS OTP

Story: IB-102
Steps:
1. Login from a new browser profile with valid password. 2. Enter the 6-digit SMS OTP within validity.
Expected: OTP is accepted, session is created, and the device can be marked trusted.

#### TC-05 — Login MFA rejected for expired OTP

Story: IB-102
Steps:
1. Trigger OTP on a new device. 2. Wait until the OTP expires. 3. Submit the expired code.
Expected: Expired OTP is rejected. Login is not completed. Customer can request a new OTP.

#### TC-06 — Login MFA rejected for invalid OTP

Story: IB-102
Steps:
1. Trigger OTP on a new device. 2. Enter an incorrect 6-digit code.
Expected: Invalid OTP error is shown. Session is not established.

#### TC-07 — Idle session times out after 10 minutes

Story: IB-103
Steps:
1. Login successfully. 2. Leave the session idle for 10 minutes. 3. Attempt to open account details.
Expected: Customer is redirected to login with a session-expired message. Protected pages are not accessible.

#### TC-08 — Explicit logout invalidates the session

Story: IB-103
Steps:
1. Login. 2. Click Logout. 3. Use the browser Back button to the summary URL.
Expected: Session is invalidated. Back navigation does not show account data. Login is required.

#### TC-09 — New device always challenges MFA even with correct password

Story: IB-102
Steps:
1. Login with valid credentials from a cleared-cookie browser.
Expected: Password is accepted then OTP challenge is presented before account summary.

#### TC-10 — Password reset with mobile OTP then login with new password

Story: IB-119
Steps:
1. Choose Forgot password. 2. Authenticate with registered mobile OTP. 3. Set a new complex password. 4. Login with the new password.
Expected: Reset succeeds. Old password is no longer valid. New password opens internet banking.

#### TC-11 — Account summary lists all eligible retail accounts

Story: IB-104
Steps:
1. Login as a customer with savings, current, and FD. 2. Open Account summary.
Expected: Each eligible account is listed with product type, masked number, nickname, and available balance.

#### TC-12 — Savings and current available balances match the channel snapshot

Story: IB-104
Steps:
1. Note core/channel snapshot balances for savings and current. 2. Open summary.
Expected: Displayed available balances match the snapshot for both CASA accounts.

#### TC-13 — Hide and show balances on summary

Story: IB-104
Steps:
1. On summary click Hide balances. 2. Click Show balances.
Expected: Amounts are masked when hidden and restored when shown without an error.

#### TC-14 — Account details show recent posted transactions

Story: IB-105
Steps:
1. From summary open a current account with activity. 2. Review the transaction list.
Expected: At least the last 10 posted transactions show date, description, debit/credit, and running balance.

#### TC-15 — New customer with a single zero-balance account still sees summary

Story: IB-104
Steps:
1. Login as a newly onboarded customer with one savings account at zero.
Expected: Summary loads with one account and 0.00 available balance. No empty-state crash.

#### TC-16 — Joint account shows joint indicator on details

Story: IB-105
Steps:
1. Open a joint savings account.
Expected: Joint indicator is visible. Transactions still load for the operating customer.

#### TC-17 — Own-account transfer from savings to current succeeds

Story: IB-106
Steps:
1. Transfer 500.00 from savings to own current. 2. Confirm on the review screen. 3. Submit.
Expected: Both ledgers update. Receipt shows a channel reference. Summary balances reflect the movement.

#### TC-18 — IMPS transfer to an active beneficiary succeeds

Story: IB-107
Steps:
1. Select an active beneficiary. 2. Choose IMPS. 3. Enter an amount within IMPS and daily limits. 4. Confirm.
Expected: Payment is accepted with an IMPS reference. Beneficiary account is credited in the simulator.

#### TC-19 — Transfer blocked when amount exceeds available balance

Story: IB-118
Steps:
1. Start a beneficiary transfer for available balance plus 1.00. 2. Continue to confirm.
Expected: Insufficient-funds error is shown. No debit is posted.

#### TC-20 — Third-party transfer blocked when daily limit is exhausted

Story: IB-108
Steps:
1. Note remaining daily limit. 2. Attempt a beneficiary transfer above remaining daily limit.
Expected: Transfer is blocked for daily-limit breach before OTP. No debit is posted.

#### TC-21 — Third-party transfer blocked when per-transaction limit is exceeded

Story: IB-108
Steps:
1. Enter an amount above the per-transaction cap but within available balance.
Expected: Per-transaction limit error is shown on the amount step. Confirm is not offered.

#### TC-22 — Own-account transfer requires a confirmation screen

Story: IB-106
Steps:
1. Capture from account, to account, amount, remarks. 2. Click Continue.
Expected: Review screen shows from, to, amount, and remarks. Submit is available only from review.

#### TC-23 — High-value beneficiary transfer requires OTP

Story: IB-107
Steps:
1. Enter a beneficiary amount above the high-value threshold. 2. Confirm.
Expected: OTP challenge is presented. Transfer commits only after a valid OTP.

#### TC-24 — Future-dated beneficiary transfer is stored as pending

Story: IB-120
Steps:
1. Choose a date 3 business days ahead. 2. Amount within limits. 3. Confirm.
Expected: Instruction is saved as Pending with the value date. Immediate debit does not occur.

#### TC-25 — Duplicate transfer warning on repeat submit within two minutes

Story: IB-118
Steps:
1. Complete a 100.00 beneficiary transfer. 2. Immediately repeat the same beneficiary and amount.
Expected: Duplicate-transaction warning is shown. Customer can cancel or confirm a second payment.

#### TC-26 — Pay a registered electricity biller

Story: IB-109
Steps:
1. Open Bill pay. 2. Select a registered electricity biller. 3. Enter a valid amount. 4. Confirm.
Expected: Payment succeeds. Receipt shows biller name, consumer number, amount, and reference.

#### TC-27 — Register a new water biller with a valid consumer number

Story: IB-110
Steps:
1. Add biller. 2. Category Water. 3. Select biller. 4. Enter a valid consumer number. 5. Save.
Expected: Biller is saved as Active and appears on the bill-pay list.

#### TC-28 — Add biller rejected for invalid consumer number

Story: IB-110
Steps:
1. Add biller. 2. Enter a consumer number the biller network rejects.
Expected: Biller-network validation error is shown. Record is not saved.

#### TC-29 — Fetch outstanding and pay credit-card biller

Story: IB-109
Steps:
1. Select a fetch-enabled card biller. 2. Fetch bill. 3. Pay the fetched outstanding.
Expected: Fetched amount is the default. Payment posts for that amount with a receipt.

#### TC-30 — Schedule a future bill payment

Story: IB-109
Steps:
1. On a registered biller choose Pay later. 2. Set a future date and amount. 3. Confirm.
Expected: Scheduled bill pay appears as Pending. Immediate debit does not occur.

#### TC-31 — Add a new IMPS beneficiary with account number and IFSC

Story: IB-111
Steps:
1. Add beneficiary. 2. Enter name, account number, IFSC. 3. Confirm with OTP.
Expected: Beneficiary is created in Cooling-off status. It appears on the payee list.

#### TC-32 — Transfer to a beneficiary still in cool-off is blocked

Story: IB-111
Steps:
1. Add a beneficiary. 2. Immediately start a transfer to that payee.
Expected: Transfer is blocked with a cool-off message until the cooling period ends.

#### TC-33 — Delete an existing beneficiary

Story: IB-111
Steps:
1. Open an active beneficiary. 2. Delete and confirm.
Expected: Beneficiary is removed from the payee list and cannot be selected for transfer.

#### TC-34 — Edit beneficiary nickname without resetting cool-off on an active payee

Story: IB-111
Steps:
1. Open an active beneficiary. 2. Change nickname only. 3. Save.
Expected: Nickname updates. Status remains Active. Cool-off is not reapplied.

#### TC-35 — Duplicate beneficiary account number is rejected

Story: IB-111
Steps:
1. Add a beneficiary using an account number already saved for this customer.
Expected: Duplicate account error is shown. Second beneficiary is not created.

#### TC-36 — View last 30 days of posted transactions

Story: IB-112
Steps:
1. Open Statements for current account. 2. Leave the default date range.
Expected: Transactions for the last 30 days are listed. Pending authorizations are excluded or labelled.

#### TC-37 — Download statement as PDF

Story: IB-112
Steps:
1. Choose a valid date range within 12 months. 2. Download PDF.
Expected: A PDF file downloads and contains the account number (masked) and the selected period.

#### TC-38 — Download statement as CSV

Story: IB-112
Steps:
1. Choose a valid date range. 2. Download CSV.
Expected: CSV downloads with date, description, debit, credit columns for posted transactions.

#### TC-39 — Statement date range rejected when end is before start

Story: IB-112
Steps:
1. Set start date after end date. 2. Search.
Expected: Validation error is shown. No statement is produced.

#### TC-40 — Email statement to registered email only

Story: IB-112
Steps:
1. Choose Email statement for a valid range. 2. Submit.
Expected: Statement is queued to the registered email. The UI does not allow an arbitrary address.

#### TC-41 — Cards hub lists debit and credit cards with masked PAN

Story: IB-113
Steps:
1. Open Cards.
Expected: Each linked card shows product name, status, linked account, and PAN masked except last 4.

#### TC-42 — Temporary block of debit card

Story: IB-114
Steps:
1. Select an active debit card. 2. Block card and confirm.
Expected: Status becomes Blocked. Subsequent simulated POS is declined.

#### TC-43 — Unblock a previously blocked debit card

Story: IB-114
Steps:
1. On a blocked debit card choose Unblock. 2. Confirm OTP if prompted.
Expected: Status returns to Active.

#### TC-44 — Set debit-card PIN with OTP

Story: IB-114
Steps:
1. Choose Set PIN. 2. Enter new PIN and confirm. 3. Submit OTP.
Expected: PIN change is accepted. Success message is shown. PIN value is never displayed.

#### TC-45 — Toggle international usage on a debit card

Story: IB-114
Steps:
1. Disable international usage. 2. Re-enable international usage.
Expected: Each toggle persists after refresh. Status matches the last action.

#### TC-46 — View bank max

Story: daily limit
Steps:
and remaining limit
Expected: IB-115

#### TC-47 — Increase daily transfer limit within bank maximum

Story: IB-108
Steps:
1. Set daily limit below bank max and above current usage. 2. Confirm OTP if required.
Expected: New daily limit is saved and remaining today is recalculated.

#### TC-48 — Reject daily limit above bank maximum

Story: IB-115
Steps:
1. Enter a daily limit greater than bank max. 2. Save.
Expected: Validation error is shown. Previous limit remains in force.

#### TC-49 — Enable SMS alerts for debits

Story: IB-116
Steps:
1. Open Alerts. 2. Enable SMS for debit transactions. 3. Save. 4. Logout and login.
Expected: SMS debit alerts show as On after re-login.

#### TC-50 — Disable email alerts for successful login

Story: IB-116
Steps:
1. Open Alerts. 2. Disable email login alerts. 3. Save.
Expected: Preference persists as Off. No email login alert is queued for the next login in the simulator.

## 4. First-week onboarding checklist

1. Read every story in section 2 in order (auth → accounts → money movement → servicing).
2. Walk the happy-path regression cases for login, account summary, own-account transfer, bill pay, and statements.
3. Re-run the negative pack: lockout, insufficient funds, limit breach, invalid beneficiary, expired OTP.
4. Note coverage gaps and ask an SME before automating those stories.
5. Confirm dual-control / cool-off rules on beneficiaries and high-value transfers with operations.

Suggested read order:

1. `IB-101` Retail customer login with user ID and password
2. `IB-102` Step-up MFA with SMS OTP on login from a new device
3. `IB-103` Idle session timeout and explicit logout
4. `IB-104` Account summary dashboard
5. `IB-105` Account details and recent transactions
6. `IB-106` Own-account funds transfer
7. `IB-107` Transfer to an approved beneficiary (IMPS/NEFT)
8. `IB-108` Per-transaction and daily transfer limits
9. `IB-109` Pay a registered biller
10. `IB-110` Register a new biller
11. `IB-111` Manage transfer beneficiaries with cool-off
12. `IB-112` Account statements view, download, and email
13. `IB-113` View debit and credit cards linked to the customer
14. `IB-114` Card controls: block, unblock, PIN set, international usage
15. `IB-115` Customer-managed channel limits
16. `IB-116` Transaction and login alerts
17. `IB-117` Lock internet banking after repeated failed logins
18. `IB-118` Insufficient funds and duplicate-transfer protection
19. `IB-119` Self-service password reset
20. `IB-120` Future-dated own and beneficiary transfers

## 5. Automation context

Automate in this order (highest regression value, stable oracles):

1. Login success / fail / lockout and session timeout.
2. Account summary balances vs ledger-style expected values in the master.
3. Own-account transfer and beneficiary transfer with limit and insufficient-funds negatives.
4. Bill pay + add biller.
5. Statement download and card block/unblock.
6. Alert opt-in/out last (channel-dependent).

Prefer UI or API cases that already name a story key and expected result in the regression master.
