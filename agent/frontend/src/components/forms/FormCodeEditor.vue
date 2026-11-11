<template>
  <div class="form-field" :class="{ 'has-error': hasError }">
    <label v-if="label" :id="labelId" class="form-label" @click="focus">
      <i v-if="icon" :class="['bi', icon]"></i>
      <span>{{ label }}</span>
      <span v-if="required" class="required-indicator">*</span>
    </label>

    <div
      ref="editorRef"
      class="code-editor code-highlight"
      :class="{ 'is-invalid': hasError, 'is-readonly': readonly }"
      :style="{ '--code-editor-min-height': minHeight }"
      :data-language="languageName"
    ></div>

    <div v-if="hasError" class="invalid-feedback">
      {{ error }}
    </div>

    <small v-if="helpText && !hasError" class="form-help">
      <i v-if="helpIcon" :class="['bi', helpIcon, 'me-1']"></i>
      {{ helpText }}
    </small>
  </div>
</template>

<script setup>
  import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue';
  import { Compartment, EditorState } from '@codemirror/state';
  import {
    EditorView,
    drawSelection,
    highlightActiveLine,
    highlightActiveLineGutter,
    keymap,
    lineNumbers,
    placeholder as placeholderExtension,
  } from '@codemirror/view';
  import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands';
  import { bracketMatching, indentOnInput, syntaxHighlighting } from '@codemirror/language';
  import { classHighlighter } from '@lezer/highlight';
  import { getLanguageSupport } from '@/utils/codeLanguages.js';
  import '@/assets/code-highlight.css';

  /**
   * Code editor form field with syntax highlighting (CodeMirror 6).
   * Drop-in replacement for a monospace FormTextarea; the highlighted
   * language follows the `language` prop (a quality check type such as SQL or PYTHON).
   */
  const props = defineProps({
    modelValue: {
      type: String,
      default: '',
    },
    language: {
      type: String,
      default: '',
    },
    label: {
      type: String,
      default: '',
    },
    icon: {
      type: String,
      default: '',
    },
    placeholder: {
      type: String,
      default: '',
    },
    helpText: {
      type: String,
      default: '',
    },
    helpIcon: {
      type: String,
      default: '',
    },
    error: {
      type: String,
      default: '',
    },
    required: {
      type: Boolean,
      default: false,
    },
    readonly: {
      type: Boolean,
      default: false,
    },
    rows: {
      type: [String, Number],
      default: 8,
    },
    id: {
      type: String,
      default: '',
    },
  });

  const emit = defineEmits(['update:modelValue']);

  const editorRef = ref(null);
  const view = shallowRef(null);

  const languageCompartment = new Compartment();
  const placeholderCompartment = new Compartment();
  const readonlyCompartment = new Compartment();
  const ariaCompartment = new Compartment();

  const inputId = computed(
    () => props.id || `form-code-editor-${Math.random().toString(36).slice(2, 11)}`
  );
  const labelId = computed(() => `${inputId.value}-label`);
  const hasError = computed(() => !!props.error);
  const languageName = computed(() => props.language?.toLowerCase() || 'plain');
  // 1.6 line-height in rem plus vertical padding, mirroring textarea `rows`
  const minHeight = computed(() => `calc(${Number(props.rows) * 1.6}em + 1.5rem)`);

  const languageExtension = () => getLanguageSupport(props.language) ?? [];
  const placeholderExt = () => (props.placeholder ? placeholderExtension(props.placeholder) : []);
  const readonlyExt = () => [EditorState.readOnly.of(props.readonly)];
  const ariaExt = () =>
    EditorView.contentAttributes.of({
      id: inputId.value,
      role: 'textbox',
      'aria-multiline': 'true',
      ...(props.label ? { 'aria-labelledby': labelId.value } : {}),
      ...(props.required ? { 'aria-required': 'true' } : {}),
      ...(hasError.value ? { 'aria-invalid': 'true' } : {}),
    });

  onMounted(() => {
    view.value = new EditorView({
      parent: editorRef.value,
      state: EditorState.create({
        doc: props.modelValue ?? '',
        extensions: [
          lineNumbers(),
          highlightActiveLineGutter(),
          highlightActiveLine(),
          history(),
          drawSelection(),
          indentOnInput(),
          bracketMatching(),
          syntaxHighlighting(classHighlighter),
          EditorView.lineWrapping,
          keymap.of([...defaultKeymap, ...historyKeymap, indentWithTab]),
          languageCompartment.of(languageExtension()),
          placeholderCompartment.of(placeholderExt()),
          readonlyCompartment.of(readonlyExt()),
          ariaCompartment.of(ariaExt()),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) {
              emit('update:modelValue', update.state.doc.toString());
            }
          }),
        ],
      }),
    });
  });

  onBeforeUnmount(() => {
    view.value?.destroy();
    view.value = null;
  });

  // Sync external model changes (e.g. a check loaded from the API) into the editor
  watch(
    () => props.modelValue,
    (value) => {
      const editor = view.value;
      const next = value ?? '';
      if (editor && editor.state.doc.toString() !== next) {
        editor.dispatch({ changes: { from: 0, to: editor.state.doc.length, insert: next } });
      }
    }
  );

  const reconfigure = (compartment, extension) => {
    view.value?.dispatch({ effects: compartment.reconfigure(extension) });
  };

  watch(
    () => props.language,
    () => reconfigure(languageCompartment, languageExtension())
  );
  watch(
    () => props.placeholder,
    () => reconfigure(placeholderCompartment, placeholderExt())
  );
  watch(
    () => props.readonly,
    () => reconfigure(readonlyCompartment, readonlyExt())
  );
  watch([hasError, () => props.required, () => props.label], () =>
    reconfigure(ariaCompartment, ariaExt())
  );

  function focus() {
    view.value?.focus();
  }

  defineExpose({ focus, view });
