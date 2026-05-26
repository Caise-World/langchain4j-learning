<template>
  <div class="chat-view">
    <header class="chat-header">
      <h1>AI Chat</h1>
      <div class="header-actions">
        <router-link to="/knowledge" class="nav-link">知识库</router-link>
        <button class="clear-btn" @click="clearChat">清空会话</button>
      </div>
    </header>

    <main class="chat-messages" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <p>你好！有什么可以帮你的吗？</p>
      </div>
      <div v-for="msg in messages" :key="msg.id" class="message-wrapper">
        <MessageBubble :role="msg.role" :content="msg.content" />
        <CitationBlock
          v-if="msg.role === 'assistant' && msg.citations.length > 0"
          :citations="msg.citations"
        />
      </div>
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
import CitationBlock from '../components/CitationBlock.vue'

const store = useChatStore()
const { isStreaming, streamChat } = useStreamChat()

const messagesContainer = ref(null)
const messages = store.messages

async function handleSend(text) {
  store.addUserMessage(text)

  const assistantId = generateUUID()
  store.addAssistantMessage('', assistantId, [])
  store.setStreaming(true)

  await nextTick()
  scrollToBottom()

  let fullResponse = ''

  await streamChat({
    message: text,
    sessionId: store.sessionId,
    onChunk: (chunk) => {
      if (!chunk || chunk.trim() === '') return
      fullResponse += chunk
      store.updateAssistantMessage(assistantId, fullResponse)
      nextTick(() => scrollToBottom())
    },
    onMetadata: (citations) => {
      store.updateAssistantCitations(assistantId, citations)
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

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
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

.nav-link {
  padding: 8px 16px;
  color: var(--accent-color);
  text-decoration: none;
  border-radius: var(--radius);
}

.nav-link:hover {
  background: var(--bg-hover);
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
}
</style>