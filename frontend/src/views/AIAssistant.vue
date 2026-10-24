<template>
  <div class="ai-assistant-container">
    <h1 class="page-title">AI 智能助手</h1>

    <div class="chat-container">
      <!-- 模型选择 -->
      <div class="model-selector">
        <label for="model-select">选择模型：</label>
        <select id="model-select" v-model="selectedModel" class="model-select">
          <option value="DeepSeek">DeepSeek</option>
          <option value="GLM-4.7-Flash">GLM-4.7-Flash</option>
        </select>
      </div>

      <!-- 聊天记录 -->
      <div class="chat-history" ref="chatHistory">
        <div v-for="(message, index) in messages" :key="index" :class="['message', message.type]">
          <!-- AI消息 -->
          <template v-if="message.type === 'ai'">
            <!-- 思考过程 -->
            <div v-if="message.reasoningContent" class="reasoning-container">
              <div class="reasoning-header" @click="message.isReasoningCollapsed = !message.isReasoningCollapsed">
                <span class="reasoning-label">🤔 思考过程</span>
                <span class="reasoning-toggle">{{ message.isReasoningCollapsed ? '展开' : '折叠' }}</span>
              </div>
              <div v-if="!message.isReasoningCollapsed" class="reasoning-content">
                {{ message.reasoningContent }}
              </div>
            </div>
            <!-- 实际回答 -->
            <div class="message-content" v-html="renderMarkdown(message.content)"></div>
          </template>
          <!-- 用户消息 -->
          <div v-else class="message-content">{{ message.content }}</div>
          <div class="message-time">{{ message.time }}</div>
        </div>
        <div v-if="isLoading" class="loading-message">
          <div class="loading-spinner"></div>
          <span>AI 正在生成回复...</span>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <input type="text" v-model="inputMessage" @keyup.enter="sendMessage" placeholder="请输入您的问题..."
          class="message-input" :disabled="isLoading" />
        <button @click="sendMessage" class="send-button" :disabled="isLoading || !inputMessage.trim()">
          发送
        </button>
      </div>
    </div>

    <!-- 管理员建议生成 -->
    <div v-if="userStore.user?.role === 'admin'" class="admin-section">
      <h2>管理员功能</h2>
      <button @click="generateSuggestions" class="suggestion-button" :disabled="isLoading">
        生成网站改进建议
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * AIAssistant.vue - AI智能助手页面组件
 * 
 * 此组件提供AI智能助手功能：
 * 1. 实时对话聊天界面
 * 2. 多模型支持（DeepSeek / GLM-4.7-Flash）
 * 3. 流式响应显示（实时打字效果）
 * 4. 思考过程展示与折叠
 * 5. Markdown格式渲染
 * 6. 管理员专属：网站改进建议生成
 * 
 * 技术特点：
 * - 使用 Server-Sent Events (SSE) 实现流式响应
 * - 聊天历史记录临时存储（重启后丢失）
 * - 自动滚动到最新消息
 */

import { ref, onMounted, watch, nextTick } from 'vue'
import { useUserStore } from '../stores/userStore'
import request from '../utils/request'
import { marked } from 'marked'

const userStore = useUserStore()
const selectedModel = ref('DeepSeek')
const inputMessage = ref('')
const messages = ref<Array<{ type: string, content: string, reasoningContent: string, time: string, isReasoningCollapsed: boolean }>>([])
const isLoading = ref(false)
const chatHistory = ref<HTMLElement | null>(null)

