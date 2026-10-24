<template>
  <div class="garbage-detection-container">
    <h1 class="page-title">智能垃圾识别</h1>
    
    <!-- 上传区域 -->
    <div class="upload-section">
      <div class="upload-card">
        <h2 class="section-title">上传图片</h2>
        <div class="upload-area">
          <n-upload
            v-model:file-list="fileList"
            :multiple="false"
            :max="1"
            accept="image/*"
            @change="handleFileChange"
            class="upload-component"
          >
            <n-button type="primary" dashed>
              <template #icon>
                <n-icon>
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/>
                  </svg>
                </n-icon>
              </template>
              选择图片
            </n-button>
          </n-upload>
          
          <!-- 预览图片 -->
          <div v-if="selectedImage" class="image-preview">
            <img :src="selectedImage" alt="预览图片" class="preview-img" />
            <n-button 
              type="error" 
              size="small" 
              @click="clearImage"
              class="clear-button"
            >
              清除
            </n-button>
          </div>
        </div>
        
        <!-- 识别模式选择 -->
        <div class="mode-selection">
          <h3 class="mode-title">识别模式</h3>
          <n-radio-group v-model:value="detectionMode" name="mode">
            <n-radio value="simple">简单模式 (仅识别垃圾)</n-radio>
            <n-radio value="detailed">详细模式 (分类识别)</n-radio>
          </n-radio-group>
        </div>
        
        <!-- 识别按钮 -->
        <div class="action-buttons">
          <n-button 
            type="primary" 
            size="large" 
            @click="startDetection"
            :disabled="!selectedImage || isLoading"
            :loading="isLoading"
            class="detect-button"
          >
            {{ isLoading ? '识别中...' : '开始识别' }}
          </n-button>
        </div>
      </div>
    </div>
    
    <!-- 识别结果 -->
    <div v-if="detectionResult" class="result-section">
      <div class="result-card">
        <h2 class="section-title">识别结果</h2>
        
        <!-- 结果统计 -->
        <div class="result-stats">
          <n-statistic 
            title="识别到的垃圾数量" 
            :value="detectionResult.predictions.length" 
            class="stat-item"
          >
            <template #suffix>
              个
            </template>
          </n-statistic>
          <n-statistic
            title="模型推理时间"
            :value="detectionResult.inference_time"
            class="stat-item"
          >
            <template #suffix>
              秒
            </template>
          </n-statistic>
        </div>
        
        <!-- 识别详情 -->
        <div class="result-details">
          <n-data-table
            :columns="resultColumns"
            :data="detectionResult.predictions"
            class="result-table"
          />
        </div>
        
        <!-- 可视化结果 -->
        <div class="result-visualization">
          <div class="visualization-header">
            <h3 class="visualization-title">识别可视化</h3>
            <div class="color-picker-container">
              <span class="color-label">边框颜色：</span>
              <n-color-picker 
                v-model:value="boundingBoxColor" 
                @update:value="handleColorChange"
                size="small"
              />
            </div>
          </div>
          <div class="visualization-container">
            <img 
              :src="visualizedImageUrl || selectedImage" 
              alt="识别结果" 
              class="result-image"
              @click="openImageModal"
              style="cursor: pointer;"
            />
          </div>
        </div>
      </div>
    </div>
    
    <!-- 原生图片放大模态框 -->
    <div v-if="imageModalVisible" class="custom-modal" @click="closeImageModal">
      <div class="modal-content" @wheel="handleWheel" @click.stop @mousedown="startDrag" @mousemove="drag" @mouseup="endDrag" @mouseleave="endDrag">
        <button class="modal-close" @click="closeImageModal">&times;</button>
        <div class="image-container" :style="{ transform: `scale(${imageScale}) translate(${offsetX}px, ${offsetY}px)` }">
          <img 
            :src="visualizedImageUrl || selectedImage" 
            alt="放大的识别结果" 
            class="modal-image"
          />
        </div>
        <div class="modal-controls">
          <button class="reset-button" @click="resetScale">重置大小</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
