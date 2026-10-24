<template>
  <div class="garbage-data-container">
    <n-card title="垃圾数据管理" class="data-card">
      <div class="total-count">
        垃圾总数: {{ garbageStore.total }}
      </div>
      <div class="filter-section">
          <div class="batch-actions" v-if="userStore.user?.role === 'admin'">
            <button 
              class="select-all-btn"
              @click="toggleSelectAll"
            >
              {{ isAllSelected ? '取消全选' : '全选' }}
            </button>
            <n-button 
              type="error" 
              @click="handleBatchDelete" 
              :disabled="selectedRows.length === 0"
            >
              批量删除 ({{ selectedRows.length }})
            </n-button>
          </div>
          <div class="export-section">
            <n-button 
              type="primary" 
              @click="exportToExcel"
              :disabled="tableData.length === 0"
            >
              导出Excel
            </n-button>
          </div>
          <div class="search-section">
            <input 
              type="text" 
              v-model="searchKeyword" 
              placeholder="搜索垃圾类型..." 
              class="search-input"
              @keyup.enter="handleSearch"
            />
            <n-button type="primary" @click="handleSearch">搜索</n-button>
            <n-button v-if="searchKeyword" @click="clearSearch">清除搜索</n-button>
          </div>
          <div class="time-range-selector">
            <h3>垃圾类型：</h3>
            <n-select
              v-model:value="filterForm.typeId"
              placeholder="请选择垃圾类型"
              :options="garbageTypeOptions"
            />
          </div>
          <div class="time-range-selector">
            <h3>选择时间范围：</h3>
            <n-date-picker v-model:value="filterForm.startTime" type="date" placeholder="开始日期" />
            <span class="separator">至</span>
            <n-date-picker v-model:value="filterForm.endTime" type="date" placeholder="结束日期" />
            <n-button type="primary" @click="applyFilter">查询</n-button>
            <n-button @click="resetFilter">重置</n-button>
          </div>
        </div>

      <div class="data-table">
        <n-data-table
          :columns="columns"
          :data="tableData"
          :loading="garbageStore.loading"
          bordered
          :pagination="false"
        >
          <template #body-cell:imageUrl="{ row }">
            <div v-if="row.processedImageUrl || row.imageUrl" class="image-cell">
              <n-image
                :src="(row.processedImageUrl || row.imageUrl).startsWith('http') ? (row.processedImageUrl || row.imageUrl) : '/api' + (row.processedImageUrl || row.imageUrl)"
                width="80"
                height="80"
                fit="cover"
                @click="showImagePreview((row.processedImageUrl || row.imageUrl).startsWith('http') ? (row.processedImageUrl || row.imageUrl) : '/api' + (row.processedImageUrl || row.imageUrl))"
                style="cursor: pointer;"
              />
            </div>
            <n-empty v-else description="无图片" />
          </template>
          <template #body-cell:confidence="{ row }">
            {{ (row.confidence * 100).toFixed(2) }}%
          </template>
        </n-data-table>
        
        <!-- Custom Pagination -->
        <div class="custom-pagination">
          <div class="pagination-controls">
            <button 
              class="pagination-btn" 
              :disabled="currentPage === 1" 
              @click="handlePageChange(currentPage - 1)"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <path d="M15.41 16.09l-4.58-4.59 4.58-4.59L14 5.5l-6 6 6 6z"/>
              </svg>
            </button>
            
            <div class="page-numbers">
              <button 
                v-for="page in pageNumbers" 
                :key="page"
                class="page-btn"
                :class="{ active: page === currentPage, ellipsis: page === '...' }"
                @click="page !== '...' && handlePageChange(Number(page))"
              >
                {{ page }}
              </button>
              <button 
                v-if="totalPages > 1" 
                class="page-btn"
                :class="{ active: totalPages === currentPage }"
                @click="handlePageChange(totalPages)"
              >
                {{ totalPages }}
              </button>
            </div>
            
            <button 
              class="pagination-btn" 
              :disabled="currentPage === totalPages" 
              @click="handlePageChange(currentPage + 1)"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <path d="M8.59 16.34l4.58-4.59-4.58-4.59L10 5.75l6 6-6 6z"/>
              </svg>
            </button>
          </div>
          
          <div class="page-size-selector">
            <select 
              v-model="pageSize" 
              @change="handlePageSizeChange(Number(pageSize))"
              class="page-size-select"
            >
              <option value="10">10/page</option>
              <option value="20">20/page</option>
              <option value="50">50/page</option>
            </select>
          </div>
        </div>
      </div>
    </n-card>

    <!-- 自定义详情弹窗 -->
    <div v-if="detailDialogVisible" class="modal-overlay" @click="closeDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">垃圾数据详情</h3>
          <button class="modal-close" @click="closeDialog">
            <svg viewBox="0 0 12 12" version="1.1" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
              <g stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
                <g fill="currentColor" fill-rule="nonzero">
                  <path d="M2.08859116,2.2156945 L2.14644661,2.14644661 C2.32001296,1.97288026 2.58943736,1.95359511 2.7843055,2.08859116 L2.85355339,2.14644661 L6,5.293 L9.14644661,2.14644661 C9.34170876,1.95118446 9.65829124,1.95118446 9.85355339,2.14644661 C10.0488155,2.34170876 10.0488155,2.65829124 9.85355339,2.85355339 L6.707,6 L9.85355339,9.14644661 C10.0271197,9.32001296 10.0464049,9.58943736 9.91140884,9.7843055 L9.85355339,9.85355339 C9.67998704,10.0271197 9.41056264,10.0464049 9.2156945,9.91140884 L9.14644661,9.85355339 L6,6.707 L2.85355339,9.85355339 C2.65829124,10.0488155 2.34170876,10.0488155 2.14644661,9.85355339 C1.95118446,9.65829124 1.95118446,9.34170876 2.14644661,9.14644661 L5.293,6 L2.14644661,2.85355339 C1.97288026,2.67998704 1.95359511,2.41056264 2.08859116,2.2156945 L2.14644661,2.14644661 L2.08859116,2.2156945 Z"></path>
                </g>
              </g>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div v-if="selectedGarbageData" class="detail-content">
            <div class="detail-item">
              <span class="detail-label">ID:</span>
              <span class="detail-value">{{ selectedGarbageData.id }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">垃圾类型:</span>
              <span class="detail-value">{{ selectedGarbageData.garbageType?.name || '未知' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">置信度:</span>
              <span class="detail-value">{{ (selectedGarbageData.confidence * 100).toFixed(2) }}%</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">纬度:</span>
              <span class="detail-value">{{ selectedGarbageData.latitude }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">经度:</span>
              <span class="detail-value">{{ selectedGarbageData.longitude }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">识别时间:</span>
              <span class="detail-value">{{ selectedGarbageData.timestamp ? new Date(selectedGarbageData.timestamp).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' }) : '' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">图片:</span>
              <div class="detail-image">
                <div v-if="selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl">
                  <img
                    :src="(selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl).startsWith('http') ? (selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl) : '/api' + (selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl)"
                    width="200"
                    height="200"
                    style="object-fit: cover; cursor: pointer;"
                    alt="垃圾图片"
                    @click="showImagePreview((selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl).startsWith('http') ? (selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl) : '/api' + (selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl))"
                    @error="handleImageError((selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl).startsWith('http') ? (selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl) : '/api' + (selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl))"
                  />
                  <div style="margin-top: 8px; font-size: 12px; color: #999;">
                    图片URL: {{ selectedGarbageData.processedImageUrl || selectedGarbageData.imageUrl }}
                  </div>
                </div>
                <div v-else class="no-image">无图片</div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="modal-button" @click="closeDialog">关闭</button>
        </div>
      </div>
    </div>

    <!-- 图片预览弹窗 -->
    <div v-if="imagePreviewVisible" class="modal-overlay" @click="closeImagePreview">
      <div class="image-preview-content" @click.stop>
        <button class="modal-close" @click="closeImagePreview" style="position: absolute; top: 10px; right: 10px; z-index: 10; background: rgba(0,0,0,0.5); color: white; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 4px;">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
          </svg>
        </button>
        
        <!-- 控制工具栏 -->
        <div class="image-controls" style="position: absolute; top: 16px; left: 16px; z-index: 10; display: flex; gap: 8px; background: rgba(0,0,0,0.5); padding: 8px; border-radius: 4px;">
          <button @click="zoomOut" style="background: white; border: none; border-radius: 4px; padding: 4px 8px; cursor: pointer;">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M15 12H9v-2h6v2z"/>
            </svg>
          </button>
          <button @click="resetZoom" style="background: white; border: none; border-radius: 4px; padding: 4px 8px; cursor: pointer;">
            1:1
          </button>
          <button @click="zoomIn" style="background: white; border: none; border-radius: 4px; padding: 4px 8px; cursor: pointer;">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
            </svg>
          </button>
        </div>
        
        <div class="image-container" @wheel="handleWheel" style="overflow: auto; max-width: 90vw; max-height: 90vh;">
          <img
            v-if="previewImageUrl"
            :src="previewImageUrl"
            :style="{ transform: `scale(${scale})`, transformOrigin: 'center center', transition: 'transform 0.2s ease' }"
            alt="预览图片"
            style="max-width: 100%; max-height: 100%; object-fit: contain;"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * GarbageData.vue - 垃圾数据管理页面组件
 * 
 * 此组件提供垃圾识别数据的综合管理功能：
 * 1. 垃圾数据列表展示（分页）
 * 2. 垃圾类型筛选
 * 3. 时间范围筛选
 * 4. 数据详情查看
 * 5. 图片预览（支持缩放）
 * 6. 批量删除（管理员）
 * 7. Excel导出
 * 
 * 技术特点：
 * - 自定义分页组件，支持页码省略显示
 * - 图片URL兼容处理（相对路径/绝对路径）
 * - 响应式数据刷新
 */

import { ref, computed, onMounted, h } from 'vue'
import { useGarbageStore } from '../stores/garbageStore'
import { useUserStore } from '../stores/userStore'
import * as XLSX from 'xlsx'

const garbageStore = useGarbageStore()
const userStore = useUserStore()

const filterForm = ref({
  typeId: null,
  startTime: null as number | null,
  endTime: null as number | null
})

// 详情对话框相关
const detailDialogVisible = ref(false)
const selectedGarbageData = ref<any>(null)

// 图片预览相关
const imagePreviewVisible = ref(false)
const previewImageUrl = ref('')
const scale = ref(1)
const scaleStep = 0.1
const minScale = 0.5
const maxScale = 3

// 批量选择相关
const selectedRows = ref<any[]>([])

// 搜索关键词
const searchKeyword = ref('')

// 确保表格数据不为 null 或 undefined
const tableData = computed(() => {
  return garbageStore.garbageDataList || []
})

// 全选状态
const isAllSelected = computed(() => {
  return tableData.value.length > 0 && selectedRows.value.length === tableData.value.length
})

// 切换全选状态
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedRows.value = []
  } else {
    selectedRows.value = [...tableData.value]
  }
}

const garbageTypeOptions = computed(() => {
  return garbageStore.garbageTypes.map(type => ({
    label: type.name,
    value: type.id
  }))
})

// 分页相关变量
const currentPage = ref(1)
const pageSize = ref(10)

// 计算总页数
const totalPages = computed(() => {
  return Math.ceil(garbageStore.total / pageSize.value)
})

// 计算显示的页码
const pageNumbers = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const pages = []
  
  // 总是显示第1页
  pages.push(1)
  
  // 当总页数大于7时，显示省略号和中间的页码
  if (total > 7) {
    // 显示当前页附近的页码
    const start = Math.max(2, current - 2)
    const end = Math.min(total - 1, current + 2)
    
    // 如果当前页前面有省略号
    if (start > 2) {
      pages.push('...')
    }
    
    // 添加中间的页码
    for (let i = start; i <= end; i++) {
      pages.push(i)
    }
    
    // 如果当前页后面有省略号
    if (end < total - 1) {
      pages.push('...')
    }
  } else {
    // 当总页数小于等于7时，显示所有页码
    for (let i = 2; i < total; i++) {
      pages.push(i)
    }
  }
  
  return pages
})

// 计算省略号前的页码
const prevEllipsisPage = computed(() => {
  return Math.max(2, currentPage.value - 3)
})

const handlePageChange = (page: number) => {
  currentPage.value = page
  // 切换页码时清空选中状态
  selectedRows.value = []
  // 跳转到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
  // 检查是否有过滤条件
  if (filterForm.value.typeId || filterForm.value.startTime || filterForm.value.endTime) {
    const formatDate = (ts: number | null) => {
      if (!ts) return null
      const d = new Date(ts)
      // 调整为中国上海时区（UTC+8）
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day} 00:00:00`
    }
    
    const formatDateEnd = (ts: number | null) => {
      if (!ts) return null
      const d = new Date(ts)
      // 调整为中国上海时区（UTC+8）
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day} 23:59:59`
    }
    
    const condition = {
      typeId: filterForm.value.typeId,
      startTime: formatDate(filterForm.value.startTime),
      endTime: formatDateEnd(filterForm.value.endTime)
    }
    garbageStore.fetchGarbageDataByCondition(condition, page, pageSize.value)
  } else {
    garbageStore.fetchGarbageData(page, pageSize.value)
  }
}

const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1 // 重置为第一页
  // 跳转到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
  // 检查是否有过滤条件
  if (filterForm.value.typeId || filterForm.value.startTime || filterForm.value.endTime) {
    const formatDate = (ts: number | null) => {
      if (!ts) return null
      const d = new Date(ts)
      // 调整为中国上海时区（UTC+8）
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day} 00:00:00`
    }
    
    const formatDateEnd = (ts: number | null) => {
      if (!ts) return null
      const d = new Date(ts)
      // 调整为中国上海时区（UTC+8）
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day} 23:59:59`
    }
    
    const condition = {
      typeId: filterForm.value.typeId,
      startTime: formatDate(filterForm.value.startTime),
      endTime: formatDateEnd(filterForm.value.endTime)
    }
    garbageStore.fetchGarbageDataByCondition(condition, 1, size)
  } else {
    garbageStore.fetchGarbageData(1, size)
  }
}

