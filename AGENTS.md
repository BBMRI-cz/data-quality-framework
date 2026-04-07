# Agent Instructions

This repository uses centralized Copilot guidance.

## Primary Instruction Source

Read and follow:
- `.github/copilot-instructions.md`
- Relevant language rule files in `.github/instructions/`:
  - `.github/instructions/java.instructions.md` for `*.java`
  - `.github/instructions/spring.instructions.md` for Spring backend changes
  - `.github/instructions/vue.instructions.md` for `*.vue`, `*.js`, `*.ts`, `*.scss`

## Required Workflow

1. Start by listing the instruction files you used.
2. Follow TDD whenever possible (tests first).
3. Fix compiler warnings and errors before moving on.
4. Keep prompts, rules, and contributor-facing comments/docs in English.
5. Run verification in the component you changed:
   - Backends: `cd agent/backend && mvn clean install` or `cd server/backend && mvn clean install`
   - Frontends: `cd agent/frontend && npm run build` or `cd server/frontend && npm run build`

## Additional Rules

If task-specific rule files are referenced from `.github/copilot-instructions.md` (for example in `.github/instructions/`), treat them as mandatory for the relevant work.

- Keep architecture boundaries explicit in changes: this repo contains two applications, `agent/` and `server/`, each split into `backend/` (Spring Boot, Java 21, Maven) and `frontend/` (Vue 3, Vite).
- For local full-stack smoke checks, prefer the documented root flow in `README.md` (`docker compose up -d`); the default quick-start endpoint is the quality agent on `http://localhost:8081`.