</script>

<style scoped>
  .form-field {
    margin-bottom: var(--spacing-xl);
  }

  .form-label {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--color-gray-700);
    margin-bottom: var(--spacing-sm);
    cursor: text;
  }

  .form-label i {
    color: var(--color-primary);
    font-size: 1rem;
  }

  .required-indicator {
    color: var(--color-danger);
    font-weight: 700;
  }

  .code-editor {
    border: 2px solid var(--color-gray-200);
    border-radius: var(--radius-md);
    background-color: var(--color-gray-50);
    transition: all var(--transition-base);
    overflow: hidden;
    resize: vertical;
  }

  .code-editor:focus-within {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  }

  .code-editor.is-invalid {
    border-color: var(--color-danger);
    background-color: #fff5f5;
  }

  .code-editor.is-invalid:focus-within {
    box-shadow: 0 0 0 4px rgba(220, 53, 69, 0.1);
  }

  .code-editor :deep(.cm-editor) {
    height: 100%;
    font-size: 0.95rem;
    background: transparent;
  }

  /* Min height on content and gutter (not the editor) so the gutter spans the full field */
  .code-editor :deep(.cm-content),
  .code-editor :deep(.cm-gutter) {
    min-height: var(--code-editor-min-height);
  }

  .code-editor :deep(.cm-editor.cm-focused) {
    outline: none;
  }

  .code-editor :deep(.cm-scroller) {
    font-family: var(--font-mono), monospace;
    line-height: 1.6;
  }

  .code-editor :deep(.cm-content) {
    padding: 0.75rem 0;
  }

  .code-editor :deep(.cm-line) {
    padding: 0 var(--spacing-md) 0 var(--spacing-sm);
  }

  .code-editor :deep(.cm-gutters) {
    background-color: var(--color-gray-100);
    border-right: 1px solid var(--color-gray-200);
    color: var(--color-gray-400);
  }

  .code-editor :deep(.cm-activeLine),
  .code-editor :deep(.cm-activeLineGutter) {
    background-color: rgba(102, 126, 234, 0.06);
  }

  .code-editor.is-readonly :deep(.cm-activeLine),
  .code-editor.is-readonly :deep(.cm-activeLineGutter) {
    background-color: transparent;
  }

  .code-editor :deep(.cm-placeholder) {
    color: var(--color-gray-400);
  }

  .code-editor :deep(.cm-matchingBracket) {
    background-color: rgba(102, 126, 234, 0.18);
    outline: none;
  }

  .invalid-feedback {
    display: block;
    margin-top: var(--spacing-xs);
    font-size: 0.875rem;
    color: var(--color-danger);
  }

  .form-help {
    display: block;
    margin-top: var(--spacing-sm);
    font-size: 0.875rem;
    color: var(--color-gray-500);
    line-height: 1.4;
  }

  .form-help i {
    color: var(--color-gray-400);
  }
</style>
