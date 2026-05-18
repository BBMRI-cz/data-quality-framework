import { computed, nextTick, onMounted, ref, watch } from 'vue';

const getScrollingElement = () => {
  return document.scrollingElement || document.documentElement;
};

export function usePaginatedTableLayout({ loading, paginatedItems, onPageChange }) {
  const pendingRestore = ref(null);
  const contentRef = ref(null);
  const contentMinHeight = ref(null);

  const updateContentHeight = () => {
    const content = contentRef.value;
    if (!content) {
      return;
    }
    const height = content.getBoundingClientRect().height;
    if (height > 0) {
      contentMinHeight.value = height;
    }
  };

  const bodyStyle = computed(() => {
    return contentMinHeight.value ? { minHeight: `${contentMinHeight.value}px` } : undefined;
  });

  const restoreScrollAndFocus = async () => {
    const pending = pendingRestore.value;
    if (!pending) {
      return;
    }
    pendingRestore.value = null;

    await nextTick();

    const scrollingElement = getScrollingElement();
    const restoreScrollPosition = () => {
      if (scrollingElement && typeof pending.scrollTop === 'number') {
        scrollingElement.scrollTop = pending.scrollTop;
      }
    };

    restoreScrollPosition();
    requestAnimationFrame(restoreScrollPosition);

    if (pending.focusedElement?.focus) {
      try {
        pending.focusedElement.focus({ preventScroll: true });
      } catch {
        pending.focusedElement.focus();
      }
    }
  };

  watch(loading, (isLoading) => {
    if (!isLoading) {
      restoreScrollAndFocus();
      nextTick(updateContentHeight);
    }
  });

  watch(paginatedItems, () => {
    if (!loading.value) {
      nextTick(updateContentHeight);
    }
  });

  onMounted(() => {
    nextTick(updateContentHeight);
  });

  const handlePageChange = (nextPage, event) => {
    const focusedElement = event?.currentTarget || null;
    const scrollingElement = getScrollingElement();
    const scrollTop = scrollingElement ? scrollingElement.scrollTop : null;

    onPageChange(nextPage);

    pendingRestore.value = { scrollTop, focusedElement };
    if (!loading.value) {
      restoreScrollAndFocus();
    }
  };

  return {
    bodyStyle,
    contentRef,
    handlePageChange,
  };
}
