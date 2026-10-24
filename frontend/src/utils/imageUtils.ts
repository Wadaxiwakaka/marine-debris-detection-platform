/**
 * 图片工具函数模块
 * 
 * 此模块提供一系列图片处理相关的工具函数，包括：
 * 1. Base64 与 Blob/File 格式转换
 * 2. 图片可视化处理（绘制 bounding box）
 * 3. 地理位置获取
 * 4. 文件上传事件处理
 * 5. 模型服务调用
 */

/**
 * 将 Base64 字符串转换为 Blob 对象
 * 
 * @param {string} base64 - Base64 编码的图片字符串（包含 data:image/xxx;base64, 前缀）
 * @returns {Blob} 转换后的 Blob 对象
 */
export function base64ToBlob(base64: string): Blob {
  // 分割 Base64 字符串，提取内容类型和数据部分
  const parts = base64.split(';base64,')
  const contentType = parts[0].split(':')[1]
  const raw = window.atob(parts[1])
  const rawLength = raw.length
  const uInt8Array = new Uint8Array(rawLength)

  // 将解码后的字符串转换为 Uint8Array
  for (let i = 0; i < rawLength; ++i) {
    uInt8Array[i] = raw.charCodeAt(i)
  }

  return new Blob([uInt8Array], { type: contentType })
}

/**
 * 将 Base64 字符串转换为 File 对象
 * 
 * @param {string} base64 - Base64 编码的图片字符串
 * @param {string} filename - 文件名
 * @returns {File} 转换后的 File 对象
 */
export function base64ToFile(base64: string, filename: string): File {
  const blob = base64ToBlob(base64)
  return new File([blob], filename, { type: blob.type })
}

/**
 * 生成带 bounding box 的可视化图片
 * 
 * 在原始图片上绘制检测框和标签，用于展示垃圾识别结果
 * 
 * @param {string} imageUrl - 原始图片 URL
 * @param {any[]} predictions - 预测结果数组，每个元素包含 bbox 和 type
 * @param {string} color - 边框颜色（如 '#FF0000'）
 * @returns {Promise<string>} 处理后的图片 Data URL
 */
export function generateVisualizedImage(
  imageUrl: string,
  predictions: any[],
  color: string
): Promise<string> {
  return new Promise((resolve) => {
    // 参数校验：如果没有图片或预测结果，直接返回原始图片
    if (!imageUrl || !predictions || predictions.length === 0) {
      resolve(imageUrl)
      return
    }

    // 创建 Canvas 元素用于绘制
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')

    // 如果 Canvas 上下文获取失败，返回原始图片
    if (!ctx) {
      resolve(imageUrl)
      return
    }

    // 创建图片对象并加载
    const img = new Image()
    img.src = imageUrl

    img.onload = () => {
      // 设置 Canvas 尺寸与图片一致
      canvas.width = img.width
      canvas.height = img.height

      // 绘制原始图片到 Canvas
      ctx.drawImage(img, 0, 0)

      // 遍历预测结果，绘制 bounding box 和标签
      predictions.forEach((pred) => {
        const bbox = pred.bbox
        const [x1, y1, x2, y2] = bbox

        // 绘制矩形边框
        ctx.strokeStyle = color
        ctx.lineWidth = 2
        ctx.strokeRect(x1, y1, x2 - x1, y2 - y1)

        // 绘制标签（类型 + 置信度）
        ctx.fillStyle = color
        ctx.font = '14px Arial'
        ctx.fillText(`${pred.type} ${(pred.confidence * 100).toFixed(1)}%`, x1, y1 - 5)
      })

      // 将 Canvas 转换为 Data URL 返回
      resolve(canvas.toDataURL('image/jpeg'))
    }

    // 图片加载失败时返回原始图片
    img.onerror = () => {
      resolve(imageUrl)
    }
  })
}

/**
 * 获取本地时间字符串（符合后端 LocalDateTime 格式）
 * 
 * 格式：YYYY-MM-DDTHH:MM:SS
 * 
 * @returns {string} 格式化的时间字符串
 */
export function getLocalTimeString(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

/**
 * 获取用户当前地理位置
 * 
 * 使用浏览器 Geolocation API 获取经纬度信息
 * 
 * @param {(position: { latitude: number; longitude: number }) => void} onSuccess - 成功回调
 * @param {(error: string) => void} onError - 失败回调
 */
export function getGeolocation(
  onSuccess: (position: { latitude: number; longitude: number }) => void,
  onError: (error: string) => void
): void {
  // 检查浏览器是否支持地理位置 API
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        // 成功获取位置，调用成功回调
        onSuccess({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude
        })
      },
      (error) => {
        // 获取位置失败，调用错误回调
        onError(`获取地理位置失败: ${error.message}`)
      }
    )
  } else {
    onError('浏览器不支持地理位置服务')
  }
}

/**
 * 处理 Naive UI 上传组件的文件选择事件
 * 
 * Naive UI 的上传组件事件结构可能有多种形式，此函数统一处理这些情况
 * 
 * @param {any} uploadEvent - Naive UI 上传事件对象
 * @param {(fileUrl: string) => void} onFileSelected - 文件选择成功回调（返回 Data URL）
 * @param {() => void} onFileRemoved - 文件移除回调
 */
export function handleFileChange(
  uploadEvent: any,
  onFileSelected: (fileUrl: string) => void,
  onFileRemoved: () => void
): void {
  // 处理 Naive UI 上传组件的多种事件结构
  let files: any[] = []
  
  if (uploadEvent.fileList) {
    // 标准事件对象格式
    files = uploadEvent.fileList
  } else if (Array.isArray(uploadEvent)) {
    // 直接传入文件数组
    files = uploadEvent
  }

  if (files && files.length > 0) {
    const file = files[0]

    // 从不同的文件对象结构中提取 Blob
    let fileBlob: Blob | null = null
    if (file.file) {
      fileBlob = file.file
    } else if (file.originFile) {
      fileBlob = file.originFile
    } else if (file.raw) {
      fileBlob = file.raw
    } else if (file instanceof Blob) {
      fileBlob = file
    }

    // 使用 FileReader 将文件转换为 Data URL
    if (fileBlob) {
      const reader = new FileReader()
      reader.onload = (e) => {
        if (e.target?.result) {
          onFileSelected(e.target.result as string)
        }
      }
      reader.readAsDataURL(fileBlob)
    }
  } else {
    // 当文件被删除时，清除相关状态
    onFileRemoved()
  }
}

/**
 * 调用模型服务进行垃圾识别
 * 
 * 通过 HTTP 请求调用本地运行的模型服务（端口 8000）
 * 
 * @param {File} imageFile - 图片文件
 * @param {string} mode - 识别模式（如 'fast', 'accurate'）
 * @param {any} message - Naive UI 消息通知实例
 * @returns {Promise<any>} 识别结果
 */
export async function detectGarbage(
  imageFile: File,
  mode: string,
  message: any
): Promise<any> {
  try {
    // 构建 FormData
    const formData = new FormData()
    formData.append('file', imageFile)
    formData.append('mode', mode)

    // 调用模型服务 API
    const result = await fetch('http://localhost:8000/predict', {
      method: 'POST',
      body: formData
    })

    const resultData = await result.json()

    // 根据响应状态处理
    if (resultData.status === 'success') {
      message.success('识别成功！')
      return resultData
    } else {
      message.error(`识别失败: ${resultData.message}`)
      throw new Error(resultData.message)
    }
  } catch (error) {
    console.error('识别错误:', error)
    message.error('识别过程中发生错误，请检查模型服务是否运行')
    throw error
  }
}