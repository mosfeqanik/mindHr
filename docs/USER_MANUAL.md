# mindHR user manual: what to enter, and in what order

This manual explains how to fill mindHR with data so the web app and the mobile app work.
Follow the steps in order: each step needs the data from the steps before it.

- **Web app:** http://localhost:5173 (or your server address)
- **Mobile app:** the Android app in `mobile_app/`
- **Swagger UI** (for the few things the web app cannot do yet): http://localhost:8080/swagger-ui.html
- **Just want to try it?** Ready-made QA accounts and data: [QA_CREDENTIALS.md](QA_CREDENTIALS.md)

---

## 0. Before you start

| You need | Where it comes from |
|---|---|
| The backend running | see [README.md](../README.md#run) |
| A **Super Admin** login | the first start creates it from `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` in `backend/.env` |

**Who does what**

| Who | Signs in to | Does |
|---|---|---|
| Super Admin (you, the SaaS company) | Web app | Steps 1 and 2: plans, organizations, owners |
| Organization owner or HR admin | Web app | Steps 3 to 8: branches, employees, leave, shifts, payroll |
| Employee | Mobile app | Step 9: check in, apply for leave, send requests |
| Manager (Team Lead / Branch Manager) | Mobile app or web app | Step 10: approve leave and requests |

---

## 1. Create a subscription plan (Super Admin)

Web app → **Subscription Plans** → **Add plan**

| Field | What to enter | Example |
|---|---|---|
| Name | Plan name | `Standard` |
| Price, Currency | Monthly or yearly price | `49.00`, `USD` |
| Billing cycle | `MONTHLY` or `YEARLY` | `MONTHLY` |
| Max employees | Maximum number of employees (empty = no limit) | `50` |
| Max branches | Maximum number of branches (empty = no limit) | `3` |
| Trial days | Free days before payment | `14` |
| Features | Tick what the organization may use | tick **ATTENDANCE, LEAVE, SHIFTS, PAYROLL, ASSETS, MOBILE_APP** |

> ⚠️ If a feature is not ticked, its menu is hidden in the web app, and Payroll and Assets are blocked by the server.
> For a full test, tick every feature.

## 2. Create the organization and its owner (Super Admin)

Web app → **Organizations** → **Add organization**

| Field | What to enter |
|---|---|
| Name, Email, Phone, Address | The client company's details |
| **Subscription plan** | The plan from step 1 (required) |
| Trial ends | Optional; when the trial stops |

Then press **Set owner** on the new organization and enter the owner's **Full name**, **Email (login)** and
**Password**. The owner gets the `ORG_OWNER` role and can do everything inside this organization.

Every new organization automatically gets 5 roles:

| Role | Sees | Can approve leave / requests |
|---|---|---|
| `ORG_OWNER` | whole organization | ✅ |
| `HR_ADMIN` | whole organization | ✅ (not payroll approval) |
| `BRANCH_MANAGER` | own branch | ✅ |
| `TEAM_LEAD` | own direct reports (employees whose **manager** is them) | ✅ |
| `EMPLOYEE` | only themselves | ❌ |

> The organization needs an active subscription (trial, active or grace period). Without one you cannot add
> employees or branches.

From now on, **sign in as the organization owner** (log out of the Super Admin account). A Super Admin can also do
these steps after choosing the organization in the top bar.

## 3. Create branches

Web app → **Branches** → **Add branch**

| Field | What to enter | Example |
|---|---|---|
| Name, Code | Branch name and short code | `Head Office`, `HO` |
| Address, Phone | Contact details | |
| Manager | Leave empty for now; set it after step 5 | |
| **Latitude, Longitude** | The office location (decimal degrees) | `23.7808`, `90.4067` |
| **Geofence radius (m)** | How far from the office an employee may check in | `150` |

**How to get the latitude and longitude:** open Google Maps, press and hold on the office, and copy the two numbers
shown at the top (for example `23.7808, 90.4067`).

> 📍 The location check works **only if all three fields are filled** (latitude, longitude and radius) **and** the
> employee belongs to this branch (step 6). Otherwise the check-in is accepted from anywhere.
> Use 100–200 m: phone GPS is often 10–50 m off.

## 4. Create departments (optional)

Web app → **Departments** → **Add department**: **Name**, **Branch**, **Head** (can be set later).

## 5. Create employees

Web app → **Employees** → **Add employee**

| Field | What to enter |
|---|---|
| Full name | Required |
| **Email** | Required for the mobile app: this is the **login email**. Must be unique across the whole system |
| Phone | Must be unique inside the organization |
| Position | Job title, e.g. `Sales Executive` |
| Employment type | `FULL_TIME`, `PART_TIME`, `CONTRACT`, `INTERN` or `PROBATION` |
| Joined on | Start date |
| **Initial password (login)** | The password the employee uses in the mobile app (give it to them) |

Saving creates the employee **and** their login, with the `EMPLOYEE` role. They can now sign in to the mobile app with
this email and password.

Create at least **two** employees so you can test approvals: one worker and one manager.

## 6. Set branch, department and manager (Swagger, for now)

The web form cannot set these fields yet. Without a **branch**, the location check does not run; without a
**manager**, a Team Lead cannot see the employee's leave and requests.

1. Open http://localhost:8080/swagger-ui.html.
2. Run `POST /api/auth/login` with the owner's email and password. Copy `accessToken`.
3. Click **Authorize** (top right), paste the token, click **Authorize**.
4. Run `GET /api/employees` and note the `id` of each employee, and `GET /api/branches` for the branch `id`.
5. For each employee, run `GET /api/employees/{id}`, copy the whole response, change these fields and send it with
   `PUT /api/employees/{id}`:

```json
{
  "...": "all other fields exactly as returned by GET",
  "branchId": 3,
  "departmentId": 2,
  "managerId": 7
}
```

> ⚠️ `PUT` **replaces** the whole employee. Always start from the GET response, or fields you leave out become empty.

The changes reach the mobile app at the employee's **next sign-in** (the branch is stored in the login token).

### Make someone a manager (roles)

The web app has no screen for roles yet, so use Swagger with the **Super Admin** account:

1. Signed in as Super Admin, run `GET /api/roles?organizationId=<org id>` and note the id of `TEAM_LEAD` (or
   `BRANCH_MANAGER`).
2. Run `GET /api/users/{userId}` for the manager's login (the employee's `userId`).
3. Send it back with `PUT /api/users/{userId}`, with `roleIds` containing **both** the `EMPLOYEE` role id and the new
   role id (the list replaces all roles). Leave `password` empty to keep the current one.

