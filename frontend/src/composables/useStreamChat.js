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