/**
 * GarbageDetection.vue - 智能垃圾识别页面组件
 * 
 * 此组件提供海漂垃圾智能识别功能：
 * 1. 图片上传和预览
 * 2. 识别模式选择（简单模式/详细模式）
 * 3. 调用YOLO模型进行垃圾识别
 * 4. 识别结果可视化（边界框标注）
 * 5. 图片放大查看（支持缩放和拖动）
 * 6. 识别结果存储（登录用户）
 * 
 * 技术流程：
 * 1. 用户上传图片 → 2. 选择识别模式 → 3. 调用本地模型服务 → 4. 显示识别结果 → 5. 可选：保存结果
 */

import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { useUserStore } from '../stores/userStore'
import { 
  handleFileChange as handleFileChangeUtil, 
  generateVisualizedImage as generateVisualizedImageUtil,
  getGeolocation as getGeolocationUtil,
  base64ToFile,
  detectGarbage
} from '../utils/imageUtils'
import { 
  uploadImage as uploadImageApi,
  saveDetectionResult as saveDetectionResultApi,
  fetchGarbageTypes as fetchGarbageTypesApi,
  buildGarbageTypeMap
} from '../utils/apiService'

// 状态管理
const fileList = ref<any[]>([])
const selectedImage = ref<string>('')
const detectionMode = ref<string>('simple')
const isLoading = ref<boolean>(false)
const detectionResult = ref<any>(null)
const visualizedImageUrl = ref<string>('')
const boundingBoxColor = ref<string>('#FF4D4F')
const imageModalVisible = ref<boolean>(false)
const imageScale = ref<number>(1)

// 地理位置相关状态
const location = ref<{ latitude: number; longitude: number } | null>(null)
const locationError = ref<string>('')

// 垃圾类型映射
const garbageTypes = ref<any[]>([])
const garbageTypeMap = ref<Record<string, number>>({})

// 拖动相关状态
const isDragging = ref<boolean>(false)
const startX = ref<number>(0)
const startY = ref<number>(0)
const offsetX = ref<number>(0)
const offsetY = ref<number>(0)

// 消息通知
const message = useMessage()

// 用户状态管理
const userStore = useUserStore()

/**
 * 检查用户登录状态
 * @returns boolean 是否登录
 */
const checkLoginStatus = () => {
  userStore.initUser()
  return userStore.isLoggedIn
}

/**
 * 初始化组件数据
 */
const initComponent = async () => {
  await fetchGarbageTypes()
  getLocation()
}

/**
 * 获取垃圾类型列表
 */
const fetchGarbageTypes = async () => {
  try {
    const types = await fetchGarbageTypesApi()
    garbageTypes.value = types
    // 建立类型名称到typeId的映射
    garbageTypeMap.value = buildGarbageTypeMap(types)
    console.log('垃圾类型映射:', garbageTypeMap.value)
  } catch (error) {
    console.error('获取垃圾类型失败:', error)
  }
}

/**
 * 获取地理位置
 */
const getLocation = () => {
  getGeolocationUtil(
    (position) => {
      location.value = position
      locationError.value = ''
      console.log('获取地理位置成功:', location.value)
    },
    (error) => {
      locationError.value = error
      location.value = null
      console.error('获取地理位置失败:', error)
    }
  )
}

// 组件初始化时获取垃圾类型和地理位置
onMounted(() => {
  initComponent()
})

/**
 * 处理文件选择
 * @param uploadEvent 上传事件对象
 */
const handleFileChange = (uploadEvent: any) => {
  handleFileChangeUtil(
    uploadEvent,
    (fileUrl) => {
      selectedImage.value = fileUrl
      console.log('Selected image:', selectedImage.value)
    },
    () => {
      clearImage()
    }
  )
}

/**
 * 清除图片
 */
