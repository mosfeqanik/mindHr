# English practice

I am learning English. In every reply, including replies to slash commands such as `/commit-push`, if my message has grammar, spelling or wording mistakes, start with a short "English tip": show the corrected sentence and briefly explain the main mistakes in simple English. Keep it short and kind, then do the task. Skip the tip if my message is already correct.

# Project

mindHR: HRM / workforce management multi-tenant SaaS. Spring Boot API in `backend/`; React web app in `frontend/`;
Android employee app in `mobile_app/`. How to enter data so everything works: [docs/USER_MANUAL.md](docs/USER_MANUAL.md).
Deeper backend notes (entities, access model, conventions): [backend/CLAUDE.md](backend/CLAUDE.md). ER diagram: [backend/docs/er-diagram.md](backend/docs/er-diagram.md).

## Run commands

All commands run from `backend/`.

```bash
# Docker Compose (builds the image, starts the API on :8080)
docker compose up --build -d      # compose auto-reads backend/.env
docker compose logs -f app
docker compose down

# Local (no Docker)
./mvnw spring-boot:run
./mvnw compile
./mvnw test
./mvnw clean package -DskipTests  # jar in target/
```

- `docker-compose.yml` has one service, `app` (container `mindhr-api`). There is **no local Postgres container**:
  the database is Neon, given by `DB_URL`.
- Needed in `backend/.env` (gitignored; `application.properties` also imports it for local runs): `DB_URL`, `JWT_SECRET`.
  Optional: `JWT_EXPIRATION_MS` (default 3600000), `SEED_ENABLED` (default true), `SEED_ADMIN_EMAIL`, `SEED_ADMIN_PASSWORD`.
- First start seeds one `SUPER_ADMIN` (`DataSeeder`) when no user exists. Change its password after first login.
- Dockerfile is multi-stage: Maven 3.9 + Temurin 21 build, then `eclipse-temurin:21-jre` runtime.
- Swagger UI: `http://localhost:8080/swagger-ui.html` (springdoc; spec at `/v3/api-docs`). Auth is manual: log in, click Authorize, paste the JWT.
- Postman: `backend/postman/mindhr.postman_collection.json` is generated from the controllers and DTOs.
  After adding or changing endpoints, run `python postman/generate_postman.py` from `backend/`. Do not add token-saving scripts; the user pastes the token into the `token` variable.

## Notes

- Never commit or print `backend/.env` (it holds the real Neon credentials and JWT secret). Its `DB_URL` contains `&`,
  so `source .env` fails in zsh; read single keys with `grep '^KEY=' .env | cut -d= -f2-`.
- `backend/CLAUDE.md` still says only the entity layer exists; controllers, services, DTOs and repositories are now built.
- **E2E API test:** `python3 scripts/api_e2e_test.py` (from `backend/`, backend running) seeds a "QA Test Org" with
  QA logins (`qa.owner|qa.lead|qa.emp1|qa.emp2@mindhr.test`, platform `qa.admin|qa.staff@mindhr.test`, password `QaTest@123`) and runs ~150 checks: setup,
  check-in/geofence, leave, requests, approvals, payroll, data scope, tenant isolation. Re-runnable; exits 1 on any FAIL.
- Unreadable request bodies (bad JSON, unknown enum value) return 400 via `GlobalExceptionHandler`; `/error` is
  `permitAll` so Spring's error forward no longer turns errors into 401.
- `mvnw` is not executable in git; run it as `sh ./mvnw ...` (same for `mobile_app/gradlew`).
- **Adding a value to an existing enum:** Hibernate created a `CHECK (col IN (...))` constraint when the table was made and
  `ddl-auto=update` never refreshes it, so inserts with the new value fail. Add `table.column` to
  `EnumCheckConstraintRunner.COLUMNS` (drops the stale constraint at startup). Done for `work_requests.request_type`
  (`ATTENDANCE_CORRECTION`).
- `POST /api/account/change-password` (`AccountController` -> `AuthService.changePassword`) is for the signed-in user.
  It is not under `/api/auth/**` because that path is `permitAll`.
- `PUT /api/employees/{id}` and `PUT /api/users/{id}` replace the whole record: send every field back (GET it first).
  `roleIds` on a user replaces all roles.
- The web app cannot yet set an employee's branch, department or manager; use Swagger (see the manual). Roles are
  set on the Users page (Super Admin).
- UI names for user types live in `frontend/src/lib/format.ts` (`USER_TYPE_LABELS`): `SAAS_STAFF` shows as
  "mindHR Staff". `Field` in `ui.tsx` takes `required` to show a red `*`.
