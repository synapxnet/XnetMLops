<div align="center">

[简体中文](./README.md) | **English** | [日本語](./README.ja-JP.md)

# XnetMLops

**Open-source MLOps connecting data, training, deployment, resources, and agents**

[![Version](https://img.shields.io/badge/version-1.0.0-1677ff.svg)](https://www.xnetmlops.synapxnet.cn)
[![Java](https://img.shields.io/badge/Java-17-e76f00.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.6-6db33f.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-2ea44f.svg)](./LICENSE)

[Live Demo](https://www.xnetmlops.synapxnet.cn) · [Frontend: XnetMLops-web](https://github.com/synapxnet/XnetMLops-web) · [OpenXnet](https://openxnet.synapxnet.com) · [License](./LICENSE)

</div>

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

This backend repository and [XnetMLops-web](https://github.com/synapxnet/XnetMLops-web) form an enterprise-grade, multi-tenant, frontend/backend-separated system. DPP, MTP, MEP, SMP, and XAA are delivered as focused microservices.

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

## Quick Start

```bash
mvn -DskipTests package
cp .env.example .env
docker compose up -d --build
docker compose ps
```

Requirements: JDK 17+, Maven 3.9+, Docker Compose, MySQL 8.x, Redis 7.x, plus external systems required by enabled modules.

Showcase data is maintained in `demo/showcase_data.sql`.

## Demo

- URL: <https://www.xnetmlops.synapxnet.cn>
- Phone: `12345678900`
- Verification code: `000000`

The fixed code is only for the public showcase. Production must use secure authentication.

## Community and License

XnetMLops is part of [OpenXnet](https://openxnet.synapxnet.com). Issues and pull requests are welcome.

Released under the [MIT License](./LICENSE). Copyright © 2026 SynapXnet.
