import { ref, computed } from 'vue'
import { CheckStatus } from '../utils/qualityCheckUtils.js'

export function useSiteStats(reports, qualityCheckMap, agents) {
  const selectedCategory = ref(null)
  const selectedGroup = ref(null)

  // Helper function for status calculation
  const getResultStatus = (resultValue, qualityCheck) => {
    const percentage = resultValue <= 1 ? resultValue * 100 : resultValue

    if (percentage > qualityCheck.errorThreshold) {
      return CheckStatus.FAILED
    } else if (percentage > qualityCheck.warningThreshold) {
      return CheckStatus.WARNING
    }
    return CheckStatus.PASSED
  }

  // Get unique categories from quality checks
  const categories = computed(() => {
    const cats = new Set()
    if (qualityCheckMap.value) {
      qualityCheckMap.value.forEach(check => {
        if (check.category && check.category.name) {
          cats.add(check.category.name)
        } else {
          cats.add('No Category')
        }
      })
    }
    return Array.from(cats).sort()
  })

  // Get unique groups from agents
  const groups = computed(() => {
    const groupSet = new Set()
    if (agents.value) {
      agents.value.forEach(agent => {
        if (agent.groups && Array.isArray(agent.groups)) {
          agent.groups.forEach(group => {
            if (group && group.name) {
              groupSet.add(group.name)
            }
          })
        }
      })
    }
    return Array.from(groupSet).sort()
  })

  // Filter agents based on selected group
  const filteredAgents = computed(() => {
    if (!agents.value) return []
    if (!selectedGroup.value) {
      return agents.value
    }

    return agents.value.filter(agent => {
      if (!agent.groups || !Array.isArray(agent.groups)) {
        return false
      }
      return agent.groups.some(group => group && group.name === selectedGroup.value)
    })
  })

  // Total number of quality checks (filtered by category if selected)
  const totalChecks = computed(() => {
    if (!qualityCheckMap.value) return 0
    if (!selectedCategory.value) {
      return qualityCheckMap.value.size
    }

    let count = 0
    qualityCheckMap.value.forEach(check => {
      const categoryName = check.category && check.category.name ? check.category.name : 'No Category'
      if (categoryName === selectedCategory.value) {
        count++
      }
    })
    return count
  })

  // Helper to calculate sites with specific status
  const calculateSitesWithStatus = (targetStatus) => {
    if (!reports.value || !qualityCheckMap.value || !filteredAgents.value) return 0

    const sitesSet = new Set()
    const filteredAgentIds = new Set(filteredAgents.value.map(agent => agent.id))

    // Group reports by agent and get the latest report for each
    const latestReportsByAgent = new Map()
    reports.value.forEach(report => {
      const reportAgentId = report.agentId || report.agent?.id
      if (!reportAgentId || !filteredAgentIds.has(reportAgentId)) return

      const existing = latestReportsByAgent.get(reportAgentId)
      if (!existing || new Date(report.timestamp) > new Date(existing.timestamp)) {
        latestReportsByAgent.set(reportAgentId, report)
      }
    })

    // Check only the latest report for each agent
    latestReportsByAgent.forEach((report, agentId) => {
      if (!report.results) return

      report.results.forEach(result => {
        const qualityCheck = qualityCheckMap.value.get(result.hash)
        if (!qualityCheck) return

        // Filter by category if one is selected
        if (selectedCategory.value) {
          const categoryName = qualityCheck.category && qualityCheck.category.name
            ? qualityCheck.category.name
            : 'No Category'
          if (categoryName !== selectedCategory.value) return
        }

        const raw = result.result
        if (typeof raw !== 'number' || isNaN(raw)) return

        const status = getResultStatus(raw, qualityCheck)
        if (status === targetStatus) {
          sitesSet.add(agentId)
        }
      })
    })

    return sitesSet.size
  }

  // Calculate sites with at least one error (filtered by group and category)
  const sitesWithErrors = computed(() => calculateSitesWithStatus(CheckStatus.FAILED))

  // Calculate sites with at least one warning (filtered by group and category)
  const sitesWithWarnings = computed(() => calculateSitesWithStatus(CheckStatus.WARNING))

  return {
    selectedCategory,
    selectedGroup,
    categories,
    groups,
    filteredAgents,
    totalChecks,
    sitesWithErrors,
    sitesWithWarnings
  }
}
