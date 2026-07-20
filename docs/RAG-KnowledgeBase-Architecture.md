# RAG 知识库模块架构设计

## 1. 模块概述

RAG (Retrieval-Augmented Generation) 知识库模块是 DPP 数据处理平台的核心扩展，提供企业级知识管理和智能检索能力，支持将非结构化文档转化为可检索的知识库，并与 XAA 智能体工作流集成。

## 2. 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        RAG Knowledge Base System                     │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │
│  │ 知识库管理  │  │ 文档处理    │  │ 检索服务    │  │ 集成接口   │ │
│  │ - 创建/删除 │  │ - 文档上传  │  │ - 语义检索  │  │ - XAA集成  │ │
│  │ - 配置管理  │  │ - 文本提取  │  │ - 关键词检索│  │ - API服务  │ │
│  │ - 权限控制  │  │ - 分块处理  │  │ - 混合检索  │  │ - WebHook  │ │
│  └─────────────┘  │ - 向量化    │  │ - 重排序    │  └────────────┘ │
│                   └─────────────┘  └─────────────┘                   │
├─────────────────────────────────────────────────────────────────────┤
│                          Core Services                               │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    Document Processing Pipeline                │   │
│  │  Upload → Extract → Clean → Chunk → Embed → Index → Store     │   │
│  └──────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                          Storage Layer                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │
│  │  PostgreSQL │  │   Milvus    │  │    MinIO    │  │   Redis    │ │
│  │  元数据存储 │  │  向量存储   │  │  文件存储   │  │   缓存     │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

## 3. 数据模型

### 3.1 核心实体

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Entity Relationship                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────┐       ┌──────────────┐       ┌──────────────┐     │
│  │ KnowledgeBase│ 1───N │   Document   │ 1───N │    Chunk     │     │
│  │──────────────│       │──────────────│       │──────────────│     │
│  │ id           │       │ id           │       │ id           │     │
│  │ name         │       │ kb_id        │       │ doc_id       │     │
│  │ description  │       │ name         │       │ content      │     │
│  │ embedding_id │       │ type         │       │ embedding    │     │
│  │ vector_db    │       │ status       │       │ position     │     │
│  │ chunk_config │       │ file_path    │       │ metadata     │     │
│  │ retrieval_cfg│       │ word_count   │       │ tokens       │     │
│  │ tenant_uid   │       │ chunk_count  │       │ hash         │     │
│  └──────────────┘       └──────────────┘       └──────────────┘     │
│                                                                       │
│  ┌──────────────┐       ┌──────────────┐                            │
│  │EmbeddingModel│       │RetrievalLog  │                            │
│  │──────────────│       │──────────────│                            │
│  │ id           │       │ id           │                            │
│  │ name         │       │ kb_id        │                            │
│  │ provider     │       │ query        │                            │
│  │ model_name   │       │ results      │                            │
│  │ dimension    │       │ latency_ms   │                            │
│  │ max_tokens   │       │ user_id      │                            │
│  └──────────────┘       └──────────────┘                            │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 数据库表设计

#### 知识库表 (xnet_mlops_dpp_knowledge_base)
```sql
CREATE TABLE xnet_mlops_dpp_knowledge_base (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  uid VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(200) NOT NULL,
  description TEXT,
  
  -- 嵌入模型配置
  embedding_model_id BIGINT,
  embedding_provider VARCHAR(50),  -- openai, huggingface, local
  embedding_model VARCHAR(100),
  embedding_dimension INT DEFAULT 1536,
  
  -- 向量数据库配置
  vector_db_type VARCHAR(50) DEFAULT 'milvus',  -- milvus, chroma, pgvector
  vector_collection VARCHAR(200),
  
  -- 分块配置
  chunk_strategy VARCHAR(50) DEFAULT 'recursive',  -- fixed, recursive, semantic
  chunk_size INT DEFAULT 500,
  chunk_overlap INT DEFAULT 50,
  
  -- 检索配置
  retrieval_method VARCHAR(50) DEFAULT 'hybrid',  -- semantic, keyword, hybrid
  top_k INT DEFAULT 5,
  score_threshold DECIMAL(5,4) DEFAULT 0.5,
  rerank_enabled TINYINT(1) DEFAULT 0,
  rerank_model VARCHAR(100),
  
  -- 状态
  status VARCHAR(20) DEFAULT 'active',  -- active, indexing, error, archived
  doc_count INT DEFAULT 0,
  chunk_count INT DEFAULT 0,
  total_tokens BIGINT DEFAULT 0,
  
  -- 租户与权限
  tenant_uid VARCHAR(64),
  creator_id VARCHAR(64),
  visibility VARCHAR(20) DEFAULT 'private',  -- private, team, public
  
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_tenant (tenant_uid),
  INDEX idx_status (status)
);
```

