"""Exercise release preflight against isolated local Git repositories."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("release-status.sh").resolve()


class ReleaseStatusTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.remote = self.root / "remote"
        self.remote.mkdir()
        self.git("init", "-q", cwd=self.remote)
        self.git("-c", "user.name=Test", "-c", "user.email=test@example.invalid",
                 "commit", "--allow-empty", "-qm", "Initial", cwd=self.remote)
        self.repo = self.root / "checkout"
        self.git("clone", "-q", str(self.remote), str(self.repo), cwd=self.root)
        (self.repo / ".version").write_text("0.3.0\n")
        self.output = self.root / "output"
        self.summary = self.root / "summary"

    def git(self, *args, cwd=None):
        return subprocess.run(["git", *args], cwd=cwd or self.repo,
                              check=True, capture_output=True, text=True)

    def check_release(self):
        return subprocess.run(
            ["bash", str(SCRIPT), "operator", ".version"], cwd=self.repo,
            env={**os.environ, "GITHUB_OUTPUT": str(self.output),
                 "GITHUB_STEP_SUMMARY": str(self.summary)},
            capture_output=True, text=True)

    def test_new_version_can_publish(self):
        self.assertEqual(self.check_release().returncode, 0)
        self.assertEqual(self.output.read_text(), "published=false\n")
        self.assertFalse(self.summary.exists())

    def test_remote_tag_skips_even_at_another_commit(self):
        self.git("-c", "user.name=Test", "-c", "user.email=test@example.invalid",
                 "commit", "--allow-empty", "-qm", "New release", cwd=self.remote)
        self.git("-c", "user.name=Test", "-c", "user.email=test@example.invalid",
                 "tag", "-a", "operator-v0.3.0", "-m", "Release", cwd=self.remote)
        for _ in range(2):
            self.assertEqual(self.check_release().returncode, 0)
        self.assertEqual(self.output.read_text(), "published=true\npublished=true\n")
        self.assertIn("Version already published", self.summary.read_text())

    def test_remote_error_does_not_allow_publication(self):
        self.git("remote", "set-url", "origin", str(self.root / "missing"))
        self.assertNotEqual(self.check_release().returncode, 0)
        self.assertFalse(self.output.exists())


if __name__ == "__main__":
    unittest.main()