const columns = [
  {
    title: '选择',
    key: 'select',
    width: 80,
    render: (row: any) => {
      return h('input', {
        type: 'checkbox',
        checked: selectedRows.value.some(item => item.id === row.id),
        onChange: (e: any) => {
          if (e.target.checked) {
            selectedRows.value.push(row)
          } else {
            selectedRows.value = selectedRows.value.filter(item => item.id !== row.id)
          }
        },
        style: {
          margin: '0 auto',
          display: 'block'
        }
      })
    }
  },
  {
    title: 'ID',
    key: 'id'
  },
  {
    title: '垃圾类型',
    key: 'garbageType.name'
  },
  {
    title: '置信度',
    key: 'confidence'
  },
  {
    title: '纬度',
    key: 'latitude'
  },
  {
    title: '经度',
    key: 'longitude'
  },
  {
    title: '识别时间',
    key: 'timestamp',
    render: (row: any) => {
      const timestamp = row.timestamp;
      if (!timestamp) return '';
      // 直接使用中国上海时区处理时间
      const localDate = new Date(timestamp);
      return localDate.toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' });
    }
  },
  {
    title: '图片',
    key: 'imageUrl',
    width: 100
  },
  {
    title: '操作',
    key: 'action',
    render: (row: any) => {
      const buttons = [
        h('button', {
          style: {
            padding: '4px 12px',
            backgroundColor: '#1890ff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '12px'
          },
          onClick: () => viewDetail(row)
        }, '查看')
      ]
      
      // 只有管理员才能看到删除按钮
      if (userStore.user?.role === 'admin') {
        buttons.push(
          h('button', {
            style: {
              padding: '4px 12px',
              backgroundColor: '#ff4d4f',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '12px'
            },
            onClick: () => deleteData(row.id)
          }, '删除')
        )
      }
      
      return h('div', { style: { display: 'flex', gap: '8px' } }, buttons)
    }
  }
]