// 为了兼容新版 marked，在模板中这样写： v-html="renderMarkdown(message.content)"
const renderMarkdown = (text: string) => {
  if (!text) return ''

  // 用正则拦截大模型的文本
  // 将行首类似 "###🌊文字" 强制加上空格变成 "### 🌊文字"
  // ^(\s*#+) 匹配行首的一个或多个 # 号
  // ([^#\s]) 匹配紧跟着的非空格且非 # 的字符（比如 Emoji 或文字）
  // '$1 $2' 在它们中间强行插入一个空格
  let fixedText = text.replace(/^(\s*#+)([^#\s])/gm, '$1 $2')

  // 兼容不同版本的 marked，新版推荐使用 marked.parse()
  return typeof marked.parse === 'function' ? marked.parse(fixedText) : marked(fixedText)
}

// 加载历史聊天记录
const loadChatRecords = async () => {
  try {
    const records = await request.get('/ai/chat-records')
    if (records && Array.isArray(records)) {
      records.forEach((record: any) => {
        messages.value.push({
          type: 'user',
          content: record.userMessage,
          reasoningContent: '',
          time: new Date(record.createdAt).toLocaleString(),
          isReasoningCollapsed: false
        })
        messages.value.push({
          type: 'ai',
          content: record.aiResponse,
          reasoningContent: '',
          time: new Date(record.createdAt).toLocaleString(),
          isReasoningCollapsed: false
        })
      })
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载聊天记录失败:', error)
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return

  const message = inputMessage.value.trim()
  messages.value.push({
    type: 'user',
    content: message,
    reasoningContent: '',
    time: new Date().toLocaleString(),
    isReasoningCollapsed: false
  })
  inputMessage.value = ''
  scrollToBottom()

  isLoading.value = true

  try {
    // 直接使用fetch API发送请求，因为需要处理流式响应
    const token = localStorage.getItem('token')
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        model: selectedModel.value,
        message: message
      })
    })

    console.log('AI chat response status:', response.status)
    if (response.ok) {
      const reader = response.body?.getReader()
      if (reader) {
        let aiResponse = ''
        let aiReasoning = ''
        let hasStartedContent = false
        const aiMessageIndex = messages.value.length
        messages.value.push({
          type: 'ai',
          content: '',
          reasoningContent: '',
          time: new Date().toLocaleString(),
          isReasoningCollapsed: false
        })

        try {
          let buffer = ''
          const decoder = new TextDecoder('utf-8')
          while (true) {
            const { done, value } = await reader.read()
            if (done) break

            // 加上 { stream: true }，防止截断中文导致乱码
            const chunk = decoder.decode(value, { stream: true })
            buffer += chunk

            // 处理SSE格式，提取data字段
            const lines = buffer.split('\n')
            for (let i = 0; i < lines.length - 1; i++) { // 只处理完整的行，保留最后一行可能不完整的部分
              const line = lines[i]
              if (line && line.startsWith('data:')) {
                let data = line.substring(5)

                if (data === '[DONE]') {
                  continue
                }

                // 💥 核心修复 2：把后端传过来的安全标记，还原成真正的换行符
                data = data.replace(/\[NEWLINE\]/g, '\n')

                try {
                  const delta = JSON.parse(data)
                  if (delta.content !== null) {
                    // 开始生成真正的回答内容
                    aiResponse += delta.content
                    hasStartedContent = true
                    
                    // 当开始生成内容时，自动折叠思考过程
                    if (hasStartedContent && messages.value[aiMessageIndex]) {
                      messages.value[aiMessageIndex].isReasoningCollapsed = true
                    }
                  } else if (delta.reasoning_content !== null) {
                    // 生成思考过程
                    aiReasoning += delta.reasoning_content
                  }
                } catch (parseError) {
                  // 如果解析失败，当作普通内容处理
                  aiResponse += data
                }

                if (messages.value[aiMessageIndex]) {
                  // 实时更新消息内容
                  messages.value[aiMessageIndex].content = aiResponse
                  messages.value[aiMessageIndex].reasoningContent = aiReasoning
                }
              }
            }

            // 保留缓冲区中最后一行可能不完整的部分
            buffer = lines[lines.length - 1]
          }
        } catch (streamError) {
          console.error('流式响应处理失败:', streamError)
          if (messages.value[aiMessageIndex]) {
            messages.value[aiMessageIndex].content = '抱歉，接收AI响应时出现错误，请稍后重试。'
          } else {
            messages.value.push({
              type: 'ai',
              content: '抱歉，接收AI响应时出现错误，请稍后重试。',
              reasoningContent: '',
              time: new Date().toLocaleString(),
              isReasoningCollapsed: false
            })
          }
        }
      } else {
        messages.value.push({
          type: 'ai',
          content: '抱歉，AI 服务暂时不可用，请稍后重试。',
          reasoningContent: '',
          time: new Date().toLocaleString(),
          isReasoningCollapsed: false
        })
      }
    } else {
      messages.value.push({
        type: 'ai',
        content: '抱歉，AI 服务暂时不可用，请稍后重试。',
        reasoningContent: '',
        time: new Date().toLocaleString(),
        isReasoningCollapsed: false
      })
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    messages.value.push({
      type: 'ai',
      content: '抱歉，发送消息时出错，请稍后重试。',
      reasoningContent: '',
      time: new Date().toLocaleString(),
      isReasoningCollapsed: false
    })
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

// 生成建议
const generateSuggestions = async () => {
  if (isLoading.value) return

  messages.value.push({
    type: 'user',
    content: '生成网站改进建议',
    reasoningContent: '',
    time: new Date().toLocaleString(),
    isReasoningCollapsed: false
  })
  scrollToBottom()

  isLoading.value = true

  try {
    // 直接使用fetch API发送请求，因为需要处理流式响应
    const token = localStorage.getItem('token')
    console.log('Token for suggestions:', token)
    const response = await fetch('/api/ai/suggestions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        model: selectedModel.value
      })
    })

    console.log('Suggestions response status:', response.status)
    if (response.ok) {
      const reader = response.body?.getReader()
      if (reader) {
        let suggestionResponse = ''
        let suggestionReasoning = ''
        let hasStartedContent = false
        const suggestionIndex = messages.value.length
        messages.value.push({
          type: 'ai',
          content: '',
          reasoningContent: '',
          time: new Date().toLocaleString(),
          isReasoningCollapsed: false
        })

        try {
          let buffer = ''
          const decoder = new TextDecoder('utf-8')
          while (true) {
            const { done, value } = await reader.read()
            if (done) break

            const chunk = decoder.decode(value, { stream: true })
            buffer += chunk

            // 处理SSE格式，提取data字段
            const lines = buffer.split('\n')
            for (let i = 0; i < lines.length - 1; i++) { // 只处理完整的行，保留最后一行可能不完整的部分
              const line = lines[i]
              if (line && line.startsWith('data:')) {
                let data = line.substring(5)

                if (data === '[DONE]') {
                  continue
                }

                // 💥 核心修复 2：把后端传过来的安全标记，还原成真正的换行符
                // 注意：千万不要再写 if (data === '') aiResponse += '\n' 了！
                data = data.replace(/\[NEWLINE\]/g, '\n')

                try {
                  const delta = JSON.parse(data)
                  if (delta.content !== null) {
                    // 开始生成真正的回答内容
                    suggestionResponse += delta.content
                    hasStartedContent = true
                    
                    // 当开始生成内容时，自动折叠思考过程
                    if (hasStartedContent && messages.value[suggestionIndex]) {
                      messages.value[suggestionIndex].isReasoningCollapsed = true
                    }
                  } else if (delta.reasoning_content !== null) {
                    // 生成思考过程
                    suggestionReasoning += delta.reasoning_content
                  }
                } catch (parseError) {
                  // 如果解析失败，当作普通内容处理
                  suggestionResponse += data
                }

                if (messages.value[suggestionIndex]) {
                  // 实时更新消息内容
                  messages.value[suggestionIndex].content = suggestionResponse
                  messages.value[suggestionIndex].reasoningContent = suggestionReasoning
                }
              }
            }

            // 保留缓冲区中最后一行可能不完整的部分
            buffer = lines[lines.length - 1]
          }
        } catch (streamError) {
          console.error('流式响应处理失败:', streamError)
          if (messages.value[suggestionIndex]) {
            messages.value[suggestionIndex].content = '抱歉，接收AI响应时出现错误，请稍后重试。'
          } else {
            messages.value.push({
              type: 'ai',
              content: '抱歉，接收AI响应时出现错误，请稍后重试。',
              reasoningContent: '',
              time: new Date().toLocaleString(),
              isReasoningCollapsed: false
            })
          }
        }
      } else {
        messages.value.push({
          type: 'ai',
          content: '抱歉，AI 服务暂时不可用，请稍后重试。',
          reasoningContent: '',
          time: new Date().toLocaleString(),
          isReasoningCollapsed: false
        })
      }
    } else {
      messages.value.push({
        type: 'ai',
        content: '抱歉，生成建议失败，请稍后重试。',
        reasoningContent: '',
        time: new Date().toLocaleString(),
        isReasoningCollapsed: false
      })
    }
  } catch (error) {
    console.error('生成建议失败:', error)
    messages.value.push({
      type: 'ai',
      content: '抱歉，生成建议时出错，请稍后重试。',
      reasoningContent: '',
      time: new Date().toLocaleString(),
      isReasoningCollapsed: false
    })
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatHistory.value) {
      chatHistory.value.scrollTop = chatHistory.value.scrollHeight
    }
  })
}

