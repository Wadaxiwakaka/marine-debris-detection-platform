/**
 * 用户状态管理模块
 * 
 * 此模块使用 Pinia 管理用户相关的全局状态，包括：
 * 1. 用户登录/注册/退出功能
 * 2. 用户信息存储
 * 3. 登录状态管理
 * 4. 头像更新
 * 
 * 用户信息会持久化存储到 localStorage，实现页面刷新后自动恢复登录状态
 */

import { defineStore } from 'pinia'
import request from '../utils/request'

/**
 * 用户接口定义
 */
interface User {
  id: number
  username: string
  role: string
  createTime: string
  avatar?: string
}

/**
 * 用户状态接口定义
 */
interface UserState {
  user: User | null
  isLoggedIn: boolean
  loading: boolean
  error: string | null
}

/**
 * 创建用户状态管理 Store
 */
export const useUserStore = defineStore('user', {
  /**
   * 状态初始化
   */
  state: (): UserState => ({
    user: null,
    isLoggedIn: false,
    loading: false,
    error: null
  }),

  actions: {
    /**
     * 用户登录
     * 
     * @param {string} username - 用户名
     * @param {string} password - 密码
     */
    async login(username: string, password: string) {
      this.loading = true
      this.error = null
      try {
        console.log('Sending login request to:', '/api/user/login')
        console.log('Login params:', { username, password })
        
        // 使用 URLSearchParams 发送表单数据
        const params = new URLSearchParams()
        params.append('username', username)
        params.append('password', password)
        
        const data = await request.post('/user/login', params)
        console.log('Login response:', data)
        
        // 类型断言，确保 TypeScript 识别 data 的结构
        const loginData = data as unknown as { user: User; token: string }
        this.user = loginData.user
        this.isLoggedIn = true
        
        // 持久化用户信息和 token 到 localStorage
        localStorage.setItem('user', JSON.stringify(loginData.user))
        localStorage.setItem('token', loginData.token)
        
        console.log('Login successful, user stored:', this.user)
        console.log('Login successful, token stored:', loginData.token)
      } catch (error: any) {
        console.error('Login error:', error)
        this.error = error.message || '登录失败，请检查用户名和密码'
        this.isLoggedIn = false
        throw error
      } finally {
        this.loading = false
      }
    },

    /**
     * 用户注册
     * 
     * @param {string} username - 用户名
     * @param {string} password - 密码
     */
    async register(username: string, password: string) {
      this.loading = true
      this.error = null
      try {
        console.log('Sending register request to:', '/api/user')
        console.log('Register data:', { username, password, role: 'user' })
        
        const response = await request.post('/user/register', {
          username,
          password,
          role: 'user' // 默认角色为普通用户
        })
        console.log('Register response:', response)
        
        // 注册成功后自动登录
        await this.login(username, password)
      } catch (error: any) {
        console.error('Register error:', error)
        this.error = error.message || '注册失败，请稍后重试'
        this.isLoggedIn = false
        throw error
      } finally {
        this.loading = false
      }
    },

    /**
     * 用户退出登录
     * 
     * 清除用户状态和本地存储中的数据
     */
    async logout() {
      this.user = null
      this.isLoggedIn = false
      localStorage.removeItem('user')
      localStorage.removeItem('token')
    },

    /**
     * 初始化用户状态
     * 
     * 从 localStorage 中恢复用户信息，实现页面刷新后自动登录
     */
    initUser() {
      const userStr = localStorage.getItem('user')
      const token = localStorage.getItem('token')
      
      if (userStr && token) {
        try {
          this.user = JSON.parse(userStr)
          this.isLoggedIn = true
        } catch (error) {
          console.error('Error parsing user from localStorage:', error)
          // 如果解析失败，清除本地存储
          localStorage.removeItem('user')
          localStorage.removeItem('token')
          this.user = null
          this.isLoggedIn = false
        }
      } else {
        // 本地存储中不存在用户信息或 token，设置为未登录状态
        this.user = null
        this.isLoggedIn = false
      }
    },

    /**
     * 更新用户头像
     * 
     * @param {string} avatarUrl - 头像 URL
     */
    async updateAvatar(avatarUrl: string) {
      if (!this.user) return
      
      try {
        // 直接更新本地用户信息
        // 头像已在后端接口中更新，此处只需更新本地状态
        this.user = {
          ...this.user,
          avatar: avatarUrl
        }
        localStorage.setItem('user', JSON.stringify(this.user))
      } catch (error) {
        console.error('更新头像失败:', error)
        throw error
      }
    }
  }
})