const applyFilter = () => {
  const formatDate = (ts: number | null) => {
    if (!ts) return null
    const d = new Date(ts)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day} 00:00:00`
  }
  
  const formatDateEnd = (ts: number | null) => {
    if (!ts) return null
    const d = new Date(ts)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day} 23:59:59`
  }
  
  const condition = {
    typeId: filterForm.value.typeId,
    startTime: formatDate(filterForm.value.startTime),
    endTime: formatDateEnd(filterForm.value.endTime)
  }
  garbageStore.fetchGarbageDataByCondition(condition, currentPage.value, pageSize.value)
}

const resetFilter = () => {
  filterForm.value = {
    typeId: null,
    startTime: null as number | null,
    endTime: null as number | null
  }
  garbageStore.fetchGarbageData(currentPage.value, pageSize.value)
}

const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    // 如果搜索关键词为空，显示所有数据
    garbageStore.fetchGarbageData(1, pageSize.value)
    currentPage.value = 1
    return
  }
  // 在所有数据中搜索关键词
  garbageStore.searchGarbageData(searchKeyword.value.trim(), 1, pageSize.value)
  currentPage.value = 1
}

const clearSearch = () => {
  searchKeyword.value = ''
  garbageStore.fetchGarbageData(1, pageSize.value)
  currentPage.value = 1
}

