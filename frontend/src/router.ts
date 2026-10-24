/**
 * 路由配置文件
 * 
 * 此文件定义了应用的所有路由规则，包括路由路径、组件映射、
 * 权限控制元信息等。同时配置了全局路由守卫，实现登录状态检查和访问控制。
 */

// 导入 Vue Router 核心模块
import { createRouter, createWebHistory } from 'vue-router'

// 导入用户状态管理，用于路由守卫中的权限判断
import { useUserStore } from './stores/userStore'

/**
 * 路由配置数组
 * 每个路由对象包含：
 * - path: 路由路径
 * - name: 路由名称（用于编程式导航）
 * - component: 懒加载的组件
 * - meta: 元数据对象，包含权限控制信息
 *   - requiresAuth: 是否需要登录才能访问
 *   - guest: 是否仅允许未登录用户访问（如登录/注册页）
 */
const routes = [
  // 默认路由重定向到首页
  {
    path: '/',
    redirect: '/home'
  },
  // 首页 - 无需登录
  {
    path: '/home',
    name: 'Home',
    component: () => import('./views/Home.vue'),
    meta: { requiresAuth: false }
  },
  // 垃圾数据管理页面 - 需要登录
  {
    path: '/garbage-data',
    name: 'GarbageData',
    component: () => import('./views/GarbageData.vue'),
    meta: { requiresAuth: true }
  },
  // 数据统计页面 - 需要登录
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('./views/Statistics.vue'),
    meta: { requiresAuth: true }
  },
  // 关于系统页面 - 无需登录
  {
    path: '/about',
    name: 'About',
    component: () => import('./views/About.vue'),
    meta: { requiresAuth: false }
  },
  // 垃圾识别页面 - 无需登录
  {
    path: '/garbage-detection',
    name: 'GarbageDetection',
    component: () => import('./views/GarbageDetection.vue'),
    meta: { requiresAuth: false }
  },
  // 登录页面 - 仅未登录用户可访问
  {
    path: '/login',
    name: 'Login',
    component: () => import('./views/Login.vue'),
    meta: { guest: true }
  },
  // 注册页面 - 仅未登录用户可访问
  {
    path: '/register',
    name: 'Register',
    component: () => import('./views/Register.vue'),
    meta: { guest: true }
  },
  // 用户管理页面 - 需要登录（管理员专用）
  {
    path: '/user-management',
    name: 'UserManagement',
    component: () => import('./views/UserManagement.vue'),
    meta: { requiresAuth: true }
  },
  // 个人资料页面 - 需要登录
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('./views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  // AI 助手页面 - 需要登录
  {
    path: '/ai-assistant',
    name: 'AIAssistant',
    component: () => import('./views/AIAssistant.vue'),
    meta: { requiresAuth: true }
  }
]

/**
 * 创建路由实例
 * 使用 HTML5 History 模式（需要服务器端配置支持）
 */
const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局路由守卫（前置守卫）
 * 
 * 在每次路由跳转前执行，用于权限控制：
 * 1. 如果路由需要登录但用户未登录，重定向到登录页
 * 2. 如果路由是登录/注册页但用户已登录，重定向到首页
 * 3. 其他情况正常导航
 * 
 * @param {RouteLocationNormalized} to - 目标路由
 * @param {RouteLocationNormalized} from - 当前路由
 * @param {NavigationGuardNext} next - 导航控制函数
 */
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth === true
  const isGuestRoute = to.meta.guest === true
  const isLoggedIn = userStore.isLoggedIn

  if (requiresAuth && !isLoggedIn) {
    // 需要登录但未登录，跳转到登录页
    next('/login')
  } 
  else if (isGuestRoute && isLoggedIn) {
    // 已登录用户访问登录/注册页
    next('/home')
  } 
  else {
    // 正常导航
    next()
  }
})

export default router