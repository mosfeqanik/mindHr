# QA test accounts and data

Logins and test data for checking mindHR by hand (web app, mobile app, Swagger). Everything here was created by the
end-to-end test script [`backend/scripts/api_e2e_test.py`](../backend/scripts/api_e2e_test.py). Run it again any time
to re-create missing data (it reuses what already exists).

> ⚠️ **Test data only.** These accounts live in the same database as the backend in `backend/.env`. Do not use these
> passwords anywhere real, and remove or disable these accounts before going to production.

## Where to sign in

| App | URL / how |
|---|---|
| Web app | http://localhost:5173 |
| API / Swagger | http://localhost:8080/swagger-ui.html (`POST /api/auth/login`, then **Authorize**) |
| Android app (emulator) | `cd mobile_app && sh ./gradlew installDebug` (talks to `http://10.0.2.2:8080/`) |

## Accounts

All QA accounts use the same password: **`QaTest@123`**

| Email | Role | Data scope | Use it to test |
|---|---|---|---|
| `qa.owner@mindhr.test` | `ORG_OWNER` | whole organization | Web app: employees, branches, leave setup, shifts, payroll, approvals |
| `qa.lead@mindhr.test` | `EMPLOYEE` + `TEAM_LEAD` | own team (emp1, emp2) | Mobile **Approvals** screen, approving / rejecting leave and requests |
| `qa.emp1@mindhr.test` | `EMPLOYEE` | only self | Mobile app: check-in, leave, requests, payslips, notifications |
| `qa.emp2@mindhr.test` | `EMPLOYEE` | only self | A second employee in the same team (data-scope checks) |

**Super Admin** (platform side: organizations, plans, users, roles): email and password are `SEED_ADMIN_EMAIL` and
`SEED_ADMIN_PASSWORD` in `backend/.env`. They are not copied here on purpose.

## Test data

| Item | Value |
|---|---|
| Organization | **QA Test Org** (status ACTIVE) |
| Plan | **QA Full Plan**: every feature, 100 employees, 10 branches, 365 trial days |
| Branch | **QA Head Office** (`QAHO`), latitude `23.7808`, longitude `90.4067`, geofence radius **150 m** |
| Department | QA Operations |
| Employees | EMP-0001 QA Team Lead, EMP-0002 QA Employee One, EMP-0003 QA Employee Two (all in QA Head Office; emp1 and emp2 report to the lead) |
| Leave types | QA Annual (18 days, paid), QA Sick (14 days, paid), QA Unpaid (unpaid) |
| Leave balances | QA Annual, 18 days, current year, for all three employees |
| Shift | QA Morning 09:00–18:00 (emp1 is rostered on the day the script ran) |
| Salaries | Lead 60,000, emp1 40,000, emp2 38,000 (+5,000 allowances, BDT) |
| Payroll | A paid run for last month, so payslips show in the mobile app |
| Asset | QA Laptop (`QA-LAPTOP-1`), assigned to emp1 |

## Handy test scenarios

| Scenario | Steps | Expected |
|---|---|---|
| Check in inside the office | Emulator **⋮ → Location**: `23.7809, 90.4068` → Set location; sign in as emp1 → **Check in** | Saved: Present (or Late after 09:00) |
| Check in outside the office | Set location `23.8103, 90.4125` (~3 km away) → **Check in** | Refused: "You are outside the allowed area … (N m away, limit 150 m)" |
| Leave approval | emp1 applies for leave → sign in as lead → More → Approvals → Approve | emp1 sees APPROVED |
| Not enough balance | emp1 applies QA Annual for more than 18 days | "Insufficient QA Annual balance …" |
| Can't approve yourself | lead applies leave, then tries to approve it | 403 "You cannot decide your own leave request" |
| Data scope | emp1 opens Swagger `GET /api/employees` | only emp1 is returned |
| Attendance correction | emp1: Attendance tab → tap a day → Continue | request type "Attendance correction" for that date |

Already checked in today? Each employee can check in once per day; use another QA employee or wait until tomorrow.

## Other organizations in this database

Existing organizations (Acme, acme Ltd Company, sewtex, shoyong) belong to other test runs. Their passwords are not
stored anywhere readable (only hashes). Use the QA accounts above, or ask the Super Admin to set a new password on the
**Users** page.
