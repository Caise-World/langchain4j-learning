# LangChain4j Learning Project

## Project Structure

```
langchain4j-learning/
├── pom.xml
├── src/main/java/com/example/ai/
│   ├── LangChain4jApplication.java    # Spring Boot entry point
│   ├── config/
│   │   └── LangChain4jConfig.java     # LLM configuration
│   ├── service/
│   │   └── LlmService.java             # LLM service wrapper
│   └── controller/
│       └── AiController.java           # REST API endpoint
└── src/main/resources/
    └── application.yml                 # Configuration
```

## Run Instructions

### 1. Prerequisites
- JDK 17+
- Maven 3.8+
- OpenAI API key (or use Ollama for local models)

### 2. Configure API Key

**Option A: Environment Variable (Recommended)**
```bash
export OPENAI_API_KEY=sk-your-key-here
```

**Option B: Edit application.yml**
Edit `src/main/resources/application.yml` and replace `your-api-key-here` with your actual key.

### 3. Build & Run

```bash
cd langchain4j-learning
mvn spring-boot:run
```

### 4. Test

```bash
curl "http://localhost:8080/api/ai/chat?message=Hello, what is Java?"
```

## API Endpoint

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/ai/chat?message=YOUR_MESSAGE` | Send message to LLM |

## Next Steps to Expand

1. **Add Chat Memory** - Maintain conversation history
2. **Implement RAG** - Retrieval Augmented Generation
3. **Add Tool/Function Calling** - Give AI abilities to call external APIs
4. **Switch to Ollama** - Use local models for privacy/cost savings
5. **Add Streaming** - Stream responses for better UX
