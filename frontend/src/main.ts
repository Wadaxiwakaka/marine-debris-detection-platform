/**
 * 应用入口文件
 * 
 * 此文件负责初始化 Vue 应用实例，配置核心依赖（Pinia、Router、Naive UI），
 * 并挂载到 DOM 中。同时在应用启动时初始化用户状态。
 */

// 导入 Vue 核心模块
import { createApp } from 'vue'

// 导入 Pinia 状态管理库
import { createPinia } from 'pinia'

// 导入根组件
import App from './App.vue'

// 导入全局样式
import './style.css'

// 导入路由配置
import router from './router'

// 导入 Naive UI 组件库
import naive from 'naive-ui'

/**
 * 创建 Vue 应用实例
 * @returns {App} Vue 应用实例对象
 */
const app = createApp(App)

/**
 * 创建 Pinia 状态管理实例
 * Pinia 是 Vue 官方推荐的状态管理库，提供简洁的 API 和类型安全支持
 */
const pinia = createPinia()

// 将 Pinia 注入到 Vue 应用中
app.use(pinia)

// 将 Vue Router 注入到 Vue 应用中
app.use(router)

// 将 Naive UI 组件库注入到 Vue 应用中
app.use(naive)

/**
 * 挂载应用到 DOM
 * 将应用挂载到 index.html 中的 #app 元素
 */
app.mount('#app')

/**
 * 初始化用户状态
 * 在应用挂载后立即执行，从 localStorage 中恢复用户登录状态
 */
import { useUserStore } from './stores/userStore'
const userStore = useUserStore()
userStore.initUser()
