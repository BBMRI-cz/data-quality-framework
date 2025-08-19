<template>
  <ul class="pagination pagination-sm mt-2">
    <li class="page-item"
        :class="{ disabled: isInFirstPage}">
      <button class="page-link" @click="onClickPrevious">
        Prev
      </button>
    </li>

    <li class="page-item"
        :class="{active: isInFirstPage}">
      <button class="page-link" @click="onClickFirst"
      >
        1
      </button>
    </li>

    <li
        class="page-item"
        v-for="item in pages"
        :key="item.page"
        :class="{ active: isPageActive(item.page) }"
    >
      <button
          class="page-link"
          @click.prevent="onClickPage(item.page)"
          :disabled="item.isDisabled"
      >
        {{ item.page }}
      </button>
    </li>

    <li class="page-item"
    :class="{active: isInLastPage}">
      <button class="page-link" @click="onClickLast">
        {{ totalPages }}
      </button>
    </li>
    <li class="page-item"
        :class="{disabled: isInLastPage}">
      <button class="page-link" @click="onClickNext">
        Next
      </button>
    </li>
    <template v-if="showGoToPage">
      <li class="page-item disabled">
        <span class="page-link bg-transparent border-0">Page</span>
      </li>
      <li class="page-item">
        <input
            v-model="goToPage"
            type="number"
            min="1"
            :max="totalPages"
            class="form-control form-control-sm"
            style="width: 80px;"
            @keydown="allowOnlyNumbers"
        />
      </li>
      <li class="page-item">
        <button class="page-link" @click="onClickGoTo">Go</button>
      </li>
    </template>
  </ul>
</template>

<script setup>
import { computed, ref } from "vue";

const props = defineProps({
  maxVisibleButtons: {
    type: Number,
    default: 5
  },
  totalPages: {
    type: Number,
    required: true
  },
  pageSize: {
    type: Number,
    default: 60
  },
  currentPage: {
    type: Number,
    required: true
  }
});
const goToPage = ref(null);
const emit = defineEmits(['pageChanged']);

const half = Math.floor(props.maxVisibleButtons / 2);

const startPage = computed(() => {
  if (props.totalPages <= props.maxVisibleButtons + 2) return 2;
  if (props.currentPage <= half + 2) return 2;
  if (props.currentPage >= props.totalPages - half - 1) return props.totalPages - props.maxVisibleButtons;
  return props.currentPage - half;
});

const endPage = computed(() => {
  if (props.totalPages <= props.maxVisibleButtons + 2) return props.totalPages - 1;
  return Math.min(startPage.value + props.maxVisibleButtons - 1, props.totalPages - 1);
});

const pages = computed(() => {
  const result = [];
  for (let i = startPage.value; i <= endPage.value; i++) {
    result.push({
      page: i,
      isDisabled: i === props.currentPage
    });
  }
  return result;
});

const isInFirstPage = computed(() => props.currentPage === 1);
const isInLastPage = computed(() => props.currentPage === props.totalPages);

const showGoToPage = computed(() => {
  return props.totalPages >= 8;
})

function onClickFirst() {
  emit('pageChanged', 1);
}

function onClickPrevious() {
  emit('pageChanged', props.currentPage - 1);
}

function onClickPage(page) {
  emit('pageChanged', page);
}

function onClickNext() {
  emit('pageChanged', props.currentPage + 1);
}

function onClickLast() {
  emit('pageChanged', props.totalPages);
}

function onClickGoTo() {
  const page = Number(goToPage.value);
  if (page >= 1 && page <= props.totalPages && page !== props.currentPage) {
    emit('pageChanged', page);
  }
}

function isPageActive(page) {
  return props.currentPage === page;
}

function allowOnlyNumbers(event) {
  const allowedKeys = ['Backspace', 'Tab', 'ArrowLeft', 'ArrowRight', 'Delete'];
  const isNumber = /^[0-9]$/.test(event.key);

  if (!isNumber && !allowedKeys.includes(event.key)) {
    event.preventDefault();
  }
}
</script>
