#!/usr/bin/env python3
"""
End-to-end API test for mindHR. Seeds a QA organization through the API, then exercises the main flows
(platform setup, HR setup, employee self-service, manager approvals, payroll, data scope, tenant isolation)
with positive and negative cases.

    cd backend && python3 scripts/api_e2e_test.py [--base http://localhost:8080]

- Needs the backend running and SEED_ADMIN_EMAIL / SEED_ADMIN_PASSWORD in backend/.env (Super Admin login).
- Safe to run repeatedly: it reuses the "QA Test Org" data it created before (fixed names and emails) and only
  adds new leave / requests / attendance where the old ones would conflict.
- QA logins (all use password QaTest@123): qa.owner@mindhr.test, qa.lead@mindhr.test (TEAM_LEAD),
  qa.emp1@mindhr.test, qa.emp2@mindhr.test (EMPLOYEE).
- Exit code 0 when every check passes. Known issues are reported as FAIL on purpose.
"""
import argparse
import base64
import datetime as dt
import json
import os
import sys
import urllib.error
import urllib.request

PASSWORD = "QaTest@123"
ORG_NAME = "QA Test Org"
PLAN_NAME = "QA Full Plan"
BRANCH = {"name": "QA Head Office", "code": "QAHO", "latitude": 23.7808, "longitude": 90.4067, "geofenceRadiusM": 150}
INSIDE = (23.7809, 90.4068)    # ~15 m from the branch
OUTSIDE = (23.8103, 90.4125)   # ~3.3 km away
PEOPLE = {
    "lead": ("QA Team Lead", "qa.lead@mindhr.test", "01900000001", "Team Lead"),
    "emp1": ("QA Employee One", "qa.emp1@mindhr.test", "01900000002", "Executive"),
    "emp2": ("QA Employee Two", "qa.emp2@mindhr.test", "01900000003", "Executive"),
}
ALL_FEATURES = ["ATTENDANCE", "LEAVE", "SHIFTS", "PAYROLL", "REPORTS", "ASSETS", "PERFORMANCE", "MOBILE_APP",
                "MULTI_BRANCH", "ADVANCED_PERMISSIONS"]

BASE = "http://localhost:8080"
results = []  # (section, name, ok, detail)
section = ""


# ---------------------------------------------------------------- helpers

def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=60) as res:
            raw = res.read().decode()
            return res.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw) if raw else None
        except json.JSONDecodeError:
            return e.code, {"raw": raw[:200]}


def msg(body):
    if isinstance(body, dict):
        return body.get("message") or body.get("error") or json.dumps(body)[:160]
    return str(body)[:160]


def check(name, ok, detail=""):
    results.append((section, name, bool(ok), detail))
    print(f"  {'PASS' if ok else 'FAIL'}  {name}" + (f"  ({detail})" if detail and not ok else ""))
    return ok


def expect(name, status, body, want):
    """want: an int status or a tuple of allowed statuses."""
    allowed = want if isinstance(want, tuple) else (want,)
    return check(name, status in allowed, f"got {status}: {msg(body)}")


def begin(title):
    global section
    section = title
    print(f"\n== {title}")


def login(email, password):
    s, b = call("POST", "/api/auth/login", body={"email": email, "password": password})
    return (b or {}).get("accessToken") if s == 200 else None


def claims(token):
    p = token.split(".")[1]
    return json.loads(base64.urlsafe_b64decode(p + "=" * (-len(p) % 4)))


def env_value(key):
    path = os.path.join(os.path.dirname(__file__), "..", ".env")
    with open(path) as f:
        for line in f:
            if line.startswith(key + "="):
                return line.split("=", 1)[1].strip().strip('"').strip("'")
    return None


def find(items, **match):
    for it in items or []:
        if all(str(it.get(k)).lower() == str(v).lower() for k, v in match.items()):
            return it
    return None


def free_window(existing, start, days):
    """First start date >= start where [d, d+days-1] overlaps no pending/approved leave and stays in one year."""
    taken = [(dt.date.fromisoformat(r["fromDate"]), dt.date.fromisoformat(r["toDate"]))
             for r in existing if r["status"] in ("PENDING", "APPROVED")]
    d = start
    for _ in range(400):
        end = d + dt.timedelta(days=days - 1)
        if end.year == d.year and all(end < a or d > b for a, b in taken):
            return d, end
        d += dt.timedelta(days=1)
    raise RuntimeError("no free leave window")


