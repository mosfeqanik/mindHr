#!/usr/bin/env python3
"""Regenerate mindhr.postman_collection.json from the controllers and request DTOs.

Run from backend/:  python postman/generate_postman.py
The collection holds no token: log in via POST /api/auth/login, then paste the JWT into the
collection variable `token` (Collection > Variables) yourself.
"""
import json
import pathlib
import re

SRC = pathlib.Path("src/main/java/com/mindhr")
OUT = pathlib.Path("postman/mindhr.postman_collection.json")
HTTP = {"Get": "GET", "Post": "POST", "Put": "PUT", "Patch": "PATCH", "Delete": "DELETE"}


def split_args(s):
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch in "<(":
            depth += 1
        if ch in ">)":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur.strip())
            cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


def record_fields(name):
    f = SRC / "dto" / f"{name}.java"
    if not f.exists():
        return None
    m = re.search(r"record\s+\w+\s*\((.*?)\)\s*\{", f.read_text(encoding="utf-8"), re.S)
    if not m:
        return None
    fields = []
    for part in split_args(re.sub(r"@\w+(\([^)]*\))?", "", m.group(1))):
        toks = part.split()
        if len(toks) >= 2:
            fields.append((toks[-1], " ".join(toks[:-1])))
    return fields


def enum_first(t):
    f = SRC / "entity" / "enums" / f"{t}.java"
    if not f.exists():
        return None
    m = re.search(r"enum\s+\w+\s*\{\s*(\w+)", f.read_text(encoding="utf-8"))
    return m.group(1) if m else None


def example(name, t, depth=0):
    n = name.lower()
    if t.startswith("List<") or t.startswith("Set<"):
        inner = t[t.index("<") + 1:-1]
        return [example(name, inner, depth + 1)] if depth < 2 else []
    if t == "String":
        if "email" in n:
            return "user@example.com"
        if "password" in n:
            return "ChangeMe123!"
        if "url" in n:
            return "https://example.com/file"
        if "phone" in n:
            return "+8801700000000"
        if "currency" in n:
            return "USD"
        return "string"
    if t in ("Long", "long", "Integer", "int"):
        return 1
    if t == "BigDecimal":
        return 100.0
    if t in ("Double", "double"):
        return 1.5
    if t in ("Boolean", "boolean"):
        return True
    if t == "LocalDate":
        return "2026-01-01"
    if t == "LocalTime":
        return "09:00:00"
    if t in ("LocalDateTime", "Instant", "OffsetDateTime"):
        return "2026-01-01T09:00:00"
    if t == "YearMonth":
        return "2026-01"
    e = enum_first(t)
    if e:
        return e
    if depth < 2 and record_fields(t) is not None:
        return {n2: example(n2, t2, depth + 1) for n2, t2 in record_fields(t)}
    return "string"


def parse_controller(path):
    text = path.read_text(encoding="utf-8")
    cls = re.search(r"public class (\w+)", text).group(1)
    base = re.search(r'@RequestMapping\("([^"]*)"\)', text)
    base = base.group(1) if base else ""
    doc, auth, eps = "", "", []
    lines = text.split("\n")
    i = 0
    while i < len(lines):
        ln = lines[i].strip()
        if ln.startswith("/**"):
            buf = ln
            while "*/" not in buf and i + 1 < len(lines):
                i += 1
                buf += " " + lines[i].strip()
            doc = re.sub(r"\s+", " ", re.sub(r"/\*\*|\*/|\*", " ", buf)).strip()
        elif ln.startswith("@PreAuthorize"):
            m = re.search(r"hasAuthority\('(\w+)'\)", ln)
            auth = m.group(1) if m else ""
        else:
            m = re.match(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\(\s*(?:value\s*=\s*|path\s*=\s*)?"([^"]*)"[^)]*\))?\s*$', ln)
            if m:
                verb, sub = m.group(1), m.group(2) or ""
                sig = ""
                while "{" not in sig and i + 1 < len(lines):
                    i += 1
                    if lines[i].strip().startswith("@") and "(" not in sig:
                        continue
                    sig += " " + lines[i].strip()
                sm = re.search(r"public\s+.*?\s(\w+)\((.*)\)\s*\{", sig)
                params = split_args(sm.group(2))
                query, body = [], None
                for p in params:
                    pm = re.match(r"@(\w+)(\((.*?)\))?\s+(?:final\s+)?(.+)\s+(\w+)$", re.sub(r"@Valid\s+", "", p.strip()))
                    if not pm:
                        continue
                    ann, ann_args, ptype, pname = pm.group(1), pm.group(3) or "", pm.group(4).strip(), pm.group(5)
                    nm = re.search(r'name\s*=\s*"(\w+)"|value\s*=\s*"(\w+)"|^"(\w+)"', ann_args)
                    qname = next((g for g in nm.groups() if g), pname) if nm else pname
                    if ann == "RequestBody":
                        body = ptype
                    elif ann == "RequestParam":
                        query.append(qname)
                eps.append(dict(verb=HTTP[verb], path=base + sub, doc=doc, auth=auth, query=query, body=body))
                doc, auth = "", ""
        i += 1
    return cls, eps


