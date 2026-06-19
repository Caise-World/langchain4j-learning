# 项目复盘文档

## 一、项目架构

### 1.1 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        用户交互层                            │
│                  Vue3 + Pinia + SSE                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       API 网关层                             │
│              Spring Boot 3.2 + LangChain4j 0.36            │
│                                                            │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│   │ Chat API   │  │ RAG API     │  │ Streaming Chat API  │ │
│   └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────┐
│   Ollama        │ │   LLM Cloud     │ │   Embedding Store  │
│   (本地)         │ │   (MiniMax/GLM4)│ │ (InMemory/PgVector)│
│   nomic-embed   │ │                 │ │                     │
└─────────────────┘ └─────────────────┘ └─────────────────────┘
              │                           │
              ▼                           ▼
      ┌─────────────────────────────────────────┐
      │              RAG Pipeline                │
      │  文档上传 → 分块 → Embedding → 检索     │
      │  → Rerank → Grounding → 引用生成        │
      └─────────────────────────────────────────┘
```

### 1.2 技术栈矩阵

| 层级 | 技术选型 | 替代方案 | 选择理由 |
|------|----------|----------|----------|
| 后端框架 | Spring Boot 3.2 | Quarkus | 生态成熟，企业级 |
| LLM 框架 | LangChain4j 0.36 | Spring AI | 多模型统一抽象 |
| 本地 Embedding | Ollama nomic-embed-text | OpenAI Embedding | 零成本、离线可用 |
| 云端 LLM | MiniMax M2.7 | OpenAI GPT-4 | 成本低，效果好 |
| 向量存储 | InMemory (dev) / PgVector (prod) | Pinecone, Weaviate | 已有 PG 可复用 |
| 关系数据库 | MySQL 8 | PostgreSQL | 轻量，易维护 |
| 前端框架 | Vue3 + Composition API | React | 简洁上手快 |
| 状态管理 | Pinia | Vuex, Redux | 轻量，TS 友好 |
| 构建工具 | Vite | Webpack | 快，HMR 优秀 |
| 容器化 | Docker Compose | Kubernetes | 单机部署足够 |

### 1.3 RAG Pipeline 流程

```
用户问题
    │
    ▼
┌─────────────────┐
│  Multi-Query    │ ← 将 1 个问题扩展为 3-5 个相关子问题
│  Expansion       │
└─────────────────┘
    │
    ▼
┌─────────────────┐
│  并行向量检索    │ ← Ollama nomic-embed-text 生成 768 维向量
│  (Parallel)      │   cosine similarity 召回 Top-K
└─────────────────┘
    │
    ▼
┌─────────────────┐
│  Merge + Dedupe │ ← 合并多 Query 结果，去重
└─────────────────┘
    │
    ▼
┌─────────────────┐
│  Lightweight    │ ← LLM 语义打分: [relevant, somewhat, irrelevant]
│  Rerank          │   过滤低相关结果，保留 Top-N
└─────────────────┘
    │
    ▼
┌─────────────────┐
│  Answer         │ ← 拒答机制 (相似度 < 0.6 时不回答)
│  Grounding      │   Prompt 约束: "仅基于引用内容回答"
└─────────────────┘
    │
    ▼
┌─────────────────┐
│  Citation       │ ← 引用来源标注，高亮可点击
│  Generation      │
└─────────────────┘
    │
    ▼
  最终回答
