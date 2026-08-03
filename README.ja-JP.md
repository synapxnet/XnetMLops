<div align="center">

[简体中文](./README.md) | [English](./README.en-US.md) | **日本語**

# XnetMLops

**データ、学習、配備、リソース、エージェントを結ぶオープンソース MLOps**

[![Version](https://img.shields.io/badge/version-1.0.0-1677ff.svg)](https://www.xnetmlops.synapxnet.cn)
[![Java](https://img.shields.io/badge/Java-17-e76f00.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.6-6db33f.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-2ea44f.svg)](./LICENSE)

[オンラインデモ](https://www.xnetmlops.synapxnet.cn) · [フロントエンド: XnetMLops-web](https://github.com/synapxnet/XnetMLops-web) · [OpenXnet](https://openxnet.synapxnet.com) · [ライセンス](./LICENSE)

</div>

![XnetMLops MLOps 分析センター](./docs/images/xnetmlops-analytics-2026.png)

## 画面プレビュー

| データセット管理 | RAG ナレッジベース |
| --- | --- |
| ![データセット](./docs/images/xnetmlops-dpp-datasets.png) | ![RAG](./docs/images/xnetmlops-dpp-knowledge-base.png) |
| 学習ジョブ | モデル配備 |
| ![学習](./docs/images/xnetmlops-mtp-training.png) | ![配備](./docs/images/xnetmlops-mep-deployments.png) |
| ワークステーション | エージェントワークフロー |
| ![ワークステーション](./docs/images/xnetmlops-smp-workstations.png) | ![ワークフロー](./docs/images/xnetmlops-xaa-workflows.png) |
| メタスキル | AI アシスタント |
| ![スキル](./docs/images/xnetmlops-xaa-skills.png) | ![アシスタント](./docs/images/xnetmlops-xaa-assistants.png) |
| デモログイン | プロジェクト情報 |
| ![ログイン](./docs/images/xnetmlops-login.png) | ![プロジェクト情報](./docs/images/xnetmlops-about.png) |

## 概要

XnetMLops は **SynapXnet チーム**が公開するフルライフサイクル MLOps プラットフォームです。データ準備、モデル学習、配備、インフラリソース、RAG、エージェント編成を一つの工程として接続します。

本バックエンドと [XnetMLops-web](https://github.com/synapxnet/XnetMLops-web) は、企業向けマルチテナント、フロントエンド・バックエンド分離システムを構成します。DPP、MTP、MEP、SMP、XAA を独立したサービスとして提供します。

## GOAI Competition 1.0.0

`GOAI-Competition` ブランチはデプロイ証拠、Fixture に基づく推論 Probe、承認で保護された永続ロールバック Action を追加します。職務分離、冪等性、楽観ロック、プロセス復旧、独立検証を実装し、`dryRun=true` はデータベースと Runtime を変更しません。

[マイグレーション、承認連携、API 例、検証結果](./docs/goai-handoff/HANDOFF-GOAI-COMPETITION-1.0.0.md) · [対応するモデル証拠 UI](https://github.com/synapxnet/XnetMLops-web/tree/GOAI-Competition)

## 特長

- テナント、部門、チーム、ロール、リソースを横断するマルチテナント。
- データセット、定期学習、モデル API、エージェントまでの一貫した工程。
- フロントエンドとバックエンドを独立配備。
- Hadoop、Jenkins、オブジェクトストレージ、Harbor、計算ノードと統合。
- 学習、推論、RAG、エージェント、ドキュメントを継続改善。

## モジュール

| モジュール | サービス | 主な機能 |
| --- | --- | --- |
| DPP | `mlops-dpp-service` | データセット、前処理、特徴量、パイプライン、RAG |
| MTP | `mlops-mtp-service` | アルゴリズム、即時・定期学習、パラメータ、ログ、成果物 |
| MEP | `mlops-mep-service` | モデル配備、LLM、API キー、ノード、OpenClaw |
| SMP | `mlops-smp-service` | テナント、チーム、データソース、ストレージ、計算資源 |
| XAA | `mlops-xaa-service` | アシスタント、会話、ワークフロー、スキル、モジュール連携 |
| Login | `mlops-login` | 認証と統一エントリ |

## クイックスタート

```bash
mvn -DskipTests package
cp .env.example .env
docker compose up -d --build
docker compose ps
```

JDK 17+、Maven 3.9+、Docker Compose、MySQL 8.x、Redis 7.x、および有効化するモジュールの外部システムが必要です。

演示データは `demo/showcase_data.sql` で管理します。

## デモ

- URL: <https://www.xnetmlops.synapxnet.cn>
- 電話番号: `12345678900`
- 確認コード: `000000`

固定確認コードは公開デモ専用です。本番環境では安全な認証方式を使用してください。

## コミュニティとライセンス

XnetMLops は [OpenXnet](https://openxnet.synapxnet.com) の一部です。

[MIT License](./LICENSE) の下で公開されています。Copyright © 2026 SynapXnet.