# ---------------------------------------------------------------- test

def main():
    global BASE
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default=BASE)
    BASE = ap.parse_args().base.rstrip("/")
    today = dt.date.today()

    # ------------------------------------------------ auth basics
    begin("Auth")
    s, b = call("GET", "/v3/api-docs")
    expect("OpenAPI spec is public", s, b, 200)
    s, b = call("GET", "/api/employees")
    expect("No token -> 401", s, b, 401)
    s, b = call("POST", "/api/auth/login", body={"email": "nobody@mindhr.test", "password": "wrong-password"})
    expect("Wrong credentials -> 401", s, b, 401)
    s, b = call("POST", "/api/auth/login", body={"email": "not-an-email", "password": ""})
    expect("Invalid login body -> 400", s, b, 400)
    admin = login(env_value("SEED_ADMIN_EMAIL"), env_value("SEED_ADMIN_PASSWORD"))
    if not check("Super Admin login", admin, "check SEED_ADMIN_* in backend/.env"):
        return finish()

    # ------------------------------------------------ platform setup (Super Admin)
    begin("Platform setup (Super Admin)")
    s, plans = call("GET", "/api/subscription-plans", admin)
    expect("List plans", s, plans, 200)
    plan = find(plans, name=PLAN_NAME)
    if not plan:
        s, plan = call("POST", "/api/subscription-plans", admin, {
            "name": PLAN_NAME, "description": "All features, used by the API e2e test", "price": 0, "currency": "USD",
            "billingCycle": "MONTHLY", "maxEmployees": 100, "maxBranches": 10, "features": ALL_FEATURES,
            "trialDays": 365, "active": True})
        if not expect("Create plan with all features", s, plan, (200, 201)):
            return finish()
    check("Plan has every feature", set(plan.get("features", [])) >= set(ALL_FEATURES), str(plan.get("features")))

    s, orgs = call("GET", "/api/organizations", admin)
    expect("List organizations", s, orgs, 200)
    org = find(orgs, name=ORG_NAME)
    if not org:
        s, b = call("POST", "/api/organizations", admin, {"name": ORG_NAME + " no plan", "email": "x@mindhr.test"})
        expect("Create organization without plan -> 400", s, b, 400)
        s, org = call("POST", "/api/organizations", admin, {
            "name": ORG_NAME, "status": "ACTIVE", "email": "qa.org@mindhr.test", "phone": "01900000000",
            "address": "Dhaka", "planId": plan["id"]})
        if not expect("Create organization with plan", s, org, (200, 201)):
            return finish()
    org_id = org["id"]

    s, users = call("GET", f"/api/users?organizationId={org_id}", admin)
    expect("List org users", s, users, 200)
    owner_user = find(users, email="qa.owner@mindhr.test")
    if not owner_user:
        s, owner_user = call("POST", "/api/users", admin, {
            "organizationId": org_id, "email": "qa.owner@mindhr.test", "password": PASSWORD,
            "fullName": "QA Owner", "phone": "01900000009", "userType": "ORG_USER", "active": True})
        if not expect("Create owner user", s, owner_user, (200, 201)):
            return finish()
    s, b = call("PATCH", f"/api/organizations/{org_id}/owner", admin, {"ownerUserId": owner_user["id"]})
    expect("Set organization owner", s, b, 200)
    s, b = call("POST", "/api/users", admin, {
        "organizationId": org_id, "email": "qa.owner@mindhr.test", "password": PASSWORD,
        "fullName": "Dup", "userType": "ORG_USER"})
    expect("Duplicate user email -> 409", s, b, 409)

    s, roles = call("GET", f"/api/roles?organizationId={org_id}", admin)
    expect("List org roles", s, roles, 200)
    role_ids = {r["name"]: r["id"] for r in roles or []}
    check("Org has the 5 built-in roles",
          {"ORG_OWNER", "HR_ADMIN", "BRANCH_MANAGER", "TEAM_LEAD", "EMPLOYEE"} <= set(role_ids), str(sorted(role_ids)))

    owner = login("qa.owner@mindhr.test", PASSWORD)
    if not check("Owner login", owner):
        return finish()
    c = claims(owner)
    check("Owner token: org + ORGANIZATION scope", c.get("organizationId") == org_id and c.get("dataScope") == "ORGANIZATION",
          f"{c.get('organizationId')} {c.get('dataScope')}")
    s, b = call("GET", "/api/subscriptions/current", owner)
    expect("Owner reads current plan", s, b, 200)

    # ------------------------------------------------ HR setup (owner)
    begin("HR setup (owner)")
    s, branches = call("GET", "/api/branches", owner)
    expect("List branches", s, branches, 200)
    branch = find(branches, code=BRANCH["code"])
    if not branch:
        s, branch = call("POST", "/api/branches", owner, dict(BRANCH, address="Gulshan, Dhaka", active=True))
        if not expect("Create branch with geofence", s, branch, (200, 201)):
            return finish()
    check("Branch geofence saved", branch.get("geofenceRadiusM") == 150 and branch.get("latitude") is not None)

    s, depts = call("GET", "/api/departments", owner)
    dept = find(depts, name="QA Operations")
    if not dept:
        s, dept = call("POST", "/api/departments", owner, {"name": "QA Operations", "branchId": branch["id"], "active": True})
        expect("Create department", s, dept, (200, 201))

    s, emps = call("GET", "/api/employees", owner)
    expect("List employees", s, emps, 200)
    ids = {}
    for key in ("lead", "emp1", "emp2"):
        name, email, phone, position = PEOPLE[key]
        e = find(emps, email=email)
        if not e:
            body = {"fullName": name, "email": email, "phone": phone, "position": position,
                    "employmentType": "FULL_TIME", "joinedAt": "2026-01-01", "password": PASSWORD,
                    "branchId": branch["id"], "departmentId": dept["id"],
                    "managerId": ids.get("lead")}
            s, e = call("POST", "/api/employees", owner, body)
            if not expect(f"Create employee {key}", s, e, (200, 201)):
                return finish()
        ids[key] = e["id"]
        check(f"{key} has a login user", e.get("userId"))
        # Make sure branch / department / manager are set (PUT replaces the record: send it back whole).
        want_mgr = ids["lead"] if key != "lead" else None
        if e.get("branchId") != branch["id"] or e.get("managerId") != want_mgr:
            body = dict(e, branchId=branch["id"], departmentId=dept["id"], managerId=want_mgr)
            s, e = call("PUT", f"/api/employees/{e['id']}", owner, body)
            expect(f"Set branch/manager on {key}", s, e, 200)
    lead_user_id = find(call("GET", "/api/employees", owner)[1], email=PEOPLE["lead"][1])["userId"]

    s, b = call("POST", "/api/employees", owner, {"fullName": "Dup", "email": PEOPLE["emp1"][1]})
    expect("Duplicate employee email -> 409", s, b, 409)
    s, b = call("POST", "/api/employees", owner, {"fullName": "Dup", "email": "qa.dup@mindhr.test", "phone": PEOPLE["emp1"][2]})
    expect("Duplicate phone in org -> 409", s, b, 409)
    s, b = call("POST", "/api/employees", owner, {"fullName": "", "email": "qa.blank@mindhr.test"})
    expect("Blank employee name -> 400", s, b, 400)

    # TEAM_LEAD role for the lead (Super Admin; roleIds replaces all roles).
    s, u = call("GET", f"/api/users/{lead_user_id}", admin)
    current = {r["id"] for r in (u or {}).get("roles", [])}
    want = {role_ids["EMPLOYEE"], role_ids["TEAM_LEAD"]}
    if not want <= current:
        body = {k: u.get(k) for k in ("email", "fullName", "phone", "userType", "active")}
        body.update(organizationId=org_id, roleIds=sorted(current | want))
        s, u = call("PUT", f"/api/users/{lead_user_id}", admin, body)
        expect("Give lead the TEAM_LEAD role", s, u, 200)

    # Leave types
    s, types = call("GET", "/api/leave-types", owner)
    expect("List leave types", s, types, 200)
    lt = {}
    for name, days, paid in (("QA Annual", 18, True), ("QA Sick", 14, True), ("QA Unpaid", 0, False)):
        t = find(types, name=name)
        if not t:
            s, t = call("POST", "/api/leave-types", owner, {"name": name, "annualDays": days, "paid": paid, "active": True})
            expect(f"Create leave type {name}", s, t, (200, 201))
        lt[name] = t["id"]

    # Balances for this year
    s, bals = call("GET", "/api/leave-balances", owner)
    expect("List leave balances", s, bals, 200)
    for key in ("emp1", "emp2", "lead"):
        if not find(bals, employeeId=ids[key], leaveTypeId=lt["QA Annual"], year=today.year):
            s, b = call("POST", "/api/leave-balances", owner, {"employeeId": ids[key], "leaveTypeId": lt["QA Annual"],
                                                               "year": today.year, "entitledDays": 18, "usedDays": 0})
            expect(f"Create annual balance {key}", s, b, (200, 201))

    # Shift + today's roster for emp1 (so lateness is measured)
    s, shifts = call("GET", "/api/shifts", owner)
    shift = find(shifts, name="QA Morning")
    if not shift:
        s, shift = call("POST", "/api/shifts", owner, {"name": "QA Morning", "startTime": "09:00:00", "endTime": "18:00:00", "active": True})
        expect("Create shift", s, shift, (200, 201))
    s, roster = call("GET", "/api/shift-assignments", owner)
    if not find(roster, employeeId=ids["emp1"], workDate=today.isoformat()):
        s, b = call("POST", "/api/shift-assignments", owner, {"employeeId": ids["emp1"], "shiftId": shift["id"], "workDate": today.isoformat()})
        expect("Roster emp1 for today", s, b, (200, 201))

    # Salary structures
    s, sal = call("GET", "/api/salary-structures", owner)
    expect("List salary structures", s, sal, 200)
    for key, basic in (("lead", 60000), ("emp1", 40000), ("emp2", 38000)):
        if not find(sal, employeeId=ids[key]):
            s, b = call("POST", "/api/salary-structures", owner, {"employeeId": ids[key], "effectiveFrom": "2026-01-01",
                                                                  "basicSalary": basic, "allowances": 5000, "currency": "BDT"})
            expect(f"Salary structure {key}", s, b, (200, 201))

    # Asset assigned to emp1
    s, assets = call("GET", "/api/assets", owner)
    expect("List assets", s, assets, 200)
    asset = find(assets, assetTag="QA-LAPTOP-1")
    if not asset:
        s, asset = call("POST", "/api/assets", owner, {"name": "QA Laptop", "assetTag": "QA-LAPTOP-1", "category": "Laptop", "status": "AVAILABLE"})
        expect("Create asset", s, asset, (200, 201))
    if asset and asset.get("status") == "AVAILABLE":
        s, b = call("POST", "/api/asset-assignments", owner, {"assetId": asset["id"], "employeeId": ids["emp1"], "assignedAt": today.isoformat()})
        expect("Assign asset to emp1", s, b, (200, 201))

    # Notification for emp1
    emp1_user = find(call("GET", "/api/employees", owner)[1], email=PEOPLE["emp1"][1])["userId"]
    s, b = call("POST", "/api/notifications", owner, {"userId": emp1_user, "channel": "EMAIL",
                                                       "subject": "Welcome to mindHR", "body": f"QA notification {dt.datetime.now():%H:%M}"})
    expect("Send notification to emp1", s, b, (200, 201))

    # ------------------------------------------------ employee self-service
    begin("Employee self-service (emp1)")
    emp1 = login(PEOPLE["emp1"][1], PASSWORD)
    if not check("emp1 login", emp1):
        return finish()
    c = claims(emp1)
    check("emp1 token: SELF scope, employeeId, branchId",
          c.get("dataScope") == "SELF" and c.get("employeeId") == ids["emp1"] and c.get("branchId") == branch["id"],
          f"{c.get('dataScope')} {c.get('employeeId')} {c.get('branchId')}")
    s, me = call("GET", "/api/employees/me", emp1)
    expect("GET /employees/me", s, me, 200)
    check("me is emp1", (me or {}).get("id") == ids["emp1"])

    s, today_rec = call("GET", "/api/attendance/today", emp1)
    expect("GET /attendance/today", s, today_rec, (200, 204))
    if s == 204 or not (today_rec or {}).get("checkInAt"):
        s, b = call("POST", "/api/attendance/check-in", emp1, {"method": "MOBILE_GPS", "latitude": OUTSIDE[0], "longitude": OUTSIDE[1]})
        expect("Check-in outside geofence -> 400", s, b, 400)
        check("Outside message names the distance", "outside the allowed area" in msg(b), msg(b))
        s, b = call("POST", "/api/attendance/check-in", emp1, {"method": "MOBILE_GPS"})
        expect("Check-in without location -> 400", s, b, 400)
        s, b = call("POST", "/api/attendance/check-in", emp1, {"method": "MOBILE_GPS", "latitude": INSIDE[0], "longitude": INSIDE[1], "note": "QA"})
        expect("Check-in inside geofence", s, b, (200, 201))
        check("Status is PRESENT or LATE", (b or {}).get("status") in ("PRESENT", "LATE"), str((b or {}).get("status")))
    s, b = call("POST", "/api/attendance/check-in", emp1, {"method": "MOBILE_GPS", "latitude": INSIDE[0], "longitude": INSIDE[1]})
    expect("Second check-in -> 400", s, b, 400)
    s, today_rec = call("GET", "/api/attendance/today", emp1)
    if not (today_rec or {}).get("checkOutAt"):
        s, b = call("POST", "/api/attendance/check-out", emp1, {"method": "MOBILE_GPS", "latitude": INSIDE[0], "longitude": INSIDE[1]})
        expect("Check-out", s, b, (200, 201))
    s, att = call("GET", "/api/attendance", emp1)
    expect("List own attendance", s, att, 200)
    check("Attendance list has only own rows", att and all(a["employeeId"] == ids["emp1"] for a in att), f"{len(att or [])} rows")

    s, b = call("GET", "/api/leave-types", emp1)
    expect("Employee lists leave types", s, b, 200)
    s, b = call("GET", "/api/leave-balances", emp1)
    expect("Employee lists own balances", s, b, 200)
    check("Balances only own", all(x["employeeId"] == ids["emp1"] for x in b or []))

    s, mine = call("GET", "/api/leave-requests", emp1)
    expect("List own leave requests", s, mine, 200)
    f1, t1 = free_window(mine, today + dt.timedelta(days=7), 2)
    s, l_cancel = call("POST", "/api/leave-requests", emp1, {"leaveTypeId": lt["QA Annual"], "fromDate": f1.isoformat(), "toDate": t1.isoformat(), "reason": "QA cancel test"})
    expect("Apply leave (to cancel)", s, l_cancel, (200, 201))
    check("New leave is PENDING, 2 days", (l_cancel or {}).get("status") == "PENDING" and float((l_cancel or {}).get("days", 0)) == 2)
    s, b = call("POST", "/api/leave-requests", emp1, {"leaveTypeId": lt["QA Annual"], "fromDate": f1.isoformat(), "toDate": t1.isoformat(), "reason": "overlap"})
    expect("Overlapping leave -> 400", s, b, 400)
    s, b = call("POST", f"/api/leave-requests/{l_cancel['id']}/cancel", emp1)
    expect("Cancel own pending leave", s, b, 200)
    s, b = call("POST", f"/api/leave-requests/{l_cancel['id']}/cancel", emp1)
    expect("Cancel again -> 400", s, b, 400)

    s, b = call("POST", "/api/leave-requests", emp1, {"leaveTypeId": lt["QA Annual"], "fromDate": f"{today.year + 1}-06-01", "toDate": f"{today.year + 1}-06-25", "reason": "too long"})
    expect("Leave above balance -> 400", s, b, 400)
    check("Message says insufficient balance", "Insufficient" in msg(b), msg(b))
    s, b = call("POST", "/api/leave-requests", emp1, {"leaveTypeId": lt["QA Annual"], "fromDate": f"{today.year}-12-30", "toDate": f"{today.year + 1}-01-02", "reason": "x"})
    expect("Leave across two years -> 400", s, b, 400)
    s, b = call("POST", "/api/leave-requests", emp1, {"leaveTypeId": lt["QA Annual"], "fromDate": t1.isoformat(), "toDate": f1.isoformat(), "reason": "x"})
    expect("Leave with end before start -> 400", s, b, 400)

    s, mine = call("GET", "/api/leave-requests", emp1)
    f2, t2 = free_window(mine, today + dt.timedelta(days=7), 1)
    s, l_approve = call("POST", "/api/leave-requests", emp1, {"leaveTypeId": lt["QA Sick"], "fromDate": f2.isoformat(), "toDate": t2.isoformat(), "reason": "QA approve test"})
    expect("Apply leave (to approve)", s, l_approve, (200, 201))
    s, mine = call("GET", "/api/leave-requests", emp1)
    f3, t3 = free_window(mine, today + dt.timedelta(days=7), 1)
    s, l_reject = call("POST", "/api/leave-requests", emp1, {"leaveTypeId": lt["QA Unpaid"], "fromDate": f3.isoformat(), "toDate": t3.isoformat(), "reason": "QA reject test"})
    expect("Apply unpaid leave (to reject)", s, l_reject, (200, 201))

    s, r_adv = call("POST", "/api/requests", emp1, {"type": "SALARY_ADVANCE", "title": "QA advance", "amount": 5000, "requestDate": today.isoformat()})
    expect("Request salary advance", s, r_adv, (200, 201))
    s, r_fix = call("POST", "/api/requests", emp1, {"type": "ATTENDANCE_CORRECTION", "title": "QA correction", "details": "In 09:00 Out 18:00", "requestDate": today.isoformat()})
    expect("Request attendance correction (new enum value)", s, r_fix, (200, 201))
    s, r_wfh = call("POST", "/api/requests", emp1, {"type": "WORK_FROM_HOME", "title": "QA WFH", "requestDate": today.isoformat()})
    expect("Request work from home", s, r_wfh, (200, 201))
    s, b = call("POST", "/api/requests", emp1, {"type": "LOAN", "title": "", "amount": -5})
    expect("Invalid request (blank title, negative amount) -> 400", s, b, 400)
    s, b = call("POST", "/api/requests", emp1, {"type": "NOT_A_TYPE", "title": "x"})
    expect("Unknown request type -> 400", s, b, 400)
    s, b = call("POST", f"/api/requests/{r_wfh['id']}/cancel", emp1)
    expect("Cancel own pending request", s, b, 200)
    s, reqs = call("GET", "/api/requests", emp1)
    expect("List own requests", s, reqs, 200)
    check("Requests only own", all(r["employeeId"] == ids["emp1"] for r in reqs or []))

    s, notes = call("GET", "/api/notifications", emp1)
    expect("List own notifications", s, notes, 200)
    unread = [n for n in notes or [] if not n.get("read")]
    if check("Has an unread notification", unread):
        s, b = call("PATCH", f"/api/notifications/{unread[0]['id']}/read", emp1)
        expect("Mark notification read", s, b, 200)

    s, b = call("POST", "/api/account/change-password", emp1, {"currentPassword": "wrong-one", "newPassword": "Another@123"})
    expect("Change password with wrong current -> 400", s, b, 400)
    s, b = call("POST", "/api/account/change-password", emp1, {"currentPassword": PASSWORD, "newPassword": "short"})
    expect("Change password too short -> 400", s, b, 400)
    s, b = call("POST", "/api/account/change-password", emp1, {"currentPassword": PASSWORD, "newPassword": PASSWORD})
    expect("Change password to the same -> 400", s, b, 400)
    s, b = call("POST", "/api/account/change-password", emp1, {"currentPassword": PASSWORD, "newPassword": "Changed@123"})
    expect("Change password", s, b, 204)
    check("Old password no longer works", login(PEOPLE["emp1"][1], PASSWORD) is None)
    tmp = login(PEOPLE["emp1"][1], "Changed@123")
    check("New password works", tmp)
    if tmp:
        s, b = call("POST", "/api/account/change-password", tmp, {"currentPassword": "Changed@123", "newPassword": PASSWORD})
        expect("Change password back", s, b, 204)
    s, b = call("POST", "/api/account/change-password", None, {"currentPassword": "a", "newPassword": "bbbbbbbb"})
    expect("Change password without token -> 401", s, b, 401)

    # ------------------------------------------------ what an employee must NOT do
    begin("Employee permissions and data scope")
    s, b = call("POST", f"/api/leave-requests/{l_approve['id']}/approve", emp1, {})
    expect("Employee approves leave -> 403", s, b, 403)
    s, b = call("POST", f"/api/requests/{r_adv['id']}/approve", emp1, {})
    expect("Employee approves request -> 403", s, b, 403)
    s, b = call("POST", "/api/branches", emp1, {"name": "Hack branch"})
    expect("Employee creates branch -> 403", s, b, 403)
    s, b = call("POST", "/api/employees", emp1, {"fullName": "Hack"})
    expect("Employee creates employee -> 403", s, b, 403)
    s, b = call("GET", "/api/payroll-runs", emp1)
    expect("Employee lists payroll runs -> 403", s, b, 403)
    s, b = call("GET", f"/api/employees/{ids['emp2']}", emp1)
    expect("Employee reads a colleague -> 403/404", s, b, (403, 404))
    s, b = call("GET", "/api/employees", emp1)
    check("Employee list shows only self", s == 200 and [e["id"] for e in b] == [ids["emp1"]], f"{s} {[e.get('id') for e in b or []]}")
    s, b = call("GET", "/api/leave-requests?employeeId=" + str(ids["emp2"]), emp1)
    check("Employee cannot list a colleague's leave", s in (403, 404) or (s == 200 and not any(r["employeeId"] == ids["emp2"] for r in b)), f"{s}")
    s, b = call("GET", "/api/users", emp1)
    expect("KNOWN ISSUE: employee lists users -> should be 403", s, b, 403)
    s, b = call("GET", "/api/roles", emp1)
    expect("KNOWN ISSUE: employee lists roles -> should be 403", s, b, 403)

    # ------------------------------------------------ manager approvals
    begin("Team Lead approvals")
    lead = login(PEOPLE["lead"][1], PASSWORD)
    if not check("Lead login", lead):
        return finish()
    c = claims(lead)
    check("Lead token: TEAM scope + approve rights",
          c.get("dataScope") == "TEAM" and {"LEAVE_APPROVE", "REQUEST_APPROVE"} <= set(c.get("authorities", [])),
          f"{c.get('dataScope')}")
    s, team_leave = call("GET", "/api/leave-requests", lead)
    expect("Lead lists team leave", s, team_leave, 200)
    check("Team leave includes emp1's pending leave", find(team_leave, id=l_approve["id"]))
    check("Team leave is only the team", all(r["employeeId"] in (ids["lead"], ids["emp1"], ids["emp2"]) for r in team_leave or []))
    s, b = call("POST", f"/api/leave-requests/{l_approve['id']}/approve", lead, {"note": "QA approved"})
    expect("Lead approves leave", s, b, 200)
    check("Leave now APPROVED", (b or {}).get("status") == "APPROVED")
    s, b = call("POST", f"/api/leave-requests/{l_approve['id']}/approve", lead, {})
    expect("Approve twice -> 400", s, b, 400)
    s, b = call("POST", f"/api/leave-requests/{l_approve['id']}/cancel", emp1)
    expect("Employee cancels approved leave -> 400", s, b, 400)
    s, b = call("POST", f"/api/leave-requests/{l_reject['id']}/reject", lead, {"note": "QA rejected"})
    expect("Lead rejects leave", s, b, 200)
    check("Reject note saved", (b or {}).get("decisionNote") == "QA rejected")
    s, b = call("POST", f"/api/requests/{r_adv['id']}/approve", lead, {"note": "ok"})
    expect("Lead approves salary advance", s, b, 200)
    s, b = call("POST", f"/api/requests/{r_fix['id']}/reject", lead, {"note": "use web attendance"})
    expect("Lead rejects correction", s, b, 200)
    s, b = call("GET", "/api/attendance", lead)
    expect("Lead lists team attendance", s, b, 200)
    check("Team attendance includes emp1", any(a["employeeId"] == ids["emp1"] for a in b or []))

    s, mine = call("GET", "/api/leave-requests?employeeId=" + str(ids["lead"]), lead)
    f4, t4 = free_window(mine or [], today + dt.timedelta(days=7), 1)
    s, own = call("POST", "/api/leave-requests", lead, {"leaveTypeId": lt["QA Unpaid"], "fromDate": f4.isoformat(), "toDate": t4.isoformat(), "reason": "own"})
    if expect("Lead applies own leave", s, own, (200, 201)):
        s, b = call("POST", f"/api/leave-requests/{own['id']}/approve", lead, {})
        expect("Lead approves own leave -> 403", s, b, 403)
        call("POST", f"/api/leave-requests/{own['id']}/cancel", lead)

    # ------------------------------------------------ payroll (owner)
    begin("Payroll (owner)")
    prev = (today.replace(day=1) - dt.timedelta(days=1))
    s, runs = call("GET", "/api/payroll-runs", owner)
    expect("List payroll runs", s, runs, 200)
    run = find(runs, year=prev.year, month=prev.month) if runs and "year" in (runs[0] if runs else {}) else None
    if not run:
        s, run = call("POST", "/api/payroll-runs", owner, {"year": prev.year, "month": prev.month, "note": "QA"})
        if s == 409:
            run = next((r for r in call("GET", "/api/payroll-runs", owner)[1] if r.get("status")), None)
        else:
            expect("Generate payroll run (last month)", s, run, (200, 201))
    s, b = call("POST", "/api/payroll-runs", owner, {"year": prev.year, "month": prev.month})
    expect("Generate the same month again -> 409", s, b, 409)
    if run:
        s, slips = call("GET", f"/api/payslips?payrollRunId={run['id']}", owner)
        expect("List payslips", s, slips, 200)
        slips = [p for p in slips or [] if p.get("payrollRunId") == run["id"]]
        check("Payslip for each QA employee", {ids["lead"], ids["emp1"], ids["emp2"]} <= {p["employeeId"] for p in slips})
        p1 = find(slips, employeeId=ids["emp1"])
        if run.get("status") == "DRAFT" and p1:
            s, b = call("PUT", f"/api/payslips/{p1['id']}/adjustments", owner, {"bonus": 1000})
            expect("Adjust draft payslip", s, b, 200)
            check("Net salary includes bonus", b and float(b["netSalary"]) == float(p1["netSalary"]) + 1000 - float(p1.get("bonus") or 0),
                  f"{p1['netSalary']} -> {(b or {}).get('netSalary')}")
            s, b = call("PUT", f"/api/payslips/{p1['id']}/adjustments", owner, {"bonus": -1})
            expect("Negative adjustment -> 400", s, b, 400)
            s, b = call("POST", f"/api/payroll-runs/{run['id']}/pay", owner)
            expect("Pay before finalize -> 400", s, b, 400)
            s, b = call("POST", f"/api/payroll-runs/{run['id']}/finalize", owner)
            expect("Finalize run", s, b, 200)
            s, b = call("PUT", f"/api/payslips/{p1['id']}/adjustments", owner, {"bonus": 5})
            expect("Adjust finalized payslip -> 400", s, b, 400)
            s, b = call("POST", f"/api/payroll-runs/{run['id']}/pay", owner)
            expect("Pay run", s, b, 200)
        s, b = call("DELETE", f"/api/payroll-runs/{run['id']}", owner)
        expect("Delete a paid/finalized run -> 400", s, b, 400)
    s, b = call("GET", "/api/payslips", emp1)
    expect("Employee lists own payslips", s, b, 200)
    check("Employee sees only own payslips", b and all(p["employeeId"] == ids["emp1"] for p in b), f"{len(b or [])}")

    # ------------------------------------------------ tenant isolation
    begin("Tenant isolation")
    s, all_emps = call("GET", "/api/employees", owner)
    check("Owner sees only own organization", s == 200 and all(e["organizationId"] == org_id for e in all_emps))
    s, other_orgs = call("GET", "/api/organizations", admin)
    other = next((o for o in other_orgs if o["id"] != org_id), None)
    if other:
        s, b = call("GET", f"/api/employees?organizationId={other['id']}", owner)
        check("Owner cannot list another org's employees", s in (400, 403) or (s == 200 and all(e["organizationId"] == org_id for e in b)), f"{s}")
        s, b = call("GET", f"/api/organizations/{other['id']}", owner)
        expect("Owner reads another organization -> 403/404", s, b, (403, 404))
    s, b = call("GET", "/api/organizations", owner)
    check("Owner's organization list has only theirs", s in (403,) or (s == 200 and all(o["id"] == org_id for o in b)), f"{s}")

    return finish()


def finish():
    total = len(results)
    failed = [r for r in results if not r[2]]
    print("\n" + "=" * 70)
    print(f"{total - len(failed)}/{total} checks passed, {len(failed)} failed")
    for sec, name, _, detail in failed:
        print(f"  FAIL [{sec}] {name}: {detail}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