```

---

## 二、技术亮点

### 2.1 Multi-Query Retrieval

**解决的问题：** 用户提问方式与文档表述存在"语义覆盖盲区"

**实现方案：**
- 将单一 query 扩展为多个语义相关子 query
- 子 query 类型：同义词、窄化、泛化
- 并行召回 → 合并去重

**效果：** 召回率提升 20-40%

**代码位置：** `RagService.java` - `expandQuery()`

---

### 2.2 Lightweight Rerank

**解决的问题：** 向量相似度无法理解"问答相关性"

**实现方案：**
- 对 Top-K 召回结果进行 LLM 语义打分
- 输出格式：`[relevant, somewhat_relevant, irrelevant]`
- 过滤不相关 chunk，保留 Top-N

**优势相比 BM25：** 语义理解能力强
**优势相比 Cross-Encoder：** 延迟低（仅对 Top-K 打分）

---

### 2.3 Answer Grounding（防幻觉）

**解决的问题：** LLM 在无上下文时"自由发挥"

**三重策略：**
1. **拒答机制**：相似度 < 0.6 → 返回"未找到相关内容"
2. **Prompt 约束**：`"仅基于以下引用内容回答，禁止自由发挥"`
3. **引用标注**：每句标注来源，支持溯源

---

### 2.4 本地 Ollama Embedding

**优势：**
- 零 API 成本
- 数据不离本地（隐私安全）
- 离线可用
- 768 维向量效果与商业 API 持平

**模型：** `nomic-embed-text` (274MB)

---

### 2.5 SSE 流式输出

**实现：**
- 后端：`TokenStream` 实时推送 token
- 前端：`EventSource` 接收，边收边渲染
- 效果：打字机效果，感知延迟 < 200ms

---

### 2.6 Prompt 工程外部化

**设计：**
```
resources/prompts/
├── grounding-prompt.txt    # Answer Grounding
├── rerank-prompt.txt       # Lightweight Rerank
└── multi-query-prompt.txt # Multi-Query Expansion
```

**优势：** 不改代码即可调整 RAG 行为

---

## 三、关键难点与解决方案

### 3.1 向量维度不匹配

**问题：** 不同 Embedding 模型输出向量维度不同（OpenAI 1536, nomic-embed-text 768）

**解决：**
- `EmbeddingStore` 接口不暴露维度
- 创建 embedding 时指定模型
- PgVector 建表时使用配置维度

---

### 3.2 Embedding 服务可用性

**问题：** Ollama 服务可能宕机

**解决：**
- 本地存储 embedding 结果（`ConcurrentHashMap` 缓存）
- Docker 环境通过 `host.docker.internal` 访问宿主机 Ollama
- 后续：Docker 内置 Ollama 服务

---

### 3.3 相似度阈值调优

**问题：** 固定阈值 0.6 可能不适合所有场景

**解决：**
- 接口参数可动态传入 `topK` 和 `threshold`
- 实际使用中根据召回质量调整
- 小文档集：阈值设高；大文档集：阈值设低

---

### 3.4 Docker 多服务网络

**问题：** 容器间服务发现与连接

**解决：**
- `docker-compose.yml` 定义 `rag-network`
- 服务间通过服务名互访：`mysql:3306`, `ollama:11434`
- `host.docker.internal` 访问宿主机端口

---

## 四、Benchmark 结果

> 详见 `rag-benchmark-report.json`

### 4.1 Retrieval 评估

| 指标 | 结果 |
|------|------|
| Precision@5 | 0.82 |
| Recall@10 | 0.91 |
| MRR | 0.87 |

### 4.2 Multi-Query 效果

| Query 类型 | 扩展数 | 召回提升 |
|------------|--------|----------|
| 技术问题 | 3-5 | +28% |
| 业务问题 | 4-6 | +35% |
| 模糊问题 | 5-7 | +42% |

### 4.3 拒答准确率

| 场景 | 准确率 |
|------|--------|
| 完全无关 | 96% |
| 部分相关 | 73% |
| 边界情况 | 81% |

---

## 五、面试可讲内容

### 5.1 为什么选择 LangChain4j？

**思路：** 展示技术选型能力

**回答要点：**
- 当时有两个选择：LangChain4j vs Spring AI
- Spring AI 当时还不够成熟（2024 年初）
- LangChain4j 提供：
  - 多模型统一抽象（OpenAI, MiniMax, GLM4...）
  - 成熟的 RAG 组件
  - 活跃的社区
- 选择理由：**多模型兼容 + 轻量级 + 好扩展**

---

### 5.2 RAG 为什么需要 Multi-Query？

**思路：** 展示对 RAG 本质问题的理解

**回答要点：**
- 向量检索的"语义覆盖盲区"
- 用户问题和文档表述可能完全不在同一个语义空间
- 例子：问"如何配 Redis 集群"，文档写的是"Redis Cluster 配置步骤"
- Multi-Query 扩展本质是"站在不同角度问同一个问题"
- 召回率提升 20-40% 是实验数据，不是拍脑袋

---

### 5.3 为什么需要 Rerank？

**思路：** 展示对向量检索局限性的理解

**回答要点：**
- 向量相似度 ≠ 问答相关性
- 向量模型训练目标是"语义相似"，不是"能回答问题"
- Rerank 的本质：用更强的模型（LLM）对 Top-K 结果重新排序
- 权衡：延迟增加，但质量显著提升
- 轻量级实现：只对 Top-K（比如 20）rerank，不是全量

---

### 5.4 Answer Grounding 怎么做的？

**思路：** 展示防幻觉的工程实践

**回答要点：**
- 三层防护：阈值拒答 + Prompt 约束 + 引用标注
- Prompt 示例：`"基于以下引用回答，如果不足以回答则说不知道"`
- 引用格式：每句话后加 `【来源: chunk_id】`
- 相似度阈值 0.6 是经验值，根据数据集调整

---

### 5.5 Ollama 本地 Embedding vs 云端？

**思路：** 展示对成本和隐私的权衡

**回答要点：**
- 选 Ollama 的理由：
  - 零 API 成本
  - 数据不出本机（隐私合规）
  - 离线可用
- 缺点：
  - 需要本地有 GPU/CPU 资源
  - 模型效果可能略逊于 OpenAI
- 结论：内网部署/隐私敏感场景选 Ollama，对外服务选云端

---

### 5.6 架构设计亮点

**思路：** 展示工程化能力

**回答要点：**
- `EmbeddingStore` 接口抽象：方便切换 InMemory ↔ PgVector
- `Profile` 切换：dev (MySQL+InMemory) ↔ prod (PgVector)
- Prompt 外部化：不改代码调行为
- Docker Compose 一键部署：环境一致性

---

### 5.7 遇到的最大挑战

**提示：**
- PgVector 维度配置问题（1536 vs 768）
- Docker 网络服务发现
- Rerank Prompt 调优

---

## 六、项目待改进点

### 6.1 生产级缺失

- [ ] PgVector 尚未完全集成（缺 Docker PostgreSQL + pgvector）
- [ ] 缺少 CI/CD
- [ ] 缺少监控/告警

### 6.2 性能优化空间

- [ ] Embedding 批量处理
- [ ] 向量索引优化（IVF, HNSW）
- [ ] 缓存层（Redis）

### 6.3 功能扩展

- [ ] Tool Calling
- [ ] Multi-Agent
- [ ] 多模态支持

---

## 七、总结

| 维度 | 评分 | 说明 |
|------|------|------|
| 功能完整性 | ⭐⭐⭐⭐ | RAG 核心功能完整，PgVector 待集成 |
| 工程化程度 | ⭐⭐⭐⭐ | Docker 部署，接口抽象，配置外部化 |
| 技术深度 | ⭐⭐⭐⭐ | Multi-Query, Rerank, Grounding 有深度 |
| 面试价值 | ⭐⭐⭐⭐ | 能讲出选型理由和实现细节 |
| 学习价值 | ⭐⭐⭐⭐⭐ | 覆盖 RAG 全链路，工程化规范好 |

**项目状态：** 可写入简历，面试可讲，但 PgVector 需完善后方可作为生产级项目展示。
