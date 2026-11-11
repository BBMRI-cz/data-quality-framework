<template>
  <pre
    class="code-viewer code-highlight"
    :data-language="language?.toLowerCase() || 'plain'"
  ><template v-if="segments.length"><span
    v-for="(segment, index) in segments"
    :key="index"
    :class="segment.classes || undefined"
  >{{ segment.text }}</span></template><span v-else class="code-viewer-empty">{{ emptyText }}</span></pre>
</template>

<script setup>
  import { computed } from 'vue';
  import { classHighlighter, highlightCode } from '@lezer/highlight';
  import { getLanguageSupport } from '@/utils/codeLanguages.js';
  import '@/assets/code-highlight.css';

  /**
   * Read-only, syntax-highlighted code block. Highlighting is computed
   * statically with the Lezer parser of the given language (a quality check
   * type such as SQL or PYTHON); unsupported types are shown as plain text.
   * Output is rendered as text spans, never as raw HTML.
   */
  const props = defineProps({
    code: {
      type: String,
      default: '',
    },
    language: {
      type: String,
      default: '',
    },
    emptyText: {
      type: String,
      default: 'N/A',
    },
  });

  const segments = computed(() => {
    const code = props.code ?? '';
    if (!code) return [];

    const support = getLanguageSupport(props.language);
    if (!support) return [{ text: code, classes: '' }];

    const result = [];
    const tree = support.language.parser.parse(code);
    highlightCode(
      code,
      tree,
      classHighlighter,
      (text, classes) => result.push({ text, classes }),
      () => result.push({ text: '\n', classes: '' })
    );
    return result;
  });
</script>

<style scoped>
  .code-viewer {
    width: 100%;
    background: var(--color-gray-50);
    border: 1px solid var(--color-gray-200);
    border-radius: var(--radius-md);
    padding: var(--spacing-md);
    overflow-x: auto;
    font-family: var(--font-mono), monospace;
    font-size: 0.875rem;
    line-height: 1.5;
    white-space: pre-wrap;
    word-wrap: break-word;
    margin: var(--spacing-xs) 0 0;
    color: var(--color-gray-800);
  }

  .code-viewer-empty {
    color: var(--color-gray-500);
  }
</style>