The manager must sign out and sign in again in the mobile app. The **Approvals** screen then appears under **More**.

## 7. Set up leave

Web app → **Leave**

**a) Leave types** (tab *Leave types* → *Add leave type*)

| Field | Example | Notes |
|---|---|---|
| Name | `Annual Leave`, `Sick Leave`, `Unpaid Leave` | |
| Annual days | `18`, `14`, `0` | Days each employee gets per year |
| Paid | on for Annual and Sick, off for Unpaid | Paid types are limited by the balance; unpaid types are not |
| Active | on | Inactive types cannot be chosen |

**b) Leave balances** (tab *Balances* → *Add leave balance*): **Employee**, **Leave type**, **Year**,
**Entitled days**, **Used days** (usually `0`).

> If you skip balances, mindHR creates one automatically from the type's **Annual days** the first time an employee
> applies. Add them yourself if you want the mobile app to show a balance before the first application.

**Leave rules**
- Days are **calendar days**, weekends included (Mon–Fri = 5, Thu–Sat = 3).
- One request cannot cross into the next year; apply twice.
- Dates cannot overlap another pending or approved leave.
- Approved leave days show as **On leave** in attendance, and the employee cannot check in on those days.

## 8. Set up shifts (for late marks)

Web app → **Shifts**

1. **Shifts** tab → *Add shift*: **Name** `Morning`, **Start time** `09:00`, **End time** `18:00`.
2. **Roster** tab → *Assign shift*: **Employee**, **Shift**, **Work date**. One entry per employee per day.

When an employee checks in **after** the shift's start time, they are marked **Late** (there is no grace period:
09:01 is late). With no roster entry for the day, the check-in is simply **Present**.

## 9. Use the mobile app (employee)

