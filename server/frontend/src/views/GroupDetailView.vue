<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          :title="isNew ? 'New Group' : (group?.name || 'Unnamed Group')"
          :subtitle="isNew ? 'Create a new group' : 'Group Details'"
          icon="bi bi-collection-fill"
        />

        <!-- Back Button and Actions -->
        <div class="mb-4 d-flex gap-2 align-items-center">
          <button class="btn btn-outline-secondary btn-sm" @click="goBack">
            <i class="bi bi-arrow-left me-2"></i>Back to Groups
          </button>
          <button
            v-if="!isNew"
            @click="showDeleteModal = true"
            class="btn btn-outline-danger btn-sm d-flex align-items-center"
            :disabled="saving"
          >
            <i class="bi bi-trash me-2"></i>
            Delete Group
          </button>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading group...</span>
          </div>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading Group</h6>
          <p class="mb-0">{{ error }}</p>
        </div>

        <!-- Detail View -->
        <div v-else-if="group || isNew">
          <!-- Edit Form Card -->
          <div class="card border-0 shadow-sm mb-4">
            <div class="card-header bg-white border-bottom py-3">
              <h5 class="mb-0 fw-semibold">
                <i class="bi bi-pencil text-primary me-2"></i>
                {{ isNew ? 'Create Group' : 'Edit Group' }}
              </h5>
            </div>
            <div class="card-body p-4">
              <div class="row g-4">
                <!-- Name Field -->
                <div class="col-12">
                  <label class="form-label fw-semibold">Name</label>
                  <input
                    v-model="editForm.name"
                    type="text"
                    class="form-control"
                    :class="{ 'is-invalid': validationErrors.name }"
                    placeholder="Enter group name"
                  >
                  <div v-if="validationErrors.name" class="invalid-feedback">
                    {{ validationErrors.name }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Assign Agents Card (only for existing groups) -->
          <div v-if="!isNew" class="card border-0 shadow-sm mb-4">
            <div class="card-header bg-white border-bottom py-3">
              <div class="d-flex justify-content-between align-items-center">
                <h5 class="mb-0 fw-semibold">
                  <i class="bi bi-people-fill text-primary me-2"></i>
                  Assign Agents
                </h5>
                <span class="badge bg-primary-soft text-primary px-3 py-2">
                  {{ editForm.agentIds.length }} / {{ filteredAgents.length }} selected
                </span>
              </div>
            </div>
            <div class="card-body p-4">
              <!-- Agents Loading -->
              <div v-if="loadingAgents" class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">Loading agents...</span>
                </div>
                <p class="text-muted mt-3 mb-0">Loading available agents...</p>
              </div>

              <!-- Agents List -->
              <div v-else>
                <!-- Search and Actions Bar -->
                <div class="agent-controls mb-4">
                  <div class="row g-3">
                    <div class="col-md-8">
                      <div class="search-box">
                        <i class="bi bi-search"></i>
                        <input
                          v-model="agentSearchQuery"
                          type="text"
                          class="form-control"
                          placeholder="Search agents by name or ID..."
                        >
                        <button
                          v-if="agentSearchQuery"
                          @click="agentSearchQuery = ''"
                          class="btn-clear"
                          type="button"
                        >
                          <i class="bi bi-x-circle-fill"></i>
                        </button>
                      </div>
                    </div>
                    <div class="col-md-4">
                      <div class="d-flex gap-2">
                        <button
                          @click="selectAllAgents"
                          class="btn btn-outline-primary btn-sm flex-fill"
                          type="button"
                          :disabled="filteredAgents.length === 0"
                        >
                          <i class="bi bi-check-all me-1"></i>
                          Select All
                        </button>
                        <button
                          @click="deselectAllAgents"
                          class="btn btn-outline-secondary btn-sm flex-fill"
                          type="button"
                          :disabled="editForm.agentIds.length === 0"
                        >
                          <i class="bi bi-x me-1"></i>
                          Clear
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- No Agents State -->
                <div v-if="availableAgents.length === 0" class="empty-agents-state">
                  <i class="bi bi-inbox"></i>
                  <h6>No Agents Available</h6>
                  <p class="text-muted mb-0">There are no agents registered in the system yet.</p>
                </div>

                <!-- No Results State -->
                <div v-else-if="filteredAgents.length === 0" class="empty-agents-state">
                  <i class="bi bi-search"></i>
                  <h6>No Results Found</h6>
                  <p class="text-muted mb-0">Try adjusting your search criteria</p>
                </div>

                <!-- Agents Grid -->
                <div v-else class="agent-selection-grid">
                  <div
                    v-for="agent in filteredAgents"
                    :key="agent.id"
                    class="agent-card"
                    :class="{ 'selected': editForm.agentIds.includes(agent.id) }"
                    @click="toggleAgent(agent.id)"
                  >
                    <div class="agent-checkbox-wrapper">
                      <input
                        class="form-check-input"
                        type="checkbox"
                        :id="`agent-${agent.id}`"
                        :value="agent.id"
                        v-model="editForm.agentIds"
                        @click.stop
                      >
                    </div>
                    <div class="agent-info">
                      <div class="agent-name">
                        <i class="bi bi-database-gear me-1"></i>
                        {{ agent.name || 'Unnamed Agent' }}
                      </div>
                      <div class="agent-id">{{ agent.id }}</div>
                      <div v-if="agent.status" class="agent-status mt-2">
                        <span
                          class="status-badge"
                          :class="{
                            'status-active': agent.status === 'ACTIVE',
                            'status-inactive': agent.status === 'INACTIVE',
                            'status-pending': agent.status === 'PENDING'
                          }"
                        >
                          {{ agent.status }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="action-buttons d-flex gap-3 justify-content-center">
            <button
              @click="saveGroup"
              class="btn btn-action btn-save"
              :disabled="saving || (!isNew && !hasChanges)"
            >
              <span v-if="saving" class="spinner-border spinner-border-sm me-2" role="status"></span>
              <i v-else class="bi bi-check-lg me-2"></i>
              {{ saving ? 'Saving...' : (isNew ? 'Create Group' : 'Save Changes') }}
            </button>
            <button
              @click="resetForm"
              class="btn btn-action btn-reset"
              :disabled="saving || (!isNew && !hasChanges)"
            >
              <i class="bi bi-x-circle me-2"></i>
              Reset
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <DeleteConfirmModal
      :show="showDeleteModal"
      title="Delete Group"
      :message="`Are you sure you want to delete the group '${group?.name}'?`"
      warning="This will remove the group but will not delete the agents."
      confirm-text="Delete Group"
      :loading="deleting"
      @close="showDeleteModal = false"
      @confirm="deleteGroup"
    />
  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted, watch} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiService } from '../services/apiService.js'