const clearImage = () => {
  fileList.value = []
  selectedImage.value = ''
  detectionResult.value = null
  visualizedImageUrl.value = ''
  imageModalVisible.value = false
}

/**
 * 打开图片模态框
 */
const openImageModal = () => {
  imageScale.value = 1
  imageModalVisible.value = true
}

/**
 * 处理滚轮缩放
 * @param event 滚轮事件
 */
const handleWheel = (event: WheelEvent) => {
  event.preventDefault()
  
  // 计算缩放因子
  const delta = event.deltaY > 0 ? 0.9 : 1.1
  
  // 更新缩放比例，限制范围在0.1到3之间
  imageScale.value = Math.max(0.1, Math.min(3, imageScale.value * delta))
}

/**
 * 开始拖动
 * @param event 鼠标事件
 */
const startDrag = (event: MouseEvent) => {
  // 只响应鼠标左键
  if (event.button === 0) {
    isDragging.value = true
    startX.value = event.clientX - offsetX.value
    startY.value = event.clientY - offsetY.value
  }
}

/**
 * 拖动中
 * @param event 鼠标事件
 */
const drag = (event: MouseEvent) => {
  if (isDragging.value) {
    offsetX.value = event.clientX - startX.value
    offsetY.value = event.clientY - startY.value
  }
}

/**
 * 结束拖动
 */
const endDrag = () => {
  isDragging.value = false
}

/**
 * 重置缩放和位置
 */
const resetScale = () => {
  imageScale.value = 1
  offsetX.value = 0
  offsetY.value = 0
}

/**
 * 关闭图片模态框
 */
const closeImageModal = () => {
  imageModalVisible.value = false
  imageScale.value = 1
  offsetX.value = 0
  offsetY.value = 0
}

/**
 * 处理颜色变化
 */
const handleColorChange = async () => {
  if (selectedImage.value && detectionResult.value) {
    visualizedImageUrl.value = await generateVisualizedImageUtil(
      selectedImage.value,
      detectionResult.value.predictions,
      boundingBoxColor.value
    )
  }
}

/**
 * 开始识别
 */
const startDetection = async () => {
  if (!selectedImage.value) {
    message.error('请先选择图片')
    return
  }
  
  isLoading.value = true
  
  try {
    // 从base64创建文件对象
    const file = base64ToFile(selectedImage.value, 'garbage.jpg')
    
    // 调用模型服务进行识别
    const resultData = await detectGarbage(file, detectionMode.value, message)
    
    if (resultData.status === 'success') {
      detectionResult.value = resultData
      
      // 生成带bounding box的图片
      visualizedImageUrl.value = await generateVisualizedImageUtil(
        selectedImage.value,
        resultData.predictions,
        boundingBoxColor.value
      )
      
      // 检查用户登录状态，只有登录用户才存储结果
      if (checkLoginStatus()) {
        try {
          // 上传原始图片和处理后的图片
          const originalImageUrl = await uploadImageApi(selectedImage.value, 'original.jpg', message)
          const processedImageUrl = await uploadImageApi(visualizedImageUrl.value, 'processed.jpg', message)
          
          // 存储识别结果
          await saveDetectionResultApi(
            originalImageUrl,
            processedImageUrl,
            resultData,
            location.value,
            garbageTypeMap.value,
            message
          )
        } catch (error) {
          console.error('处理图片和存储结果失败:', error)
          message.error('处理图片和存储结果失败')
        }
      }
    }
  } catch (error) {
    console.error('识别错误:', error)
  } finally {
    isLoading.value = false
  }
}

// 结果表格列定义
const resultColumns = [
  {
    title: '垃圾类型',
    key: 'type'
  },
  {
    title: '置信度',
    key: 'confidence',
    render: (row: any) => `${(row.confidence * 100).toFixed(2)}%`
  },
  {
    title: '位置',
    key: 'bbox',
    render: (row: any) => {
      const bbox = row.bbox
      return `(${Math.round(bbox[0])}, ${Math.round(bbox[1])}) - (${Math.round(bbox[2])}, ${Math.round(bbox[3])})`
    }
  }
]

