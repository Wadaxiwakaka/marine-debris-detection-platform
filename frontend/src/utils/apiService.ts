/**
 * API服务模块
 * 
 * 此模块封装了与后端API交互的核心业务逻辑，包括：
 * 1. 图片上传服务
 * 2. 识别结果存储服务
 * 3. 垃圾类型数据获取服务
 */

import request from './request'
import { base64ToFile } from './imageUtils'

/**
 * 上传图片到服务器
 * 
 * @param {string} imageData - base64格式的图片数据（包含 data:image/xxx;base64, 前缀）
 * @param {string} filename - 文件名
 * @param {any} message - Naive UI 消息通知实例
 * @returns {Promise<string>} 上传后的图片路径
 */
export async function uploadImage(
  imageData: string,
  filename: string,
  message: any
): Promise<string> {
  try {
    console.log('开始上传图片:', filename)
    console.log('图片数据长度:', imageData.length)
    
    // 从base64创建File对象
    const file = base64ToFile(imageData, filename)
    console.log('File对象:', file)
    
    // 创建FormData
    const formData = new FormData()
    formData.append('file', file)
    console.log('FormData:', formData)
    
    // 调用上传接口
    console.log('开始调用上传接口...')
    const uploadResponse = await request.post('/upload/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    console.log('上传响应:', uploadResponse)
    console.log('图片上传成功:', uploadResponse)
    // 假设响应中包含图片URL，根据实际接口返回结构调整
    return typeof uploadResponse === 'string' ? uploadResponse : (uploadResponse.data || '')
  } catch (error) {
    console.error('上传图片失败:', error)
    message.error('上传图片失败')
    throw error
  }
}

/**
 * 存储识别结果到数据库
 * 
 * 将垃圾识别结果批量存储到后端数据库，每个识别到的目标都会创建一条记录。
 * 
 * @param {string} originalImageUrl - 原始图片URL
 * @param {string} processedImageUrl - 处理后的图片URL（带边界框）
 * @param {any} detectionResult - 识别结果对象，包含 predictions 数组
 * @param {{ latitude: number; longitude: number } | null} location - 地理位置信息
 * @param {Record<string, number>} garbageTypeMap - 垃圾类型名称到ID的映射
 * @param {any} message - Naive UI 消息通知实例
 * @returns {Promise<void>}
 */
export async function saveDetectionResult(
  originalImageUrl: string,
  processedImageUrl: string,
  detectionResult: any,
  location: { latitude: number; longitude: number } | null,
  garbageTypeMap: Record<string, number>,
  message: any
): Promise<void> {
  try {
    if (!detectionResult || !detectionResult.predictions) {
      throw new Error('没有识别结果')
    }
    
    // 准备存储数据
    const savePromises = detectionResult.predictions.map(async (pred: any) => {
      const typeId = garbageTypeMap[pred.type] || 0 // 默认类型ID为0（垃圾）
      
      const garbageData = {
        typeId,
        confidence: pred.confidence,
        latitude: location?.latitude || 0, // 使用获取的地理位置或默认值
        longitude: location?.longitude || 0, // 使用获取的地理位置或默认值
        timestamp: new Date().toISOString(),
        imageUrl: originalImageUrl,
        processedImageUrl
      }
      
      // 调用存储接口
      const response = await request.post('/garbage-data', garbageData)
      return response
    })
    
    // 等待所有存储请求完成
    await Promise.all(savePromises)
    message.success('识别结果已成功存储')
    
    // 触发垃圾数据页面的刷新
    // 这里可以通过事件总线或其他方式通知垃圾数据页面刷新
    // 简单起见，我们可以直接调用垃圾存储的fetch方法
    const { useGarbageStore } = await import('../stores/garbageStore')
    const garbageStore = useGarbageStore()
    await garbageStore.fetchGarbageData(1, 10) // 刷新第一页数据
  } catch (error) {
    console.error('存储识别结果失败:', error)
    message.error('存储识别结果失败')
    throw error
  }
}

/**
 * 获取垃圾类型列表
 * 
 * 从后端获取所有垃圾类型定义，用于识别结果的类型映射。
 * 
 * @returns {Promise<any[]>} 垃圾类型列表，每个类型包含 id、name、description 等字段
 */
export async function fetchGarbageTypes(): Promise<any[]> {
  try {
    const response = await request.get('/garbage-type')
    // 处理响应数据，确保是数组
    const data = Array.isArray(response) ? response : (response.data || [])
    return data
  } catch (error) {
    console.error('获取垃圾类型失败:', error)
    return []
  }
}

/**
 * 构建垃圾类型映射
 * 
 * 将垃圾类型列表转换为名称到ID的映射对象，便于快速查找。
 * 
 * @param {any[]} garbageTypes - 垃圾类型列表
 * @returns {Record<string, number>} 垃圾类型名称到ID的映射对象
 */
export function buildGarbageTypeMap(garbageTypes: any[]): Record<string, number> {
  return garbageTypes.reduce((map: Record<string, number>, type: any) => {
    map[type.name] = type.id
    return map
  }, {})
}