import { notificationService } from '../services/notificationService.js'
import PageHeader from '../components/PageHeader.vue'
import DeleteConfirmModal from '../components/DeleteConfirmModal.vue'

const route = useRoute()
const router = useRouter()

const isNew = computed(() => route.path === '/groups/new')
const groupId = ref(route.params.id)
const group = ref(null)
const loading = ref(!isNew.value)
const saving = ref(false)
const deleting = ref(false)
const error = ref(null)
const showDeleteModal = ref(false)
const loadingAgents = ref(false)
const availableAgents = ref([])
const agentSearchQuery = ref('')

const editForm = reactive({
  name: '',
  agentIds: []
})

const validationErrors = reactive({
  name: ''
})

const filteredAgents = computed(() => {
  if (!agentSearchQuery.value) {
    return availableAgents.value
  }

  const query = agentSearchQuery.value.toLowerCase()
  return availableAgents.value.filter(agent => {
    const name = (agent.name || '').toLowerCase()
    const id = (agent.id || '').toLowerCase()
    return name.includes(query) || id.includes(query)
  })
})

const hasChanges = computed(() => {
  if (isNew.value) return true
  if (!group.value) return false

  const nameChanged = editForm.name !== group.value.name
  const agentsChanged = JSON.stringify([...editForm.agentIds].sort()) !==
                        JSON.stringify([...(group.value.agentIds || [])].sort())

  return nameChanged || agentsChanged
})

