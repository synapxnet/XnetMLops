<div align="center">

[简体中文](./README.md) | **English** | [日本語](./README.ja-JP.md)

# XnetMLops

**Open-source MLOps connecting data, training, deployment, resources, and agents**

[![GOAI release](https://img.shields.io/badge/GOAI_release-1.3.0-1677ff.svg)](https://github.com/synapxnet/XnetMLops/releases/tag/v1.3.0)
[![Java](https://img.shields.io/badge/Java-17-e76f00.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.6-6db33f.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-2ea44f.svg)](./LICENSE)

[Live Demo](https://goai.xnetmlops.synapxnet.online) · [Frontend: XnetMLops-web](https://github.com/synapxnet/XnetMLops-web/tree/v1.3.0) · [OpenXnet](https://openxnet.synapxnet.com) · [License](./LICENSE)

</div>

## GOAI v1.3.0 — release and downloads

This default `display` branch retains the earlier showcase code. The **GOAI v1.3.0 release** and current finals source are available through the links below; this documentation update does not upgrade this branch's application code.

**[Release notes](https://github.com/synapxnet/XnetMLops/releases/tag/v1.3.0) · [Download source ZIP](https://github.com/synapxnet/XnetMLops/releases/download/v1.3.0/XnetMLops-v1.3.0-source.zip) · [Pinned v1.3.0 source](https://github.com/synapxnet/XnetMLops/tree/v1.3.0) · [Validated program baseline guide](https://github.com/synapxnet/XnetMLops/blob/c9cae0607fc6fba6b76b26d9da4ca1ace0ab13bc/docs/GOAI-FINALS-V1.3.0-SOURCE-DELIVERY.md)**

[Matching frontend v1.3.0](https://github.com/synapxnet/XnetMLops-web/releases/tag/v1.3.0) · [OpenXnet v1.3.0](https://github.com/synapxnet/OpenXnet/releases/tag/v1.3.0)

The GOAI release provides model evidence/artifact reads, Skill candidate queries, workflow contracts and bounded-memory HDFS downloads. MEP governed execution checks delegated identity, approval digests, resource versions and idempotency; actions without a configured executor fail explicitly.

The [resident Agent runtime](https://github.com/synapxnet/OpenXnet/tree/c841ef841da8477fc312e27cd390aecac8ed2d7e/services/platform-resident-agent) runs as a separate platform service and cooperates with OpenXnet AgentTeams; it requires platform identity, model configuration and delegated permissions. The built-in assistant alone is not proof of a configured resident runtime.

Program baseline validation: all 8 Maven Reactor projects packaged and 118 isolated unit tests passed. External Application/Integration tests were excluded; migration SQL has not been rehearsed against isolated MySQL. Publishing this version does not redeploy online services or certify production readiness. See the delivery guide for configuration and limitations.

Program validation baseline: [c9cae060](https://github.com/synapxnet/XnetMLops/commit/c9cae0607fc6fba6b76b26d9da4ca1ace0ab13bc). The GOAI release's subsequent changes are limited to README documentation; use the release asset checksums for the revised source archive.

> **Historical UI screenshots below:** these showcase images are retained for context. They are not the current v1.3.0 UI acceptance evidence or a record of live governance execution.

![XnetMLops analytics center](./docs/images/xnetmlops-analytics-2026.png)

## Product Tour

| Dataset management | RAG knowledge base |
| --- | --- |
| ![Datasets](./docs/images/xnetmlops-dpp-datasets.png) | ![RAG knowledge base](./docs/images/xnetmlops-dpp-knowledge-base.png) |
| Training jobs | Model deployments |
| ![Training](./docs/images/xnetmlops-mtp-training.png) | ![Deployment](./docs/images/xnetmlops-mep-deployments.png) |
| Workstations | Agent workflows |
| ![Workstations](./docs/images/xnetmlops-smp-workstations.png) | ![Workflows](./docs/images/xnetmlops-xaa-workflows.png) |
| Meta-skills | AI assistants |
| ![Skills](./docs/images/xnetmlops-xaa-skills.png) | ![Assistants](./docs/images/xnetmlops-xaa-assistants.png) |
| Demo login | About |
| ![Login](./docs/images/xnetmlops-login.png) | ![About](./docs/images/xnetmlops-about.png) |

## Overview

XnetMLops is an open-source, full-lifecycle MLOps platform maintained by the **SynapXnet team**. It connects data preparation, model training, deployment, infrastructure resources, RAG, and agent orchestration into one engineering loop.

This backend repository and [XnetMLops-web](https://github.com/synapxnet/XnetMLops-web/tree/v1.3.0) form an enterprise-grade, multi-tenant, frontend/backend-separated system. DPP, MTP, MEP, SMP, and XAA are delivered as focused microservices.

## Highlights

- Enterprise multi-tenancy across tenants, departments, teams, roles, and resources.
- Complete flow from datasets and scheduled training to model APIs and agents.
- Independent frontend and backend delivery.
- Integration with Hadoop, Jenkins, object storage, Harbor, and compute nodes.
- Continuous improvements to training, serving, RAG, agents, and documentation.

## Modules

| Module | Service | Responsibility |
| --- | --- | --- |
| DPP | `mlops-dpp-service` | Datasets, preprocessing, feature engineering, pipelines, RAG, and Jenkins jobs |
| MTP | `mlops-mtp-service` | Algorithms, immediate/scheduled training, parameters, logs, and artifacts |
| MEP | `mlops-mep-service` | Model deployments, LLM services, API keys, nodes, logs, and OpenClaw |
| SMP | `mlops-smp-service` | Tenants, teams, data sources, storage, workstations, Hadoop, Jenkins, Harbor, and images |
| XAA | `mlops-xaa-service` | Assistants, conversations, visual workflows, skills, and cross-module orchestration |
| Login | `mlops-login` | Authentication and the unified platform entry |

## Quick start (v1.3.0)

The verified backend uses JDK **17**, Spring Boot **3.4.6** and Maven **3.9+**. Runtime dependencies include MySQL 8.x, Redis and the HDFS/Jenkins/model runtimes used by enabled modules. Compose does not supply a complete initialized database or training cluster.

```bash
git clone --branch v1.3.0 --single-branch https://github.com/synapxnet/XnetMLops.git
cd XnetMLops
mvn -B -ntp -DskipTests package
```

Build the matching `XnetMLops-web` v1.3.0 first. Copy `.env.example` to a protected local `.env` and configure database/Redis, HDFS, JWT, delegation and approval service settings. Set `WEB_DIST_PATH` to `../XnetMLops-web/apps/web-antd/dist`. Review the base SQL and migrations in the delivery guide; do not run a destructive database initialization against an existing environment. Migration SQL still requires an isolated MySQL rehearsal.

The checked-in `nginx/default.conf` routes `/api/login/`, `/api/dpp/` and other `/api/<service>/` prefixes, while the published web app uses the prefixes listed below. **Align the gateway and frontend configuration before starting containers**; preserve controller path prefixes, organization authorization and resident/governed-tool routes. The generic Compose/Nginx templates are not the current live deployment configuration. After completing these prerequisites, validate with `docker compose config --quiet`, then use `docker compose up -d --build`. Backend ports below are service ports, not public website addresses.

## Demo access and API routes

- GOAI demo: <https://goai.xnetmlops.synapxnet.online/#/auth/login>.
- Public demo account: **`17870171303`**; verification code: **`000000`** (demo environment only, publication authorized by the project owner). Login uses an **11-digit mobile number and a 6-digit verification code** through `POST /api/auth/login`; it is not a password login or the OpenXnet AgentTeams access-code field.
- On 2026-09-18, the site returned HTTP 200 with title `XnetMLops`. An authenticated read of `/api/resident/v1/status` returned `platform=mlops` and `agentId=agt-mlops-resident-v130`; the same read without login returned 401. This verifies platform identity and access control, not a new full business-flow acceptance.

All web API routes use the same origin as the demo:

| Service | Frontend API base | Backend service port |
| --- | --- | --- |
| Auth | `/api` | `8181` |
| DPP | `/dpp` | `8182` |
| MTP | `/mtp` | `8183` |
| MEP | `/mep` | `8184` |
| SMP | `/smp` | `8185` |
| XAA | `/xaa` | `8186` |


The resident API base is `/api/resident/v1`. Platform login, resident model-provider keys and OpenXnet AgentTeams demo access codes are different credentials. Subsequent feature calls remain subject to tenant/team permissions and execution approval.

## Community and License

XnetMLops is part of [OpenXnet](https://openxnet.synapxnet.com). Issues and pull requests are welcome.

Released under the [MIT License](./LICENSE). Copyright © 2026 SynapXnet.
