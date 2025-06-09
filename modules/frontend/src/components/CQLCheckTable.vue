<template>
  <div>
    <h2 class="mb-3">CQL Checks</h2>
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
        <td><input v-model="check.name" class="form-control form-control-sm" /></td>
        <td><input v-model="check.description" class="form-control form-control-sm" /></td>
        <td><textarea v-model="check.query" class="form-control form-control-sm" rows="2" /></td>
        <td>
          <button @click="updateCheck(check)" class="btn btn-sm btn-primary me-2">💾</button>
          <button @click="deleteCheck(check.id)" class="btn btn-sm btn-danger">🗑️</button>
        </td>
      </tr>
      <tr>
        <td></td>
        <td><input v-model="newCheck.name" class="form-control form-control-sm" placeholder="Name" /></td>
        <td><input v-model="newCheck.description" class="form-control form-control-sm" placeholder="Description" /></td>
        <td><textarea v-model="newCheck.query" class="form-control form-control-sm" rows="2" placeholder="Query" /></td>
        <td>
          <button @click="addCheck" class="btn btn-sm btn-success">➕</button>
        </td>
      </tr>
      </tbody>
    </table>
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
  const { data } = await axios.get('/api/cQLChecks')
  checks.value = data._embedded?.cqlChecks || [] // Adjust to match HAL naming
}

const addCheck = async () => {
  await axios.post('/api/cQLChecks', newCheck)
  newCheck.name = ''
  newCheck.description = ''
  newCheck.query = ''
  fetchChecks()
}

const updateCheck = async (check) => {
  await axios.put(`/api/cQLChecks/${check.id}`, check)
}

const deleteCheck = async (id) => {
  await axios.delete(`/api/cQLChecks/${id}`)
  fetchChecks()
}

onMounted(fetchChecks)
</script>
