/**
 * Report service
 * Handles report API calls
 */
import { api } from '@/api';

const BASE_URL = '/api/reports';

/**
 * Fetches all reports
 * @returns {Promise<Array>}
 */
export async function getAll() {
  const { data } = await api.get(BASE_URL);
  return data._embedded?.reports || [];
}

/**
 * Fetches a report by ID
 * @param {string|number} id
 * @returns {Promise<object>}
 */
export async function get(id) {
  const { data } = await api.get(`${BASE_URL}/${id}`);
  return data;
}

/**
 * Generates a new report
 * @returns {Promise<object>}
 */
export async function generate() {
  const { data } = await api.post(BASE_URL, {});
  return data;
}

/**
 * Extracts the relative path from a URL (handles both absolute and relative URLs)
 * @param {string} url
 * @returns {string}
 */
function toRelativePath(url) {
  try {
    const urlObj = new URL(url);
    return urlObj.pathname + urlObj.search;
  } catch {
    // Already a relative path
    return url;
  }
}

/**
 * Polls a report until it's no longer generating
 * @param {string} reportUrl
 * @returns {Promise<object>}
 */
export async function pollUntilComplete(reportUrl) {
  const relativePath = toRelativePath(reportUrl);
  let report = await api.get(relativePath).then((res) => res.data);

  while (report.status === 'GENERATING') {
    await new Promise((resolve) => setTimeout(resolve, 2000));
    report = await api.get(relativePath).then((res) => res.data);
  }

  return report;
}

export const reportService = {
  getAll,
  get,
  generate,
  pollUntilComplete,
};
