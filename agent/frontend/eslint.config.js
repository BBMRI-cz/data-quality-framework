import js from '@eslint/js';
import pluginVue from 'eslint-plugin-vue';
import eslintConfigPrettier from 'eslint-config-prettier';

export default [
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  eslintConfigPrettier,
  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        // Browser globals
        window: 'readonly',
        document: 'readonly',
        console: 'readonly',
        sessionStorage: 'readonly',
        localStorage: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        confirm: 'readonly',
        alert: 'readonly',
        fetch: 'readonly',
        URL: 'readonly',
        atob: 'readonly',
        btoa: 'readonly',
      },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/require-default-prop': 'off',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    },
  },
  {
    // Node.js config files
    files: ['vite.config.js', 'eslint.config.js'],
    languageOptions: {
      globals: {
        __dirname: 'readonly',
        __filename: 'readonly',
        process: 'readonly',
      },
    },
  },
  {
    ignores: ['node_modules/', 'dist/'],
  },
];

