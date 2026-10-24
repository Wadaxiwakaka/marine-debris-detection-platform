/**
 * 垃圾数据状态管理模块
 * 
 * 此模块使用 Pinia 管理垃圾数据相关的全局状态，包括：
 * 1. 垃圾类型管理
 * 2. 垃圾数据列表（分页）
 * 3. 统计数据（按类型、按时间范围）
 * 4. 数据删除操作（单个、批量）
 */

import { defineStore } from 'pinia'
import request from '../utils/request'

/**
 * 垃圾类型接口定义
 */
interface GarbageType {
  id: number
  name: string
  description: string
  createTime: string
}

/**
 * 垃圾数据接口定义
 */
interface GarbageData {
  id: number
  typeId: number
  confidence: number
  latitude: number
  longitude: number
  timestamp: string
  imageUrl: string
  processedImageUrl: string
  createTime: string
  garbageType: GarbageType
}

/**
 * 垃圾数据状态接口定义
 */
interface GarbageState {
  garbageTypes: GarbageType[]
  garbageDataList: GarbageData[]
  total: number
  loading: boolean
  error: string | null
  statistics: any[]
  timeRangeStatistics: any[]
  currentPage: number
  pageSize: number
}

/**
 * 创建垃圾数据状态管理 Store
 */
export const useGarbageStore = defineStore('garbage', {
  /**
   * 状态初始化
   */
  state: (): GarbageState => ({
    garbageTypes: [],
    garbageDataList: [],
    total: 0,
    loading: false,
    error: null,
    statistics: [],
    timeRangeStatistics: [],
    currentPage: 1,
    pageSize: 10
  }),

  actions: {
    /**
     * 获取垃圾类型列表
     */
    async fetchGarbageTypes() {
      this.loading = true
      this.error = null
      try {
        const response = await request.get('/garbage-type')
        this.garbageTypes = response || []
      } catch (error: any) {
        this.error = error.message || '获取垃圾类型失败'
        console.error('Fetch garbage types error:', error)
      } finally {
        this.loading = false
      }
    },

    /**
     * 获取所有垃圾数据（不分页，用于导出）
     */
    async fetchAllGarbageDataForExport() {
      this.loading = true
      this.error = null
      try {
        const response = await request.get('/garbage-data/export')
        console.log('All garbage data for export:', response)
        
        if (response && typeof response === 'object' && 'data' in response) {
          return Array.isArray(response.data) ? response.data : []
        } else {
          return Array.isArray(response) ? response : []
        }
      } catch (error: any) {
        this.error = error.message || '获取垃圾数据失败'
        console.error('Fetch all garbage data error:', error)
        return []
      } finally {
        this.loading = false
      }
    },

    /**
     * 根据条件获取所有垃圾数据（不分页，用于导出）
     * 
     * @param {any} condition - 查询条件对象
     */
    async fetchAllGarbageDataByConditionForExport(condition: any) {
      this.loading = true
      this.error = null
      try {
        const response = await request.post('/garbage-data/condition', condition)
        console.log('All garbage data by condition for export:', response)
        
        if (response && typeof response === 'object' && 'data' in response) {
          return Array.isArray(response.data) ? response.data : []
        } else {
          return Array.isArray(response) ? response : []
        }
      } catch (error: any) {
        this.error = error.message || '获取垃圾数据失败'
        console.error('Fetch all garbage data by condition error:', error)
        return []
      } finally {
        this.loading = false
      }
    },

    /**
     * 获取垃圾数据列表（分页）
     * 
     * @param {number} page - 页码（从 1 开始）
     * @param {number} pageSize - 每页数量
     */
    async fetchGarbageData(page: number = 1, pageSize: number = 10) {
      console.log('Fetching garbage data...', { page, pageSize })
      this.loading = true
      this.error = null
      try {
        this.currentPage = page
        this.pageSize = pageSize
        
        const response = await request.get('/garbage-data', {
          params: { page, pageSize }
        })
        console.log('Garbage data response:', response)
        
        // 处理后端返回的数据结构
        if (response && typeof response === 'object' && 'data' in response) {
          this.garbageDataList = Array.isArray(response.data) ? response.data : []
          this.total = response.total || this.garbageDataList.length
        } else {
          this.garbageDataList = Array.isArray(response) ? response : []
          this.total = this.garbageDataList.length
        }
        
        console.log('Garbage data list length:', this.garbageDataList.length)
        console.log('Garbage data sample:', this.garbageDataList.slice(0, 5))
        
        // 打印图片 URL 格式用于调试
        if (this.garbageDataList.length > 0) {
          const firstItem = this.garbageDataList[0]
          console.log('First item imageUrl:', firstItem.imageUrl)
          console.log('First item processedImageUrl:', firstItem.processedImageUrl)
        }
      } catch (error: any) {
        this.error = error.message || '获取垃圾数据失败'
        console.error('Fetch garbage data error:', error)
      } finally {
        this.loading = false
        console.log('Fetch garbage data completed')
      }
    },

    /**
     * 根据关键词搜索垃圾数据（分页）
     * 
     * @param {string} keyword - 搜索关键词
     * @param {number} page - 页码
     * @param {number} pageSize - 每页数量
     */
    async searchGarbageData(keyword: string, page: number = 1, pageSize: number = 10) {
      console.log('Searching garbage data...', { keyword, page, pageSize })
      this.loading = true
      this.error = null
      try {
        this.currentPage = page
        this.pageSize = pageSize
        
        const response = await request.get('/garbage-data/search', {
          params: { keyword, page, pageSize }
        })
        console.log('Search garbage data response:', response)
        
        // 处理后端返回的数据结构
        if (response && typeof response === 'object' && 'data' in response) {
          this.garbageDataList = Array.isArray(response.data) ? response.data : []
          this.total = response.total || this.garbageDataList.length
        } else {
          this.garbageDataList = Array.isArray(response) ? response : []
          this.total = this.garbageDataList.length
        }
        
        console.log('Search garbage data list length:', this.garbageDataList.length)
      } catch (error: any) {
        this.error = error.message || '搜索垃圾数据失败'
        console.error('Search garbage data error:', error)
      } finally {
        this.loading = false
        console.log('Search garbage data completed')
      }
    },

    /**
     * 根据条件查询垃圾数据（分页）
     * 
     * @param {any} condition - 查询条件对象
     * @param {number} page - 页码
     * @param {number} pageSize - 每页数量
     */
    async fetchGarbageDataByCondition(condition: any, page: number = 1, pageSize: number = 10) {
      console.log('Fetching garbage data by condition...', { condition, page, pageSize })
      this.loading = true
      this.error = null
      try {
        this.currentPage = page
        this.pageSize = pageSize
        
        const response = await request.post('/garbage-data/condition/page', condition, {
          params: { page, pageSize }
        })
        console.log('Garbage data by condition response:', response)
        
        // 处理后端返回的数据结构
        if (response && typeof response === 'object' && 'data' in response) {
          this.garbageDataList = Array.isArray(response.data) ? response.data : []
          this.total = response.total || this.garbageDataList.length
        } else {
          this.garbageDataList = Array.isArray(response) ? response : []
          this.total = this.garbageDataList.length
        }
        
        console.log('Garbage data list length:', this.garbageDataList.length)
        
        // 打印图片 URL 格式用于调试
        if (this.garbageDataList.length > 0) {
          const firstItem = this.garbageDataList[0]
          console.log('First item imageUrl:', firstItem.imageUrl)
          console.log('First item processedImageUrl:', firstItem.processedImageUrl)
        }
      } catch (error: any) {
        this.error = error.message || '获取垃圾数据失败'
        console.error('Fetch garbage data by condition error:', error)
      } finally {
        this.loading = false
        console.log('Fetch garbage data by condition completed')
      }
    },

    /**
     * 获取按垃圾类型统计的数据
     * 
     * @param {string} startTime - 开始时间
     * @param {string} endTime - 结束时间
     */
    async fetchStatisticsByType(startTime: string, endTime: string) {
      this.loading = true
      this.error = null
      try {
        console.log('开始获取垃圾类型统计数据:', { startTime, endTime })
        
        const response = await request.get('/garbage-data/statistics/type', {
          params: { startTime, endTime }
        })
        console.log('获取垃圾类型统计数据成功:', response)
        console.log('响应类型:', typeof response)
        console.log('响应结构:', JSON.stringify(response, null, 2))
        
        // 统一处理后端返回的数据结构，转换为图表所需的格式
        const raw = (response && Array.isArray(response)) ? response : []
        console.log('原始数据:', raw)
        
        // 扁平化嵌套数组
        const flattened = Array.isArray(raw) && raw.length > 0 && Array.isArray(raw[0])
          ? raw.flat()
          : raw
        console.log('扁平化后的数据:', flattened)
        
        // 标准化数据格式为 { value, name } 结构（图表库常用格式）
        const normalized = Array.isArray(flattened)
          ? flattened.map((item: any) => {
              console.log('处理统计项:', item)
              const value = typeof item.value === 'number' ? item.value
                : typeof item.count === 'number' ? item.count
                : 0
              const name = item.typeName ?? item.name ?? item.type ?? item.garbageType?.name ?? `Type ${item.typeId ?? ''}`
              console.log('处理后的统计项:', { value, name })
              return { value, name }
            })
          : []
        console.log('标准化后的数据:', normalized)
        
        this.statistics = normalized.length > 0 ? normalized : []
        console.log('最终统计数据:', this.statistics)
      } catch (error: any) {
        this.error = error.message || '获取统计数据失败'
        console.error('Fetch statistics by type error:', error)
      } finally {
        this.loading = false
      }
    },

    /**
     * 获取按时间范围统计的数据
     * 
     * @param {string} startTime - 开始时间
     * @param {string} endTime - 结束时间
     */
    async fetchStatisticsByTimeRange(startTime: string, endTime: string) {
      this.loading = true
      this.error = null
      try {
        console.log('开始获取时间范围统计数据:', { startTime, endTime })
        
        const response = await request.get('/garbage-data/statistics/time-range', {
          params: { startTime, endTime }
        })
        console.log('获取时间范围统计数据成功:', response)
        
        // 确保数据格式符合图表组件要求
        const raw = (response && Array.isArray(response)) ? response : []
        
        // 扁平化嵌套数组
        const flattened = Array.isArray(raw) && raw.length > 0 && Array.isArray(raw[0])
          ? raw.flat()
          : raw
        
        // 标准化数据格式为 { date, count } 结构
        const normalized = Array.isArray(flattened)
          ? flattened.map((item: any) => {
              const date = item.date ?? item.startTime ?? item.endTime ?? item.timestamp ?? ''
              const count = item.count ?? item.value ?? 0
              return { date, count }
            })
          : []
        
        this.timeRangeStatistics = normalized.length > 0 ? normalized : []
        console.log('最终时间范围统计数据:', this.timeRangeStatistics)
      } catch (error: any) {
        this.error = error.message || '获取时间范围统计数据失败'
        console.error('Fetch statistics by time range error:', error)
      } finally {
        this.loading = false
      }
    },

    /**
     * 删除单条垃圾数据
     * 
     * @param {number} id - 数据 ID
     */
    async deleteGarbageData(id: number) {
      this.loading = true
      this.error = null
      try {
        await request.delete(`/garbage-data/${id}`)
        
        // 删除成功后重新获取当前页数据
        if (this.currentPage && this.pageSize) {
          await this.fetchGarbageData(this.currentPage, this.pageSize)
        } else {
          // 直接从列表中移除
          this.garbageDataList = this.garbageDataList.filter(item => item.id !== id)
        }
      } catch (error: any) {
        this.error = error.message || '删除垃圾数据失败'
        console.error('Delete garbage data error:', error)
      } finally {
        this.loading = false
      }
    },

    /**
     * 批量删除垃圾数据
     * 
     * @param {number[]} ids - 数据 ID 数组
     */
    async batchDeleteGarbageData(ids: number[]) {
      this.loading = true
      this.error = null
      try {
        await request.delete('/garbage-data/batch', {
          data: { ids }
        })
        
        // 删除成功后重新获取当前页数据
        if (this.currentPage && this.pageSize) {
          await this.fetchGarbageData(this.currentPage, this.pageSize)
        } else {
          // 直接从列表中移除
          this.garbageDataList = this.garbageDataList.filter(item => !ids.includes(item.id))
        }
      } catch (error: any) {
        this.error = error.message || '批量删除垃圾数据失败'
        console.error('Batch delete garbage data error:', error)
      } finally {
        this.loading = false
      }
    }
  }
})
