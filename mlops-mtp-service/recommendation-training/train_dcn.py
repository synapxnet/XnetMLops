#!/usr/bin/env python3
"""使用 DataOps 已发布的推荐数据产品训练可审计 CPU 版 DCN 模型。"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import os
import random
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Mapping, Sequence

import numpy as np
import pandas as pd
import torch
from torch import nn
from torch.utils.data import DataLoader, TensorDataset


CATEGORICAL_COLUMNS = (
    "user_key",
    "item_key",
    "behavior_type",
    "user_type",
    "user_sex",
    "user_manufacturer_type",
    "user_source",
    "item_type",
    "item_category_key",
    "item_tag_set_key",
)
NUMERIC_COLUMNS = (
    "item_duration_seconds",
    "behavior_duration_ms",
    "read_percent",
    "like_status",
)
REQUIRED_COLUMNS = {
    "product_version",
    "event_key",
    "label",
    "dataset_split",
    *CATEGORICAL_COLUMNS,
    *NUMERIC_COLUMNS,
}


@dataclass(frozen=True)
class TrainingConfig:
    """保存可复现训练所需的输入、模型和优化参数。"""

    input_csv: Path
    output_dir: Path
    product_version: str
    expected_schema_digest: str
    expected_artifact_digest: str
    epochs: int
    batch_size: int
    learning_rate: float
    hash_buckets: int
    seed: int
    cpu_threads: int


class FeatureEncoder:
    """将分类键哈希到固定桶，并按训练集统计量标准化数值字段。"""

    def __init__(self, hash_buckets: int) -> None:
        """创建固定桶数的特征编码器。"""

        self.hash_buckets = hash_buckets
        self.numeric_means: dict[str, float] = {}
        self.numeric_stds: dict[str, float] = {}

    @property
    def output_dimension(self) -> int:
        """返回编码后的固定特征维度。"""

        return len(CATEGORICAL_COLUMNS) * self.hash_buckets + len(NUMERIC_COLUMNS)

    def fit(self, frame: pd.DataFrame) -> None:
        """仅使用训练集合计算数值特征的均值和标准差。"""

        for column in NUMERIC_COLUMNS:
            values = self._prepare_numeric(frame[column], column)
            mean = float(values.mean())
            std = float(values.std())
            self.numeric_means[column] = mean
            self.numeric_stds[column] = std if std > 1e-6 else 1.0

    @classmethod
    def from_contract(cls, contract: Mapping[str, object]) -> "FeatureEncoder":
        """从训练制品中的预处理契约恢复编码器。"""

        encoder = cls(int(contract["hash_buckets"]))
        encoder.numeric_means = {
            str(key): float(value)
            for key, value in dict(contract["numeric_means"]).items()
        }
        encoder.numeric_stds = {
            str(key): float(value)
            for key, value in dict(contract["numeric_stds"]).items()
        }
        if int(contract["output_dimension"]) != encoder.output_dimension:
            raise ValueError("预处理契约的输出维度不一致")
        return encoder

    def transform(self, frame: pd.DataFrame) -> np.ndarray:
        """将数据帧转换为可直接输入 DCN 的稠密浮点矩阵。"""

        if len(self.numeric_means) != len(NUMERIC_COLUMNS):
            raise ValueError("FeatureEncoder 必须先调用 fit")
        matrix = np.zeros((len(frame), self.output_dimension), dtype=np.float32)
        row_indices = np.arange(len(frame))
        for feature_index, column in enumerate(CATEGORICAL_COLUMNS):
            offset = feature_index * self.hash_buckets
            buckets = frame[column].fillna("unknown").astype(str).map(self._stable_bucket).to_numpy()
            matrix[row_indices, offset + buckets] = 1.0
        numeric_offset = len(CATEGORICAL_COLUMNS) * self.hash_buckets
        for feature_index, column in enumerate(NUMERIC_COLUMNS):
            values = self._prepare_numeric(frame[column], column)
            normalized = (
                values - self.numeric_means[column]
            ) / self.numeric_stds[column]
            matrix[:, numeric_offset + feature_index] = normalized.astype(np.float32)
        return matrix

    def to_contract(self) -> dict[str, object]:
        """返回可随模型保存的预处理契约。"""

        return {
            "categorical_columns": list(CATEGORICAL_COLUMNS),
            "numeric_columns": list(NUMERIC_COLUMNS),
            "hash_algorithm": "BLAKE2b-64",
            "hash_buckets": self.hash_buckets,
            "numeric_means": self.numeric_means,
            "numeric_stds": self.numeric_stds,
            "output_dimension": self.output_dimension,
        }

    def _stable_bucket(self, value: object) -> int:
        """使用稳定 BLAKE2b 摘要将分类值映射到固定桶。"""

        digest = hashlib.blake2b(str(value).encode("utf-8"), digest_size=8).digest()
        return int.from_bytes(digest, byteorder="big") % self.hash_buckets

    def _prepare_numeric(self, series: pd.Series, column: str) -> np.ndarray:
        """清理数值字段，并对持续时间应用对数压缩。"""

        values = pd.to_numeric(series, errors="coerce").fillna(0.0).to_numpy(dtype=np.float64)
        values = np.nan_to_num(values, nan=0.0, posinf=0.0, neginf=0.0)
        if column in {"item_duration_seconds", "behavior_duration_ms"}:
            values = np.log1p(np.clip(values, 0.0, None))
        elif column == "read_percent":
            values = np.clip(values, 0.0, 100.0)
        return values


class CrossLayer(nn.Module):
    """实现原推荐代码包中的单层显式特征交叉。"""

    def __init__(self, input_dimension: int) -> None:
        """创建权重投影和逐维偏置。"""

        super().__init__()
        self.weight = nn.Linear(input_dimension, 1, bias=False)
        self.bias = nn.Parameter(torch.zeros(input_dimension))

    def forward(self, original: torch.Tensor, current: torch.Tensor) -> torch.Tensor:
        """计算当前层交叉项。"""

        return self.weight(current) * original + self.bias


class CrossNetwork(nn.Module):
    """堆叠多层显式特征交叉并保留残差。"""

    def __init__(self, input_dimension: int, layer_count: int) -> None:
        """创建指定层数的交叉网络。"""

        super().__init__()
        self.layers = nn.ModuleList(CrossLayer(input_dimension) for _ in range(layer_count))

    def forward(self, inputs: torch.Tensor) -> torch.Tensor:
        """依次执行交叉层并累加残差。"""

        current = inputs
        for layer in self.layers:
            current = current + layer(inputs, current)
        return current


class DCN(nn.Module):
    """复用原代码包结构的 Deep & Cross Network 二分类模型。"""

    def __init__(
        self,
        input_dimension: int,
        embedding_dimension: int = 64,
        cross_layer_count: int = 3,
        deep_layer_count: int = 3,
    ) -> None:
        """创建输入投影、交叉网络、深层网络和二分类输出层。"""

        super().__init__()
        self.input_projection = nn.Linear(input_dimension, embedding_dimension, bias=False)
        self.cross_network = CrossNetwork(embedding_dimension, cross_layer_count)
        deep_layers: list[nn.Module] = []
        for _ in range(deep_layer_count):
            deep_layers.extend(
                (
                    nn.Linear(embedding_dimension, embedding_dimension),
                    nn.ReLU(),
                    nn.Dropout(0.1),
                )
            )
        self.deep_network = nn.Sequential(*deep_layers)
        self.output_layer = nn.Linear(embedding_dimension * 2, 2)

    def forward(self, inputs: torch.Tensor) -> torch.Tensor:
        """并行计算交叉分支和深层分支后输出二分类 logits。"""

        embedded = self.input_projection(inputs)
        crossed = self.cross_network(embedded)
        deep = self.deep_network(embedded)
        return self.output_layer(torch.cat((crossed, deep), dim=-1))


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    """解析训练数据、契约摘要和 CPU 优化参数。"""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-csv", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--product-version", required=True)
    parser.add_argument("--expected-schema-digest", default="")
    parser.add_argument("--expected-artifact-digest", default="")
    parser.add_argument("--epochs", type=int, default=4)
    parser.add_argument("--batch-size", type=int, default=1024)
    parser.add_argument("--learning-rate", type=float, default=0.001)
    parser.add_argument("--hash-buckets", type=int, default=16)
    parser.add_argument("--seed", type=int, default=20260827)
    parser.add_argument("--cpu-threads", type=int, default=min(4, os.cpu_count() or 1))
    return parser.parse_args(argv)


def build_config(args: argparse.Namespace) -> TrainingConfig:
    """校验命令行参数并生成不可变训练配置。"""

    if not args.input_csv.is_file():
        raise ValueError(f"训练数据文件不存在: {args.input_csv}")
    if args.epochs < 1 or args.epochs > 100:
        raise ValueError("epochs 必须位于 1 到 100 之间")
    if args.batch_size < 32 or args.batch_size > 65_536:
        raise ValueError("batch-size 必须位于 32 到 65536 之间")
    if args.hash_buckets < 4 or args.hash_buckets > 1024:
        raise ValueError("hash-buckets 必须位于 4 到 1024 之间")
    return TrainingConfig(
        input_csv=args.input_csv.resolve(),
        output_dir=args.output_dir.resolve(),
        product_version=args.product_version.strip(),
        expected_schema_digest=args.expected_schema_digest.strip().lower(),
        expected_artifact_digest=args.expected_artifact_digest.strip().lower(),
        epochs=args.epochs,
        batch_size=args.batch_size,
        learning_rate=args.learning_rate,
        hash_buckets=args.hash_buckets,
        seed=args.seed,
        cpu_threads=max(1, args.cpu_threads),
    )


def set_reproducible_seed(seed: int, cpu_threads: int) -> None:
    """固定 Python、NumPy 和 PyTorch 随机状态与 CPU 线程数。"""

    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.set_num_threads(cpu_threads)
    torch.use_deterministic_algorithms(True)


def load_and_validate_dataset(config: TrainingConfig) -> pd.DataFrame:
    """读取 UTF-8 CSV 并验证字段、版本、标签和集合划分。"""

    frame = pd.read_csv(config.input_csv, encoding="utf-8", low_memory=False)
    missing = sorted(REQUIRED_COLUMNS - set(frame.columns))
    if missing:
        raise ValueError(f"训练数据缺少字段: {', '.join(missing)}")
    versions = set(frame["product_version"].dropna().astype(str).unique())
    if versions != {config.product_version}:
        raise ValueError(f"训练数据产品版本不匹配: {sorted(versions)}")
    labels = set(pd.to_numeric(frame["label"], errors="coerce").dropna().astype(int).unique())
    if labels != {0, 1}:
        raise ValueError(f"训练标签必须同时包含 0 和 1，当前为: {sorted(labels)}")
    splits = set(frame["dataset_split"].dropna().astype(str).unique())
    if splits != {"train", "validation", "test"}:
        raise ValueError(f"集合划分不完整: {sorted(splits)}")
    return frame


def calculate_schema_digest() -> str:
    """计算与 DataOps 字段契约一致的 Schema 摘要。"""

    contract_columns = (
        "product_version",
        "event_key",
        "user_key",
        "item_key",
        "behavior_type",
        "user_type",
        "user_sex",
        "user_manufacturer_type",
        "user_source",
        "item_type",
        "item_category_key",
        "item_tag_set_key",
        "item_duration_seconds",
        "behavior_duration_ms",
        "read_percent",
        "like_status",
        "label",
        "event_date",
        "dataset_split",
    )
    canonical = json.dumps(contract_columns, ensure_ascii=False, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def calculate_artifact_digest(frame: pd.DataFrame) -> str:
    """按事件键稳定顺序计算与 DataOps 相同的训练记录摘要。"""

    digest = hashlib.sha256()
    ordered = frame.sort_values("event_key", kind="mergesort")
    for row in ordered[["event_key", "label", "dataset_split"]].itertuples(index=False):
        digest.update(f"{row.event_key}|{int(row.label)}|{row.dataset_split}\n".encode("utf-8"))
    return digest.hexdigest()


def verify_contract_digests(config: TrainingConfig, frame: pd.DataFrame) -> tuple[str, str]:
    """计算并核对 DataOps Schema 与制品摘要。"""

    schema_digest = calculate_schema_digest()
    artifact_digest = calculate_artifact_digest(frame)
    if config.expected_schema_digest and schema_digest != config.expected_schema_digest:
        raise ValueError("Schema 摘要与 DataOps 发布契约不一致")
    if config.expected_artifact_digest and artifact_digest != config.expected_artifact_digest:
        raise ValueError("制品摘要与 DataOps 发布记录不一致")
    return schema_digest, artifact_digest


def create_tensor_dataset(features: np.ndarray, labels: np.ndarray) -> TensorDataset:
    """将 NumPy 特征和标签转换为 PyTorch 张量数据集。"""

    return TensorDataset(
        torch.from_numpy(features.astype(np.float32, copy=False)),
        torch.from_numpy(labels.astype(np.int64, copy=False)),
    )


def calculate_auc(labels: np.ndarray, probabilities: np.ndarray) -> float:
    """使用带并列秩修正的 Mann-Whitney 统计量计算二分类 AUC。"""

    order = np.argsort(probabilities, kind="mergesort")
    sorted_scores = probabilities[order]
    sorted_labels = labels[order]
    ranks = np.empty(len(sorted_scores), dtype=np.float64)
    start = 0
    while start < len(sorted_scores):
        end = start + 1
        while end < len(sorted_scores) and sorted_scores[end] == sorted_scores[start]:
            end += 1
        ranks[start:end] = (start + 1 + end) / 2.0
        start = end
    positive_count = int((sorted_labels == 1).sum())
    negative_count = int((sorted_labels == 0).sum())
    if positive_count == 0 or negative_count == 0:
        return 0.5
    positive_rank_sum = float(ranks[sorted_labels == 1].sum())
    return (
        positive_rank_sum - positive_count * (positive_count + 1) / 2
    ) / (positive_count * negative_count)


def calculate_metrics(labels: np.ndarray, probabilities: np.ndarray) -> dict[str, float]:
    """计算 AUC、准确率、精确率、召回率和 F1。"""

    predictions = (probabilities >= 0.5).astype(np.int64)
    true_positive = int(((predictions == 1) & (labels == 1)).sum())
    false_positive = int(((predictions == 1) & (labels == 0)).sum())
    false_negative = int(((predictions == 0) & (labels == 1)).sum())
    accuracy = float((predictions == labels).mean())
    precision = true_positive / max(1, true_positive + false_positive)
    recall = true_positive / max(1, true_positive + false_negative)
    f1 = 2 * precision * recall / max(1e-12, precision + recall)
    return {
        "accuracy": round(accuracy, 6),
        "auc": round(float(calculate_auc(labels, probabilities)), 6),
        "f1": round(float(f1), 6),
        "precision": round(float(precision), 6),
        "recall": round(float(recall), 6),
    }


def evaluate_model(model: nn.Module, dataset: TensorDataset, batch_size: int) -> dict[str, float]:
    """在指定集合上执行无梯度推理并计算指标。"""

    loader = DataLoader(dataset, batch_size=batch_size, shuffle=False)
    labels: list[np.ndarray] = []
    probabilities: list[np.ndarray] = []
    model.eval()
    with torch.no_grad():
        for features, batch_labels in loader:
            logits = model(features)
            batch_probabilities = torch.softmax(logits, dim=1)[:, 1]
            labels.append(batch_labels.numpy())
            probabilities.append(batch_probabilities.numpy())
    return calculate_metrics(np.concatenate(labels), np.concatenate(probabilities))


def train_model(
    model: nn.Module,
    train_dataset: TensorDataset,
    validation_dataset: TensorDataset,
    config: TrainingConfig,
) -> tuple[dict[str, torch.Tensor], list[dict[str, object]]]:
    """训练 DCN，并根据验证集 AUC 保存最佳权重。"""

    generator = torch.Generator().manual_seed(config.seed)
    loader = DataLoader(
        train_dataset,
        batch_size=config.batch_size,
        shuffle=True,
        generator=generator,
    )
    optimizer = torch.optim.AdamW(model.parameters(), lr=config.learning_rate, weight_decay=1e-5)
    loss_function = nn.CrossEntropyLoss()
    best_auc = -math.inf
    best_state: dict[str, torch.Tensor] = {}
    history: list[dict[str, object]] = []

    for epoch in range(1, config.epochs + 1):
        model.train()
        total_loss = 0.0
        trained_rows = 0
        for features, labels in loader:
            optimizer.zero_grad(set_to_none=True)
            logits = model(features)
            loss = loss_function(logits, labels)
            loss.backward()
            optimizer.step()
            total_loss += float(loss.item()) * len(labels)
            trained_rows += len(labels)
        validation_metrics = evaluate_model(model, validation_dataset, config.batch_size)
        epoch_result = {
            "epoch": epoch,
            "train_loss": round(total_loss / max(1, trained_rows), 6),
            "validation": validation_metrics,
        }
        history.append(epoch_result)
        print(json.dumps(epoch_result, ensure_ascii=False), flush=True)
        if validation_metrics["auc"] > best_auc:
            best_auc = validation_metrics["auc"]
            best_state = {
                name: tensor.detach().cpu().clone()
                for name, tensor in model.state_dict().items()
            }
    return best_state, history


def sha256_file(path: Path) -> str:
    """流式计算训练制品文件摘要。"""

    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def write_json(path: Path, data: Mapping[str, object]) -> None:
    """以 UTF-8 和固定缩进写入 JSON 证据文件。"""

    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def write_model_card(path: Path, metrics: Mapping[str, object]) -> None:
    """生成说明数据来源、用途、指标与边界的中文模型卡。"""

    test_metrics = metrics["test_metrics"]
    content = f"""# 推荐 DCN 模型卡

