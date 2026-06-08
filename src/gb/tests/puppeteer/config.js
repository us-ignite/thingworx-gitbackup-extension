const path = require('path');
const fs = require('fs');

require('dotenv').config({ path: path.join(__dirname, '.env') });

// Check for a shared Thing name written by global setup
const THING_NAME_FILE = path.join(__dirname, '.thing-name');
let sharedThingName = null;
try {
  if (fs.existsSync(THING_NAME_FILE)) {
    sharedThingName = fs.readFileSync(THING_NAME_FILE, 'utf8').trim();
  }
} catch {
  // fall back to env/default
}

const config = {
  // ThingWorx
  twxUrl: process.env.TWX_URL || 'http://localhost:8080/Thingworx',
  twxUser: process.env.TWX_USER || 'Administrator',
  twxPass: process.env.TWX_PASS || 'TwxAdm1nP@ssw0rd!',

  // Gitea
  giteaApiUrl: process.env.GITEA_API_URL || 'http://localhost:3000/api/v1',
  giteaUser: process.env.GITEA_USER || 'testadmin',
  giteaPass: process.env.GITEA_PASS || 'testadmin123',
  giteaRepoUrl: process.env.GITEA_REPO_URL || 'http://gitea:3000/testadmin/gitbackup-test.git',
  giteaRepoName: process.env.GITEA_REPO_NAME || 'gitbackup-test',

  // Test Fixtures
  testFileRepoName: process.env.TEST_FILE_REPO || 'GitBackupTestRepository',
  testThingName: sharedThingName || process.env.TEST_THING_NAME || 'PuppeteerTestRepo',
  testRepoPath: process.env.TEST_REPO_PATH || 'PuppeteerGitBackup',

  // Puppeteer
  headless: process.env.PUPPETEER_HEADLESS !== 'false',
  slowMo: parseInt(process.env.PUPPETEER_SLOW_MO || '0', 10),
  viewportWidth: 1440,
  viewportHeight: 900,
  defaultTimeout: parseInt(process.env.TIMEOUT || '30000', 10),
  navigationTimeout: parseInt(process.env.NAV_TIMEOUT || '60000', 10),
  serviceTimeout: parseInt(process.env.SERVICE_TIMEOUT || '45000', 10),

  // Paths
  screenshotDir: require('path').join(__dirname, 'screenshots'),
};

module.exports = config;
