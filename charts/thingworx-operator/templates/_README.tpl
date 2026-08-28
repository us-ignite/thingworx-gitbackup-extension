{{/*
Chart templates install only the operator control plane. Keep permissions
least-privileged as managed resources are added, and add health probes before
enabling more than one operator replica.
*/}}
