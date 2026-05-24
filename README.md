# LangChain4j Learning Project

## 项目简介

基于 LangChain4j 的企业级 RAG + Chat 应用，支持本地 Ollama embedding 和多种 LLM。

## 技术栈

- **Spring Boot 3.2** + **LangChain4j 0.36**
- **Embedding**: Ollama `nomic-embed-text` (本地语义向量)
- **LLM**: MiniMax / GLM4 / OpenAI
- **向量存储**: InMemory (开发) / PgVector (生产)

## RAG 功能特性

| 特性 | 说明 |
|------|------|
| Multi-Query Retrieval | 多 query 扩展召回，提升检索覆盖率 |
| Lightweight Rerank | LLM 语义打分排序，过滤低相关 chunk |
| Answer Grounding | 防幻觉约束，禁止自由发挥 |
| 动态 topK | 根据文档量自动调整检索数量 |
| 相似度阈值 | 0.6，过滤低相关结果 |
| 句子级分块 | 200~400 字，保留语义完整性 |

## 项目结构

```
src/main/java/com/example/ai/
├── config/                      # 配置
│   └── AppConfig.java           # Bean 配置
├── controller/
│   ├── ChatController.java     # 通用聊天接口
│   ├── RagController.java       # RAG 接口
│   └── StreamingChatController.java  # SSE 流式响应
├── service/
│   ├── llm/                     # LLM 服务
│   │   ├── LlmService.java
│   │   ├── ChatMemoryService.java
│   │   └── ModelFactory.java
│   └── rag/
│       └── RagService.java      # RAG 核心逻辑
├── rag/
│   ├── TextSplitter.java        # 句子级分块
│   ├── DocumentLoader.java      # 文档加载
│   └── Document.java
└── infrastructure/
    └── embedding/
        ├── EmbeddingStore.java  # 接口
        └── InMemoryEmbeddingStore.java  # Ollama 语义 embedding
```

## API 接口

### RAG

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/rag/upload` | 上传文档并索引 |
| POST | `/api/rag/query` | RAG 问答 |
| DELETE | `/api/rag/clear` | 清空索引 |
| GET | `/api/rag/stats` | 查看统计 |

### Chat

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/chat/query` | 通用聊天 |
| POST | `/api/chat/stream` | SSE 流式聊天 |
| DELETE | `/api/chat/clear` | 清空会话 |

## 本地运行

### 1. 启动 Ollama

```bash
# 安装 Ollama
brew install ollama

# 拉取模型
ollama pull nomic-embed-text
ollama pull minimax/xxx  # 你的 LLM 模型

# 启动服务
ollama serve
```

### 2. 配置与启动

```bash
# 设置环境变量
export MINIMAX_API_KEY=your-key

# 运行
mvn spring-boot:run
```

### 3. 测试 RAG

```bash
# 上传文档
curl -X POST http://localhost:8081/api/rag/upload \
  -F "file=@path/to/your/document.txt"

# 问答
curl -X POST http://localhost:8081/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "你的问题", "topK": 3}'
```

## 开发说明

- 使用 `ConcurrentHashMap` 缓存 embedding 结果
- cosine similarity 计算修复，正确的点积公式
- chunk 按句子切分，避免语义断裂
- Rerank 使用 LLM 语义打分，避免纯向量相似度的偏差