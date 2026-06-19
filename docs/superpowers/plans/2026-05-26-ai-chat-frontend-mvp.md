# AI Chat Frontend MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a minimal Vue3 + Vite frontend that can stream chat responses from the existing Spring Boot backend and render them as Markdown.

**Architecture:** Single-page Vue3 app with Vue Router for navigation. Pinia for chat state. Fetch API with ReadableStream for SSE. markdown-it for Markdown rendering. No SSR, no TypeScript (keep it simple).

**Tech Stack:** Vue 3, Vite, Vue Router, Pinia, markdown-it, highlight.js

---

## File Structure

```
frontend/
├── index.html
├── vite.config.js
├── package.json
├── src/
│   ├── main.js
│   ├── App.vue
│   ├── router/
│   │   └── index.js
│   ├── stores/
│   │   └── chat.js
│   ├── views/
│   │   └── ChatView.vue
│   ├── components/
│   │   ├── MessageBubble.vue
│   │   └── ChatInput.vue
│   ├── composables/
│   │   └── useStreamChat.js
│   └── styles/
│       └── common.css
```

---

## Task 1: Scaffold Vue3 + Vite Project

**Files:**
- Create: `frontend/index.html`
- Create: `frontend/vite.config.js`
- Create: `frontend/package.json`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/stores/chat.js`
- Create: `frontend/src/styles/common.css`

- [ ] **Step 1: Create `frontend/package.json`**

```json
{
  "name": "ai-chat-frontend",
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.0",
    "pinia": "^2.1.0",
    "markdown-it": "^14.0.0",
    "highlight.js": "^11.9.0"
  },
  "devDependencies": {
    "vite": "^5.0.0",
    "@vitejs/plugin-vue": "^5.0.0"
  }
}
```

- [ ] **Step 2: Create `frontend/vite.config.js`**

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: Create `frontend/index.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>AI Chat</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

- [ ] **Step 4: Create `frontend/src/main.js`**

```javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router/index.js'
import './styles/common.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
```

- [ ] **Step 5: Create `frontend/src/router/index.js`**

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '../views/ChatView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'chat',
      component: ChatView
    }
  ]
})

export default router
```

- [ ] **Step 6: Create `frontend/src/App.vue`**

```vue
<template>
  <router-view />
</template>

<style>
#app {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
```

- [ ] **Step 7: Create `frontend/src/styles/common.css`**

```css
:root {
  --bg-primary: #ffffff;
  --bg-secondary: #f7f7f8;
  --bg-hover: #ececec;
  --text-primary: #1a1a1a;
  --text-secondary: #666666;
  --border-color: #e5e5e5;
  --accent-color: #10a37f;
  --radius: 8px;
  --font-size: 15px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: var(--font-size);
  color: var(--text-primary);
  background: var(--bg-primary);
}

button {
  cursor: pointer;
  border: none;
  background: none;
  font-family: inherit;
  font-size: inherit;
}

input, textarea {
  font-family: inherit;
  font-size: inherit;
}
```

- [ ] **Step 8: Create `frontend/src/stores/chat.js`**

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useChatStore = defineStore('chat', () => {
  const messages = ref([])
  const sessionId = ref(null)
  const isStreaming = ref(false)

  function addUserMessage(content) {
    messages.value.push({
      id: Date.now(),
      role: 'user',
      content
    })
  }

  function addAssistantMessage(content, id) {
    messages.value.push({
      id: id || Date.now(),
      role: 'assistant',
      content
    })
  }

  function updateAssistantMessage(id, content) {
    const msg = messages.value.find(m => m.id === id)
    if (msg) {
      msg.content = content
    }
  }

  function setSessionId(id) {
    sessionId.value = id
  }

  function setStreaming(value) {
    isStreaming.value = value
  }

  function clearMessages() {
    messages.value = []
  }

  return {
    messages,
    sessionId,
    isStreaming,
    addUserMessage,
    addAssistantMessage,
    updateAssistantMessage,
    setSessionId,
    setStreaming,
    clearMessages
  }
})
```

- [ ] **Step 9: Commit**

```bash
cd /Users/wanshun/Documents/Code/langchain4j-learning
mkdir -p frontend/src/router frontend/src/stores frontend/src/views frontend/src/components frontend/src/composables frontend/src/styles
git add frontend/
git commit -m "feat: scaffold Vue3 + Vite project structure"
```

