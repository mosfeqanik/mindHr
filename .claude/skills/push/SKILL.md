---
name: push
description: Push the current branch to the remote (origin), setting upstream if needed. Use when the user says "push", "/push", or asks to push their commits.
allowed-tools: Bash(git status:*), Bash(git branch:*), Bash(git log:*), Bash(git push:*), Bash(git remote:*), Bash(git fetch:*)
---

# Push

Push the current branch's commits to the remote.

## Steps

1. Run `git status -sb` to get the current branch and its upstream tracking status.
2. Run `git remote -v` to confirm a remote named `origin` exists. If none exists, tell the user and stop.
3. If the branch has no upstream, push with `git push -u origin <branch>`. Otherwise run `git push`.
4. If the push is rejected because the remote is ahead:
   - Run `git fetch origin` and show `git log --oneline HEAD..origin/<branch>` so the user can see the incoming commits.
   - Ask the user whether to `git pull --rebase` before retrying. Do not force-push.
5. Show the final `git log --oneline -5` and the remote branch URL.

## Rules

- Never use `--force` or `--force-with-lease` unless the user explicitly asks in this session.
- Do not commit anything. If there are uncommitted changes, mention them but still push what is committed.
- If `$ARGUMENTS` names a remote or branch (e.g. `upstream main`), use that instead of the defaults.