def title(cls):
    return re.sub(r"(?<!^)(?=[A-Z])", " ", cls.replace("Controller", "")).strip()


def build():
    folders, count = [], 0
    for f in sorted((SRC / "controller").glob("*Controller.java")):
        cls, eps = parse_controller(f)
        if not eps:
            continue
        items = []
        for e in eps:
            url_path = re.sub(r"\{(\w+)\}", r":\1", e["path"])
            var_vals = [{"key": k, "value": "1"} for k in re.findall(r"\{(\w+)\}", e["path"])]
            req = {"method": e["verb"], "header": [],
                   "url": {"raw": "{{baseUrl}}" + url_path, "host": ["{{baseUrl}}"],
                           "path": [p for p in url_path.split("/") if p]}}
            if var_vals:
                req["url"]["variable"] = var_vals
            if e["query"]:
                req["url"]["query"] = [{"key": k, "value": "{{organizationId}}" if k == "organizationId" else "",
                                        "disabled": True} for k in e["query"]]
                req["url"]["raw"] += "?" + "&".join(f"{k}=" for k in e["query"])
            if e["body"]:
                fields = record_fields(e["body"])
                payload = {n: example(n, t) for n, t in fields} if fields else {}
                req["header"].append({"key": "Content-Type", "value": "application/json"})
                req["body"] = {"mode": "raw", "raw": json.dumps(payload, indent=2),
                               "options": {"raw": {"language": "json"}}}
            desc = (e["doc"] + " " if e["doc"] else "") + (f"Requires authority: {e['auth']}." if e["auth"] else "")
            if cls == "AuthController":
                req["auth"] = {"type": "noauth"}
                desc = "Public. Copy `token` from the response and paste it into the collection variable `token`."
            if desc:
                req["description"] = desc.strip()
            items.append({"name": f"{e['verb']} {e['path']}", "request": req})
            count += 1
        folders.append({"name": title(cls), "item": items})
    return folders, count


# QA accounts seeded by scripts/api_e2e_test.py (docs/QA_CREDENTIALS.md). Test data only.
QA_LOGINS = [
    ("QA owner (ORG_OWNER)", "qa.owner@mindhr.test"),
    ("QA team lead (TEAM_LEAD)", "qa.lead@mindhr.test"),
    ("QA employee 1 (EMPLOYEE)", "qa.emp1@mindhr.test"),
    ("QA employee 2 (EMPLOYEE)", "qa.emp2@mindhr.test"),
]


def qa_folder():
    items = []
    for name, email in QA_LOGINS:
        items.append({"name": f"Login as {name}", "request": {
            "method": "POST", "auth": {"type": "noauth"},
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "url": {"raw": "{{baseUrl}}/api/auth/login", "host": ["{{baseUrl}}"], "path": ["api", "auth", "login"]},
            "body": {"mode": "raw", "raw": json.dumps({"email": email, "password": "QaTest@123"}, indent=2),
                     "options": {"raw": {"language": "json"}}},
            "description": "QA test account. Copy `accessToken` from the response into the collection variable `token`.",
        }})
    return {"name": "QA logins", "description": "Test accounts from docs/QA_CREDENTIALS.md (password QaTest@123).",
            "item": items}


folders, count = build()
folders.insert(0, qa_folder())
count += len(QA_LOGINS)
collection = {
    "info": {
        "name": "mindHR API",
        "description": ("Generated from the Spring controllers (postman/generate_postman.py). "
                        "1) Run Auth > POST /api/auth/login. 2) Copy the token from the response and paste it into "
                        "the collection variable `token`. Every other request sends it as a Bearer token. "
                        "Platform staff must pass `organizationId` on org-owned resources; org users are scoped automatically."),
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "auth": {"type": "bearer", "bearer": [{"key": "token", "value": "{{token}}", "type": "string"}]},
    "variable": [
        {"key": "baseUrl", "value": "http://localhost:8080", "type": "string"},
        {"key": "token", "value": "", "type": "string"},
        {"key": "organizationId", "value": "1", "type": "string"},
    ],
    "item": folders,
}
OUT.write_text(json.dumps(collection, indent=2, ensure_ascii=False), encoding="utf-8")
print(f"{count} requests in {len(folders)} folders -> {OUT}")
