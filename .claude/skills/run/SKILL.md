---
name: run
description: Start the Spring Boot backend (:8080) and the React frontend dev server (:5173) together. Use when the user says "run", "/run", "start the app", or wants both projects running.
allowed-tools: Bash(./mvnw spring-boot:run:*), Bash(npm install:*), Bash(npm run dev:*), Bash(curl:*), Bash(netstat:*)
---

# Run

Start the Java backend and the frontend dev server, each as a background process.

## Steps

1. Check `backend/.env` exists and has `DB_URL` and `JWT_SECRET`. Never print its contents. If it is missing, tell the user and stop.
2. Check that ports 8080 and 5173 are free (`netstat -ano | findstr :8080`). If one is in use, tell the user which process holds it and ask before continuing; the app may already be running.
3. Start the backend in the background from `backend/`: `./mvnw spring-boot:run` (use `run_in_background`).
4. If `frontend/node_modules` does not exist, run `npm install` in `frontend/` first.
5. Start the frontend in the background from `frontend/`: `npm run dev` (use `run_in_background`).
6. Wait until the backend answers (poll `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/v3/api-docs` until it returns 200; the first start can take a minute). If it never comes up, read the backend output and report the error.
7. Report the URLs:
   - Web app: http://localhost:5173 (proxies `/api` to the backend)
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html

## Rules

- Do not use Docker unless `$ARGUMENTS` contains `docker`. In that case run `docker compose up --build -d` in `backend/`, then in `frontend/` (backend first), and report http://localhost:3000.
- If `$ARGUMENTS` is `backend` or `frontend`, start only that one.
- Never print `backend/.env` or the JWT secret.
- To stop, use TaskStop on the background tasks (or `docker compose down` in Docker mode).
- The first start seeds a `SUPER_ADMIN` user; remind the user to change its password.
