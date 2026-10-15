# Repository Guidelines

## Project Structure & Module Organization

The root Maven project aggregates backend modules. The first bounded context lives in `backend/movimento-manual-service`. Place Java code under `src/main/java/com/antlia/movimanu`, preserving the existing DDD/hexagonal boundaries: `domain` for business rules, `application` for ports and use cases, `infrastructure` for persistence, configuration, and cloud adapters, and `interfaces/rest` for HTTP endpoints. Resources and Flyway migrations belong in `src/main/resources`; tests mirror the package structure under `src/test/java`, split into `unit` and `integration`.

`frontend/movimanu-web/src/app` is currently a scaffold only. Keep reusable UI code in `shared`, application-wide services in `core`, and manual-movement code in `features/manual-movements`. Do not add Node commands until an Angular workspace manifest exists.

## Build, Test, and Development Commands

Run commands from the repository root on Windows:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd test
```

`clean verify` compiles all Maven modules, runs tests, checks JaCoCo coverage, and packages the backend JAR. `test` provides a faster test-only cycle. The project requires Java 21; the wrapper supplies Maven 3.9.16. The backend is not executable yet because Spring Boot repackaging is intentionally skipped until an application entry point is added.

## Coding Style & Naming Conventions

Follow `.editorconfig`: UTF-8, CRLF, final newline, trimmed trailing whitespace, four-space Java indentation, and two-space indentation for XML, YAML, SQL, and Markdown. Use lowercase package names, `PascalCase` Java types, `camelCase` methods and fields, and descriptive suffixes such as `Controller`, `UseCase`, `Repository`, and `Adapter`. Name Flyway scripts `V<number>__<snake_case_description>.sql`.

## Testing Guidelines

Use JUnit 5 and Spring Boot Test. Name unit tests `*Test` and integration tests `*IT`; keep them in their corresponding test packages. Add tests with every behavior change. JaCoCo currently enforces 100% line coverage during `verify`, excluding configuration classes and the application bootstrap class.

## Commit & Pull Request Guidelines

The latest history uses Conventional Commit style (for example, `chore: estrutura inicial do projeto + arquivos de config`). Prefer `feat:`, `fix:`, `test:`, `docs:`, or `chore:` with a short imperative summary. Pull requests should explain scope and design choices, link the relevant issue, list validation commands, and call out schema or configuration changes. Include screenshots for future UI changes and never commit secrets, `.env` files, runtime H2 data, or build output.