## 基本信息

- 数据产品：`{metrics['product_version']}`
- 模型结构：Deep & Cross Network
- 运行设备：CPU
- 训练记录：{metrics['row_counts']['train']}
- 验证记录：{metrics['row_counts']['validation']}
- 测试记录：{metrics['row_counts']['test']}

## 测试指标

- AUC：{test_metrics['auc']}
- Accuracy：{test_metrics['accuracy']}
- Precision：{test_metrics['precision']}
- Recall：{test_metrics['recall']}
- F1：{test_metrics['f1']}

## 数据与安全边界

模型只使用 XnetDataOps 已发布的脱敏训练数据产品。姓名、IP、设备标识、图片地址、学校、城市、标题与正文未进入训练。用户、内容和事件通过一致性脱敏键关联。

## 部署边界

该模型必须先完成独立评估和审批，才能进入灰度部署。线上服务需校验模型摘要、数据产品版本和预处理契约，任一不一致均拒绝加载。
"""
    path.write_text(content, encoding="utf-8", newline="\n")


def run_training(config: TrainingConfig) -> dict[str, object]:
    """执行数据校验、特征编码、模型训练、评估与制品写入闭环。"""

    set_reproducible_seed(config.seed, config.cpu_threads)
    frame = load_and_validate_dataset(config)
    schema_digest, artifact_digest = verify_contract_digests(config, frame)
    split_frames = {
        split: frame.loc[frame["dataset_split"] == split].reset_index(drop=True)
        for split in ("train", "validation", "test")
    }
    encoder = FeatureEncoder(config.hash_buckets)
    encoder.fit(split_frames["train"])
    tensor_datasets = {
        split: create_tensor_dataset(
            encoder.transform(split_frame),
            pd.to_numeric(split_frame["label"], errors="raise").to_numpy(dtype=np.int64),
        )
        for split, split_frame in split_frames.items()
    }
    model_config = {
        "input_dimension": encoder.output_dimension,
        "embedding_dimension": 64,
        "cross_layer_count": 3,
        "deep_layer_count": 3,
    }
    model = DCN(**model_config)
    best_state, history = train_model(
        model,
        tensor_datasets["train"],
        tensor_datasets["validation"],
        config,
    )
    model.load_state_dict(best_state)
    test_metrics = evaluate_model(model, tensor_datasets["test"], config.batch_size)

    config.output_dir.mkdir(parents=True, exist_ok=True)
    model_path = config.output_dir / "model.pt"
    torch.save(
        {
            "model_state_dict": best_state,
            "model_config": model_config,
            "product_version": config.product_version,
            "schema_digest_sha256": schema_digest,
            "artifact_digest_sha256": artifact_digest,
        },
        model_path,
    )
    write_json(config.output_dir / "preprocessor.json", encoder.to_contract())
    metrics: dict[str, object] = {
        "artifact_digest_sha256": artifact_digest,
        "completed_at": datetime.now(timezone.utc).isoformat(),
        "history": history,
        "model_config": model_config,
        "model_sha256": sha256_file(model_path),
        "product_version": config.product_version,
        "row_counts": {split: len(split_frame) for split, split_frame in split_frames.items()},
        "schema_digest_sha256": schema_digest,
        "test_metrics": test_metrics,
        "training_config": {
            key: str(value) if isinstance(value, Path) else value
            for key, value in asdict(config).items()
            if key not in {"expected_artifact_digest", "expected_schema_digest"}
        },
    }
    write_json(config.output_dir / "metrics.json", metrics)
    write_model_card(config.output_dir / "MODEL_CARD.md", metrics)
    return metrics


def main(argv: Sequence[str] | None = None) -> int:
    """运行训练任务并输出精简的机器可读结果。"""

    config = build_config(parse_args(argv))
    metrics = run_training(config)
    print(
        json.dumps(
            {
                "model_sha256": metrics["model_sha256"],
                "output_dir": str(config.output_dir),
                "product_version": metrics["product_version"],
                "test_metrics": metrics["test_metrics"],
            },
            ensure_ascii=False,
        ),
        flush=True,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
