<template>
  <div class="knowledge-view">
    <header class="page-header">
      <h1>知识库上传</h1>
      <router-link to="/" class="back-link">返回对话</router-link>
    </header>

    <main class="upload-area">
      <div
        class="drop-zone"
        :class="{ 'drag-over': isDragOver }"
        @dragover.prevent="isDragOver = true"
        @dragleave.prevent="isDragOver = false"
        @drop.prevent="handleDrop"
        @click="triggerFileInput"
      >
        <input
          ref="fileInputRef"
          type="file"
          accept=".txt,.md,.pdf"
          multiple
          @change="handleFileSelect"
          style="display: none"
        />
        <div class="drop-icon">📄</div>
        <p class="drop-text">点击选择文件或拖拽文件到此处</p>
        <p class="drop-hint">支持 .txt, .md, .pdf 格式</p>
      </div>

      <div class="file-list" v-if="files.length > 0">
        <div v-for="(file, index) in files" :key="index" class="file-item">
          <span class="file-name">{{ file.name }}</span>
          <span class="file-status" :class="file.status">
            {{ file.status === 'uploading' ? '上传中...' : file.status === 'success' ? '成功' : '失败' }}
          </span>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const fileInputRef = ref(null)
const isDragOver = ref(false)
const files = ref([])

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileSelect(event) {
  const selectedFiles = Array.from(event.target.files)
  uploadFiles(selectedFiles)
}

function handleDrop(event) {
  isDragOver.value = false
  const droppedFiles = Array.from(event.dataTransfer.files)
    .filter(f => f.name.match(/\.(txt|md|pdf)$/i))
  uploadFiles(droppedFiles)
}

async function uploadFiles(filesToUpload) {
  for (const file of filesToUpload) {
    const fileObj = { name: file.name, status: 'uploading' }
    files.value.push(fileObj)

    try {
      const formData = new FormData()
      formData.append('file', file)

      const response = await fetch('/api/upload', {
        method: 'POST',
        body: formData
      })

      if (response.ok) {
        fileObj.status = 'success'
      } else {
        fileObj.status = 'failed'
      }
    } catch (e) {
      fileObj.status = 'failed'
    }
  }
}
</script>

<style scoped>
.knowledge-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
}

.page-header h1 {
  font-size: 18px;
  font-weight: 600;
}

.back-link {
  font-size: 14px;
  color: var(--accent-color);
  text-decoration: none;
}

.back-link:hover {
  text-decoration: underline;
}

.upload-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.drop-zone {
  width: 100%;
  max-width: 500px;
  padding: 48px;
  border: 2px dashed var(--border-color);
  border-radius: var(--radius);
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.drop-zone:hover, .drop-zone.drag-over {
  border-color: var(--accent-color);
  background: rgba(16, 163, 127, 0.05);
}

.drop-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.drop-text {
  font-size: 15px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.drop-hint {
  font-size: 13px;
  color: var(--text-secondary);
}

.file-list {
  width: 100%;
  max-width: 500px;
  margin-top: 24px;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-radius: 6px;
  margin-bottom: 8px;
}

.file-name {
  font-size: 14px;
  color: var(--text-primary);
}

.file-status {
  font-size: 13px;
}

.file-status.uploading {
  color: var(--text-secondary);
}

.file-status.success {
  color: var(--accent-color);
}

.file-status.failed {
  color: #dc3545;
}
</style>