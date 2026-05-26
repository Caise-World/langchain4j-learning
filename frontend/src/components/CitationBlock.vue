<template>
  <div class="citation-block" v-if="citations.length > 0">
    <div class="citation-header">
      <span class="citation-title">引用来源</span>
      <button class="toggle-btn" @click="expanded = !expanded">
        {{ expanded ? '收起' : '展开' }}
      </button>
    </div>
    <div class="citation-list" v-show="expanded">
      <div
        v-for="citation in citations"
        :key="citation.index"
        class="citation-item"
      >
        <div class="citation-index">[{{ citation.index }}]</div>
        <div class="citation-content">{{ citation.content }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  citations: {
    type: Array,
    default: () => []
  }
})

const expanded = ref(false)
</script>

<style scoped>
.citation-block {
  margin-top: 12px;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius);
  border: 1px solid var(--border-color);
}

.citation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.citation-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.toggle-btn {
  font-size: 12px;
  color: var(--accent-color);
  padding: 4px 8px;
  border-radius: 4px;
}

.toggle-btn:hover {
  background: var(--bg-hover);
}

.citation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.citation-item {
  display: flex;
  gap: 12px;
  padding: 8px;
  background: var(--bg-primary);
  border-radius: 6px;
  font-size: 13px;
}

.citation-index {
  color: var(--accent-color);
  font-weight: 600;
  flex-shrink: 0;
}

.citation-content {
  color: var(--text-secondary);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>