- **User mobile number is required** (`UserRequest.phone`: `@NotBlank` + Bangladeshi mobile pattern
  `^(\+?88)?01[3-9]\d{8}$`). The web forms (Users page, Organizations > Set owner) use `MOBILE_NUMBER_RE` and
  `cleanPhone` from `frontend/src/lib/format.ts`; keep the two patterns in sync.
- **User types:** `SUPER_ADMIN`, `ADMIN`, `SAAS_STAFF`, `ORG_USER` (all but ORG_USER are platform staff in
  `TenantContext.isPlatformStaff`). Super Admins cannot be created (form hides the type; `UserService.create` rejects it,
  `update` locks the type both ways); only a Super Admin may edit/delete a Super Admin (`guardSuperAdminAccount`);
  nobody deletes their own account. An `ADMIN` is platform staff who can manage users, with platform role `ADMIN`
  (pre-ticked in the form); plans, permissions, settings and role edits stay Super Admin only.
- **Users and roles are managed by the Super Admin or an Admin:** `UserService` create/update/delete (incl. passwords)
  call `TenantContext.requireUserManager()`; user and role reads need platform staff (`requirePlatformStaff`, so mindHR
  Staff can view the Users page and assign support tickets). Org users get 403. `resolveRoles` only accepts roles from
  the user's own side (org roles for an org user, platform roles for a platform user). Signed-in users still change
  their own password via `POST /api/account/change-password`.
- **mindHR Staff roles** (`PlatformRoleProvisioner`, created at startup if missing): platform roles `ADMIN`, `HR_ADMIN`,
  `ACCOUNTS`, `TEAM_LEAD`, `EMPLOYEE` (no organization, no permissions yet: platform pages are guarded by user type).
  The platform `SUPER_ADMIN` role grants `hasRole('SUPER_ADMIN')` (plans, permissions, system settings), so
  `UserService.resolveRoles` only allows it on SUPER_ADMIN users. **A user holds exactly one role** (`resolveRoles`
  rejects more; the web form is single-choice; `grantOwnerRole` replaces roles with ORG_OWNER). So TEAM_LEAD also
  carries BRANCH/DEPARTMENT VIEW (topped up on existing orgs by `OrgRoleProvisioner.provision`). The web form requires at least one role for every
  user, shows the `SUPER_ADMIN` role only for a Super Admin and pre-ticks it (or `ADMIN` for an Admin).

## Frontend (`frontend/`)

Vite + React + TypeScript, FreeDash admin template (Bootstrap 5 CSS in `public/template`, `react-feather` icons; no Tailwind), React Router, TanStack Query, React Hook Form + Zod, axios. Node 22+.

```bash
# from frontend/
npm install
npm run dev        # http://localhost:5173, proxies /api -> http://localhost:8080 (the backend has no CORS config)
npm run build      # tsc -b && vite build

# Docker: start the backend compose first (it creates the backend_mindhr-net network), then:
docker compose up --build -d   # http://localhost:3000, nginx serves the SPA and proxies /api to http://app:8080
```

- Token is kept in localStorage; `src/auth/AuthContext.tsx` decodes the JWT claims (`authorities`, `dataScope`, ...). `<Can permission="EMPLOYEE_CREATE">` and the sidebar only hide UI; the backend enforces access.
- **Platform users' HR modules are mindHR's own staff HR** (shown as "mindHR Staff HR" in the sidebar). One hidden
  internal organization (`Organization.internal`, slug `mindhr-internal`, created by `InternalOrganizationProvisioner`)
  holds it. HR services use `TenantContext.resolveHrOrganizationId` / `canAccessHr`: a platform user always gets the
  internal org (any `organizationId` sent is ignored), so platform users never read a client's HR data (spec section
  29). Non-HR services keep `resolveOrganizationId` / `canAccess`. The internal org is left out of the organization list
  and skips plan limits (`PlanLimitService`). An employee added there gets a `SAAS_STAFF` login with the platform
  `EMPLOYEE` role (`EmployeeService.createLogin`).
- HR access for platform users: `SUPER_ADMIN` and `ADMIN` get every HR permission (`superAdminHrAuthorities`) and
  ORGANIZATION scope; `SAAS_STAFF` get their platform role's permissions and scope (`PlatformRoleProvisioner`:
  HR_ADMIN and ACCOUNTS = ORGANIZATION, TEAM_LEAD = TEAM, EMPLOYEE = SELF, reusing `OrgRoleProvisioner.grantsOf`).
- Platform users pick an organization in the top bar (`OrgPicker` in `AppShell`) for the **Client account** pages only;
  `src/api/orgScope.ts` adds that `organizationId` to requests, and those pages show "Select an organization" until
  one is picked. HR pages need no pick and no plan check for platform users. Self-service actions (apply leave,
  check-in) still need an employee profile.
