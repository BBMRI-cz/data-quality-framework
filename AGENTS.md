# Agent Instructions

This repository uses centralized Copilot guidance alongside language-specific rule files.

## Primary Instruction Sources

Read and follow:
- `.github/copilot-instructions.md` - Core workflow and language policy
- `.github/instructions/spring.instructions.md` - Backend Java/Spring development
- `.github/instructions/java.instructions.md` - General Java best practices  
- `.github/instructions/vue.instructions.md` - Vue 3 frontend development

## Required Workflow

1. **List instruction files used**: Start by identifying which of the above rule files apply to your task
2. **Follow TDD**: Write tests before implementation when possible
3. **Fix compiler warnings**: Resolve all build warnings before moving on
4. **Language policy**: Keep all prompts, rules, and contributor-facing comments/docs in English

## Project Structure

This is a federated data quality framework with a decentralized architecture:

- **`agent/`** - Deployed at data provider sites for privacy-preserving quality assessment
  - `backend/` - Spring Boot 3.5.7 application (.jar) with modular architecture (Spring Modulith)
  - `frontend/` - Vue 3 + Vite web UI for configuration and monitoring
  
- **`server/`** - Central coordination server aggregating quality metrics
  - `backend/` - Spring Boot 3.5.7 application (PostgreSQL database)
  - `frontend/` - Vue 3 + Vite web UI for viewing aggregated metrics

## Technology Stack

- **Java**: 21 (target and source)
- **Backend**: Spring Boot 3.5.7, Spring Modulith for modular architecture
- **Frontend**: Vue 3 with Composition API, Pinia for state management, Bootstrap 5 for styling
- **Build**: Maven for Java, npm/Vite for frontends
- **Testing**: JUnit, Mockito for backend; Vue Test Utils for frontend components
- **Domain**: HAPI FHIR 8.8.1 for healthcare data, differential privacy mechanisms

## Backend Architecture (Spring Modulith)

Each backend is organized by domain modules within `eu.bbmri_eric.quality.[agent|server]`:

```
[module]/
  ├── [Module]Service.java          # Service interface
  ├── controller/                   # REST controllers (annotated with @RestController)
  ├── impl/                         # Service implementation + repositories
  ├── domain/                       # Entity domain models
  ├── dto/                          # Data Transfer Objects
  ├── event/                        # Domain events for inter-module communication
  ├── listener/                     # Event listeners (optional)
  └── scheduler/                    # Scheduled tasks (optional)
```

Example modules: `settings`, `dataquality`, `user`, `server` (communication with central server).

**Key patterns:**
- Service interface declared at module root; implementation in `impl/` package
- Repositories are package-private in `impl/` and accessed through service
- Controllers use DTOs; services work with domain entities internally
- Inter-module communication via Spring events (`@DomainEvents`, `@EventListener`)
- Swagger annotations required on all controllers and DTOs

## Frontend Architecture (Vue 3 + Composition API)

Structure for both `agent/frontend` and `server/frontend`:

```
src/
  ├── components/           # UI components (PascalCase filenames)
  ├── views/                # Page-level components (routed)
  ├── composables/          # Reusable logic (useAuth, useFetch, etc.)
  ├── stores/               # Pinia stores for global state
  ├── api/                  # API client integration
  ├── router/               # Vue Router 4 configuration
  ├── utils/                # Utility functions
  └── assets/               # Static assets, styles
```

**Key frameworks:**
- Pinia for state management (no Vuex)
- Bootstrap 5 (CSS framework) - do NOT use arbitrary colors; use only colors defined in `base.css`
- Axios for HTTP requests

## Build Commands

### Agent Backend
```bash
cd agent/backend
mvn clean install                          # Build and test
mvn --quiet clean com.spotify.fmt:fmt-maven-plugin:check  # Check formatting
mvn test                                   # Run tests only
```

### Agent Frontend
```bash
cd agent/frontend
npm install                                # Install dependencies
npm run dev                                # Development server (Vite hot reload)
npm run build                              # Production build
npm run lint                               # Check code formatting (ESLint + Prettier)
npm run format                             # Auto-format code
```

### Server Backend
```bash
cd server/backend
mvn clean install                          # Build and test
mvn --quiet clean com.spotify.fmt:fmt-maven-plugin:check  # Check formatting
mvn test                                   # Run tests only
```

### Server Frontend
```bash
cd server/frontend
npm install                                # Install dependencies
npm run dev                                # Development server (Vite hot reload)
npm run build                              # Production build
npm run lint                               # Check code formatting
npm run format                             # Auto-format code
```

### Running the Full Stack
```bash
docker compose up -d                       # Start agent, server, and PostgreSQL
# Agent: http://localhost:8081
# Server: http://localhost:8082
```

## Key Guidelines by Language

### Java/Spring Backend
- **Instructions used**: `spring.instructions.md`, `java.instructions.md`
- Use constructor injection; declare dependency fields as `private final`
- Use YAML configuration (`application.yml`); externalize secrets via environment variables
- Package by feature/domain, not by layer
- Keep controllers thin; place business logic in services
- Use ModelMapper for entity↔DTO conversions
- Throw custom exceptions; handle them in @ExceptionHandler
- Use SLF4J for logging (via @CommonsLog or inject); no System.out
- Use parameterized queries via Spring Data JPA
- Validate inputs with JSR-380 annotations (@NotNull, @Size, etc.)
- Run `mvn clean install` before committing

### Vue 3 Frontend
- **Instructions used**: `vue.instructions.md`
- Use `<script setup>` syntax with Composition API exclusively (no Options API)
- Use Pinia for global state; `ref` and `reactive` for local state
- Use `computed` for derived state, not watchers where possible
- Use `<style scoped>` for component-local styles
- Use only Bootstrap 5 classes and colors from `base.css` (no arbitrary colors)
- Lazy-load routes with dynamic imports (`defineAsyncComponent`)
- Validate props with TypeScript; use runtime validation only for external data
- Run `npm run lint` and `npm run format` before committing
- Run `npm run build` and verify no errors before deployment

## Testing & Quality

- **Backend**: Unit tests in `src/test/java`; integration tests use Spring Test framework
- **Frontend**: Component tests use Vue Test Utils
- **Code Coverage**: Both backends report coverage via codecov
- **Formatting**: 
  - Java: Spotify fmt plugin (checked in CI)
  - Frontend: ESLint + Prettier (checked in CI)
- **CI/CD**: GitHub Actions pipeline in `.github/workflows/ci.yml` runs on every PR and push to master

## Common Tasks

- **Add a new quality check module**: Create new package following Spring Modulith patterns in agent backend; define interface + implementation, add controller
- **Add frontend feature**: Create components in `components/`, use composables for shared logic, add Pinia store if needed
- **Update styling**: Edit Bootstrap classes or add custom styles to `base.css` (both frontends share styling patterns)
- **Connect to FHIR endpoint**: Use HAPI FHIR client in appropriate agent module; add configuration via `application.yml`
- **Debug module boundaries**: Use Spring Modulith's module export mechanism; check generated `spring-modulith/` documentation

## Debugging & Common Issues

- **Module not found error**: Verify module is listed in Spring Modulith configuration; check package structure
- **Frontend style not applied**: Ensure class names match Bootstrap/base.css definitions; check selector specificity
- **Test failure in CI but passing locally**: Verify Maven cache and clean build with `mvn clean install`
- **API mismatch**: Check Swagger docs at http://localhost:808[1|2]/swagger-ui.html after startup
