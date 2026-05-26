import { ref } from 'vue'

export function useStreamChat() {
  const isStreaming = ref(false)
  const error = ref(null)

  async function streamChat({ message, sessionId = null, modelType = null, onChunk, onMetadata, onDone }) {
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
      let currentEvent = null

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          const trimmed = line.trim()
          if (trimmed.startsWith('event:')) {
            currentEvent = trimmed.slice(6).trim()
          } else if (trimmed.startsWith('data: ')) {
            const data = trimmed.slice(6)
            if (currentEvent === 'metadata') {
              try {
                const citations = JSON.parse(data)
                onMetadata?.(citations)
              } catch (e) {
                // ignore parse error
              }
            } else if (data === '[DONE]') {
              onDone?.()
              return
            } else if (currentEvent === 'message' && data && data.trim()) {
              onChunk?.(data)
            }
          } else if (trimmed === 'data:') {
            // empty data line, skip
          } else if (trimmed.startsWith('data:')) {
            const data = trimmed.slice(5)
            if (currentEvent === 'metadata') {
              try {
                const citations = JSON.parse(data)
                onMetadata?.(citations)
              } catch (e) {
                // ignore parse error
              }
            } else if (data && data.trim()) {
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