---

## Task 2: ChatInput Component

**Files:**
- Create: `frontend/src/components/ChatInput.vue`

- [ ] **Step 1: Create `frontend/src/components/ChatInput.vue`**

```vue
<template>
  <div class="chat-input-container">
    <textarea
      ref="textareaRef"
      v-model="inputText"
      class="chat-input"
      placeholder="输入问题..."
      rows="1"
      @keydown.enter.exact.prevent="handleSend"
      @input="autoResize"
    ></textarea>
    <button
      class="send-btn"
      :disabled="!inputText.trim() || loading"
      @click="handleSend"
    >
      {{ loading ? '...' : '发送' }}
    </button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['send'])

const inputText = ref('')
const textareaRef = ref(null)

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.loading) return
  emit('send', text)
  inputText.value = ''
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }
}

function autoResize() {
  const textarea = textareaRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px'
  }
}

watch(() => props.loading, (val) => {
  if (!val) {
    textareaRef.value?.focus()
  }
})
</script>

<style scoped>
.chat-input-container {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  background: var(--bg-primary);
  border-top: 1px solid var(--border-color);
}

.chat-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  resize: none;
  line-height: 1.5;
  min-height: 48px;
  max-height: 200px;
  outline: none;
  transition: border-color 0.2s;
}

.chat-input:focus {
  border-color: var(--accent-color);
}

.send-btn {
  padding: 12px 24px;
  background: var(--accent-color);
  color: white;
  border-radius: var(--radius);
  font-weight: 500;
  transition: opacity 0.2s;
  align-self: flex-end;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/ChatInput.vue
git commit -m "feat: add ChatInput component"
```

---

## Task 3: MessageBubble Component with Markdown Support

**Files:**
- Create: `frontend/src/components/MessageBubble.vue`

- [ ] **Step 1: Create `frontend/src/components/MessageBubble.vue`**

```vue
<template>
  <div :class="['message-bubble', `message-${role}`]">
    <div class="avatar">{{ role === 'user' ? '我' : 'AI' }}</div>
    <div class="content" v-html="renderedContent"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const props = defineProps({
  role: {
    type: String,
    required: true
  },
  content: {
    type: String,
    required: true
  }
})

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs code-block"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
      } catch (__) {}
    }
    return '<pre class="hljs code-block"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

const renderedContent = computed(() => {
  return md.render(props.content)
})
</script>

<style scoped>
.message-bubble {
  display: flex;
  gap: 16px;
  padding: 16px 24px;
  max-width: 100%;
}

.message-user {
  background: var(--bg-primary);
}

.message-assistant {
  background: var(--bg-secondary);
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.message-user .avatar {
  background: var(--accent-color);
  color: white;
}

.message-assistant .avatar {
  background: #ececec;
  color: var(--text-primary);
}

.content {
  flex: 1;
  line-height: 1.7;
  word-break: break-word;
}

.content :deep(p) {
  margin-bottom: 12px;
}

.content :deep(p:last-child) {
  margin-bottom: 0;
}

.content :deep(pre) {
  background: #1a1a1a;
  color: #e0e0e0;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}

.content :deep(code) {
  font-family: 'Fira Code', 'JetBrains Mono', monospace;
  font-size: 13px;
}

.content :deep(p code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
}

.content :deep(ul), .content :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
}

.content :deep(a) {
  color: var(--accent-color);
  text-decoration: none;
}

.content :deep(a:hover) {
  text-decoration: underline;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/MessageBubble.vue
git commit -m "feat: add MessageBubble with markdown-it and highlight.js"
```

---

## Task 4: useStreamChat Composable (SSE Logic)

**Files:**
- Create: `frontend/src/composables/useStreamChat.js`

- [ ] **Step 1: Create `frontend/src/composables/useStreamChat.js`**

