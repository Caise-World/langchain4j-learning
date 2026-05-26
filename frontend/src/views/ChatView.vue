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

console.log('[ChatView] Initial messages:', messages.value.length)

async function handleSend(text) {
  const userId = generateUUID()
  console.log('[ChatView] Sending:', text, 'userId:', userId)
  store.addUserMessage(text)
  console.log('[ChatView] After user msg, messages:', messages.value.map(m => ({ id: m.id, role: m.role })))

  const assistantId = generateUUID()
  console.log('[ChatView] Creating assistant msg with id:', assistantId)
  store.addAssistantMessage('', assistantId)
  console.log('[ChatView] After assistant msg, messages:', messages.value.map(m => ({ id: m.id, role: m.role, content: m.content.substring(0, 20) })))
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
    onDone: () => {
      console.log('[ChatView] Stream done, final messages:', messages.value.map(m => ({ id: m.id, role: m.role, content: m.content.substring(0, 30) })))
      store.setStreaming(false)
    }
  })
}

function clearChat() {
  store.clearMessages()
  sessionStorage.removeItem('currentSessionId')
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