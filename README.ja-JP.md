<!-- Copyright (C) 2026 Synapxnet. All rights reserved.
Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0 | Security Level: INTERNAL -->

[简体中文](./README.md) · [English](./README.en-US.md) · **日本語**

# XnetMLops

データ準備、モデル学習、配備、基盤リソース、エージェント協働をつなぐ MLOps プラットフォームです。本リポジトリはバックエンドサービスを提供し、対応する Web コンソールは独立してビルド・配備できます。

[対応するリポジトリ](https://github.com/synapxnet/XnetMLops-web/tree/main) · [過去のリリース](https://github.com/synapxnet/XnetMLops/releases) · [SynapXnet コミュニティ](https://openxnet.synapxnet.com)

現在の開発ブランチは main です。常駐 Agent、Skill、モデル証拠、分野横断の復旧、人工承認、独立検証、冪等性、ロールバックを維持しています。接続先、実行環境、企業 ID は配備時に設定します。

## 機能モジュール

| モジュール | 機能 |
| --- | --- |
| DPP | データセット、前処理、特徴量、データ製品の取り込み、RAG |
| MTP | アルゴリズム、即時・定期学習、ログ、成果物、モデル証拠 |
| MEP | モデル・LLM サービス、API キー、ノード、制御付き配備とロールバック |
| SMP | テナント、部門、チーム、データソース、ストレージ、計算資源 |
| XAA | アシスタント、会話、ワークフロー、Skill 管理と取り込み |
| 常駐協働 | プラットフォーム状態、知識・能力ワークスペース、タスクと証拠の関連付け |

## 画面プレビュー

以下は機能画面の例です。業務記録と実行状態は接続先環境から取得します。

![XnetMLops](./docs/images/xnetmlops-analytics-2026.png)

| DPP | MTP |
| --- | --- |
| ![DPP](./docs/images/xnetmlops-dpp-datasets.png) | ![MTP](./docs/images/xnetmlops-mtp-training.png) |
| MEP | XAA |
| ![MEP](./docs/images/xnetmlops-mep-deployments.png) | ![XAA](./docs/images/xnetmlops-xaa-workflows.png) |

## 開発とビルド

JDK 17、Maven 3.9+、MySQL 8.x、Redis を使用します。有効にする機能に合わせて HDFS、Jenkins、オブジェクトストレージ、学習・推論サービスを設定してください。

~~~bash
git clone --branch main https://github.com/synapxnet/XnetMLops.git
cd XnetMLops
mvn -B -ntp -DskipTests package
~~~

.env.example をローカルの .env にコピーし、DB、Redis、JWT、ストレージ、承認サービス、委任 ID を設定します。対応する Web コンソールをビルドし、WEB_DIST_PATH を apps/web-antd/dist に合わせます。既存 DB には確認済みの移行を適用してください。XnetMLops.sql は初期化用です。ゲートウェイ設定後、docker compose config --quiet で確認して必要なサービスを起動します。

## 配備設定

企業の携帯番号と SMS のワンタイムコードでログインします。バックエンドに XNET_SMS_GATEWAY_URL と XNET_SMS_GATEWAY_TOKEN を設定します。未設定時は送信不可を明示します。有効期間は 5 分で、送信頻度と失敗回数を制限し、認証成功後にコードを消費します。

常駐 Agent ランタイムは独立したサービスで、/api/resident/v1 経由で接続します。プラットフォーム ID、モデルサービス、委任権限が必要です。アシスタント、常駐ランタイム、プラットフォーム間協働は役割を分担します。組織ツリーは認証済みユーザーの所属で絞り込み、業務操作にはテナント・チーム権限と承認を適用します。

モデル実行環境の URL、トークン、配備 UID、プローブ入力は環境設定から提供します。OPENXNET_MODEL_RESOURCE_BINDINGS と OPENXNET_FEATURE_DRIFT_RESOURCE_IDS で操作可能な資源を明示します。状態は実際のランタイムから再取得します。OPENXNET_REHEARSAL_ENABLED の既定値は false で、本番処理に合成演習結果を補いません。

## API とゲートウェイ

対応するフロントエンドは以下の同一オリジン API 接頭辞を使用します。nginx/default.conf は実際の Controller 経路に対応し、公式 Nginx エントリポイントで展開されます。配備環境の XNET_*_UPSTREAM アドレスを設定してください。

| サービス | フロントエンド API 接頭辞 | 内部サービスポート |
| --- | --- | --- |
| Auth | /api | 8181 |
| DPP | /dpp | 8182 |
| MTP | /mtp | 8183 |
| MEP | /mep | 8184 |
| SMP | /smp | 8185 |
| XAA | /xaa | 8186 |

## 文書と貢献

Issue と Pull Request を歓迎します。API・データ構造の変更には契約、権限範囲、移行手順を添えてください。過去のリリースは GitHub Releases に保持し、現在の文書は開発ブランチに合わせて更新します。

- [Mainline configuration and migration](./docs/mainline/transition.md)
- [DPP](./docs/DPP-Knowledge-Base.md) · [MTP](./docs/MTP-Knowledge-Base.md) · [MEP](./docs/MEP-Knowledge-Base.md) · [SMP](./docs/SMP-Knowledge-Base.md) · [XAA](./docs/XAA-Knowledge-Base.md)

## ライセンス

リポジトリの LICENSE と各ファイルの著作権表示に従ってください。フロントエンドは Vue Vben Admin を使用し、上流ライセンスを保持しています。 [LICENSE](./LICENSE)
## データベースとゲートウェイの初期化

新規データベースでは `XnetMLops.sql`、続いて `database/mainline-capabilities.sql` を実行します。後者はナレッジ、特徴量、インフラ、アシスタント、Skill、ワークフロー、監査の構造を追加し、繰り返し実行できます。デモのノード・ワークフロー・実行データは挿入しません。既存環境は事前にバックアップしてください。組織メンバーの `data_access_enabled` は既定で 0 です。確認済みのチームに明示的に権限を付与します。

Nginx 公式エントリーポイントが環境変数から共有ゲートウェイ設定を生成します。`/api/` はログインサービスへそのまま転送し、`/dpp/`・`/mtp/`・`/smp/` は各サービスの `/api/` へ、`/mep/` はそのまま、`/xaa/` は `/api/xaa/` へ転送します。業務 API は組織認可、Agent API は委任トークンを検証します。公開するのはゲートウェイのみです。バックエンドはループバックにバインドし、Kubernetes も Web ゲートウェイのみ公開します。

独立した常駐 Agent の URL は `XNET_RESIDENT_AGENT_URL`（末尾スラッシュなし）で設定します。未設定時は利用不可を明示します。`XNET_DNS_RESOLVER` は Docker で `127.0.0.11`、Kubernetes ではクラスター DNS を指定してください。


ナレッジベースのメタデータと既存記録は利用できます。内蔵バックエンドには文書処理・ベクトル検索実行器がなく、アップロード、再構築、検索は明示的な利用不可を返します。LLM の有効化・無効化は接続設定を管理し、実サービス状態は接続テストで確認します。学習、HDFS、Jenkins、駐在実行には実際の依存サービスが必要です。

Kubernetes は `xnet-mlops` 名前空間、`xnet-mlops-runtime` ConfigMap、`xnet-mlops-secrets` Secret を使用します。`deploy/runtime-config.example.yaml` の依存ホストを修正して適用してください。Secret には最低限 `DB_PASSWORD` と `JWT_SECRET` が必要です。SMS・委任の認証情報は機能に応じて追加し、ConfigMap に保存しないでください。イメージと取得認証は実環境に合わせます。MEP/XAA は 8184/8186 を使用し、ゲートウェイと整合しています。

実際の特性ドリフト実行にトークンファイルを使う場合、`deploy/compose.feature-drift.example.yaml` の読み取り専用マウント、または `deploy/feature-drift-secret.patch.example.yaml` の Kubernetes Secret パッチを使用します。コンテナ内パスの設定だけではファイルは渡されません。Kubernetes で駐在サービスの名前解決を使う場合は、`xnet-mlops-runtime` に `XNET_RESIDENT_AGENT_URL` とクラスタの `XNET_DNS_RESOLVER` を設定してください。
