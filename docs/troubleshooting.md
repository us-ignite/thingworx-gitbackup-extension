# Troubleshooting

## Connection failures

Check the remote URL, credentials, DNS, firewall rules, and proxy settings from the ThingWorx server. A repository URL that works from a workstation may not be reachable from the platform host.

## Import or export failures

Review `GIT.ExtensionLog.DataTable`, the service result, and the Git working tree. Confirm that the repository path is writable and that the target ThingWorx entities and dependencies exist.

## GPG failures

Verify the armored key, passphrase, selected fingerprint, and repository signing selection. The key
must contain a usable signing secret key. Confirm that the configured committer identity is
acceptable to the Git server’s signature policy. Remember that `Commit` signs locally and `Push`
publishes the resulting commit separately.

## Upgrade issues

Confirm that the extension variant matches the ThingWorx platform and that the extension migrator completed. If user properties are missing, inspect the platform logs for `ExtMigrator` messages before attempting manual reinitialization.
