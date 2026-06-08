const path = require('path');
const fs = require('fs');

const THING_NAME_FILE = path.join(__dirname, '.thing-name');
const giteaFixture = require('./fixtures/gitea-config.json');

module.exports = async () => {
  // Node 18 fetch hangs when HTTP_PROXY is set to empty string
  delete process.env.HTTP_PROXY;
  delete process.env.HTTPS_PROXY;
  delete process.env.http_proxy;
  delete process.env.https_proxy;
  console.log('Jest global setup: preparing test resources...');

  // Generate a unique Thing name and save it for all tests to share
  const thingName = `PuppeteerTestRepo_${Date.now()}`;
  fs.writeFileSync(THING_NAME_FILE, thingName, 'utf8');
  console.log(`  Shared Thing name: ${thingName}`);

  // Ensure Gitea test repo exists
  console.log('  Ensuring Gitea test repo exists...');
  try {
    const auth = Buffer.from('testadmin:testadmin123').toString('base64');
    const giteaRes = await fetch('http://localhost:3000/api/v1/user/repos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Basic ${auth}` },
      body: JSON.stringify({ name: 'gitbackup-test', auto_init: false, private: false }),
    });
    if (giteaRes.status === 201) {
      console.log('  Gitea test repo created.');
    } else if (giteaRes.status === 409) {
      console.log('  Gitea test repo already exists.');
    } else {
      const text = await giteaRes.text();
      console.log(`  Gitea responded ${giteaRes.status}: ${text}`);
    }
  } catch (err) {
    console.log('  Could not reach Gitea (may be starting):', err.message);
  }

  // Create the GitBackup Thing via AddNewRepo before any spec runs
  const twxUrl = process.env.TWX_URL || 'http://localhost:8080/Thingworx';
  const twxUser = process.env.TWX_USER || 'Administrator';
  const twxPass = process.env.TWX_PASS || 'TwxAdm1nP@ssw0rd!';
  const twxAuth = Buffer.from(`${twxUser}:${twxPass}`).toString('base64');

  console.log(`  Creating GitBackup Thing "${thingName}" via AddNewRepo...`);
  const addRepoBody = {
    RepoName: thingName,
    GitRepoURL: giteaFixture.gitRepoUrl,
    FileRepo: 'GitRepository',
    RepoPath: giteaFixture.repoPath,
    InitialBranch: giteaFixture.initialBranch,
    UseProxy: false,
    User: giteaFixture.user,
    Password: giteaFixture.password,
    CommitUser: giteaFixture.commitUser,
    CommitEmail: giteaFixture.commitEmail,
  };
  try {
    const res = await fetch(`${twxUrl}/Things/GIT.Utility.Thing/Services/AddNewRepo`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Basic ${twxAuth}`,
        'X-XSRF-TOKEN': 'TWX-XSRF-TOKEN-VALUE',
        'X-Requested-By': 'ThingWorx',
        Accept: 'application/json',
      },
      body: JSON.stringify(addRepoBody),
    });
    const text = await res.text();
    console.log(`  AddNewRepo REST status: ${res.status}`);
    if (res.status >= 400) {
      console.log(`  AddNewRepo error: ${text.substring(0, 300)}`);
    }
  } catch (err) {
    console.log('  Could not create GitBackup Thing:', err.message);
  }
};
