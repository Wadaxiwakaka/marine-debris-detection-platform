/**
 * HTTP 请求封装模块
 * 
 * 此文件基于 axios 封装了统一的 HTTP 请求处理，包括：
 * 1. 请求拦截器：自动添加认证 token
 * 2. 响应拦截器：统一处理响应格式和错误
 * 3. 统一的错误处理和日志记录
 */

// 导入 axios HTTP 客户端库
import axios from 'axios'

/**
 * 创建 axios 实例
 * 
 * 配置说明：
 * - baseURL: 请求基础路径，所有请求都会在此基础上拼接
 * - timeout: 请求超时时间，60秒
 * - headers: 请求头配置（初始为空，在拦截器中动态添加）
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {}
})

/**
 * 请求拦截器
 * 
 * 在请求发送前执行，主要功能：
 * 1. 打印请求日志（方法、URL、数据）
 * 2. 自动添加 JWT 认证 token（登录请求除外）
 * 
 * @param {AxiosRequestConfig} config - 请求配置对象
 * @returns {AxiosRequestConfig} 修改后的请求配置
 */
request.interceptors.request.use(
  (config) => {
    // 打印请求信息，便于调试
    console.log('Request:', config.method?.toUpperCase(), config.url, config.data || config.params)
    
    // 从 localStorage 获取认证 token
    const token = localStorage.getItem('token')
    console.log('Token found:', token ? 'Yes' : 'No')
    
    // 不为登录请求添加 token（避免循环依赖）
    if (token && !config.url?.includes('/login')) {
      config.headers['Authorization'] = `Bearer ${token}`
      console.log('添加认证头:', config.headers['Authorization'])
    }
    
    return config
  },
  (error) => {
    // 请求配置错误处理
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 * 
 * 在响应返回后执行，主要功能：
 * 1. 打印响应日志
 * 2. 统一处理响应格式（提取 data 字段）
 * 3. 处理错误状态码（如 403 token 过期）
 * 
 * @param {AxiosResponse} response - 响应对象
 * @returns {any} 响应数据
 */
request.interceptors.response.use(
  (response) => {
    console.log('Response:', response.config.url, response.data)
    
    const res = response.data
    
    // 根据后端统一响应格式处理
    // 后端响应结构：{ code: number, message: string, data: any }
    if (res.code !== undefined && res.code !== 200) {
      console.error('响应错误:', res.message)
      const errMsg = res.message || res.msg || '请求失败'
      return Promise.reject(new Error(errMsg))
    }
    
    // 成功时直接返回 data 字段
    return res.data
  },
  (error) => {
    console.error('响应错误:', error)
    let errMsg = '请求失败，请稍后重试'
    
    if (error.response) {
      // 服务器返回错误状态码
      console.error('错误状态码:', error.response.status)
      console.error('错误数据:', error.response.data)
      
      // 处理 403 错误（token 过期或无效）
      if (error.response.status === 403 && error.config && error.config.headers) {
        console.error('Token 过期或无效，清除本地存储')
        localStorage.removeItem('user')
        localStorage.removeItem('token')
        errMsg = '登录已过期，请重新登录'
      }
      
      const errorData = error.response.data
      if (errorData) {
        if (errorData.message) {
          errMsg = errorData.message
        } else if (errorData.msg) {
          errMsg = errorData.msg
        } else if (typeof errorData === 'string') {
          errMsg = errorData
        } else if (typeof errorData === 'object') {
          const msg = Object.values(errorData).find(v => typeof v === 'string')
          if (msg) errMsg = msg as string
        }
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应（网络问题）
      console.error('没有收到响应:', error.request)
      errMsg = '网络连接失败，请检查网络'
    } else if (error.message) {
      errMsg = error.message
    }
    
    return Promise.reject(new Error(errMsg))
  }
)

export default request