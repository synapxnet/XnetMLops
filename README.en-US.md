<!-- Copyright (C) 2026 Synapxnet. All rights reserved.
Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0 | Security Level: INTERNAL -->

[简体中文](./README.md) · **English** · [日本語](./README.ja-JP.md)

# XnetMLops

An MLOps platform connecting data preparation, training, serving, infrastructure and agent collaboration. This repository contains backend services; the matching web console is built and deployed independently.

[Matching repository](https://github.com/synapxnet/XnetMLops-web/tree/main) · [Historical releases](https://github.com/synapxnet/XnetMLops/releases) · [SynapXnet community](https://openxnet.synapxnet.com)

The active development branch is main. Resident agents, Skills, model evidence, cross-domain recovery, human approval, independent verification, idempotency and rollback remain available. Deployments supply their own endpoints, runtime configuration and enterprise identities.

## Modules

| Module | Capability |
| --- | --- |
| DPP | Datasets, preprocessing, features, data-product import and RAG knowledge bases |
| MTP | Algorithms, immediate and scheduled training, logs, artifacts and model evidence |
| MEP | Model and LLM services, API keys, nodes, governed deployment and rollback |
| SMP | Tenants, departments, teams, data sources, storage and compute resources |
| XAA | Assistants, conversations, visual workflows, Skill management and import |
| Resident collaboration | Platform status, knowledge and capability workspaces, linked tasks and evidence |

## Product tour

These images illustrate product interfaces; records and runtime status come from the connected environment.

![XnetMLops](./docs/images/xnetmlops-analytics-2026.png)

| DPP | MTP |
| --- | --- |
| ![DPP](./docs/images/xnetmlops-dpp-datasets.png) | ![MTP](./docs/images/xnetmlops-mtp-training.png) |
| MEP | XAA |
| ![MEP](./docs/images/xnetmlops-mep-deployments.png) | ![XAA](./docs/images/xnetmlops-xaa-workflows.png) |

## Development and build

Use JDK 17, Maven 3.9+, MySQL 8.x and Redis. Configure HDFS, Jenkins, object storage, training and inference services for the modules you enable.

~~~bash
git clone --branch main https://github.com/synapxnet/XnetMLops.git
cd XnetMLops
mvn -B -ntp -DskipTests package
~~~

Copy .env.example to a local .env and configure the database, Redis, JWT, storage, approval service and delegated platform identity. Build the matching web console and point WEB_DIST_PATH to its apps/web-antd/dist directory. Use reviewed migrations for existing databases; XnetMLops.sql is an initialization script. After configuring the gateway, run docker compose config --quiet before starting the required services.

## Deployment configuration

Login uses an enterprise mobile number and a one-time SMS code. Configure XNET_SMS_GATEWAY_URL and XNET_SMS_GATEWAY_TOKEN on the backend; sending is explicitly unavailable without a gateway. Codes expire after five minutes, have send and attempt limits, and are consumed after successful verification.

The resident agent runtime is a separate platform service exposed through /api/resident/v1. Configure its platform identity, model provider and delegated permissions. Assistants, resident runtimes and cross-platform orchestration have distinct responsibilities. Organization trees are filtered by authenticated membership; business requests retain tenant, team and approval boundaries.

Environment configuration supplies model runtime URLs, tokens, deployment UIDs and probe inputs. OPENXNET_MODEL_RESOURCE_BINDINGS and OPENXNET_FEATURE_DRIFT_RESOURCE_IDS explicitly identify permitted resources. State must be read from the actual runtime. OPENXNET_REHEARSAL_ENABLED defaults to false; production flows do not substitute synthetic rehearsal results.

## API and gateway

The matching frontend uses these same-origin API prefixes. nginx/default.conf matches the actual controller paths and is rendered by the official Nginx entrypoint. Configure the XNET_*_UPSTREAM addresses for your deployment.

| Service | Frontend API prefix | Internal backend port |
| --- | --- | --- |
| Auth | /api | 8181 |
| DPP | /dpp | 8182 |
| MTP | /mtp | 8183 |
| MEP | /mep | 8184 |
| SMP | /smp | 8185 |
| XAA | /xaa | 8186 |

## Documentation and contributions

Issues and pull requests are welcome. API and data changes should include contracts, permission boundaries and migration instructions. Historical releases remain in GitHub Releases; current documentation follows the development branch.

- [Mainline configuration and migration](./docs/mainline/transition.md)
- [DPP](./docs/DPP-Knowledge-Base.md) · [MTP](./docs/MTP-Knowledge-Base.md) · [MEP](./docs/MEP-Knowledge-Base.md) · [SMP](./docs/SMP-Knowledge-Base.md) · [XAA](./docs/XAA-Knowledge-Base.md)

## License

Follow the repository LICENSE and per-file copyright notices. The frontend uses Vue Vben Admin and retains its upstream license. [LICENSE](./LICENSE)
## Database and gateway initialization

For a new database, apply `XnetMLops.sql` followed by `database/mainline-capabilities.sql`. The latter adds all knowledge, feature, infrastructure, assistant, Skill, workflow and governed execution tables idempotently, without demonstration nodes, workflows or run data. Back up existing databases before applying the capability migration. Organization membership defaults to `data_access_enabled=0`; grant access explicitly for verified teams.

The official Nginx entrypoint renders the shared gateway template from environment variables. `/api/` preserves the login path; `/dpp/`, `/mtp/` and `/smp/` map to `/api/` on their services; `/mep/` preserves its path; `/xaa/` maps to `/api/xaa/`. Business APIs require organization authorization, while Agent APIs validate delegated credentials. Only the gateway is public; backend ports bind to loopback. Kubernetes publishes only the web gateway Ingress.

Set `XNET_RESIDENT_AGENT_URL` to the independent resident service base URL without a trailing slash. An unconfigured resident service returns an explicit unavailable response. Set `XNET_DNS_RESOLVER` to your DNS resolver (Docker default `127.0.0.11`; use the cluster DNS in Kubernetes).


Knowledge-base metadata and existing records remain available. The built-in backend has no document/vector processing executor; upload, rebuild and retrieval return explicit unavailability. LLM enable/disable controls connection configuration; connection tests independently check external service health. Training, HDFS, Jenkins and resident execution require actual dependencies and never synthesize success when unconfigured.

Kubernetes manifests use the `xnet-mlops` namespace, `xnet-mlops-runtime` ConfigMap and `xnet-mlops-secrets` Secret. Update dependency hosts in `deploy/runtime-config.example.yaml` before applying it. The Secret requires `DB_PASSWORD` and `JWT_SECRET`; add SMS and delegation credentials for enabled capabilities. Never store credentials in the ConfigMap. Replace image addresses and pull credentials with your registry. Service names and ports match the frontend gateway; MEP/XAA use 8184/8186.

For real feature-drift token files, use the optional read-only mount in `deploy/compose.feature-drift.example.yaml` or the Kubernetes Secret patch `deploy/feature-drift-secret.patch.example.yaml`; setting a container path alone does not provide the file. Kubernetes resident DNS requires both `XNET_RESIDENT_AGENT_URL` and the cluster `XNET_DNS_RESOLVER` in `xnet-mlops-runtime`.