const loadGroup = async () => {
  if (isNew.value) {
    loading.value = false
    return
  }

  loading.value = true
  error.value = null

  try {
    group.value = await apiService.getGroup(groupId.value)

    // Initialize form (create copies to avoid reference issues)
    editForm.name = group.value.name || ''
    editForm.agentIds = [...(group.value.agentIds || [])]
  } catch (err) {
    error.value = err.message || 'Failed to load group'
    console.error('Error loading group:', err)
  } finally {
    loading.value = false
  }
}

const loadAgents = async () => {
  loadingAgents.value = true
  try {
    const data = await apiService.getAgents()
    // Handle HAL format response
    if (data._embedded && data._embedded.agents) {
      availableAgents.value = data._embedded.agents
    } else if (Array.isArray(data)) {
      availableAgents.value = data
    } else {
      availableAgents.value = []
    }
  } catch (err) {
    console.error('Error loading agents:', err)
    notificationService.error('Load Failed', 'Failed to load available agents')
  } finally {
    loadingAgents.value = false
  }
}

const validate = () => {
  validationErrors.name = ''

  let isValid = true

  if (!editForm.name.trim()) {
    validationErrors.name = 'Name is required'
    isValid = false
  }

  return isValid
}

const saveGroup = async () => {
  if (!validate()) return

  saving.value = true
  error.value = null

  try {
    if (isNew.value) {
      const created = await apiService.createGroup({ name: editForm.name })
      notificationService.success('Group Created', 'New group has been created successfully')

      // Set the group data and update groupId before navigation
      group.value = created
      groupId.value = created.id
      editForm.name = created.name || ''
      editForm.agentIds = created.agentIds || []

      // Navigate to the edit page
      await router.push(`/groups/${created.id}`)

      // Load agents for assignment after creation
      await loadAgents()
    } else {
      // Update group name
      await apiService.updateGroup(groupId.value, { name: editForm.name })

      // Update agent assignments
      await apiService.assignAgentsToGroup(groupId.value, editForm.agentIds)

      notificationService.success('Group Updated', 'Your changes have been saved successfully')
      await loadGroup()
    }
  } catch (err) {
    error.value = err.message || 'Failed to save group'
    console.error('Error saving group:', err)
    notificationService.error('Save Failed', error.value)
  } finally {
    saving.value = false
  }
}

const deleteGroup = async () => {
  deleting.value = true
  try {
    await apiService.deleteGroup(groupId.value)
    notificationService.success('Group Deleted', 'Group has been successfully deleted')
    router.push('/groups')
  } catch (err) {
    console.error('Error deleting group:', err)
    notificationService.error('Delete Failed', err.message || 'Failed to delete group')
    showDeleteModal.value = false
  } finally {
    deleting.value = false
  }
}

const resetForm = () => {
  if (isNew.value) {
    editForm.name = ''
    editForm.agentIds = []
  } else {
    editForm.name = group.value.name || ''
    editForm.agentIds = [...(group.value.agentIds || [])]
  }

  validationErrors.name = ''

  notificationService.info('Form Reset', 'Changes have been discarded')
}

const goBack = () => {
  router.push('/groups')
}

const toggleAgent = (agentId) => {
  const index = editForm.agentIds.indexOf(agentId)
  if (index > -1) {
    editForm.agentIds.splice(index, 1)
  } else {
    editForm.agentIds.push(agentId)
  }
}

const selectAllAgents = () => {
  editForm.agentIds = filteredAgents.value.map(agent => agent.id)
  notificationService.info('All Selected', `${editForm.agentIds.length} agents selected`)
}

