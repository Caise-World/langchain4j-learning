import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const STORAGE_KEY = 'ai-chat-data'

let idCounter = 0

function generateId() {
  return `msg_${Date.now()}_${++idCounter}`
}

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

function loadFromStorage() {
  try {
    const data = localStorage.getItem(STORAGE_KEY)
    if (data) {
      return JSON.parse(data)
    }
  } catch (e) {
    console.error('Failed to load chat data:', e)
  }
  return null
}

export const useChatStore = defineStore('chat', () => {
  const stored = loadFromStorage()

  const sessions = ref(stored?.sessions || [])
  const currentSessionId = ref(stored?.currentSessionId || null)
  const messages = ref(stored?.messages || [])
  const isStreaming = ref(false)

  function saveToStorage() {
    const data = {
      sessions: sessions.value,
      currentSessionId: currentSessionId.value,
      messages: messages.value
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  }

  watch([sessions, currentSessionId, messages], () => {
    saveToStorage()
  }, { deep: true })

  function getOrCreateSession() {
    if (!currentSessionId.value) {
      const sessionId = generateUUID()
      currentSessionId.value = sessionId
      sessions.value.push({
        id: sessionId,
        title: '新对话',
        createdAt: Date.now()
      })
    }
    return currentSessionId.value
  }

  function addUserMessage(content) {
    getOrCreateSession()
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

  function setStreaming(value) {
    isStreaming.value = value
  }

  function clearMessages() {
    messages.value = []
  }

  function clearHistory() {
    messages.value = []
    sessions.value = []
    currentSessionId.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  return {
    messages,
    sessions,
    currentSessionId,
    isStreaming,
    addUserMessage,
    addAssistantMessage,
    updateAssistantMessage,
    updateAssistantCitations,
    setStreaming,
    clearMessages,
    clearHistory
  }
})