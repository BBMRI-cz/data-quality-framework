/**
 * Fetches the latest version from GitHub releases
 * Falls back to git tags if GitHub API is unavailable
 */
import { execSync } from 'child_process';

const GITHUB_REPO = 'bbmri-cz/data-quality-framework';
const GITHUB_API_URL = `https://api.github.com/repos/${GITHUB_REPO}/releases/latest`;

/**
 * Get version from git tags as fallback
 */
function getVersionFromGit() {
    try {
        const version = execSync('git tag --sort=-v:refname | head -1', {
            encoding: 'utf8',
            stdio: ['pipe', 'pipe', 'pipe']
        }).trim();
        return version || null;
    } catch (error) {
        console.warn('Could not get version from git, version will be hidden');
        return null;
    }
}

/**
 * Get version from GitHub API
 */
async function getVersionFromGitHub() {
    try {
        const response = await fetch(GITHUB_API_URL, {
            headers: {
                'Accept': 'application/vnd.github+json',
                'User-Agent': 'data-quality-framework-docs'
            }
        });

        if (!response.ok) {
            console.warn(`GitHub API returned ${response.status}, falling back to git`);
            return getVersionFromGit();
        }

        const data = await response.json();
        return data.tag_name || getVersionFromGit();
    } catch (error) {
        console.warn('Failed to fetch from GitHub API, falling back to git:', error.message);
        return getVersionFromGit();
    }
}

/**
 * Get the latest version (tries GitHub API first, then git)
 */
export async function getLatestVersion() {
    const version = await getVersionFromGitHub();
    console.log(`Using version: ${version}`);
    return version;
}

/**
 * Synchronous version for use in config (uses git tags)
 */
export function getLatestVersionSync() {
    return getVersionFromGit();
}

