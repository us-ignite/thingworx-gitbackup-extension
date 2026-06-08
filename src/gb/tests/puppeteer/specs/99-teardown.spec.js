const { restPost, restGet } = require('../utils/auth');
const config = require('../config');

describe('99 — Teardown: Cleanup Test Resources', () => {

  test('99.1 Delete test GitBackup Thing', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/DeteleGitThing', {
      GitThingName: config.testThingName,
    });
    console.log(`  Delete ${config.testThingName}: ${res.status}`);

    // Verify it's gone
    const checkRes = await restGet(`/Things/${config.testThingName}`);
    if (checkRes.status === 404) {
      console.log('  Test Thing successfully removed.');
    }
  });

  test('99.2 Delete test FileRepository', async () => {
    const res = await restPost(`/Things/${config.testFileRepoName}/Services/Delete`, {});
    console.log(`  Delete FileRepository ${config.testFileRepoName}: ${res.status}`);
  });

  test('99.3 Reset user extension properties', async () => {
    const res = await restPost('/Things/GIT.Utility.Thing/Services/SetGitUserExtensionsProperties', {
      GitCommitterName: '',
      GitCommitterEmail: '',
      UseGitCommitUserValues: false,
    });
    console.log(`  Reset user extension properties: ${res.status}`);
  });

  test('99.4 Summary', async () => {
    console.log('  === Puppeteer UI Test Suite Complete ===');
    console.log(`  Test Thing: ${config.testThingName}`);
    console.log(`  FileRepository: ${config.testFileRepoName}`);
    console.log(`  Gitea Repo: ${config.giteaRepoUrl}`);
  });

});