</script>

<style scoped>
.garbage-detection-container {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  padding: 2rem 0;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-primary);
  text-align: center;
  margin-bottom: 1rem;
}

.upload-section,
.result-section {
  width: 100%;
}

.upload-card,
.result-card {
  background-color: var(--card-background);
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: var(--shadow-md);
  transition: all 0.3s ease;
}

.upload-card:hover,
.result-card:hover {
  box-shadow: var(--shadow-lg);
}

.section-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 1.5rem;
}

.upload-area {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.upload-component {
  align-self: flex-start;
}

.image-preview {
  position: relative;
  max-width: 400px;
  border-radius: 0.5rem;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.preview-img {
  width: 100%;
  height: auto;
  border-radius: 0.5rem;
}

.clear-button {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  z-index: 10;
}

.mode-selection {
  margin-bottom: 2rem;
}

.mode-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.75rem;
}

.action-buttons {
  display: flex;
  justify-content: center;
}

.detect-button {
  padding: 0.75rem 3rem;
  font-size: 1.1rem;
}

.result-stats {
  display: flex;
  gap: 2rem;
  margin-bottom: 2rem;
}

.stat-item {
  font-size: 1.25rem;
}

.result-details {
  margin-bottom: 2rem;
}

.result-table {
  width: 100%;
}

.result-visualization {
  margin-top: 2rem;
}

.visualization-header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: nowrap;
  width: 100%;
}

.color-picker-container {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 1rem;
  white-space: nowrap;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 0.5rem;
  background-color: var(--card-background);
  min-width: 200px;
}

.color-picker-container :deep(.n-color-picker) {
  width: 100%;
}

.color-picker-container :deep(.n-color-picker-trigger) {
  width: 100%;
  min-width: 120px;
}

.visualization-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0;
  flex-shrink: 0;
  margin-right: 1rem;
}

.color-label {
  font-size: 0.9rem;
  color: var(--text-secondary);
  white-space: nowrap;
  flex-shrink: 0;
}

.visualization-container {
  max-width: 600px;
  margin: 0 auto;
  border-radius: 0.5rem;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.result-image {
  width: 100%;
  height: auto;
  border-radius: 0.5rem;
}

/* 自定义模态框样式 */
.custom-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 1rem;
  position: relative;
  overflow: hidden;
  max-width: 90%;
  max-height: 90%;
  background: transparent;
  cursor: grab;
}

.modal-content:active {
  cursor: grabbing;
}

/* 隐藏滚动条 */
.modal-content::-webkit-scrollbar {
  display: none;
}

.modal-content {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.modal-close {
  position: absolute;
  top: 1rem;
  right: 1rem;
  background: transparent;
  border: none;
  color: white;
  font-size: 2rem;
  cursor: pointer;
  z-index: 10;
}

.image-container {
  transition: transform 0.2s ease;
  transform-origin: center;
  display: flex;
  justify-content: center;
  align-items: center;
  max-width: 100%;
  max-height: 80vh;
}

.modal-image {
  max-width: 100%;
  max-height: 80vh;
  border-radius: 0.5rem;
  object-fit: contain;
}

.modal-controls {
  margin-top: 1rem;
  display: flex;
  justify-content: center;
  gap: 1rem;
  background: transparent;
}

.reset-button {
  background: transparent;
  border: 1px solid white;
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.reset-button:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
  }
  
  .upload-card,
  .result-card {
    padding: 1.5rem;
  }
  
  .section-title {
    font-size: 1.25rem;
  }
  
  .image-preview {
    max-width: 100%;
  }
  
  .visualization-container {
    max-width: 100%;
  }
  
  .modal-image {
    max-height: 70vh;
  }
}
</style>