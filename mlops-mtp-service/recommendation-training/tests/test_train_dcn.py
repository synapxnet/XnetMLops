"""验证 DCN 训练适配器的契约摘要、特征编码和指标计算。"""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path

import numpy as np
import pandas as pd


SCRIPT_PATH = Path(__file__).parents[1] / "train_dcn.py"
SPEC = importlib.util.spec_from_file_location("train_dcn", SCRIPT_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class TrainDcnTest(unittest.TestCase):
    """覆盖固定特征维度、摘要稳定性和基础评估指标。"""

    def test_feature_encoder_is_stable(self) -> None:
        """确认相同分类值在拟合与转换后得到一致向量。"""

        frame = self._frame()
        encoder = MODULE.FeatureEncoder(hash_buckets=8)
        encoder.fit(frame)
        first = encoder.transform(frame)
        second = encoder.transform(frame.copy())
        self.assertEqual(first.shape, (4, 84))
        np.testing.assert_array_equal(first, second)

    def test_artifact_digest_is_order_independent(self) -> None:
        """确认记录顺序变化不会改变 DataOps 制品摘要。"""

        frame = self._frame()
        first = MODULE.calculate_artifact_digest(frame)
        second = MODULE.calculate_artifact_digest(frame.sample(frac=1.0, random_state=7))
        self.assertEqual(first, second)

    def test_metrics_are_correct_for_perfect_predictions(self) -> None:
        """确认完全正确的概率输出得到满分分类指标。"""

        metrics = MODULE.calculate_metrics(
            np.array([0, 0, 1, 1]),
            np.array([0.1, 0.2, 0.8, 0.9]),
        )
        self.assertEqual(metrics["auc"], 1.0)
        self.assertEqual(metrics["accuracy"], 1.0)
        self.assertEqual(metrics["f1"], 1.0)

    def _frame(self) -> pd.DataFrame:
        """构造包含全部训练特征的最小数据帧。"""

        return pd.DataFrame(
            {
                "user_key": ["u1", "u2", "u1", "u2"],
                "item_key": ["i1", "i2", "i3", "i4"],
                "behavior_type": ["preview", "like", "preview", "like"],
                "user_type": ["member"] * 4,
                "user_sex": ["unknown"] * 4,
                "user_manufacturer_type": ["ios", "android", "ios", "android"],
                "user_source": ["app"] * 4,
                "item_type": ["article"] * 4,
                "item_category_key": ["c1", "c2", "c1", "c2"],
                "item_tag_set_key": ["t1", "t2", "t3", "t4"],
                "item_duration_seconds": [0, 1, 2, 3],
                "behavior_duration_ms": [10, 20, 30, 40],
                "read_percent": [10, 20, 80, 90],
                "like_status": [0, 0, 1, 1],
                "event_key": ["e1", "e2", "e3", "e4"],
                "label": [0, 0, 1, 1],
                "dataset_split": ["train", "validation", "test", "train"],
            }
        )


if __name__ == "__main__":
    unittest.main()
