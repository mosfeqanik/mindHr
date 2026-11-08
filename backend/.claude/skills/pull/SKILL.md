---
name: pull
description: Pull the latest commits from the remote (origin) into the current branch. Use when the user says "pull", "/pull", "git pull", or asks to update their branch from the remote.
allowed-tools: Bash(git status:*), Bash(git branch:*), Bash(git log:*), Bash(git pull:*), Bash(git remote:*), Bash(git fetch:*), Bash(git diff:*)
---

# Pull

Bring the current branch up to date with its remote.

## Steps

1. Run `git status -sb` to get the current branch, its upstream, and any uncommitted changes.
2. Run `git remote -v` to confirm a remote named `origin` exists. If none exists, tell the user and stop.
3. If the branch has no upstream, tell the user and ask which remote branch to pull from (usually `origin/<branch>`). Do not guess.
4. Run `git fetch origin`, then `git log --oneline HEAD..@{u}` to list the incoming commits.
   - If there are none, say the branch is already up to date and stop.
5. If there are uncommitted changes, check whether the incoming commits touch the same files
   (`git diff --name-only HEAD..@{u}` against the changed files from step 1).
   - If they overlap, show the overlapping files and ask the user before pulling (commit, stash, or cancel).
   - If they do not overlap, continue; git keeps the local changes.
6. Run `git pull --ff-only`.
   - If it fails because the branches have diverged (local commits not on the remote), show
     `git log --oneline @{u}..HEAD` (local only) and `git log --oneline HEAD..@{u}` (remote only),
     then ask whether to `git pull --rebase` or `git pull --no-rebase` (merge). Do not choose for the user.
7. If a rebase or merge stops on conflicts, list the conflicted files (`git status`) and stop; do not resolve them without asking.
8. Report the pulled commits (`git log --oneline -5`) and which files changed.
   - If files under `backend/` changed, remind the user to restart the backend (`./mvnw clean spring-boot:run` if it fails with `ClassFormatError`).
   - If `frontend/package.json` or `package-lock.json` changed, suggest `npm install` in `frontend/`.

## Rules

- Never use `git reset --hard`, `git stash drop`, `git clean`, or `--force` unless the user explicitly asks in this session.
- Do not commit or push anything.
- Do not stash local changes without asking.
- If `$ARGUMENTS` names a remote or branch (e.g. `upstream main`), pull from that instead of the defaults.