Install it ([README](../README.md#run): `./gradlew installDebug`), then:

1. **First start:** swipe through the 3 intro pages, then sign in with the employee's **email** and **initial
   password** from step 5.
2. **Check in:** Home → **Check in**. Allow location. The sheet shows whether you are inside the office area, then
   press **Check in** (remarks are optional). At the end of the day press **Check out**.
   - If you are outside the area, the server refuses and shows how far away you are.
   - Testing on the emulator? Set a fake location: emulator **⋮ Extended controls → Location**, type the branch's
     latitude/longitude, press **Set location**.
3. **Apply for leave:** Leave tab → **Apply** → choose type, dates and reason → **Submit application**. Pending leave
   can be cancelled from the same list.
4. **Send a request:** More → **My requests** → **New request**. Types:

   | Type | Amount needed |
   |---|---|
   | Salary advance, Loan, Expense claim | ✅ |
   | Attendance correction, Work from home, Overtime, Late arrival, Early exit, Shift change, Asset request | — |

   Forgot to check in? Attendance tab → tap the day → **Continue**: the form opens as an *Attendance correction* for
   that date. Write the correct times in the details.
5. **Payslips:** More → **Payslips** (appear after step 11). Tap one to see earnings and deductions.
6. **Change password:** More → **My profile** → **Change password** (at least 8 characters).

## 10. Approve (manager)

**Mobile app:** More → **Approvals** (or the *To approve* card on Home). Tabs:
- **Leave** and **Requests**: press **Approve** or **Reject**, add an optional note, confirm.
- **Team today**: who has checked in today.

**Web app:** **Leave** → *Requests* tab → approve / reject.

> A manager sees only the people in their scope: Team Lead = employees whose **manager** is them (step 6);
> Branch Manager = their **branch**; Owner / HR Admin = everyone. Nobody can approve their own leave or request.

## 11. Run payroll (owner)

Payroll needs a **salary structure** for each employee (basic salary and allowances). Add it in Swagger with
`POST /api/salary-structures` (fields: `employeeId`, `basicSalary`, `allowances`, `effectiveFrom`).

Web app → **Payroll** → **Generate payroll run**: **Year**, **Month**, **Branch** (empty = whole organization),
**Note**. Then open the run:

1. Check each payslip; **Adjust** a draft payslip if needed (bonus, commission, overtime, deductions).
2. **Finalize** → **Pay**. Employees see the payslip in the mobile app.

Payroll rules: Friday is the weekly day off, 3 late marks = 1 day deducted, no holiday calendar yet.

---

## Quick checklist for a full test

| ✔ | Step | Result |
|---|---|---|
| ☐ | Plan with all features | menus visible in the web app |
| ☐ | Organization + owner | owner can sign in to the web app |
| ☐ | Branch with latitude, longitude, radius | location check works |
| ☐ | 2 employees with email + initial password | both can sign in to the mobile app |
| ☐ | Swagger: `branchId` + `managerId` on the worker; `TEAM_LEAD` role on the manager | Approvals screen appears for the manager |
| ☐ | Leave types (+ balances) | Leave tab shows balance; apply works |
| ☐ | Shift + roster for today | late check-in is marked Late |
| ☐ | Salary structure + payroll run, finalized | payslip visible in the mobile app |

## Common messages and what to do

| Message | Cause | Fix |
|---|---|---|
| *Invalid email or password* | wrong login | check the email from step 5; a Super Admin can set a new password on the **Users** page (Edit → New password) |
| *Your account has no employee profile* | signed in as Super Admin or a user made without the Employees page | sign in as an employee |
| *You are outside the allowed area for … (N m away, limit M m)* | too far from the branch | go closer, or increase the radius (step 3) |
| *Location is required to check in at this branch* | location permission denied or GPS off | allow location for mindHR and turn on GPS |
| *Already checked in today* | second check-in | use **Check out** |
| *You are on approved leave today* | approved leave covers today | nothing to do |
| *Insufficient Annual Leave balance: X day(s) available* | not enough days (pending requests count too) | add days in **Leave → Balances**, or use an unpaid type |
| *You already have leave requested or approved for these dates* | overlapping leave | change the dates or cancel the old request |
| *You cannot decide your own leave request* | approving your own | another manager must approve it |
| *Cannot reach the server* (mobile) | backend not running, or wrong address on a real phone | start the backend; build with `-PapiBaseUrl=http://<pc-ip>:8080/` |
| Plan limit message when adding an employee or branch | plan maximum reached, or no active subscription | raise the plan's maximum (step 1) or renew the subscription |
