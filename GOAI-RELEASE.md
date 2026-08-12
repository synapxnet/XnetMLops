# OpenXnet GOAI Competition Suite goai-v1.1.0

## Component

| Field | Value |
| --- | --- |
| Repository | [`synapxnet/XnetMLops`](https://github.com/synapxnet/XnetMLops) |
| Product version | `1.0.0` |
| Suite tag | `goai-v1.1.0` |
| Candidate base commit | `ae792dfc6d1130dd95e4ede1decf2e59b0864adb` |
| Component role | Model engineering and lifecycle platform |
| Repository license | `MIT` |
| SBOM components | 30 dependency records |

## Shared Contract

- AgentTeams topology: Incident Commander / Evidence Agent / Verification Agent.
- Governance Skills: `goai-evidence-collect`, `goai-change-execute`, `goai-service-verify` version `1.1.0` in the Desktop release asset.
- Scenario Skills: recommendation capacity recovery, quantitative model iteration, and feature drift recovery version `1.0.0`.
- MCP protocol: `2026-07-28`.
- Tool contract: `1.0.0`.
- Execution boundary: Workspace-scoped evidence, human approval for high-risk writes, Dry Run, resource version, parameter digest, idempotency, independent verification, audit, and rollback.

## Build and Verification

Use the commands already documented in README and the repository build manifests. Generate artifacts only from this tagged commit or a clean export of it. Do not package local `display` worktrees, caches, `.env` files, credentials, logs, or generated build directories.

The committed `sbom.cdx.json` is generated from repository lock files and build manifests. It records software dependencies declared by this component; runtime infrastructure images and external managed services require separate deployment SBOMs.

## Related Files

- [README.md](./README.md)
- [LICENSE](./LICENSE)
- [NOTICE](./NOTICE)
- [SECURITY.md](./SECURITY.md)
- [CONTRIBUTING.md](./CONTRIBUTING.md)
- [CycloneDX SBOM](./sbom.cdx.json)
