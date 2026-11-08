# mindHR: users and flows

Diagrams of **who uses mindHR** and **how the main tasks flow**. They render on GitHub and in VS Code's Markdown
preview (Mermaid). For the data you must enter first, see [USER_MANUAL.md](USER_MANUAL.md). For the database
tables (ER diagram with columns), see [backend/docs/er-diagram.md](../backend/docs/er-diagram.md); the README has a
relationships-only overview.

- [1. Users and roles](#1-users-and-roles)
- [2. What each user can do](#2-what-each-user-can-do)
- [3. Who sees which data](#3-who-sees-which-data)
- [4. Setup flow](#4-setup-flow-first-time)
- [5. Mobile app screens](#5-mobile-app-screens)
- [6. Check-in / check-out](#6-check-in--check-out)
- [7. Leave](#7-leave)
- [8. Requests](#8-requests-advance-loan-expense-correction-)
- [9. Payroll](#9-payroll)
- [10. Sign-in and session](#10-sign-in-and-session)

---

## 1. Users and roles

Two sides: the **SaaS company** that sells mindHR, and each **client organization** (tenant). A user belongs to one
side; organization users get one or more of the 5 built-in roles.

```mermaid
flowchart TB
    subgraph SAAS["SaaS company (platform)"]
        SA["Super Admin<br/>all organizations, billing, users, roles"]
        ST["SaaS Staff<br/>support / finance / sales<br/>no HR data"]
    end

    subgraph ORG["Client organization (tenant)"]
        OW["ORG_OWNER<br/>whole organization"]
        HR["HR_ADMIN<br/>whole organization"]
        BM["BRANCH_MANAGER<br/>own branch"]
        TL["TEAM_LEAD<br/>direct reports"]
        EM["EMPLOYEE<br/>only self"]
    end

    SA -- creates organization + owner --> OW
    OW -- creates --> HR
    OW -- "creates employees (login + EMPLOYEE role)" --> EM
    HR -- creates employees --> EM
    SA -. assigns roles .-> BM
    SA -. assigns roles .-> TL
    BM -- manages --> EM
    TL -- "manages (managerId)" --> EM

    classDef saas fill:#1a2f4e,color:#fff,stroke:#1a2f4e
    classDef org fill:#e6f5f8,color:#026a80,stroke:#0298b6
    class SA,ST saas
    class OW,HR,BM,TL,EM org
```

| User type | Role(s) | Uses | Data scope |
|---|---|---|---|
| `SUPER_ADMIN` | platform | Web app | every organization |
| `SAAS_STAFF` | platform role (Support, Finance, Sales) | Web app | platform data only, **no HR data** |
| `ORG_USER` | `ORG_OWNER` | Web app (+ mobile) | `ORGANIZATION` |
| `ORG_USER` | `HR_ADMIN` | Web app (+ mobile) | `ORGANIZATION` |
| `ORG_USER` | `BRANCH_MANAGER` | Mobile + web | `BRANCH` |
| `ORG_USER` | `TEAM_LEAD` | Mobile + web | `TEAM` |
| `ORG_USER` | `EMPLOYEE` | Mobile app | `SELF` |

A user with several roles gets all their permissions and the **widest** scope.

## 2. What each user can do

```mermaid
flowchart LR
    SA(("Super Admin"))
    OW(("Owner /<br/>HR Admin"))
    MG(("Manager<br/>Branch / Team"))
    EM(("Employee"))

    subgraph PLATFORM["Platform (web)"]
        P1[Manage plans]
        P2[Create organizations + owners]
        P3[Subscriptions, invoices, payments]
        P4[Users and roles]
        P5[Support tickets, contracts]
    end

    subgraph SETUP["HR setup (web)"]
        S1[Branches + geofence]
        S2[Departments]
        S3[Employees + logins]
        S4[Leave types + balances]
        S5[Shifts + roster]
        S6[Payroll runs + payslips]
        S7[Assets + documents]
    end

    subgraph SELF["Self-service (mobile)"]
        M1[Check in / out]
        M2[My attendance + correction]
        M3[Apply / cancel leave]
        M4[Requests: advance, loan, expense ...]
        M5[Payslips]
        M6[Notifications, profile, password]
    end

    subgraph APPROVE["Approvals (mobile or web)"]
        A1[Approve / reject leave]
        A2[Approve / reject requests]
        A3[Team attendance today]
    end

    SA --> P1 & P2 & P3 & P4 & P5
    OW --> S1 & S2 & S3 & S4 & S5 & S6 & S7
    OW --> APPROVE
    MG --> APPROVE
    MG --> SELF
    EM --> SELF
```

| Action | Super Admin | Owner | HR Admin | Branch Mgr | Team Lead | Employee |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| Plans, organizations, billing | ✅ | | | | | |
| Users and roles | ✅ | | | | | |
| Branches, departments, employees | ✅* | ✅ | ✅ | view | view | view |
| Leave types, balances, shifts | ✅* | ✅ | ✅ | shifts | view | view |
| Payroll runs | ✅* | ✅ | ✅ (no approve) | | | |
| Check in / apply leave / requests | | ✅ | ✅ | ✅ | ✅ | ✅ |
| Approve leave and requests | ✅* | ✅ | ✅ | ✅ | ✅ | |
| See payslips | ✅* | all | all | branch | team | own |

\* after picking the organization in the web app's top bar. Nobody can approve their own leave or request.

## 3. Who sees which data

Every list the API returns is filtered by the caller's **data scope** (a JWT claim, refreshed at sign-in).

```mermaid
flowchart TB
    R[API request with JWT] --> T{User type?}
    T -- SUPER_ADMIN --> PL[All organizations<br/>or the one picked in the top bar]
    T -- SAAS_STAFF --> NO[Platform data only<br/>HR endpoints: 403]
    T -- ORG_USER --> S{Widest data scope<br/>of the user's roles}
    S -- ORGANIZATION --> A[Every employee<br/>in the organization]
    S -- BRANCH --> B[Employees of the<br/>user's branch]
    S -- TEAM --> C[The user + employees<br/>whose manager is the user]
    S -- SELF --> D[Only the user's<br/>own records]
```

## 4. Setup flow (first time)

What must exist before the apps work. Details for each box are in the [manual](USER_MANUAL.md).

```mermaid
flowchart TD
    A([Start: backend running]) --> B[Super Admin signs in to the web app]
    B --> C["Create subscription plan<br/>limits + features"]
    C --> D["Create organization<br/>choose the plan"]
    D --> E["Set owner<br/>name, email, password"]
    E --> F[Owner signs in]
    F --> G["Add branch<br/>latitude, longitude, radius"]
    G --> H[Add departments]
    H --> I["Add employees<br/>email + initial password"]
    I --> J{"Branch / manager<br/>needed?"}
    J -- yes --> K["Swagger: PUT employee<br/>branchId, departmentId, managerId"]
    J -- no --> L
    K --> L{"Managers<br/>needed?"}
    L -- yes --> M["Super Admin in Swagger:<br/>PUT user roleIds += TEAM_LEAD"]
    L -- no --> N
    M --> N["Leave types<br/>+ balances"]
    N --> O["Shifts + roster<br/>for late marks"]
    O --> P["Salary structures<br/>for payroll"]
    P --> Q([Employees sign in to the mobile app])
```

## 5. Mobile app screens

```mermaid
flowchart TD
    START([Open app]) --> ON{First time?}
    ON -- yes --> OB[Onboarding: 3 slides] --> LG
    ON -- no --> TK{Saved login?}
    TK -- no --> LG[Sign in<br/>email + password]
    TK -- yes --> HOME
    LG -- success --> HOME

    subgraph TABS["Bottom tabs"]
        HOME[Home]
        ATT[Attendance]
        LV[Leave]
        MORE[More]
    end

    HOME --> CI[Check-in / out sheet]
    HOME --> NT[Notifications]
    HOME --> PF
    ATT --> CORR[Tap a day → correction request]
    LV --> AL[Apply for leave]
    MORE --> PS[Payslips]
    MORE --> RQ[My requests] --> NR[New request]
    MORE --> AP{Approver?}
    AP -- yes --> APR["Approvals<br/>Leave / Requests / Team today"]
    MORE --> PF[Profile] --> CP[Change password]
    MORE --> SET[24-hour clock]
    MORE --> OUT[Sign out] --> LG
    CORR --> NR
```

## 6. Check-in / check-out

```mermaid
flowchart TD
    A[Home → Check in] --> P{Location<br/>permission?}
    P -- denied --> PE[Message: allow location<br/>in settings] --> X([Stop])
    P -- allowed --> G{GPS fix?}
    G -- no --> GE[Message: turn on GPS<br/>Try again] --> G
    G -- yes --> D["App shows distance to branch<br/>(if the branch has a geofence)"]
    D --> R[Optional remarks → Check in]
    R --> S{Server checks}
    S -- on approved leave today --> E1[Refused:<br/>You are on approved leave today]
    S -- already checked in --> E2[Refused:<br/>Already checked in today]
    S -- "branch has geofence AND<br/>outside radius" --> E3[Refused:<br/>N m away, limit M m]
    S -- ok --> SH{Shift on roster<br/>for today?}
    SH -- no --> PR[Status PRESENT]
    SH -- yes --> L{After shift start?}
    L -- no --> PR
    L -- yes --> LT[Status LATE<br/>+ late minutes]
    PR --> OK([Saved: Home shows check-in time])
    LT --> OK
    OK --> CO[Later: Check out → worked minutes]
```

## 7. Leave

```mermaid
sequenceDiagram
    actor E as Employee (mobile)
    participant API as mindHR API
    actor M as Manager (mobile / web)

    E->>API: Apply (type, from, to, reason)
    API->>API: Check dates, same year, no overlap,<br/>type active, paid → enough balance
    alt invalid
        API-->>E: Error message (e.g. Insufficient balance)
    else valid
        API-->>E: Request PENDING
        Note over E: Can cancel while PENDING
        M->>API: List pending leave in my scope
        alt approve
            M->>API: Approve (+ note)
            API->>API: Add days to used balance,<br/>mark days ON_LEAVE in attendance
            API-->>E: Status APPROVED
        else reject
            M->>API: Reject (+ reason)
            API-->>E: Status REJECTED
        end
    end
```

```mermaid
stateDiagram-v2
    [*] --> PENDING: employee applies
    PENDING --> APPROVED: manager approves
    PENDING --> REJECTED: manager rejects
    PENDING --> CANCELLED: employee cancels
    APPROVED --> [*]
    REJECTED --> [*]
    CANCELLED --> [*]
```

## 8. Requests (advance, loan, expense, correction ...)

```mermaid
flowchart LR
    A[Employee: New request] --> T{Type}
    T -- "Salary advance, Loan,<br/>Expense claim" --> AM[Title + amount + date]
    T -- "Attendance correction, WFH,<br/>Overtime, Late arrival, Early exit,<br/>Shift change, Asset request" --> NA[Title + date + details]
    AM --> P[PENDING]
    NA --> P
    P -- employee cancels --> C[CANCELLED]
    P -- manager approves --> AP[APPROVED]
    P -- manager rejects --> RJ[REJECTED]
    AP -- "advance / loan" --> PAY[Deducted in a later payroll run]
    AP -- attendance correction --> HRF[HR fixes the attendance record<br/>on the web Attendance page]
```

## 9. Payroll

```mermaid
flowchart TD
    A[Salary structures exist<br/>basic + allowances] --> B[Owner: Generate payroll run<br/>year, month, branch]
    B --> C["Payslip per employee<br/>gross = basic + allowances + overtime + bonus + commission"]
    C --> D["Deductions<br/>absent days, every 3 lates = 1 day,<br/>loan, advance"]
    D --> E[Run is DRAFT]
    E --> F{Adjust?}
    F -- yes --> G[Adjust payslip:<br/>bonus, commission, overtime, deductions] --> E
    F -- no --> H[Finalize]
    H --> I[Pay]
    I --> J([Employee sees the payslip<br/>in the mobile app])
```

Rules: Friday is the weekly day off; approved leave days are covered; no holiday calendar yet.

## 10. Sign-in and session

```mermaid
sequenceDiagram
    actor U as User
    participant App as Mobile / web app
    participant API as mindHR API

    U->>App: Email + password
    App->>API: POST /api/auth/login
    API-->>App: JWT (userId, organizationId, employeeId,<br/>branchId, dataScope, authorities)
    App->>App: Save token, read claims<br/>(show Approvals if LEAVE_APPROVE / REQUEST_APPROVE)
    loop Every request
        App->>API: Authorization: Bearer token
        API->>API: Permission check + data scope filter
        API-->>App: Data
    end
    alt token expired (401)
        API-->>App: 401
        App->>U: Back to sign-in
    end
    Note over App,API: Role, branch or manager changes apply<br/>at the next sign-in (new token)
```