// 监听消息变化，自动滚动到底部
watch(
  () => messages.value.map(msg => ({ content: msg.content, reasoningContent: msg.reasoningContent })),
  () => {
    scrollToBottom()
  },
  { deep: true }
)

onMounted(() => {
  loadChatRecords()
})
</script>

<style scoped>
.ai-assistant-container {
  width: 100%;
  padding: 1rem 2rem;
  background: linear-gradient(135deg, rgba(240, 242, 245, 0.8), rgba(220, 225, 230, 0.6));
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

body {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 2rem;
  color: var(--text-primary);
  text-align: center;
}

.chat-container {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 1rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 2rem;
}

.model-selector {
  padding: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  gap: 1rem;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 0.75rem;
  margin: 0.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.model-selector label {
  font-weight: 500;
  color: var(--text-primary);
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.2);
}

.model-select {
  flex: 1;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.5rem;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: var(--text-primary);
  transition: all 0.3s ease;
  appearance: none;
  background-image: url('data:image/svg+xml;charset=utf-8,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2216%22 height=%2216%22 viewBox=%220 0 16 16%22%3E%3Cpath fill=%22%23666%22 d=%22M8 11l-4-4h8l-4 4z%22/%3E%3C/svg%3E');
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-size: 16px;
}

.model-select:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 0 15px rgba(255, 255, 255, 0.1);
}

