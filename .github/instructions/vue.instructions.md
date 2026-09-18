---
description: 'Vue 3 development standards for the agent and server frontends'
applyTo: '**/*.vue, **/*.js'
---

# Vue 3 Development Instructions

Instructions for building Vue 3 frontends in this repository. The primary goal is
**consistency** — new code must follow the conventions already established in
`agent/frontend` and `server/frontend`.

## Project Context

Both frontends share the same stack:

- Vue 3 with the Composition API, exclusively `<script setup>` (no Options API)
- Vite for development and builds
- Plain JavaScript (ES modules) — there is **no TypeScript** in the frontends
- Vue Router for navigation
- Bootstrap 5 + bootstrap-icons for styling and icons (no Tailwind, no custom CSS frameworks)
- Axios for HTTP, wrapped in service modules
- `@/` import alias pointing to `src/`
- ESLint (flat config) + Prettier enforce style; run them before committing

State management differs per app:

- `agent/frontend`: Pinia stores in `src/stores/` via `defineStore`
- `server/frontend`: lightweight shared stores — plain `reactive()` objects exported from
  `src/stores/*.js` (e.g. `authStore.js`). Follow the pattern of the app you are in.

## Project Structure

```
src/
  ├── components/           # Reusable UI components (PascalCase filenames);
  │                         # the server frontend groups primitives under components/ui/
  ├── views/                # Routed page components (*View.vue in server, *Page.vue in agent)
  ├── composables/          # Reusable logic as useXxx.js
  ├── stores/               # Global state (Pinia or reactive stores, see above)
  ├── services/ and/or api/ # Axios client + service modules (apiService, notificationService)
  ├── router/               # Route definitions and navigation guards
  ├── utils/                # Pure helper modules (dateUtils.js, qualityCheckUtils.js, ...)
  └── assets/               # base.css (design tokens), main.css
```

## Development Standards

### Components

- Use `<script setup>` with `defineProps` and `defineEmits`
- Name component files in PascalCase (`QualityCheckCard.vue`); views end with `View.vue`
  (server frontend) or `Page.vue` (agent frontend)
- Keep components small and single-purpose; extract reusable logic into `composables/useXxx.js`
- Validate props with runtime declarations (type, default, required, validator)
- Emit events in kebab-case (`version-added`) and handle them with `@event` bindings
- Use slots for composition (e.g. `PageHeader` exposes an `#actions` slot)
- Reuse existing shared components before creating new ones — both apps provide
  `PageHeader`, `BaseModal`, `ConfirmModal`/`DeleteConfirmModal`, `StatsCard`/`StatCard`,
  `SaveButton`, `CancelButton`, plus table, pagination, filter, and badge primitives
  (the server frontend groups these under `components/ui/`)

### Reactivity

- `ref` for primitives, `reactive` for grouped local state, `computed` for derived values
- Prefer `computed` over `watch`; use `watch` only for side effects
- Keep business rules out of templates — move them into computed properties or utils

### Data Fetching & Services

- Never call Axios directly from components — go through service modules
  (`apiService` / `*Service.js`) that wrap the shared Axios client
- Every async view follows the loading / error / empty / loaded pattern:

  ```vue
  <div v-if="loading" class="loading-state">
    <div class="spinner-border text-primary" role="status">
      <span class="visually-hidden">Loading...</span>
    </div>
  </div>
  <div v-else-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
  <div v-else-if="items.length === 0" class="text-muted">
    <i class="bi bi-inbox me-1"></i>No items found
  </div>
  ```

- On errors: `console.error` for diagnostics plus `notificationService.error(title, message)`
  for user feedback; use `notificationService.success(title, message)` after mutations
- Confirm destructive or immutable actions with `ConfirmModal` / `DeleteConfirmModal`

### Styling

- Bootstrap 5 utility classes first; add `<style scoped>` only for what utilities cannot express
- **Never use hardcoded color codes.** Use the design tokens from `base.css`:
  `var(--color-primary)`, `var(--color-gray-50..900)`, `var(--bg-card)`, `var(--spacing-*)`,
  `var(--radius-*)`, `var(--shadow-*)`, `var(--transition-*)`, `var(--font-mono)`
- Do not redefine Bootstrap utilities in scoped styles (no `.bg-light { ... !important }` etc.)
- Use Bootstrap Icons (`bi bi-*`) for icons; monospace text uses `font-monospace` with
  `var(--font-mono)`
- Standard card pattern: `card border-0 shadow-sm` with
  `card-header bg-white border-bottom py-3` and `card-body p-4`
- Standard table pattern: `table-responsive` wrapper with
  `table table-hover align-middle mb-0`; versions/identifiers shown as `badge bg-primary`

### Routing

- Define routes centrally in `router/index.js` with `meta: { requiresAuth, title }`
- Guard navigation via a global `router.beforeEach` checking auth state
- Use `useRoute()` / `useRouter()` inside `<script setup>` for params and navigation
- In the server frontend, set the document title with `useHead` from `@unhead/vue`

### Forms

- Use controlled `v-model` bindings with Bootstrap classes (`form-control`, `form-select`,
  `form-check-input`, `is-invalid` + `invalid-feedback`)
- Disable inputs and buttons while a request is in flight (`:disabled="saving"`)
- Submit on Enter where sensible (`@keyup.enter`)

### Security & Accessibility

- Do not use `v-html`
- Keep auth handling consistent with the existing `authStore` (token storage, guards)
- Use semantic HTML and Bootstrap's accessibility helpers (`visually-hidden`, `role="status"`,
  `aria-label` on icon-only buttons)
- Modals are rendered via `Teleport` in `BaseModal` and must trap focus / close on Escape

## Code Quality Workflow

Run these in the frontend you touched before committing:

```bash
npm run lint      # ESLint (flat config) with --fix
npm run format    # Prettier: single quotes, semicolons, printWidth 100,
                  # trailingComma es5, vueIndentScriptAndStyle (indent script/style blocks)
npm run build     # Must complete without errors
```

## Additional Guidelines

- Document exported composables and utils with JSDoc
- Keep utility functions pure and framework-agnostic in `utils/`
- Follow the official Vue style guide for anything not covered here
  (https://vuejs.org/style-guide)
