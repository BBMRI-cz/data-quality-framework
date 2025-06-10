<template>
  <div class="card">
    <div class="card-header">
      <h2 class="mb-0">CQL Checks</h2>
    </div>
    <div class="card-body">
      <table class="table table-bordered table-sm">
        <thead class="table-light">
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Description</th>
          <th>Query</th>
          <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="check in checks" :key="check.id">
          <td>{{ check.id }}</td>
          <td>
            <input
                v-model="check.name"
                class="form-control form-control-sm"
                placeholder="Enter check name"
            />
          </td>
          <td>
            <input
                v-model="check.description"
                class="form-control form-control-sm"
                placeholder="Enter description"
            />
          </td>
          <td>
              <textarea
                  v-model="check.query"
                  class="form-control form-control-sm"
                  rows="2"
                  placeholder="Enter CQL query"
              />
          </td>
          <td>
            <button
                @click="updateCheck(check)"
                class="btn btn-sm btn-primary me-2"
                title="Save"
            >
              💾
            </button>
            <button
                @click="deleteCheck(check.id)"
                class="btn btn-sm btn-danger"
                title="Delete"
            >
              🗑️
            </button>
          </td>
        </tr>
        <tr>
          <td></td>
          <td>
            <input
                v-model="newCheck.name"
                class="form-control form-control-sm"
                placeholder="New check name"
            />
          </td>
          <td>
            <input
                v-model="newCheck.description"
                class="form-control form-control-sm"
                placeholder="New description"
            />
          </td>
          <td>
              <textarea
                  v-model="newCheck.query"
                  class="form-control form-control-sm"
                  rows="2"
                  placeholder="New CQL query"
              />
          </td>
          <td>
            <button
                @click="addCheck"
                class="btn btn-sm btn-success"
                title="Add Check"
            >
              ➕
            </button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import axios from 'axios'

const checks = ref([])
const newCheck = reactive({
  name: '',
  description: '',
  query: ''
})

const fetchChecks = async () => {
  try {
    const { data } = await axios.get('/api/cQLChecks')
    checks.value = data._embedded?.cqlChecks || []
  } catch (error) {
    console.error('Error fetching checks:', error)
    checks.value = []
  }
}

const addCheck = async () => {
  try {
    await axios.post('/api/cQLChecks', newCheck)
    newCheck.name = ''
    newCheck.description = ''
    newCheck.query = ''
    await fetchChecks()
  } catch (error) {
    console.error('Error adding check:', error)
  }
}

const updateCheck = async (check) => {
  try {
    await axios.put(`/api/cQLChecks/${check.id}`, check)
  } catch (error) {
    console.error('Error updating check:', error)
  }
}

const deleteCheck = async (id) => {
  try {
    await axios.delete(`/api/cQLChecks/${id}`)
    await fetchChecks()
  } catch (error) {
    console.error('Error deleting check:', error)
  }
}

onMounted(fetchChecks)
</script>