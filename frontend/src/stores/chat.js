import { defineStore } from 'pinia'
import { ref } from 'vue'

let idCounter = 0

function generateId() {
  return `msg_${Date.now()}_${++idCounter}`
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref([])
  const sessionId = ref(null)
  const isStreaming = ref(false)

  function addUserMessage(content) {
    messages.value.push({
      id: generateId(),
      role: 'user',
      content,
      citations: []
    })
  }

  function addAssistantMessage(content, id, citations = []) {
    messages.value.push({
      id: id || generateId(),
      role: 'assistant',
      content,
      citations
    })
  }

  function updateAssistantMessage(id, content) {
    const msg = messages.value.find(m => m.id === id)
    if (msg) {
      msg.content = content
    }
  }

  function updateAssistantCitations(id, citations) {
    const msg = messages.value.find(m => m.id === id)
    if (msg) {
      msg.citations = citations
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
    updateAssistantCitations,
    setSessionId,
    setStreaming,
    clearMessages
  }
})