#### 文档表 (xnet_mlops_dpp_kb_document)
```sql
CREATE TABLE xnet_mlops_dpp_kb_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  uid VARCHAR(64) NOT NULL UNIQUE,
  kb_id BIGINT NOT NULL,
  
  -- 文档信息
  name VARCHAR(500) NOT NULL,
  type VARCHAR(50),  -- pdf, docx, txt, md, html, csv
  file_path VARCHAR(1000),
  file_size BIGINT,
  file_hash VARCHAR(64),
  
  -- 处理状态
  status VARCHAR(20) DEFAULT 'pending',  -- pending, processing, completed, failed
  error_message TEXT,
  
  -- 统计信息
  word_count INT DEFAULT 0,
  chunk_count INT DEFAULT 0,
  token_count INT DEFAULT 0,
  
  -- 元数据
  metadata JSON,
  source_url VARCHAR(1000),
  
  -- 处理配置覆盖
  custom_chunk_size INT,
  custom_chunk_overlap INT,
  
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  indexed_at DATETIME,
  
  FOREIGN KEY (kb_id) REFERENCES xnet_mlops_dpp_knowledge_base(id),
  INDEX idx_kb_id (kb_id),
  INDEX idx_status (status)
);
```

#### 文档块表 (xnet_mlops_dpp_kb_chunk)
```sql
CREATE TABLE xnet_mlops_dpp_kb_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  uid VARCHAR(64) NOT NULL UNIQUE,
  doc_id BIGINT NOT NULL,
  kb_id BIGINT NOT NULL,
  
  -- 内容
  content TEXT NOT NULL,
  content_hash VARCHAR(64),
  
  -- 位置信息
  position INT,  -- 在文档中的顺序
  start_index INT,
  end_index INT,
  
  -- 向量信息
  embedding_id VARCHAR(100),  -- 向量数据库中的ID
  token_count INT,
  
  -- 层级信息 (用于 parent-child 索引)
  parent_chunk_id BIGINT,
  chunk_level INT DEFAULT 0,  -- 0=leaf, 1=parent, 2=grandparent
  
  -- 元数据
  metadata JSON,
  keywords TEXT,  -- 提取的关键词
  
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (doc_id) REFERENCES xnet_mlops_dpp_kb_document(id),
  FOREIGN KEY (kb_id) REFERENCES xnet_mlops_dpp_knowledge_base(id),
  INDEX idx_doc_id (doc_id),
  INDEX idx_kb_id (kb_id)
);
```

#### 嵌入模型表 (xnet_mlops_dpp_embedding_model)
```sql
CREATE TABLE xnet_mlops_dpp_embedding_model (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  provider VARCHAR(50) NOT NULL,  -- openai, azure, huggingface, local
  model_name VARCHAR(100) NOT NULL,
  
  -- 模型参数
  dimension INT NOT NULL,
  max_tokens INT DEFAULT 8192,
  
  -- API配置
  api_endpoint VARCHAR(500),
  api_key_encrypted VARCHAR(500),
  
  -- 状态
  is_default TINYINT(1) DEFAULT 0,
  status VARCHAR(20) DEFAULT 'active',
  
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  UNIQUE KEY uk_provider_model (provider, model_name)
);
```

#### 检索日志表 (xnet_mlops_dpp_retrieval_log)
```sql
CREATE TABLE xnet_mlops_dpp_retrieval_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  kb_id BIGINT NOT NULL,
  
  -- 查询信息
  query TEXT NOT NULL,
  query_embedding_time_ms INT,
  
  -- 检索结果
  retrieval_method VARCHAR(50),
  result_count INT,
  results JSON,  -- 检索到的chunk信息
  
  -- 性能指标
  total_latency_ms INT,
  rerank_latency_ms INT,
  
  -- 用户信息
  user_id VARCHAR(64),
  session_id VARCHAR(64),
  
  -- 反馈
  feedback_score INT,  -- 1-5
  feedback_comment TEXT,
  
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  
  INDEX idx_kb_id (kb_id),
  INDEX idx_created_at (created_at)
);
```

