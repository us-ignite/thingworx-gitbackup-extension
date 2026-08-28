# Reconciliation

Reconcilers turn a `ThingWorxCluster` into Kubernetes resources and status. Implement HA in explicit, observable phases: dependency validation, database preparation, coordination services, platform nodes, and ingress. Every phase must be idempotent and safe to retry.