```javascript
import { ref } from 'vue'

export function useStreamChat() {
  const isStreaming = ref(false)
  const error = ref(null)

  async function streamChat({ message, sessionId = null, modelType = null, onChunk, onDone }) {
    isStreaming.value = true
    error.value = null

    const params = new URLSearchParams({ message })
    if (sessionId) params.set('sessionId', sessionId)
    if (modelType) params.set('modelType', modelType)

    try {
      const response = await fetch(`/api/chat/stream?${params.toString()}`, {
        method: 'GET',
        headers: {
          'Accept': 'text/event-stream'
        }
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6).trim()
            if (data === '[DONE]') {
              onDone?.()
              return
            }
            if (data) {
              onChunk?.(data)
            }
          }
        }
      }

      onDone?.()
    } catch (e) {
      error.value = e.message
    } finally {
      isStreaming.value = false
    }
  }

  return {
    isStreaming,
    error,
    streamChat
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/composables/useStreamChat.js
git commit -m "feat: add useStreamChat SSE composable"
```

---

## Task 5: ChatView Page (Assembly)

**Files:**
- Create: `frontend/src/views/ChatView.vue`

- [ ] **Step 1: Create `frontend/src/views/ChatView.vue`**

```vue
<template>
  <div class="chat-view">
    <header class="chat-header">
      <h1>AI Chat</h1>
      <button class="clear-btn" @click="clearChat">清空会话</button>
    </header>

    <main class="chat-messages" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <p>你好！有什么可以帮你的吗？</p>
      </div>
      <MessageBubble
        v-for="msg in messages"
        :key="msg.id"
        :role="msg.role"
        :content="msg.content"
      />
    </main>

    <ChatInput :loading="isStreaming" @send="handleSend" />
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useChatStore } from '../stores/chat.js'
import { useStreamChat } from '../composables/useStreamChat.js'
import MessageBubble from '../components/MessageBubble.vue'
import ChatInput from '../components/ChatInput.vue'

const store = useChatStore()
const { isStreaming, streamChat } = useStreamChat()

const messagesContainer = ref(null)
const messages = store.messages

async function handleSend(text) {
  store.addUserMessage(text)

  const assistantId = Date.now()
  store.addAssistantMessage('', assistantId)
  store.setStreaming(true)

  await nextTick()
  scrollToBottom()

  let fullResponse = ''

  await streamChat({
    message: text,
    sessionId: store.sessionId,
    onChunk: (chunk) => {
      fullResponse += chunk
      store.updateAssistantMessage(assistantId, fullResponse)
      nextTick(() => scrollToBottom())
    },
    onDone: () => {
      store.setStreaming(false)
    }
  })
}

function clearChat() {
  store.clearMessages()
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
}

.chat-header h1 {
  font-size: 18px;
  font-weight: 600;
}

.clear-btn {
  padding: 8px 16px;
  border-radius: var(--radius);
  color: var(--text-secondary);
  transition: background 0.2s;
}

.clear-btn:hover {
  background: var(--bg-hover);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/ChatView.vue
git commit -m "feat: add ChatView page assembling components"
```

---

## Task 6: Install Dependencies and Verify

- [ ] **Step 1: Install dependencies**

```bash
cd frontend && npm install
```

- [ ] **Step 2: Verify dev server starts**

```bash
cd frontend && npm run dev
```

Expected: Vite dev server starts on port 5173 with proxy to localhost:8081

- [ ] **Step 3: Commit**

```bash
git add frontend/package.json frontend/package-lock.json
git commit -m "chore: add npm dependencies"
```

---

## Task 7: Verify Backend Connection

- [ ] **Step 1: Start backend**

Ensure Spring Boot is running on port 8081:
```bash
cd /Users/wanshun/Documents/Code/langchain4j-learning && mvn spring-boot:run
```

- [ ] **Step 2: Test SSE endpoint directly**

```bash
curl -s "http://localhost:8081/api/chat/stream?message=hello" \
  -H "Accept: text/event-stream"
```

Expected: SSE stream response (long-lived connection, ctrl-c to stop)

- [ ] **Step 3: Test frontend**

Open browser at http://localhost:5173, type message, verify streaming response works.

---

## Spec Coverage Check

- [ ] Chat page basic layout: Task 5 (ChatView + ChatInput)
- [ ] SSE streaming output: Task 4 (useStreamChat) + Task 5 (ChatView integration)
- [ ] Markdown rendering: Task 3 (MessageBubble with markdown-it)
- [ ] Frontend-backend integration: Task 6 (Vite proxy config) + Task 7 (verification)

No placeholder gaps detected. All steps have complete code.

---

**Plan complete and saved to `docs/superpowers/plans/2026-05-26-ai-chat-frontend-mvp.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?