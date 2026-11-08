# CLAUDE.md

Multi-tenant B2B SaaS management platform: one SaaS company (Super Admin + SaaS Staff)
manages many client organizations, each an isolated tenant. Full product spec:
[multi_tenant_saas_management_platform.md](multi_tenant_saas_management_platform.md).
ER diagram: [docs/er-diagram.md](docs/er-diagram.md).

## Stack

- Java 21, Spring Boot 4.1.x (Jakarta EE), Spring Data JPA / Hibernate 7
- PostgreSQL (Neon), `spring.jpa.hibernate.ddl-auto=update` — schema is generated from entities
- Lombok (`@Getter @Setter @NoArgsConstructor`; never `@Data` on entities)
- Maven wrapper: `./mvnw compile`, `./mvnw test`, `./mvnw spring-boot:run`

## Package layout (`com.mindhr`)

```
controller/   REST controllers            (empty — not yet built)
dto/          request/response records    (empty)
mapper/       entity <-> dto mapping      (empty)
service/      business logic              (empty)
repository/   Spring Data repositories    (empty)
entity/       JPA entities (done)
  base/       BaseEntity, Tenant
  enums/      all enum types (@Enumerated(STRING), VARCHAR(30))
```

Only the entity layer exists so far. Do not scaffold the other layers unless asked.

## Entity conventions

- Every entity extends `BaseEntity` (`id` IDENTITY, `createdAt`, `updatedAt` via Hibernate timestamps).
- Every organization-owned entity extends `Tenant`, which adds a **non-null**
  `organization_id` `@ManyToOne`. This is the multi-tenant isolation rule:
  `User -> Organization -> Data`. Never expose tenant data across organizations.
- All `@ManyToOne` / `@OneToOne` are `fetch = LAZY`. `@OneToMany` is the inverse side (`mappedBy`).
- Cascade `ALL + orphanRemoval` only where the child has no life outside the parent
  (history rows, documents, comments, sales activities). Otherwise no cascade.
- Table names are snake_case plural (`users`, `menu_items`); columns snake_case via `@Column(name=...)`.
- Reserved-word-ish fields are renamed at the column level: `key`/`value` -> `setting_key`/`setting_value`,
  `Contract.value` -> `contract_value`, `Notification.read` -> `is_read`, `PaymentMethod.defaultMethod` -> `is_default`.
- Money: `BigDecimal` `NUMERIC(12,2)` (`NUMERIC(14,2)` for `contracts.contract_value`), `currency` `CHAR(3)` default `USD`.
- Enums stored as strings, `length = 30`. Lengths: email 255, phone 30, names 150, URLs 500, free text `TEXT`.
- Join tables `user_roles` and `role_permissions` are `@ManyToMany` — there are no entity classes for them.

## Entities

**Identity & access (shared):** `User` (nullable `organization_id` = SaaS-side user), `Role`
(nullable `organization_id` = built-in; set = org custom role), `Permission` (`module` + `PermissionAction`).

**Platform scope (SaaS company):** `Organization`, `SubscriptionPlan`, `Subscription`, `SubscriptionHistory`,
`Invoice`, `Payment`, `Receipt`, `Refund`, `PaymentMethod`, `SupportTicket`, `TicketComment`, `Lead`,
`SalesActivity`, `ActivityLog` (nullable org), `SystemSetting`.

**Organization scope (`Tenant`):** `Employee`, `MenuCategory`, `MenuItem`, `MenuImage`, `Contract`,
`ContractDocument`, `ContractHistory`, `OrganizationSetting`, `Notification`.
(`Subscription`, `Invoice`, `Payment`, `Receipt`, `PaymentMethod`, `SupportTicket` are also `Tenant`:
they represent the org's relationship with the SaaS company and are visible to both sides.)

Dashboards, Reports and Usage are aggregate queries, not entities.

## Key flows (from the spec)

- **Onboarding:** Lead -> Demo -> Organization -> Owner User -> Subscription -> Payment -> activate.
- **Billing:** Subscription -> Invoice -> Payment (gateway) -> Receipt; Finance staff sees all orgs.
- **Support:** Org user creates `SupportTicket`; SaaS support staff is `assignedTo`; `TicketComment.internal`
  is staff-only.
- **Permissions:** Role -> Permission(module, action). Org Owner permissions are scoped to their own organization.

## Roles (user types)

`SUPER_ADMIN`, `SAAS_STAFF` (Subscription / Support / Finance / Sales via `Role`), `ORG_USER`
(Organization Owner, Manager, Employee, custom roles via `Role` with `organization_id`).

## Gotchas

- `application.properties` currently has the Neon connection string inline; move credentials to env vars
  before committing anything real.
- `ddl-auto=update` never drops or renames columns — renaming a field leaves the old column behind.
- The spec's `## Employees` block lists Attendance / Leave as UI features; they are intentionally **not**
  entities. Do not add them without being asked.

## HRM layer (workforce management)

Spec: `SME-Workforce-Management-SaaS.md`. All HR entities extend `Tenant`; boilerplate layers (DTO, mapper, repository,
service, controller) follow the same shape as the SaaS entities.

- **Org structure:** `Branch` (geofence lat/lng/radius, plan-limited), `Department`.
- **People:** `Employee` (employeeCode, national ID, emergency contact, branch, department, manager, employmentType; every
  employee created via the API gets a login `User` with the `EMPLOYEE` role for the mobile app), `EmployeePaymentAccount`,
  `SalaryStructure` (history), `EmployeeEvent` (timeline, written automatically on join/salary/position/branch changes).
- **Time:** `Shift`, `ShiftAssignment` (roster), `Attendance` (mobile check-in/out via `AttendanceActionController`; geofence + lateness).
- **Leave:** `LeaveType`, `LeaveBalance`, `LeaveRequest` (apply / approve / reject / cancel via `LeaveActionController`).
- **Requests:** `WorkRequest` (advance, loan, shift change, expense, ... approved by a manager).
- **Payroll:** `PayrollRun`, `Payslip` (rules in `PayrollService` Javadoc; Friday off, 3 lates = 1 day, no holiday calendar yet).
- **Assets and documents:** `Asset`, `AssetAssignment`, `EmployeeDocument`.

### Access model
- **What:** `Role` -> `Permission(module, action)`, enforced with `@PreAuthorize("hasAuthority('<MODULE>_<ACTION>')")`.
  HR modules: BRANCH, DEPARTMENT, EMPLOYEE, ATTENDANCE, SHIFT, LEAVE, PAYROLL, PAYSLIP, ASSET, DOCUMENT, REQUEST.
  SaaS staff hold none of these, so they cannot read client HR data (spec section 29).
- **Which rows:** `Role.dataScope` (`PLATFORM`, `ORGANIZATION`, `BRANCH`, `TEAM`, `SELF`); widest scope across a user's roles wins.
  Enforced by `DataScopeService` (visibleEmployeeIds / assertCanAccess / resolveEmployeeForWrite). Scope, employeeId and branchId
  are JWT claims, so changes apply at next login.
- **Built-in roles per organization** (`OrgRoleProvisioner`, created on organization creation and backfilled at startup by
  `OrgRoleBackfillRunner`): `ORG_OWNER`, `HR_ADMIN` (ORGANIZATION), `BRANCH_MANAGER` (BRANCH), `TEAM_LEAD` (TEAM), `EMPLOYEE` (SELF).
- **Plan limits:** `PlanLimitService` enforces `maxEmployees`, `maxBranches` and `PlanFeature` (PAYROLL and ASSETS are feature-gated).
  An organization with no TRIAL/ACTIVE/GRACE_PERIOD subscription cannot add employees or branches.
