# 推荐 DCN 训练适配器

该适配器复用真实推荐代码包的 Deep & Cross Network 结构，并将其整理为可在 XnetMLOps 中稳定运行的 CPU 训练任务。它只消费 XnetDataOps 状态为 `published` 的版本化数据产品，不读取原始业务表。

## 输入契约

- UTF-8 CSV。
- 数据产品版本唯一。
- 同时包含 `train`、`validation`、`test` 三个集合。
- 同时包含标签 `0` 和 `1`。
- Schema 摘要与制品摘要必须和 DataOps 发布记录一致。

## 运行

```powershell
$env:CUDA_VISIBLE_DEVICES = ''
$env:TORCH_DEVICE_BACKEND_AUTOLOAD = '0'
python .\mlops-mtp-service\recommendation-training\train_dcn.py `
  --input-csv 'D:\synapxnet\.data\mlops\recommendation-dcn-integration-v2.csv' `
  --output-dir 'D:\synapxnet\.data\mlops\models\recommendation-dcn-integration-v2' `
  --product-version 'recommendation-dcn-integration-v2' `
  --expected-schema-digest '<DataOps Schema SHA-256>' `
  --expected-artifact-digest '<DataOps 制品 SHA-256>'
```

输出包括 `model.pt`、`preprocessor.json`、`metrics.json` 和 `MODEL_CARD.md`。模型进入服务前必须校验模型摘要、数据产品版本、Schema 摘要和预处理契约。

## 审批晋级与服务加载

```powershell
python .\mlops-mtp-service\recommendation-training\promote_model.py `
  --candidate-dir 'D:\synapxnet\.data\mlops\models\recommendation-dcn-integration-v2' `
  --registry-root 'D:\synapxnet\.data\mlops\registry' `
  --product-version 'recommendation-dcn-integration-v2' `
  --approval-id 'APR-MEP-20260828-001' `
  --idempotency-key 'IDEM-MEP-20260828-001'

python .\mlops-mtp-service\recommendation-training\serve_dcn.py `
  --model-dir 'D:\synapxnet\.data\mlops\registry\recommendation-dcn-integration-v2' `
  --host 127.0.0.1 `
  --port 8090
```

服务提供 `GET /health` 与 `POST /predict`。它拒绝加载未审批目录、摘要不一致模型和数据产品版本不匹配的评分请求。
