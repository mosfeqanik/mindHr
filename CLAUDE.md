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
  QA logins (`qa.owner|qa.lead|qa.emp1|qa.emp2@mindhr.test`, password `QaTest@123`) and runs ~150 checks: setup,
  check-in/geofence, leave, requests, approvals, payroll, data scope, tenant isolation. Re-runnable; exits 1 on any FAIL.
  The two `KNOWN ISSUE` checks fail until `/api/users` and `/api/roles` get permission checks.
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
- The web app cannot yet set an employee's branch, department or manager, or change a user's roles; use Swagger
  (see the manual).
- **Open security gap:** `UserController` and `RoleController` have no `@PreAuthorize`, so any signed-in org user (even
  `EMPLOYEE`) can list the org's users and roles, and likely `PUT /api/users/{id}` with `roleIds` (privilege escalation).
  Needs a permission check before production.

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
- `SUPER_ADMIN` gets every HR permission at login (`CustomUserDetails.superAdminHrAuthorities`); `SAAS_STAFF` holds none (spec section 29) and gets 403 on HR endpoints.
  Platform users pick an organization in the top bar (`OrgPicker` in `AppShell`); `src/api/orgScope.ts` adds that `organizationId` to every org-scoped request (GET params, POST/PUT bodies), and org-scoped pages show "Select an organization" until one is picked. Self-service actions (apply leave, check-in) still need an employee profile.
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
