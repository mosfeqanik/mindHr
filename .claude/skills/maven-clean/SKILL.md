---
name: maven-clean
description: Run a clean Maven build for this Spring Boot project using the Maven wrapper. Use when the user says "maven clean", "/maven-clean", "clean build", "mvn clean", or wants the target directory wiped and the project rebuilt.
allowed-tools: Bash(./mvnw:*), Bash(.\mvnw.cmd:*), Bash(mvnw.cmd:*), Bash(mvn:*)
---

# Maven Clean

Run a clean build of this Spring Boot / Maven project.

## Steps

1. Use the Maven wrapper that ships with the project, never a global `mvn`, so everyone builds with the same Maven version:
   - On Windows (this project's primary platform): `.\mvnw.cmd`
   - On macOS/Linux: `./mvnw`
2. Decide the goal from `$ARGUMENTS`:
   - No arguments → `clean install -DskipTests` (fast rebuild, wipes `target/`).
   - `test` → `clean test`
   - `install` → `clean install` (runs the full test suite)
   - `package` → `clean package -DskipTests`
   - `verify` → `clean verify`
   - Anything else → pass it through verbatim after `clean` (e.g. `/maven-clean compile -q`).
3. Run the command with a generous timeout (Spring Boot builds can take several minutes on first run because of dependency downloads).
4. Report the outcome:
   - On `BUILD SUCCESS`: state the goal that ran and the total time from the Maven summary.
   - On `BUILD FAILURE`: quote the first `[ERROR]` block (compiler errors or failing tests) and point to the file/line. Do not retry blindly — diagnose the cause.

## Rules

- Always include `clean` — that is the point of this skill.
- Never delete `target/` manually; let Maven's clean plugin do it.
- Do not modify `pom.xml` or source files as part of this skill; only report problems.