## 4. 文档处理流水线

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Document Processing Pipeline                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐          │
│  │ Upload  │───▶│ Extract │───▶│  Clean  │───▶│  Chunk  │          │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘          │
│       │              │              │              │                 │
│       ▼              ▼              ▼              ▼                 │
│  MinIO存储      文本提取器      文本清洗器      分块策略            │
│  - PDF          - PDF解析       - 去除噪音      - 固定大小          │
│  - Word         - Word解析      - 统一格式      - 递归分割          │
│  - Excel        - Excel解析     - 特殊字符      - 语义分割          │
│  - 其他         - HTML解析                                          │
│                                                                       │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐                         │
│  │  Embed  │───▶│  Index  │───▶│  Store  │                         │
│  └─────────┘    └─────────┘    └─────────┘                         │
│       │              │              │                                │
│       ▼              ▼              ▼                                │
│  嵌入模型        向量索引        持久化存储                         │
│  - OpenAI        - HNSW          - Milvus                           │
│  - BGE           - IVF           - PostgreSQL                       │
│  - E5            - Flat                                             │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.1 文档提取器

| 文件类型 | 提取方法 | 依赖库 |
|----------|----------|--------|
| PDF | 文本提取 + OCR | PyPDF2, pdfplumber, Tesseract |
| Word | XML解析 | python-docx |
| Excel | 表格转文本 | openpyxl, pandas |
| Markdown | 直接读取 | markdown |
| HTML | DOM解析 | BeautifulSoup |
| 纯文本 | 直接读取 | - |

### 4.2 分块策略

| 策略 | 描述 | 适用场景 |
|------|------|----------|
| fixed | 固定大小分块 | 简单文本 |
| recursive | 递归字符分割 | 通用文档 |
| semantic | 语义边界分割 | 结构化文档 |
| parent-child | 层级分块 | 需要上下文 |

## 5. 检索流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Retrieval Pipeline                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────┐    ┌─────────┐    ┌─────────────┐    ┌─────────┐      │
│  │  Query  │───▶│ Embed   │───▶│  Retrieve   │───▶│ Rerank  │      │
│  └─────────┘    └─────────┘    └─────────────┘    └─────────┘      │
│       │              │               │                  │            │
│       │              │               ▼                  │            │
│       │              │    ┌─────────────────────┐      │            │
│       │              │    │   Hybrid Search     │      │            │
│       │              │    │ ┌─────────────────┐ │      │            │
│       │              └───▶│ │ Semantic Search │ │      │            │
│       │                   │ │ (Vector)        │ │      │            │
│       │                   │ └─────────────────┘ │      │            │
│       │                   │ ┌─────────────────┐ │      │            │
│       └──────────────────▶│ │ Keyword Search  │ │      │            │
│                           │ │ (BM25)          │ │      │            │
│                           │ └─────────────────┘ │      │            │
│                           └─────────────────────┘      │            │
│                                      │                  │            │
│                                      ▼                  ▼            │
│                           ┌─────────────────┐   ┌─────────────┐    │
│                           │  Score Fusion   │──▶│   Top-K     │    │
│                           │  (RRF/Weighted) │   │   Results   │    │
│                           └─────────────────┘   └─────────────┘    │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.1 检索方法

| 方法 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| semantic | 向量相似度检索 | 理解语义 | 可能漏掉精确匹配 |
| keyword | BM25关键词检索 | 精确匹配 | 不理解同义词 |
| hybrid | 混合检索 | 综合优势 | 需要调参 |

### 5.2 重排序

支持的重排序模型：
- Cohere Rerank
- BGE Reranker
- Jina Reranker
- 自定义模型

## 6. 前端页面结构

```
DPP/
└── KnowledgeBase/
    ├── index.vue                 # 知识库列表页
    ├── create.vue                # 创建知识库
    ├── detail.vue                # 知识库详情
    ├── documents.vue             # 文档管理页
    ├── settings.vue              # 知识库设置
    ├── retrieval-test.vue        # 检索测试页
    └── components/
        ├── KnowledgeBaseCard.vue # 知识库卡片
        ├── DocumentList.vue      # 文档列表
        ├── DocumentUpload.vue    # 文档上传
        ├── ChunkViewer.vue       # 分块查看器
        ├── RetrievalTester.vue   # 检索测试器
        ├── ConfigPanel.vue       # 配置面板
        └── StatsChart.vue        # 统计图表
```

