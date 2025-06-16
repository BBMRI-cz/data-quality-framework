<template>
  <div class="container">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h2 class="mb-0">CQL Checks</h2>
      <button class="btn btn-success" @click="openAddModal">
        <i class="bi bi-plus me-1"></i>Add Check
      </button>
    </div>
    <div>
      <div v-for="dataQualityCheck in qualityChecks" :key="dataQualityCheck.id" class="card mb-2">
        <div class="card-body">
          <h5 class="card-title">{{ dataQualityCheck.name }}</h5>
          <p class="card-text text-start"><strong>ID:</strong> {{ dataQualityCheck.id }}</p>
          <p class="card-text text-start"><strong>Description:</strong> {{ dataQualityCheck.description }}</p>
          <p class="card-text text-start"><strong>Query:</strong> {{ truncateQuery(dataQualityCheck.query) }}</p>
          <p class="card-text text-start"><strong>Warning Threshold:</strong> {{ dataQualityCheck.warningThreshold }}</p>
          <p class="card-text text-start"><strong>Error Threshold:</strong> {{ dataQualityCheck.errorThreshold }}</p>
          <p class="card-text text-start"><strong>Epsilon Budget:</strong> {{ dataQualityCheck.epsilonBudget.toFixed(2) }}</p>
          <div class="card-actions mt-2">
            <button class="btn btn-sm btn-primary me-2" @click="openEditModal(dataQualityCheck)" title="Edit">
              <i class="bi bi-pencil"></i>
            </button>
            <button class="btn btn-sm btn-danger" @click="confirmDelete(dataQualityCheck.id)" title="Delete">
              <i class="bi bi-trash"></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit/Create Modal -->
    <div class="modal fade" id="checkModal" tabindex="-1" aria-labelledby="checkModalLabel" aria-hidden="true">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title" id="checkModalLabel">{{ isEditing ? 'Edit Check' : 'Add Check' }}</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label for="checkName" class="form-label">Name</label>
              <input v-model="currentCheck.name" class="form-control" id="checkName" placeholder="Enter dataQualityCheck name" />
            </div>
            <div class="mb-3">
              <label for="checkDescription" class="form-label">Description</label>
              <input v-model="currentCheck.description" class="form-control" id="checkDescription" placeholder="Enter description" />
            </div>
            <div class="mb-3">
              <label for="checkQuery" class="form-label">CQL Query</label>
              <textarea v-model="currentCheck.query" class="form-control" id="checkQuery" rows="4" placeholder="Enter CQL query"></textarea>
            </div>
            <div class="mb-3">
              <label for="checkWarningThreshold" class="form-label">Warning Threshold</label>
              <input v-model.number="currentCheck.warningThreshold" type="number" class="form-control" id="checkWarningThreshold" placeholder="Enter warning threshold" />
            </div>
            <div class="mb-3">
              <label for="checkErrorThreshold" class="form-label">Error Threshold</label>
              <input v-model.number="currentCheck.errorThreshold" type="number" class="form-control" id="checkErrorThreshold" placeholder="Enter error threshold" />
            </div>
            <div class="mb-3">
              <label for="checkEpsilonBudget" class="form-label">Epsilon Budget</label>
              <input v-model.number="currentCheck.epsilonBudget" type="number" step="0.1" class="form-control" id="checkEpsilonBudget" placeholder="Enter epsilon budget" />
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            <button type="button" class="btn btn-primary" @click="saveCheck">{{ isEditing ? 'Update' : 'Create' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div class="modal fade" id="deleteModal" tabindex="-1" aria-labelledby="deleteModalLabel" aria-hidden="true">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title" id="deleteModalLabel">Confirm Deletion</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            Are you sure you want to delete this dataQualityCheck?
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            <button type="button" class="btn btn-danger" @click="deleteCheck">Delete</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import axios from 'axios';
import * as bootstrap from 'bootstrap';

const qualityChecks = ref([]);
const currentCheck = reactive({
  id: null,
  name: '',
  description: '',
  query: '',
  warningThreshold: 10,
  errorThreshold: 30,
  epsilonBudget: 1.0,
});
const isEditing = ref(false);
const deleteId = ref(null);

const url = '/api/cQLChecks';

const fetchChecks = async () => {
  try {
    const { data } = await axios.get(url);
    qualityChecks.value = data._embedded?.cqlChecks || [];
  } catch (error) {
    console.error('Error fetching qualityChecks:', error);
    qualityChecks.value = [];
  }
};

const openAddModal = () => {
  isEditing.value = false;
  Object.assign(currentCheck, { id: null, name: '', description: '', query: '', warningThreshold: 10, errorThreshold: 30, epsilonBudget: 1.0 });
  new bootstrap.Modal(document.getElementById('checkModal')).show();
};

const openEditModal = (dataQualityCheck) => {
  isEditing.value = true;
  Object.assign(currentCheck, { ...dataQualityCheck });
  new bootstrap.Modal(document.getElementById('checkModal')).show();
};

const saveCheck = async () => {
  try {
    if (isEditing.value) {
      await axios.put(`${url}/${currentCheck.id}`, currentCheck);
    } else {
      await axios.post(url, currentCheck);
    }
    await fetchChecks();
    bootstrap.Modal.getInstance(document.getElementById('checkModal')).hide();
  } catch (error) {
    console.error(`Error ${isEditing.value ? 'updating' : 'adding'} dataQualityCheck:`, error);
  }
};

const confirmDelete = (id) => {
  deleteId.value = id;
  new bootstrap.Modal(document.getElementById('deleteModal')).show();
};

const deleteCheck = async () => {
  try {
    await axios.delete(`${url}/${deleteId.value}`);
    await fetchChecks();
    bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
  } catch (error) {
    console.error('Error deleting dataQualityCheck:', error);
  }
};

const getCheckResult = (dataQualityCheck) => {
  // Mock result based on CQLCheck.execute (replace with actual API call if available)
  return Math.random() * 2;
};

const isEpsilonOverBudget = (dataQualityCheck) => {
  return getCheckResult(dataQualityCheck) > dataQualityCheck.epsilonBudget;
};

const truncateQuery = (query) => {
  const maxLength = 50;
  return query.length > maxLength ? `${query.slice(0, maxLength)}...` : query;
};

onMounted(fetchChecks);
</script>

<style scoped>
.card {
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  margin-left: auto;
  margin-right: auto;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}
.card:hover {
  transform: translateY(-2px);
}
.card-body {
  padding: 0.75rem;
}
.card-title {
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
}
.card-text {
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}
.card-actions {
  margin-top: 0.5rem;
}
.warning {
  background-color: #fef2f2;
  color: #b91c1c;
  padding: 0.5rem;
  border: 1px solid #b91c1c;
  border-radius: 0.25rem;
  display: flex;
  align-items: center;
  font-size: 0.875rem;
  margin-top: 0.5rem;
}
.warning-icon {
  margin-right: 0.5rem;
  font-size: 1rem;
}
.btn-sm {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
}
</style>