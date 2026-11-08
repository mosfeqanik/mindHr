---
name: commit
description: Stage all changes and create a git commit using the Conventional Commits format. Use when the user says "commit", "/commit", or asks to commit their work.
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git add:*), Bash(git commit:*), Bash(git branch:*)
---

# Commit

Create a single git commit from the current working tree changes, following the
[Conventional Commits](https://www.conventionalcommits.org/) specification.

## Steps

1. Run `git status --porcelain` and `git diff HEAD --stat` to see what changed. If there are no changes, say so and stop.
2. Run `git log --oneline -10` to see existing commit history and reuse any scopes already in use.
3. Review the actual diff (`git diff HEAD`) so the message describes *why*, not just *what*.
4. Stage everything with `git add -A`.
   - Never stage files that look like secrets (`.env`, `*.pem`, `*.key`, credentials). If any are present, leave them unstaged and warn the user.
5. Commit using the Conventional Commits format described below.
6. Run `git log -1 --stat` and show the result to the user.

## Conventional Commits format

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Type (required)

Pick exactly one, based on the *primary* intent of the change:

| Type | Use when |
|---|---|
| `feat` | A new feature or capability visible to users/API consumers |
| `fix` | A bug fix |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `perf` | A change that improves performance |
| `test` | Adding or correcting tests only |
| `docs` | Documentation only (README, Javadoc, markdown) |
| `style` | Formatting, whitespace, missing semicolons — no logic change |
| `build` | Build system or dependencies (`pom.xml`, Maven wrapper, Dockerfile) |
| `ci` | CI configuration (GitHub Actions, pipelines) |
| `chore` | Maintenance that touches none of the above (e.g. `.gitignore`, tooling config) |
| `revert` | Reverts a previous commit; body must reference the reverted SHA |

### Scope (optional but preferred)

A short, lowercase noun in parentheses naming the area touched. For this workspace (Spring Boot `backend/`, React/Vite `frontend/`, `mobile_app/`) prefer
scopes such as `auth`, `tenant`, `user`, `billing`, `employee`, `attendance`, `leave`, `payroll`, `asset`,
`api`, `security`, `config`, `db`, `docker`, `deps`, `backend`, `frontend`, `mobile`, `workspace`.
Reuse scopes seen in `git log`. Omit the scope if the change is truly cross-cutting.

### Description (required)

- Imperative, present tense: "add", "fix", "remove" — not "added", "fixes", "removing".
- Lowercase first letter, no trailing period.
- Whole header (`type(scope): description`) ≤ 72 characters.

### Body (optional)

- Separated from the header by one blank line.
- Explain the motivation and contrast with previous behaviour. Wrap at 72 characters.
- Use bullet points when the commit touches several distinct things.

### Footers (optional)

- **Breaking changes**: add `!` after the type/scope (`feat(api)!: ...`) **and** a footer
  `BREAKING CHANGE: <explanation>`.
- Issue references: `Closes #123`, `Refs #45`.
- Always end with the attribution line required by the session (`Co-Authored-By: ...`) when one is given.

### Examples

```
feat(tenant): add row-level isolation filter to JPA repositories

Every repository now applies a Hibernate filter keyed on the current
tenant id from TenantContext, so cross-tenant reads are impossible even
if a query forgets the where clause.

Co-Authored-By: <name and email from the session's attribution line>
```

```
fix(auth): reject expired refresh tokens before rotation
```

```
build(deps): bump spring-boot-starter-parent to 4.1.1
```

```
feat(api)!: rename /v1/orgs to /v1/tenants

BREAKING CHANGE: clients must update the base path; /v1/orgs now returns 404.
```

## Handling `$ARGUMENTS`

- If `$ARGUMENTS` already matches `<type>(<scope>): <description>` or `<type>: <description>`, use it verbatim as the header.
- If `$ARGUMENTS` is a plain phrase (e.g. `add tenant filter`), infer the type and scope from the diff and build a proper header around it.
- If `$ARGUMENTS` is a bare type (e.g. `fix`), use that type and write the description yourself.

## Validation before committing

Check the header against this regex and rewrite it if it doesn't match:

```
^(feat|fix|refactor|perf|test|docs|style|build|ci|chore|revert)(\([a-z0-9-]+\))?!?: [a-z].{0,60}[^.]$
```

## Rules

- Never use `--amend` or `--no-verify`.
- If a pre-commit hook fails, report the failure and fix the underlying issue — do not bypass it.
- One logical change per commit. If the diff clearly contains unrelated changes (e.g. a feature plus a dependency bump), tell the user and offer to split into separate commits.
- Do not push. Use `/push` or `/commit-push` for that.