.model-select:focus {
  outline: none;
  border-color: rgba(255, 255, 255, 0.4);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.2);
}

.chat-history {
  height: 500px;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message {
  max-width: 80%;
  padding: 1rem;
  border-radius: 1rem;
  position: relative;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.message.user {
  align-self: flex-end;
  background: rgba(79, 70, 229, 0.15);
  color: var(--text-primary);
  border-bottom-right-radius: 0.25rem;
}

.message.ai {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.15);
  color: var(--text-primary);
  border-bottom-left-radius: 0.25rem;
}

.message-content {
  margin-bottom: 0.5rem;
  line-height: 1.5;
}

.message-time {
  font-size: 0.75rem;
  color: var(--text-secondary);
  text-align: right;
}

/* 思考过程样式 */
.reasoning-container {
  margin-bottom: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0.75rem;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.reasoning-header {
  padding: 0.5rem 0.75rem;
  background: rgba(255, 255, 255, 0.15);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.reasoning-header:hover {
  background: rgba(255, 255, 255, 0.2);
}

.reasoning-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.reasoning-toggle {
  font-size: 0.75rem;
  color: var(--primary-color);
  font-weight: 500;
}

.reasoning-content {
  padding: 0.75rem;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.05);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.loading-message {
  align-self: flex-start;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 1rem;
  border-bottom-left-radius: 0.25rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border-color);
  border-top: 2px solid var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

.input-area {
  display: flex;
  padding: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  gap: 1rem;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.message-input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 2rem;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: var(--text-primary);
  font-size: 1rem;
  transition: all 0.3s ease;
}

.message-input:focus {
  outline: none;
  border-color: rgba(255, 255, 255, 0.4);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.25);
}

.send-button {
  padding: 0 2rem;
  border: none;
  border-radius: 2rem;
  background: rgba(79, 70, 229, 0.8);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(79, 70, 229, 0.3);
}

.send-button:hover:not(:disabled) {
  background: rgba(79, 70, 229, 0.9);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.4);
}

.send-button:disabled {
  background: rgba(200, 200, 200, 0.6);
  cursor: not-allowed;
  box-shadow: none;
}

.admin-section {
  margin-top: 2rem;
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 1rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.suggestion-button {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  background: rgba(16, 185, 129, 0.8);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.3);
}

.suggestion-button:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.9);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
}

.suggestion-button:disabled {
  background: rgba(200, 200, 200, 0.6);
  cursor: not-allowed;
  box-shadow: none;
}

.admin-section h2 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: var(--text-primary);
}

.suggestion-button {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  background-color: var(--secondary-color);
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.suggestion-button:hover:not(:disabled) {
  background-color: var(--secondary-hover);
  transform: translateY(-1px);
}

.suggestion-button:disabled {
  background-color: var(--border-color);
  cursor: not-allowed;
}

/* Markdown 样式 */
.message-content {
  color: #333333;
  /* 深黑色文字，提高对比度 */
}

.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4),
.message-content :deep(h5),
.message-content :deep(h6) {
  margin: 1rem 0 0.75rem 0;
  /* 增加标题上下边距，确保换行 */
  font-weight: 700;
  /* 加粗标题 */
  line-height: 1.3;
  color: #222222;
  /* 更深的标题颜色 */
}

.message-content :deep(h1) {
  font-size: 1.25rem;
  margin-top: 1.3rem;
}

.message-content :deep(h2) {
  font-size: 1.15rem;
  margin-top: 1.2rem;
}

.message-content :deep(h3) {
  font-size: 1.05rem;
  margin-top: 1.1rem;
}

.message-content :deep(p) {
  margin: 0.75rem 0;
  /* 增加段落间距 */
  line-height: 1.6;
  /* 增加行高，提高可读性 */
  color: #000;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  margin: 0.75rem 0;
  padding-left: 1.75rem;
}

.message-content :deep(li) {
  margin: 0.35rem 0;
  line-height: 1.5;
}

.message-content code {
  background-color: #f1f1f1;
  padding: 0.2rem 0.4rem;
  border-radius: 0.25rem;
  font-family: monospace;
  font-size: 0.9rem;
  color: #333333;
}

.message-content pre {
  background-color: #f5f5f5;
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  margin: 0.75rem 0;
}

.message-content pre code {
  background-color: transparent;
  padding: 0;
}

.message-content blockquote {
  border-left: 4px solid var(--primary-color);
  padding-left: 1rem;
  margin: 0.75rem 0;
  color: #555555;
  font-style: italic;
}

.message-content a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.message-content a:hover {
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-assistant-container {
    padding: 1rem;
  }

  .chat-history {
    height: 400px;
  }

  .message {
    max-width: 90%;
  }
}
</style>