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