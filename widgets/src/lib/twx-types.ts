export interface GitStatus {
  File: string;
  Status: string;
}

export interface GitBranch {
  BranchName: string;
  BranchType: string;
  ShortBranchName: string;
}

export interface GitCommit {
  CommitID: string;
  CommitName: string;
  CommitTime: string;
}

export interface GitTag {
  TagName: string;
  CommitID: string;
  Message: string;
  Date: string;
}

export interface GitCurrentBranch {
  BranchName: string;
  DetachedHEAD: boolean;
}

export interface CommitChangedFile {
  FileName: string;
  Status: string;
}

export interface CommitInfo {
  Author: string;
  ChangedFiles: CommitChangedFile[];
  CommitDescription: string;
  Commiter: string;
  CommitID: string;
  Date: string;
  Parents: string;
  SignatureVerification: string;
}

export interface QueryRepository {
  CloneURL: string;
  FullName: string;
  Name: string;
  Owner: string;
  Type: string;
}

export interface GitConfiguration {
  CommitEmail: string;
  CommitUser: string;
  FileRepoPath: string;
  FileRepository: string;
  GitRepoURL: string;
  InitialBranch: string;
  LocalizationTokensPrefix: string;
  Password: string;
  ProxyPort: number;
  ProxyURL: string;
  UseProxy: boolean;
  User: string;
}

export interface GitCredentials {
  GitCommitterEmail: string;
  GitCommitterFullName: string;
  GitCommitterPassword: string;
  GitCommitterUser: string;
  GitThing: string;
}

export interface GitHeader {
  GitThingName: string;
  HeightY: number;
  MashupName: string;
  WidthX: number;
}

export interface GpgKey {
  GitThing: string;
  GpgPrivateKey: string;
  GpgKeyPassphrase: string;
  SignCommits: boolean;
  GpgKeyFingerprint: string;
}

export interface LogEntry {
  Content: string;
  ID: string;
  ServiceName: string;
  Source: string;
  timestamp: string;
  User: string;
}

export interface ExtensionVersion {
  ExtensionName: string;
  ExtensionVersion: string;
  IsInstalled: boolean;
}

export interface RemoteRepository {
  CloneURL: string;
  FullName: string;
  Name: string;
  Owner: string;
  Type: string;
}

export interface InfotableResponse<T> {
  dataShape: { fieldDefinitions: Record<string, { name: string; baseType: string }> };
  rows: T[];
}

export interface ServiceResult {
  result?: string;
}