- If the backend fails with `ClassFormatError: Invalid method Code length 0`, the VS Code Java extension wrote a bad class into `target/classes`; start with `./mvnw clean spring-boot:run`.
- Built so far (web): login, app shell, and screens for Organizations, Users, Subscription Plans, System Settings, Subscriptions, Billing (invoices, payments, receipts), Contracts and Support Tickets (platform side), and for Employees, Branches, Departments, Attendance (list + manual records; mobile check-in/out is not in the web app), Leave (requests with apply/approve/reject/cancel, balances, types), Shifts (roster + shifts), Payroll (runs: generate/finalize/pay/delete, payslips with adjustments) and Assets (assets, assignments with return, employee documents). Pattern: `crud()` in `src/api/crud.ts`, `useResource` for lists, `DataTable`/`Tabs`/`Modal` in `src/components/ui.tsx`. API types in `src/api/types.ts` are hand-written from the DTOs.

## Mobile app (`mobile_app/`)

Android only. Kotlin + Jetpack Compose (Material 3), Navigation Compose, Retrofit + kotlinx.serialization, OkHttp,
Play Services Location. No DI framework, no ViewModels: screens load data with `rememberLoad { ... }`.

```bash
# from mobile_app/ (JDK 21; Android SDK path in local.properties, gitignored)
sh ./gradlew assembleDebug                     # app/build/outputs/apk/debug/app-debug.apk
sh ./gradlew installDebug                      # emulator: backend at http://10.0.2.2:8080/
sh ./gradlew installDebug -PapiBaseUrl=http://<pc-ip>:8080/   # real phone on the same Wi-Fi
```

- If the build fails with `jlink executable .../redhat.java.../jre/.../jlink does not exist`: VS Code generated
  `gradle/gradle-daemon-jvm.properties` (toolchainVersion=25) and Gradle picked VS Code's JRE. Build from a terminal
  with `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`.
- **Android 17 local network:** apps targeting API 37 cannot reach local addresses (the emulator's `10.0.2.2`,
  `192.168.x.x`) without the runtime permission `ACCESS_LOCAL_NETWORK` ("Nearby devices" dialog); without it every
  call fails with "Cannot reach the server" (SocketTimeoutException) even though `adb shell` can connect. It is
  declared only in `app/src/debug/AndroidManifest.xml` and requested on start in debug builds (`MainActivity`).
  If it was denied: `adb shell pm grant com.mindhr.mobile android.permission.ACCESS_LOCAL_NETWORK`.
- Toolchain: Gradle 9.7.1 needs **AGP 9.x** (AGP 8.x uses a Gradle internal API removed in 9.6). AGP 9 has built-in
  Kotlin: do **not** apply `org.jetbrains.kotlin.android`, and use `kotlin { compilerOptions { ... } }` instead of
  `kotlinOptions`. Only the compose and serialization Kotlin plugins are applied. compileSdk/targetSdk 37, minSdk 26.
- Layout (`com.mindhr.mobile`):
  - `data/Api.kt`: `MindHrApi` (Retrofit) and `Session`, which holds the JWT, prefs (onboarded, 24h clock, remembered
    email), the decoded claims (`employeeId`, `branchId`, `dataScope`, `authorities`), `isApprover`, and `errorMessage()`.
  - `data/Models.kt`: mirrors backend DTOs. Dates stay ISO strings; BigDecimal is `Double`.
  - `ui/AppRoot.kt`: onboarding -> login -> `MainShell` (bottom tabs Home / Attendance / Leave / More + `Routes`).
  - `ui/Components.kt`: `rememberLoad` / `LoadContent` (reloads on ON_RESUME, so lists refresh after a pushed form
    closes), `MessageState` (empty/error states; no screen is ever blank), `SectionCard`, `StatusChip`, formatters.
  - `ui/theme/Theme.kt`: `MhColors`, same palette as `frontend/src/styles/colors.css`.
  - One file per screen: Home, CheckInSheet, Attendance, Leave, ApplyLeave, Payslips, Requests (+ NewRequest),
    Approvals, Notifications, Profile (+ ChangePassword), More, Onboarding, Login; `Pickers.kt` (date, dropdown).
- "My" screens pass `employeeId = Session.employeeId` because list endpoints return everything in the caller's data
  scope (a manager would otherwise see the team). Approvals call the lists without it and drop the caller's own rows.
- Accounts without an employee profile (e.g. SUPER_ADMIN) get a message instead of attendance/leave screens.
- Check-in shows distance to the branch geofence client-side (no map, no Maps API key); the backend decides.
- Payslips show "Payroll run #N": `PayslipResponse` has no period and employees cannot read payroll runs.
- Not built yet (later phases): break time, field visits, notices, holidays/leave calendar, directory, device binding,
  push (FCM).
