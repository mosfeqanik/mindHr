---
name: commit-push
description: Stage all changes, commit using the Conventional Commits format, then push to origin. Use when the user says "commit and push", "/commit-push", or wants their work committed and pushed in one step.
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git add:*), Bash(git commit:*), Bash(git branch:*), Bash(git push:*), Bash(git remote:*), Bash(git fetch:*)
---

# Commit and Push

Commit the current changes and push them to the remote in one flow.

## Steps

1. Follow the `/commit` skill exactly to create the commit:
   - Inspect `git status`, `git diff HEAD`, and `git log --oneline -10`.
   - Stage with `git add -A` (skipping anything that looks like a secret).
   - Write a Conventional Commits header (`type(scope): description`, ≤ 72 chars, imperative, lowercase, no trailing period), optional body, and the session's attribution line. See `/commit` for the full type table and validation regex.
   - If there are no changes to commit but there are unpushed commits, skip to step 2.
   - If there is nothing to commit and nothing to push, say so and stop.
2. Follow the `/push` skill to push:
   - `git push -u origin <branch>` if no upstream is set, otherwise `git push`.
   - On rejection, fetch and show incoming commits, then ask before rebasing. Never force-push.
3. Report the commit hash, the branch, and the remote it was pushed to.

## Rules

- Never `--amend`, `--no-verify`, or `--force`.
- If `$ARGUMENTS` is provided, handle it exactly as `/commit` does (verbatim if already conventional, otherwise infer type/scope around it).
