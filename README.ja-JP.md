<div align="center">

[简体中文](./README.md) | [English](./README.en-US.md) | **日本語**

# XnetMLops

**データ、学習、配備、リソース、エージェントを結ぶオープンソース MLOps**

[![GOAI release](https://img.shields.io/badge/GOAI_release-1.3.0-1677ff.svg)](https://github.com/synapxnet/XnetMLops/releases/tag/v1.3.0)
[![Java](https://img.shields.io/badge/Java-17-e76f00.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.6-6db33f.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-2ea44f.svg)](./LICENSE)

[オンラインデモ](https://goai.xnetmlops.synapxnet.online) · [フロントエンド: XnetMLops-web](https://github.com/synapxnet/XnetMLops-web/tree/v1.3.0) · [OpenXnet](https://openxnet.synapxnet.com) · [ライセンス](./LICENSE)

</div>

## GOAI v1.3.0 — リリースとダウンロード

既定の `display` ブランチは従来の展示用コードを保持しています。**GOAI v1.3.0 リリース**と決勝用ソースは以下から参照できます。この README 更新で本ブランチのアプリケーションコードは更新されません。

**[リリース説明](https://github.com/synapxnet/XnetMLops/releases/tag/v1.3.0) · [ソース ZIP](https://github.com/synapxnet/XnetMLops/releases/download/v1.3.0/XnetMLops-v1.3.0-c9cae060-source.zip) · [v1.3.0 固定ソース](https://github.com/synapxnet/XnetMLops/tree/v1.3.0) · [ビルド・交付ガイド](https://github.com/synapxnet/XnetMLops/blob/c9cae0607fc6fba6b76b26d9da4ca1ace0ab13bc/docs/GOAI-FINALS-V1.3.0-SOURCE-DELIVERY.md)**

[対応するフロントエンド v1.3.0](https://github.com/synapxnet/XnetMLops-web/releases/tag/v1.3.0) · [OpenXnet v1.3.0](https://github.com/synapxnet/OpenXnet/releases/tag/v1.3.0)

GOAI 版はモデルの証拠・成果物の読み取り、Skill 候補照会、ワークフロー契約、メモリ使用を抑えた HDFS ダウンロードを提供します。MEP の制御付き実行は委任 ID、承認ダイジェスト、リソース版、冪等性を確認し、実行器未設定の操作を明示的に拒否します。

[常駐 Agent ランタイム](https://github.com/synapxnet/OpenXnet/tree/c841ef841da8477fc312e27cd390aecac8ed2d7e/services/platform-resident-agent)は独立したプラットフォームサービスとして動作し、OpenXnet AgentTeams と協働します。プラットフォーム ID、モデル設定、委任権限の構成が必要です。既存アシスタントがあるだけで、常駐ランタイムの設定完了を意味しません。

検証結果：Maven Reactor 8 件のパッケージ化と隔離単体テスト 118 件が成功しました。外部サービス依存の Application/Integration テストは対象外で、移行 SQL は隔離 MySQL で未試行です。 リリースの公開はオンラインサービスの再配備や本番認証を意味しません。構成と制限は交付ガイドをご確認ください。

> **以下は過去の画面画像です。** 展示用の参考画像であり、v1.3.0 の最新 UI 受入証跡や実環境での制御実行記録ではありません。

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

本バックエンドと [XnetMLops-web](https://github.com/synapxnet/XnetMLops-web/tree/v1.3.0) は、企業向けマルチテナント、フロントエンド・バックエンド分離システムを構成します。DPP、MTP、MEP、SMP、XAA を独立したサービスとして提供します。

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

## クイックスタート（v1.3.0）

検証済み構成は JDK **17**、Spring Boot **3.4.6**、Maven **3.9+** です。実行には MySQL 8.x、Redis、および有効な機能が利用する HDFS/Jenkins/モデル実行環境が必要です。Compose だけでは初期化済み DB や学習クラスタは用意されません。

```bash
git clone --branch v1.3.0 --single-branch https://github.com/synapxnet/XnetMLops.git
cd XnetMLops
mvn -B -ntp -DskipTests package
```

対応する `XnetMLops-web` v1.3.0 をビルドし、`.env.example` を保護されたローカル `.env` にコピーして、DB/Redis、HDFS、JWT、委任・承認サービスを設定してください。`WEB_DIST_PATH` は `../XnetMLops-web/apps/web-antd/dist` です。基礎 SQL と移行は交付ガイドを確認し、既存環境に破壊的な初期化を実行しないでください。移行 SQL は隔離 MySQL での試行が必要です。

同梱 `nginx/default.conf` は `/api/login/`、`/api/dpp/` などを使い、公開 Web アプリの接頭辞とは異なります。**コンテナ起動前にゲートウェイとフロントエンドを整合**させ、Controller 接頭辞、組織認可、常駐 Agent・制御ツールの経路を維持してください。汎用 Compose/Nginx テンプレートは実稼働構成そのものではありません。設定後に `docker compose config --quiet` で検証し、`docker compose up -d --build` で起動します。下記のバックエンドポートは内部サービス用で、公開サイト URL ではありません。

## デモ接続と API 経路

- GOAI デモ：<https://goai.xnetmlops.synapxnet.online/#/auth/login>。
- 公開デモアカウント：**`17870171303`**、確認コード：**`000000`**（デモ環境専用、プロジェクト管理者が公開を承認）。ログインは **11 桁の携帯番号と 6 桁の確認コード**を `POST /api/auth/login` に送信します。パスワード方式でも OpenXnet AgentTeams のアクセスコード入力でもありません。
- 2026-09-18 の検証で、サイトは HTTP 200、タイトルは `XnetMLops` でした。認証後の `/api/resident/v1/status` は `platform=mlops`、`agentId=agt-mlops-resident-v130` を返し、未認証では 401 でした。これはプラットフォーム識別とアクセス制御の確認で、業務全体の再受入試験ではありません。

Web API はデモと同じオリジンを使用します。

| サービス | フロントエンド API base | バックエンドのサービスポート |
| --- | --- | --- |
| Auth | `/api` | `8181` |
| DPP | `/dpp` | `8182` |
| MTP | `/mtp` | `8183` |
| MEP | `/mep` | `8184` |
| SMP | `/smp` | `8185` |
| XAA | `/xaa` | `8186` |


常駐 API base は `/api/resident/v1` です。プラットフォームのログイン情報、常駐 Agent のモデルキー、OpenXnet AgentTeams のデモアクセスコードは別の認証情報です。機能の利用には引き続きテナント・チーム権限と実行承認が必要です。

## コミュニティとライセンス

XnetMLops は [OpenXnet](https://openxnet.synapxnet.com) の一部です。

[MIT License](./LICENSE) の下で公開されています。Copyright © 2026 SynapXnet.
