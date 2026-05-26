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