const deselectAllAgents = () => {
  editForm.agentIds = []
  notificationService.info('Cleared', 'All agents deselected')
}

onMounted(async () => {
  await loadGroup()
  if (!isNew.value) {
    await loadAgents()
  }
})

// Watch for route changes when navigating between different groups
watch(() => route.params.id, async (newId) => {
  if (newId && newId !== groupId.value) {
    groupId.value = newId
    await loadGroup()
    await loadAgents()
  }
})
</script>

<style scoped>
/* Loading State */
.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 4rem 0;
}

/* Cards */
.card {
  border-radius: 12px;
  overflow: hidden;
}

.card-header {
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
}

/* Form Controls */
.form-control:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.15);
}

/* Badge Styles */
.bg-primary-soft {
  background-color: rgba(102, 126, 234, 0.15);
}

/* Agent Controls */
.agent-controls {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.search-box {
  position: relative;
}

.search-box i.bi-search {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #6c757d;
  font-size: 0.9rem;
  pointer-events: none;
}

.search-box .form-control {
  padding-left: 2.5rem;
  padding-right: 2.5rem;
}

.search-box .btn-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #6c757d;
  padding: 0.25rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.search-box .btn-clear:hover {
  color: #dc3545;
}

.search-box .btn-clear i {
  font-size: 1rem;
}

/* Empty States */
.empty-agents-state {
  text-align: center;
  padding: 3rem 1rem;
  color: #6c757d;
}

.empty-agents-state i {
  font-size: 3rem;
  color: #dee2e6;
  margin-bottom: 1rem;
}

.empty-agents-state h6 {
  font-weight: 600;
  color: #495057;
  margin-bottom: 0.5rem;
}

/* Agent Selection Grid */
.agent-selection-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

.agent-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  border: 2px solid #e9ecef;
  border-radius: 10px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.agent-card:hover {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.03);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.agent-card.selected {
  border-color: #667eea;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

.agent-card.selected::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px 10px 0 0;
}

.agent-checkbox-wrapper {
  flex-shrink: 0;
  padding-top: 0.125rem;
}

.agent-card .form-check-input {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
  border: 2px solid #dee2e6;
}

.agent-card .form-check-input:checked {
  background-color: #667eea;
  border-color: #667eea;
  box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25);
}

.agent-info {
  flex: 1;
  min-width: 0;
}

.agent-name {
  font-weight: 600;
  font-size: 0.95rem;
  color: #212529;
  margin-bottom: 0.25rem;
  display: flex;
  align-items: center;
  word-break: break-word;
}

.agent-name i {
  color: #667eea;
  flex-shrink: 0;
}

.agent-id {
  font-size: 0.8rem;
  color: #6c757d;
  font-family: 'Courier New', monospace;
  word-break: break-all;
}

.agent-status {
  display: flex;
  align-items: center;
}

.status-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-active {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-inactive {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.status-pending {
  background-color: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

/* Action Buttons */
.action-buttons {
  padding: 1rem 0;
}

.btn-action {
  border: none;
  padding: 0.75rem 2rem;
  border-radius: 0.5rem;
  font-weight: 600;
  font-size: 0.95rem;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 160px;
}

.btn-save {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-save:hover:not(:disabled) {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  color: white;
}

.btn-save:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
}

.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-reset {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  color: #495057;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.btn-reset:hover:not(:disabled) {
  background: linear-gradient(135deg, #e9ecef 0%, #f8f9fa 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  color: #495057;
}

.btn-reset:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.btn-reset:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-action i {
  font-size: 1.1rem;
}

/* Responsive */
@media (max-width: 768px) {
  .card-body {
    padding: 1.25rem;
  }

  .action-buttons {
    flex-direction: column;
  }

  .btn-action {
    width: 100%;
    min-width: auto;
  }

  .agent-selection-grid {
    grid-template-columns: 1fr;
  }
}
</style>

