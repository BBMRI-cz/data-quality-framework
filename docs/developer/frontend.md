# Frontend Development

## Fonts

Both fonts are **self-hosted** via [`@fontsource`](https://fontsource.org/) to ensure consistent rendering across all operating systems. No external CDN requests are made.

### Packages

| Package | Font | Purpose |
|---|---|---|
| `@fontsource/open-sans` | Open Sans | UI text |
| `@fontsource/source-code-pro` | Source Code Pro | Code / monospace text |

### Imported Weights

Weights are imported in `main.js`. Only import what you use — each weight adds to the bundle size.

| Font | Weights |
|---|---|
| Open Sans | 400 (regular), 500 (medium), 600 (semibold), 700 (bold) |
| Source Code Pro | 400 (regular), 500 (medium) |

### CSS Variables

Fonts are defined **once** as CSS custom properties in `base.css`:

```css
:root {
  --font-family: 'Open Sans', sans-serif;
  --font-mono: 'Source Code Pro', monospace;
}
```

### Usage

Always reference fonts through the variables. Never hardcode font names in components.

```css
/* ✅ Correct */
.my-element {
  font-family: var(--font-family), sans-serif;
}

.my-code {
  font-family: var(--font-mono), monospace;
}

/* ❌ Wrong — hardcoded font names */
.my-element {
  font-family: 'Open Sans', sans-serif;
}
```

::: tip
The generic fallback (`sans-serif` / `monospace`) after `var()` is required to satisfy CSS linters, which cannot resolve custom properties.
:::

### Changing Fonts

1. Install the new `@fontsource/*` package
2. Update the imports in `main.js`
3. Update the CSS variable in `base.css`

That's it — all components pick up the change automatically through the variables.