const viewDetail = (row: any) => {
  console.log('View detail:', row)
  selectedGarbageData.value = row
  detailDialogVisible.value = true
}

const closeDialog = () => {
  detailDialogVisible.value = false
  selectedGarbageData.value = null
}

const showImagePreview = (imageUrl: string) => {
  previewImageUrl.value = imageUrl
  imagePreviewVisible.value = true
}

const closeImagePreview = () => {
  imagePreviewVisible.value = false
  previewImageUrl.value = ''
  scale.value = 1
}

const zoomIn = () => {
  if (scale.value < maxScale) {
    scale.value += scaleStep
  }
}

const zoomOut = () => {
  if (scale.value > minScale) {
    scale.value -= scaleStep
  }
}

const resetZoom = () => {
  scale.value = 1
}

const handleWheel = (event: WheelEvent) => {
  event.preventDefault()
  if (event.deltaY > 0) {
    zoomOut()
  } else {
    zoomIn()
  }
}

const deleteData = (id: number) => {
  console.log('Delete data:', id)
  garbageStore.deleteGarbageData(id)
  // 从选中列表中移除被删除的记录
  selectedRows.value = selectedRows.value.filter(item => item.id !== id)
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) return
  
  const ids = selectedRows.value.map(row => row.id)
  console.log('Batch delete:', ids)
  await garbageStore.batchDeleteGarbageData(ids)
  selectedRows.value = []
}

