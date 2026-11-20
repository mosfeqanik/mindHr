---
name: commit-push
description: Stage all changes, commit using the Conventional Commits format, push the branch to origin and open a pull request into the default branch (or update the PR that is already open). Use when the user says "commit and push", "/commit-push", "commit, push and create a PR", or wants their work committed, pushed and reviewed in one step.
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git add:*), Bash(git commit:*), Bash(git branch:*), Bash(git switch:*), Bash(git push:*), Bash(git remote:*), Bash(git fetch:*), Bash(git rev-parse:*), Bash(git symbolic-ref:*), Bash(gh auth status:*), Bash(gh pr view:*), Bash(gh pr create:*), Bash(gh pr list:*)
---

# Commit, Push and Create a PR

Commit the current changes, push them to a branch on `origin`, and open a pull request into the default branch.
The commit covers the whole repository (backend, frontend, docs), not only `backend/`.

## Steps

### 1. Find the branches

- **Base branch** (where the PR goes): the remote's default branch,
  `git symbolic-ref --short refs/remotes/origin/HEAD` with the `origin/` prefix removed (currently `main`).
  If that fails, run `git remote set-head origin --auto` and try again.
- **Work branch**: the current branch, `git branch --show-current`.
  - If the work branch **is** the base branch, do not commit there. Create a branch first:
    `git switch -c <type>/<short-kebab-description>` (for example `feat/admin-user-type`), using the type you
    will use in the commit header.
  - If HEAD is detached, stop and ask the user which branch to use.

### 2. Commit

Follow the `/commit` skill to create the commit:

- Inspect `git status`, `git diff HEAD` and `git log --oneline -10`.
- Stage with `git add -A`, but leave out anything that looks like a secret (`.env`, `*.pem`, `credentials*.json`,
  `.claude/settings.local.json`) and macOS junk (`.DS_Store`). Never commit or print `backend/.env`.
- Header: `type(scope): description`: ≤ 72 chars, imperative, lowercase, no trailing period. Add a short body
  (what and why, as bullets) when the change is more than one small thing. See `/commit` for the type table.
- End the message with the **session's** commit attribution line (from the system reminder), not a hard-coded one.
- If there is nothing to commit but the branch has unpushed commits, go on to step 3.
- If there is nothing to commit, nothing to push **and** the branch already has an open PR, report that PR and stop.

### 3. Push

- `git push -u origin <work branch>` if the branch has no upstream yet, otherwise `git push`.
- On rejection: `git fetch`, show the incoming commits (`git log --oneline HEAD..@{u}`), then **ask** before
  rebasing or merging. Never force-push.
- Pushing and `gh` need the network. If the call fails because of the sandbox (e.g. "Could not resolve host"),
  retry it with the sandbox disabled.

### 4. Create or update the PR

1. Check `gh auth status`. If `gh` is not signed in, stop here: say the commit was pushed, and tell the user to run
   `gh auth login` (or set `GH_TOKEN`) and then run this skill again. Do not try other ways to open the PR.
2. If the work branch equals the base branch (should not happen after step 1), stop: there is nothing to compare.
3. Look for an open PR from this branch: `gh pr view <work branch> --json url,state,number`.
   - **Open PR found:** the push already updated it. Report its URL; do not create another.
   - **None:** create one:

     ```bash
     gh pr create --base <base> --head <work branch> --title "<title>" --body-file - <<'EOF'
     ## Summary
     - <what changed and why, 1-5 bullets, covering every commit in the PR>

     ## Test plan
     - [ ] <how it was checked: build, ./mvnw test, scripts/api_e2e_test.py, manual steps>

     <the session's PR attribution line>
     EOF
     ```

     - **Title:** the commit header when the PR has one commit; otherwise a Conventional-Commits style summary of
       all commits in `git log --oneline <base>..HEAD`.
     - Base the body on **all** commits between the base and the branch, not only the last one.
     - Write `@name` only for a real GitHub login you mean to notify; put roles and code in backticks.

### 5. Report

Give the commit hash and header, the work branch, the remote (`origin`), and the PR URL (or say the PR was
updated, or why it was not created).

## Rules

- Never `--amend`, `--no-verify`, `--force` or `--force-with-lease`.
- Never commit straight onto the base branch; always go through a PR.
- Do not merge, close or approve PRs; only create them.
- If `$ARGUMENTS` is given, use it for the commit header exactly as `/commit` does: verbatim if it is already
  conventional, otherwise infer the type and scope around it. A `--base <branch>` argument overrides the base branch.
