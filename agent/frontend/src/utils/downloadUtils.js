/**
 * Saves a Blob as a file by clicking a temporary download link
 * @param {Blob} blob - File contents
 * @param {string} filename - Suggested file name
 */
export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
