#!/usr/bin/env python3
"""加载经审批的 DCN 模型并提供健康检查与批量推荐评分接口。"""

from __future__ import annotations

import argparse
import json
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Mapping, Sequence

import pandas as pd
import torch

from train_dcn import (
    CATEGORICAL_COLUMNS,
    NUMERIC_COLUMNS,
    DCN,
    FeatureEncoder,
    sha256_file,
)


class ApprovedModelRuntime:
    """保存已校验部署清单、预处理器和 DCN 模型。"""

    def __init__(self, model_dir: Path) -> None:
        """加载模型目录并校验审批状态与模型摘要。"""

        self.model_dir = model_dir.resolve()
        self.manifest = self._read_json(self.model_dir / "deployment_manifest.json")
        if self.manifest.get("status") != "approved":
            raise ValueError("推荐服务只允许加载 approved 模型")
        model_path = self.model_dir / "model.pt"
        if sha256_file(model_path) != self.manifest.get("model_digest_sha256"):
            raise ValueError("模型文件摘要与部署清单不一致")
        preprocessor = self._read_json(self.model_dir / "preprocessor.json")
        self.encoder = FeatureEncoder.from_contract(preprocessor)
        checkpoint = torch.load(model_path, map_location="cpu", weights_only=True)
        self.model = DCN(**checkpoint["model_config"])
        self.model.load_state_dict(checkpoint["model_state_dict"])
        self.model.eval()

    def predict(self, records: Sequence[Mapping[str, object]]) -> list[dict[str, object]]:
        """将最多 100 条特征记录转换为推荐正样本概率。"""

        if not records or len(records) > 100:
            raise ValueError("records 数量必须位于 1 到 100 之间")
        normalized_records = []
        for record in records:
            normalized_records.append(
                {
                    **{column: record.get(column, "unknown") for column in CATEGORICAL_COLUMNS},
                    **{column: record.get(column, 0.0) for column in NUMERIC_COLUMNS},
                }
            )
        features = self.encoder.transform(pd.DataFrame(normalized_records))
        with torch.no_grad():
            probabilities = torch.softmax(self.model(torch.from_numpy(features)), dim=1)[:, 1].numpy()
        return [
            {"probability": round(float(probability), 8), "label": int(probability >= 0.5)}
            for probability in probabilities
        ]

    def health(self) -> dict[str, object]:
        """返回不含物理路径和秘密的模型健康信息。"""

        return {
            "status": "ready",
            "model_type": self.manifest["model_type"],
            "product_version": self.manifest["product_version"],
            "model_digest_sha256": self.manifest["model_digest_sha256"],
            "approval_id": self.manifest["approval_id"],
        }

    def _read_json(self, path: Path) -> dict[str, object]:
        """读取模型目录内的 UTF-8 JSON 对象。"""

        if not path.is_file():
            raise ValueError(f"模型目录缺少文件: {path.name}")
        with path.open("r", encoding="utf-8") as handle:
            data = json.load(handle)
        if not isinstance(data, dict):
            raise ValueError(f"JSON 根节点必须是对象: {path.name}")
        return data


class RecommendationRequestHandler(BaseHTTPRequestHandler):
    """处理推荐服务健康检查与批量评分请求。"""

    runtime: ApprovedModelRuntime

    def do_GET(self) -> None:
        """处理 `/health` 健康检查。"""

        if self.path != "/health":
            self._write_json(HTTPStatus.NOT_FOUND, {"error": "not_found"})
            return
        self._write_json(HTTPStatus.OK, self.runtime.health())

    def do_POST(self) -> None:
        """处理 `/predict` 批量推荐评分。"""

        if self.path != "/predict":
            self._write_json(HTTPStatus.NOT_FOUND, {"error": "not_found"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > 1024 * 1024:
                raise ValueError("请求体大小必须位于 1 B 到 1 MB 之间")
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            if payload.get("productVersion") != self.runtime.manifest["product_version"]:
                raise ValueError("请求数据产品版本与服务模型不一致")
            records = payload.get("records")
            if not isinstance(records, list) or not all(isinstance(item, dict) for item in records):
                raise ValueError("records 必须是对象数组")
            self._write_json(HTTPStatus.OK, {"predictions": self.runtime.predict(records)})
        except (ValueError, json.JSONDecodeError) as exception:
            self._write_json(HTTPStatus.BAD_REQUEST, {"error": str(exception)})

    def log_message(self, format_string: str, *args: object) -> None:
        """使用标准服务日志格式记录请求，不输出请求体。"""

        print(f"recommendation-service {self.address_string()} {format_string % args}")

    def _write_json(self, status: HTTPStatus, payload: Mapping[str, object]) -> None:
        """以 UTF-8 JSON 返回固定响应头和内容。"""

        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status.value)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    """解析已批准模型目录和本地监听地址。"""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model-dir", type=Path, required=True)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8090)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    """校验模型后启动多线程 HTTP 推荐评分服务。"""

    args = parse_args(argv)
    if args.host not in {"127.0.0.1", "0.0.0.0"}:
        raise ValueError("host 只允许 127.0.0.1 或 0.0.0.0")
    if args.port < 1 or args.port > 65535:
        raise ValueError("port 必须位于 1 到 65535 之间")
    RecommendationRequestHandler.runtime = ApprovedModelRuntime(args.model_dir)
    server = ThreadingHTTPServer((args.host, args.port), RecommendationRequestHandler)
    print(json.dumps(RecommendationRequestHandler.runtime.health(), ensure_ascii=False), flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