## 7. API 接口设计

### 7.1 知识库管理 API

```
POST   /api/dpp/knowledge-bases              # 创建知识库
GET    /api/dpp/knowledge-bases              # 获取知识库列表
GET    /api/dpp/knowledge-bases/{id}         # 获取知识库详情
PUT    /api/dpp/knowledge-bases/{id}         # 更新知识库
DELETE /api/dpp/knowledge-bases/{id}         # 删除知识库
POST   /api/dpp/knowledge-bases/{id}/rebuild # 重建索引
```

### 7.2 文档管理 API

```
POST   /api/dpp/knowledge-bases/{id}/documents          # 上传文档
GET    /api/dpp/knowledge-bases/{id}/documents          # 获取文档列表
GET    /api/dpp/knowledge-bases/{id}/documents/{docId}  # 获取文档详情
DELETE /api/dpp/knowledge-bases/{id}/documents/{docId}  # 删除文档
POST   /api/dpp/knowledge-bases/{id}/documents/{docId}/reindex # 重新索引
GET    /api/dpp/knowledge-bases/{id}/documents/{docId}/chunks  # 获取分块
```

### 7.3 检索 API

```
POST   /api/dpp/knowledge-bases/{id}/retrieve           # 检索
POST   /api/dpp/knowledge-bases/{id}/retrieve/test      # 检索测试
GET    /api/dpp/knowledge-bases/{id}/retrieval-logs     # 检索日志
```

### 7.4 嵌入模型 API

```
GET    /api/dpp/embedding-models                        # 获取模型列表
POST   /api/dpp/embedding-models                        # 添加模型
PUT    /api/dpp/embedding-models/{id}                   # 更新模型
DELETE /api/dpp/embedding-models/{id}                   # 删除模型
POST   /api/dpp/embedding-models/{id}/test              # 测试模型
```

## 8. 与 XAA 工作流集成

### 8.1 RAG 节点类型

在 XAA 工作流中添加 RAG 相关节点：

| 节点类型 | 功能 | 输入 | 输出 |
|----------|------|------|------|
| kb-retrieve | 知识检索 | query, kb_id, top_k | chunks[] |
| kb-qa | 知识问答 | question, kb_id | answer, sources[] |
| kb-search | 多库搜索 | query, kb_ids[] | results[] |

### 8.2 集成方式

```yaml
# 工作流节点配置示例
- id: retrieve-knowledge
  type: kb-retrieve
  config:
    knowledge_base_id: "KB-xxx"
    top_k: 5
    retrieval_method: hybrid
    score_threshold: 0.6
    rerank: true
```

## 9. 部署架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                       Production Deployment                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐                │
│  │   Nginx    │───▶│  Gateway   │───▶│  DPP RAG   │                │
│  │   (LB)     │    │  Service   │    │  Service   │                │
│  └────────────┘    └────────────┘    └────────────┘                │
│                                             │                        │
│                    ┌────────────────────────┼────────────────────┐  │
│                    │                        │                    │  │
│                    ▼                        ▼                    ▼  │
│             ┌────────────┐          ┌────────────┐        ┌───────┐│
│             │ PostgreSQL │          │   Milvus   │        │ MinIO ││
│             │  (Metadata)│          │  (Vector)  │        │(Files)││
│             └────────────┘          └────────────┘        └───────┘│
│                                                                       │
│             ┌────────────┐          ┌────────────┐                  │
│             │   Redis    │          │  Celery    │                  │
│             │  (Cache)   │          │  Workers   │                  │
│             └────────────┘          └────────────┘                  │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

## 10. 实施路线图

### Phase 1: 基础功能 (2周)
- [ ] 数据库表创建
- [ ] 知识库 CRUD
- [ ] 文档上传和存储
- [ ] 基础文本提取 (PDF, Word, TXT)

### Phase 2: 核心能力 (2周)
- [ ] 分块处理
- [ ] 嵌入生成
- [ ] Milvus 集成
- [ ] 基础检索功能

### Phase 3: 高级特性 (2周)
- [ ] 混合检索
- [ ] 重排序
- [ ] 检索测试界面
- [ ] 性能优化

### Phase 4: 集成与优化 (1周)
- [ ] XAA 工作流集成
- [ ] 监控告警
- [ ] 文档完善
