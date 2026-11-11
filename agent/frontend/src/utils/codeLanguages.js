import { sql, PostgreSQL } from '@codemirror/lang-sql';
import { python } from '@codemirror/lang-python';

/**
 * Syntax highlighting support per quality check type.
 * Types without an entry (CQL, JAVA) fall back to plain text.
 */
const LANGUAGE_FACTORIES = {
  SQL: () => sql({ dialect: PostgreSQL }),
  PYTHON: () => python(),
};

const QUERY_HINTS = {
  SQL: {
    placeholder: 'SELECT person_id FROM person WHERE ...',
    helpText: 'Return the IDs of failing persons in the first column, or a single count column',
  },
  PYTHON: {
    placeholder: 'Enter your Python script here...',
    helpText:
      'Print the result as a JSON object on the last stdout line: {"count", "entityType", "ids", "error"}',
  },
  CQL: {
    placeholder: 'Enter your CQL query here...',
    helpText: 'Write a CQL query to validate data quality',
  },
};

const DEFAULT_HINT = {
  placeholder: 'Enter your query here...',
  helpText: 'Write a query to validate data quality',
};

/**
 * Returns a CodeMirror LanguageSupport for the given check type, or null for plain text.
 */
export function getLanguageSupport(type) {
  const factory = LANGUAGE_FACTORIES[type?.toUpperCase?.()];
  return factory ? factory() : null;
}

/**
 * Returns the placeholder and help text that match the given check type.
 */
export function getQueryHints(type) {
  return QUERY_HINTS[type?.toUpperCase?.()] ?? DEFAULT_HINT;
}
