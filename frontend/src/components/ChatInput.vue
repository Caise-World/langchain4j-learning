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