#!/usr/bin/env python3
"""在指标、摘要和人工审批均通过后将候选 DCN 模型晋级到部署仓库。"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Mapping, Sequence


VERSION_PATTERN = re.compile(r"^[A-Za-z0-9._-]{3,128}$")
AUDIT_PATTERN = re.compile(r"^[A-Za-z0-9._:-]{6,128}$")
REQUIRED_FILES = ("model.pt", "preprocessor.json", "metrics.json", "MODEL_CARD.md")


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    """解析候选目录、部署仓库、审批与指标阈值。"""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--candidate-dir", type=Path, required=True)
    parser.add_argument("--registry-root", type=Path, required=True)
    parser.add_argument("--product-version", required=True)
    parser.add_argument("--approval-id", required=True)
    parser.add_argument("--idempotency-key", required=True)
    parser.add_argument("--minimum-auc", type=float, default=0.85)
    parser.add_argument("--minimum-accuracy", type=float, default=0.75)
    return parser.parse_args(argv)


def sha256_file(path: Path) -> str:
    """流式计算候选制品的 SHA-256 摘要。"""

    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def read_json(path: Path) -> dict[str, object]:
    """以 UTF-8 读取 JSON 对象。"""

    with path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    if not isinstance(data, dict):
        raise ValueError(f"JSON 根节点必须是对象: {path}")
    return data


def validate_audit_fields(product_version: str, approval_id: str, idempotency_key: str) -> None:
    """校验版本、审批号和幂等键，阻止路径和审计字段注入。"""

    if not VERSION_PATTERN.fullmatch(product_version):
        raise ValueError("product-version 格式不合法")
    if not AUDIT_PATTERN.fullmatch(approval_id):
        raise ValueError("approval-id 格式不合法")
    if not AUDIT_PATTERN.fullmatch(idempotency_key):
        raise ValueError("idempotency-key 格式不合法")


def validate_candidate(
    candidate_dir: Path,
    product_version: str,
    minimum_auc: float,
    minimum_accuracy: float,
) -> dict[str, object]:
    """校验候选文件完整性、数据版本、模型摘要和测试阈值。"""

    for file_name in REQUIRED_FILES:
        if not (candidate_dir / file_name).is_file():
            raise ValueError(f"候选模型缺少文件: {file_name}")
    metrics = read_json(candidate_dir / "metrics.json")
    if metrics.get("product_version") != product_version:
        raise ValueError("候选模型的数据产品版本不匹配")
    model_digest = sha256_file(candidate_dir / "model.pt")
    if metrics.get("model_sha256") != model_digest:
        raise ValueError("候选模型摘要与 metrics.json 不一致")
    test_metrics = metrics.get("test_metrics")
    if not isinstance(test_metrics, Mapping):
        raise ValueError("metrics.json 缺少测试指标")
    if float(test_metrics.get("auc", 0.0)) < minimum_auc:
        raise ValueError("候选模型 AUC 未达到晋级阈值")
    if float(test_metrics.get("accuracy", 0.0)) < minimum_accuracy:
        raise ValueError("候选模型 Accuracy 未达到晋级阈值")
    return metrics


def build_manifest(
    metrics: Mapping[str, object],
    approval_id: str,
    idempotency_key: str,
) -> dict[str, object]:
    """构造推荐服务加载所需的受审批部署清单。"""

    return {
        "manifest_version": "1.0",
        "status": "approved",
        "model_type": "DCN",
        "product_version": metrics["product_version"],
        "schema_digest_sha256": metrics["schema_digest_sha256"],
        "artifact_digest_sha256": metrics["artifact_digest_sha256"],
        "model_digest_sha256": metrics["model_sha256"],
        "test_metrics": metrics["test_metrics"],
        "approval_id": approval_id,
        "idempotency_key": idempotency_key,
        "approved_at": datetime.now(timezone.utc).isoformat(),
    }


def write_json(path: Path, data: Mapping[str, object]) -> None:
    """以 UTF-8 写入固定格式的部署清单。"""

    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def promote_model(args: argparse.Namespace) -> Path:
    """以临时目录原子复制候选制品并返回已批准版本目录。"""

    candidate_dir = args.candidate_dir.resolve()
    registry_root = args.registry_root.resolve()
    product_version = args.product_version.strip()
    approval_id = args.approval_id.strip()
    idempotency_key = args.idempotency_key.strip()
    validate_audit_fields(product_version, approval_id, idempotency_key)
    metrics = validate_candidate(
        candidate_dir,
        product_version,
        args.minimum_auc,
        args.minimum_accuracy,
    )
    target_dir = registry_root / product_version
    manifest_path = target_dir / "deployment_manifest.json"
    if manifest_path.is_file():
        existing_manifest = read_json(manifest_path)
        if existing_manifest.get("idempotency_key") == idempotency_key:
            return target_dir
        raise ValueError("目标版本已存在且幂等键不同")

    registry_root.mkdir(parents=True, exist_ok=True)
    temporary_dir = registry_root / f".{product_version}.{uuid.uuid4().hex}.tmp"
    temporary_dir.mkdir()
    try:
        for file_name in REQUIRED_FILES:
            shutil.copy2(candidate_dir / file_name, temporary_dir / file_name)
        write_json(
            temporary_dir / "deployment_manifest.json",
            build_manifest(metrics, approval_id, idempotency_key),
        )
        temporary_dir.replace(target_dir)
    except Exception:
        shutil.rmtree(temporary_dir, ignore_errors=True)
        raise
    return target_dir


def main(argv: Sequence[str] | None = None) -> int:
    """执行候选模型晋级并输出目标目录。"""

    target_dir = promote_model(parse_args(argv))
    print(json.dumps({"approved_model_dir": str(target_dir)}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
