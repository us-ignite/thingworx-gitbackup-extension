import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from 'lit';

import './components/git-about/git-about.js';
import './components/git-branch-manager/git-branch-manager.js';
import './components/git-commit-history/git-commit-history.js';
import './components/git-delete/git-delete.js';
import './components/git-export/git-export.js';
import './components/git-import/git-import.js';
import './components/git-loading/git-loading.js';
import './components/git-log/git-log.js';
import './components/git-main/git-main.js';
import './components/git-merge-rebase/git-merge-rebase.js';
import './components/git-new-repo/git-new-repo.js';
import './components/git-pull/git-pull.js';
import './components/git-push/git-push.js';
import './components/git-status/git-status.js';
import './components/git-repo-settings/git-repo-settings.js';
import './components/git-backup-extension-settings/git-backup-extension-settings.js';
import './components/git-version/git-version.js';
import './components/git-repo/git-repo.js';

const meta: Meta = {
  title: 'GitBackup',
};

export default meta;

type Story = StoryObj;

const gitThingArgTypes = {
  gitThing: { control: 'text' as const, description: 'ThingWorx GitBackup Thing name' },
};

// Keep Storybook connected to the repository created by the local dev setup.
// GitBackup.Tests.Thing is a legacy GenericThing fixture and has no Git services.
const devGitThing = 'GitBackup.DevRepo1';

export const GitAbout: Story = {
  render: () => html`<git-about></git-about>`,
};

export const GitBranchManager: Story = {
  argTypes: gitThingArgTypes,
  args: { gitThing: devGitThing },
  render: (args) => html`<git-branch-manager .gitThing=${args.gitThing}></git-branch-manager>`,
};

export const GitCommitHistory: Story = {
  argTypes: gitThingArgTypes,
  args: { gitThing: devGitThing },
  render: (args) => html`<git-commit-history .gitThing=${args.gitThing}></git-commit-history>`,
};

export const GitDelete: Story = {
  argTypes: {
    ...gitThingArgTypes,
    thingName: { control: 'text', description: 'Thing name to delete' },
  },
  args: { gitThing: devGitThing, thingName: devGitThing },
  render: (args) => html`<git-delete .gitThing=${args.gitThing} .thingName=${args.thingName}></git-delete>`,
};

export const GitExport: Story = {
  argTypes: gitThingArgTypes,
  args: { gitThing: devGitThing },
  render: (args) => html`<git-export .gitThing=${args.gitThing}></git-export>`,
};

export const GitRepoSettings: Story = {
  argTypes: gitThingArgTypes,
  args: { gitThing: devGitThing },
  render: (args) => html`<git-repo-settings .gitThing=${args.gitThing}></git-repo-settings>`,
};

export const GitBackupExtensionSettings: Story = {
  render: () => html`<git-backup-extension-settings></git-backup-extension-settings>`,
};

export const GitImport: Story = {
  argTypes: gitThingArgTypes,
  args: { gitThing: devGitThing },
  render: (args) => html`<git-import .gitThing=${args.gitThing}></git-import>`,
};

export const GitLoading: Story = {
  argTypes: {
    message: { control: 'text', description: 'Loading message' },
  },
  args: { message: 'Loading...' },
  render: (args) => html`<git-loading .message=${args.message}></git-loading>`,
};

export const GitLog: Story = {
  render: () => html`<git-log></git-log>`,
};

export const GitMain: Story = {
  render: () => html`<git-main></git-main>`,
};

export const GitMergeRebase: Story = {
  argTypes: gitThingArgTypes,
  args: { gitThing: devGitThing },
  render: (args) => html`<git-merge-rebase .gitThing=${args.gitThing}></git-merge-rebase>`,
};

export const GitNewRepo: Story = {
  argTypes: gitThingArgTypes,
  args: { gitThing: devGitThing },
  render: (args) => html`<git-new-repo .gitThing=${args.gitThing}></git-new-repo>`,
};

export const GitPull: Story = {
  argTypes: gitThingArgTypes,
  args: { gitThing: devGitThing },
  render: (args) => html`<git-pull .gitThing=${args.gitThing}></git-pull>`,
};

export const GitPush: Story = {
  argTypes: gitThingArgTypes,
  args: { gitThing: devGitThing },
  render: (args) => html`<git-push .gitThing=${args.gitThing}></git-push>`,
};

export const GitStatus: Story = {
  argTypes: gitThingArgTypes,
  args: { gitThing: devGitThing },
  render: (args) => html`<git-status .gitThing=${args.gitThing}></git-status>`,
};

export const GitVersion: Story = {
  render: () => html`<git-version></git-version>`,
};

export const GitRepo: Story = {
  argTypes: {
    gitThing: { control: 'text', description: 'ThingWorx GitBackup Thing name' },
    defaultTab: { control: 'text', description: 'Default active tab' },
  },
  args: { gitThing: devGitThing, defaultTab: 'push' },
  render: (args) => html`<git-repo .gitThing=${args.gitThing} .defaultTab=${args.defaultTab}></git-repo>`,
};