const handleImageError = (imageUrl: string) => {
  console.error('Image loading failed:', imageUrl)
}

onMounted(() => {
  // 加载垃圾类型和垃圾数据
  garbageStore.fetchGarbageTypes()
  garbageStore.fetchGarbageData(currentPage.value, pageSize.value)
})

// 监听路由变化，当从其他页面返回时刷新数据
import { watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
watch(() => route.path, (newPath) => {
  if (newPath === '/garbage-data') {
    // 当路由切换到垃圾数据页面时，刷新数据
    garbageStore.fetchGarbageData(currentPage.value, pageSize.value)
  }
})

// 导出Excel功能
const exportToExcel = async () => {
  try {
    let exportData: any[]
    
    // 检查是否有过滤条件
    if (filterForm.value.typeId || filterForm.value.startTime || filterForm.value.endTime) {
      // 有过滤条件，导出符合条件的所有数据
      const formatDate = (ts: number | null) => {
        if (!ts) return null
        const d = new Date(ts)
        const year = d.getFullYear()
        const month = String(d.getMonth() + 1).padStart(2, '0')
        const day = String(d.getDate()).padStart(2, '0')
        return `${year}-${month}-${day} 00:00:00`
      }
      
      const formatDateEnd = (ts: number | null) => {
        if (!ts) return null
        const d = new Date(ts)
        const year = d.getFullYear()
        const month = String(d.getMonth() + 1).padStart(2, '0')
        const day = String(d.getDate()).padStart(2, '0')
        return `${year}-${month}-${day} 23:59:59`
      }
      
      const condition = {
        typeId: filterForm.value.typeId,
        startTime: formatDate(filterForm.value.startTime),
        endTime: formatDateEnd(filterForm.value.endTime)
      }
      
      exportData = await garbageStore.fetchAllGarbageDataByConditionForExport(condition)
    } else {
      // 没有过滤条件，导出所有数据
      exportData = await garbageStore.fetchAllGarbageDataForExport()
    }
    
    // 准备导出数据格式
    const formattedData = exportData.map((item: any) => ({
      ID: item.id,
      垃圾类型: item.garbageType?.name || '未知',
      置信度: `${(item.confidence * 100).toFixed(2)}%`,
      纬度: item.latitude,
      经度: item.longitude,
      识别时间: item.timestamp ? new Date(item.timestamp).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' }) : '',
      图片URL: item.processedImageUrl || item.imageUrl || ''
    }))
    
    // 创建工作簿
    const workbook = XLSX.utils.book_new()
    
    // 创建工作表
    const worksheet = XLSX.utils.json_to_sheet(formattedData)
    
    // 将工作表添加到工作簿
    XLSX.utils.book_append_sheet(workbook, worksheet, '垃圾数据')
    
    // 生成Excel文件并下载
    const fileName = `垃圾数据_${new Date().toISOString().slice(0, 10)}.xlsx`
    XLSX.writeFile(workbook, fileName)
    
    console.log('Excel导出成功')
  } catch (error) {
    console.error('Excel导出失败:', error)
  }
}
</script>

<style scoped>
.garbage-data-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.total-count {
  margin-bottom: 16px;
  padding: 8px 12px;
  background-color: #f0f9ff;
  border-left: 4px solid #1890ff;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  color: #1890ff;
}

.filter-section {
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.batch-actions {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.export-section {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.search-section {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  width: 200px;
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.select-all-btn {
  padding: 4px 12px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
}

.select-all-btn:hover {
  background-color: #40a9ff;
}

.time-range-selector {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.time-range-selector h3 {
  margin: 0;
  white-space: nowrap;
}

.separator {
  margin: 0 10px;
}

.data-table {
  margin-top: 20px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.detail-label {
  font-weight: bold;
  min-width: 80px;
  color: #666;
}

.detail-value {
  flex: 1;
  color: #333;
}

.detail-image {
  margin-top: 8px;
}

.no-image {
  width: 200px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  color: #999;
  border-radius: 4px;
}

/* 自定义弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
}

.modal-title {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
  color: #333;
}

.modal-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.modal-close:hover {
  color: #666;
  background-color: #f0f0f0;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #e8e8e8;
}

.modal-button {
  padding: 6px 16px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.modal-button:hover {
  background-color: #40a9ff;
}

.image-preview-content {
  background-color: white;
  border-radius: 8px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  max-width: 90vw;
  max-height: 90vh;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.image-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.image-controls button:hover {
  background-color: #f0f0f0;
}

.image-controls input[type="color"] {
  cursor: pointer;
}

/* Custom Pagination Styles */
.custom-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 12px 0;
  border-top: 1px solid #e8e8e8;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #666;
}

.pagination-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.pagination-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  border-color: #d9d9d9;
  color: #666;
}

.page-numbers {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #666;
  font-size: 14px;
  padding: 0 8px;
}

.page-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.page-btn.active {
  background: #1890ff;
  border-color: #1890ff;
  color: white;
}

.page-btn.ellipsis {
  border: none;
  cursor: default;
  color: #999;
}

.page-btn.ellipsis:hover {
  border: none;
  color: #999;
}

.page-size-selector {
  display: flex;
  align-items: center;
}

.page-size-select {
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 14px;
  cursor: pointer;
  background: white;
  transition: all 0.3s ease;
}

.page-size-select:hover {
  border-color: #1890ff;
}
</style>
