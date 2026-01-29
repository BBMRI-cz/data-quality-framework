import { ref, computed } from 'vue'

export function usePatientStats(reports, qualityCheckMap, agents) {
  const selectedCategory = ref(null)
  const selectedGroup = ref(null)

  /**
   * Get unique categories from quality checks
   */
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

  /**
   * Get unique groups from agents
   */
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

  /**
   * Get the most recent report per agent that has totalPatients and totalSamples data
   */
  const mostRecentReportsWithData = computed(() => {
    const reportsByAgent = new Map()

    if (reports.value) {
      reports.value.forEach(report => {
        // Only include reports that have totalPatients and totalSamples defined (not null/undefined/0)
        if (report.totalPatients != null && report.totalSamples != null) {
          const agentId = report.agentId
          const existing = reportsByAgent.get(agentId)

          if (!existing || new Date(report.timestamp) > new Date(existing.timestamp)) {
            reportsByAgent.set(agentId, report)
          }
        }
      })
    }

    return Array.from(reportsByAgent.values())
  })

  /**
   * Filter reports by selected group and category
   */
  const filteredReports = computed(() => {
    let filtered = mostRecentReportsWithData.value

    // Filter by group if selected
    if (selectedGroup.value) {
      filtered = filtered.filter(report => {
        const agent = agents.value.find(a => a.id === report.agentId)
        if (!agent || !agent.groups || !Array.isArray(agent.groups)) {
          return false
        }
        return agent.groups.some(group => group && group.name === selectedGroup.value)
      })
    }

    // Filter by category if selected - only include reports that have at least one check in the selected category
    if (selectedCategory.value) {
      filtered = filtered.filter(report => {
        if (!report.results) return false

        return report.results.some(result => {
          const qualityCheck = qualityCheckMap.value.get(result.hash)
          if (!qualityCheck) return false

          const categoryName = qualityCheck.category?.name || 'No Category'
          return categoryName === selectedCategory.value
        })
      })
    }

    return filtered
  })

  /**
   * Total number of patients across all sites (filtered by group)
   */
  const totalPatients = computed(() => {
    return filteredReports.value.reduce((sum, report) => {
      return sum + (report.totalPatients || 0)
    }, 0)
  })

  /**
   * Total number of samples across all sites (filtered by group)
   */
  const totalSamples = computed(() => {
    return filteredReports.value.reduce((sum, report) => {
      return sum + (report.totalSamples || 0)
    }, 0)
  })

  /**
   * Number of sites (agents) that have submitted reports with patient data (filtered by group)
   */
  const fromSites = computed(() => {
    return filteredReports.value.length
  })

  /**
   * Aggregate quality check results across all sites
   * For each quality check, calculate the total number of patients meeting the criteria
   * Result is (0..1) representing percentage, so we multiply by totalPatients from that site
   */
  const aggregatedCheckResults = computed(() => {
    const checkMap = new Map()

    // Process filtered reports (already filtered by group)
    filteredReports.value.forEach(report => {
      if (!report.results || !report.totalPatients) return

      report.results.forEach(result => {
        const qualityCheck = qualityCheckMap.value.get(result.hash)
        if (!qualityCheck) return

        // Filter by category if selected
        if (selectedCategory.value) {
          const categoryName = qualityCheck.category?.name || 'No Category'
          if (categoryName !== selectedCategory.value) return
        }

        // Get or create aggregated entry for this check
        if (!checkMap.has(result.hash)) {
          checkMap.set(result.hash, {
            checkHash: result.hash,
            checkName: qualityCheck.name || qualityCheck.cql || result.hash,
            category: qualityCheck.category?.name || null,
            qualityCheck: qualityCheck,
            patientsMeetingCriteria: 0
          })
        }

        const entry = checkMap.get(result.hash)

        // Result is a percentage (0..1), multiply by the site's total patients
        // to get the number of patients meeting the criteria from this site
        const resultPercentage = result.result <= 1 ? result.result : result.result / 100
        const patientsFromThisSite = Math.round(resultPercentage * report.totalPatients)

        entry.patientsMeetingCriteria += patientsFromThisSite
      })
    })

    // Convert to array and sort by status (failed first, then warning, then passed)
    return Array.from(checkMap.values()).sort((a, b) => {
      // Calculate status for each
      const getStatus = (check) => {
        if (totalPatients.value === 0) return 2 // PASSED if no data
        const percentage = (check.patientsMeetingCriteria / totalPatients.value) * 100
        if (percentage > check.qualityCheck.errorThreshold) return 0 // FAILED
        if (percentage > check.qualityCheck.warningThreshold) return 1 // WARNING
        return 2 // PASSED
      }

      const statusA = getStatus(a)
      const statusB = getStatus(b)

      if (statusA !== statusB) {
        return statusA - statusB
      }

      // Same status, sort by name
      return a.checkName.localeCompare(b.checkName)
    })
  })

  return {
    selectedCategory,
    selectedGroup,
    categories,
    groups,
    totalPatients,
    totalSamples,
    fromSites,
    aggregatedCheckResults
  }
}
