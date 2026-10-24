<template>
  <!-- Naive UI 消息提供者 - 用于全局消息提示 -->
  <n-message-provider>
    <!-- Naive UI 通知提供者 - 用于全局通知 -->
    <n-notification-provider>
      <!-- Naive UI 对话框提供者 - 用于全局对话框 -->
      <n-dialog-provider>
        <!-- 应用容器 - 包含导航栏、主内容区和页脚 -->
        <div class="app-container">
          <!-- 导航栏 - 非登录页面显示 -->
          <header v-if="!isLoginPage" class="navbar">
            <div class="navbar-container">
              <!-- Logo 区域 -->
              <div class="navbar-brand">
                <h1 class="brand-title">
                  <span class="brand-text">OceanGuard</span>
                  <span class="brand-subtitle">YOLO</span>
                </h1>
              </div>
              
              <!-- 导航菜单 -->
              <nav class="navbar-nav">
                <router-link to="/home" class="nav-link" active-class="active">
                  首页
                </router-link>
                <router-link to="/garbage-data" class="nav-link" active-class="active">
                  垃圾数据
                </router-link>
                <router-link to="/statistics" class="nav-link" active-class="active">
                  数据统计
                </router-link>
                <router-link to="/garbage-detection" class="nav-link" active-class="active">
                  垃圾识别
                </router-link>
                <router-link to="/ai-assistant" class="nav-link" active-class="active">
                  AI 助手
                </router-link>
                <router-link v-if="userStore.user?.role === 'admin'" to="/user-management" class="nav-link" active-class="active">
                  用户管理
                </router-link>
                <router-link to="/about" class="nav-link" active-class="active">
                  关于系统
                </router-link>
              </nav>
              
              <!-- 用户信息/登录按钮 -->
              <div class="navbar-actions">
                <div v-if="userStore.isLoggedIn" class="user-profile">
                  <router-link to="/profile" class="nav-link" active-class="active" style="margin-right: 1rem;">
                    个人资料
                  </router-link>
                  <span class="user-name">{{ userStore.user?.username }}</span>
                  <button 
                    class="logout-button"
                    @click="logout"
                    style="
                      background: none;
                      border: none;
                      color: var(--text-secondary);
                      cursor: pointer;
                      padding: 0.5rem 1rem;
                      font-size: 1rem;
                      transition: color 0.2s ease;
                    "
                    onmouseover="this.style.color='var(--primary-color)'"
                    onmouseout="this.style.color='var(--text-secondary)'"
                  >
                    退出登录
                  </button>
                </div>
                <button 
                  v-else 
                  class="login-button"
                  @click="goToLogin"
                  style="
                    background-color: var(--primary-color);
                    border: none;
                    border-radius: 0.5rem;
                    padding: 0.5rem 1.25rem;
                    color: white;
                    font-weight: 500;
                    cursor: pointer;
                    font-size: 1rem;
                    transition: all 0.2s ease;
                  "
                  onmouseover="this.style.backgroundColor='var(--primary-hover)'"
                  onmouseout="this.style.backgroundColor='var(--primary-color)'"
                >
                  登录
                </button>
              </div>
            </div>
          </header>
          
          <!-- 主内容区 - 根据是否登录页面应用不同样式 -->
          <main :class="['main-content', { 'login-page': isLoginPage }]">
            <!-- 页面切换动画 -->
            <transition name="fade" mode="out-in">
              <router-view />
            </transition>
          </main>
          
          <!-- 页脚 - 非登录页面显示 -->
          <footer v-if="!isLoginPage" class="footer">
            <div class="footer-container">
              <div class="footer-content">
                <!-- 页脚品牌信息 -->
                <div class="footer-section">
                  <h3>OceanGuard YOLO</h3>
                  <p>基于YOLO的海漂垃圾识别与可视化系统</p>
                </div>
                <!-- 快速链接 -->
                <div class="footer-section">
                  <h4>快速链接</h4>
                  <ul class="footer-links">
                    <li><router-link to="/home">首页</router-link></li>
                    <li><router-link to="/garbage-data">垃圾数据</router-link></li>
                    <li><router-link to="/statistics">数据统计</router-link></li>
                    <li><router-link to="/garbage-detection">垃圾识别</router-link></li>
                    <li><router-link to="/ai-assistant">AI 助手</router-link></li>
                    <li v-if="userStore.user?.role === 'admin'"><router-link to="/user-management">用户管理</router-link></li>
                    <li><router-link to="/about">关于系统</router-link></li>
                  </ul>
                </div>
                <!-- 联系方式 -->
                <div class="footer-section">
                  <h4>联系我们</h4>
                  <p>邮箱: 1325871083@qq.com</p>
                  <p>电话: +86 196 0407 1999</p>
                </div>
              </div>
              <div class="footer-bottom">
                <p>&copy; 2026 OceanGuard YOLO. 保留所有权利。</p>
              </div>
            </div>
          </footer>
        </div>
      </n-dialog-provider>
    </n-notification-provider>
  </n-message-provider>
</template>

<script setup lang="ts">
/**
 * App.vue - 应用根组件
 * 
 * 此组件是 Vue 应用的根组件，负责：
 * 1. 提供全局布局（导航栏、主内容区、页脚）
 * 2. 管理用户登录状态和导航
 * 3. 处理页面切换动画
 * 4. 提供全局消息和通知支持
 */

// 导入 Vue 组合式 API
import { onMounted, watch, nextTick, ref } from 'vue'

// 导入 Vue Router
import { useRouter, useRoute } from 'vue-router'

// 导入用户状态管理
import { useUserStore } from './stores/userStore'

// 初始化路由和状态管理
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/**
 * 判断当前是否为登录/注册页面
 * @type {Ref<boolean>}
 */
const isLoginPage = ref(false)

/**
 * 监听路由变化，更新登录页面状态
 * 当路由路径为 /login 或 /register 时，隐藏导航栏和页脚
 */
watch(() => route.path, (newPath) => {
  isLoginPage.value = newPath === '/login' || newPath === '/register'
}, { immediate: true })

/**
 * 用户退出登录处理函数
 * 
 * 执行以下操作：
 * 1. 调用 userStore 的 logout 方法清除用户信息
 * 2. 清除 localStorage 中的用户数据
 * 3. 等待 DOM 更新完成
 * 4. 强制跳转到登录页面
 */
const logout = async () => {
  console.log('=== LOGOUT BUTTON CLICKED ===')
  console.log('Current user:', userStore.user)
  console.log('Is logged in:', userStore.isLoggedIn)
  
  // 调用 store 的 logout 方法
  await userStore.logout()
  
  // 确保 localStorage 被清除
  localStorage.removeItem('user')
  
  // 等待 DOM 更新
  await nextTick()
  console.log('Next tick done')
  console.log('After logout - user:', userStore.user)
  console.log('After logout - isLoggedIn:', userStore.isLoggedIn)
  
  // 确保路由跳转在用户登出状态更新完成后执行
  if (userStore.isLoggedIn) {
    for (let i = 0; i < 5; i++) {
      await new Promise(resolve => setTimeout(resolve, 100))
      if (!userStore.isLoggedIn) break
    }
  }
  // 强制跳转到登录页
  console.log('Force redirecting to /login')
  router.push('/login')
  console.log('=== LOGOUT COMPLETE ===')
}

/**
 * 跳转到登录页面
 */
const goToLogin = () => {
  console.log('Login button clicked')
  router.push('/login')
}

/**
 * 监听路由变化，执行页面滚动到顶部
 */
watch(() => route.path, () => {
  // 路由变化时滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
})
</script>

<style>
/* 全局样式 */
body {
  margin: 0;
  padding: 0;
}
</style>

<style scoped>
/**
 * 应用容器样式
 * 使用 flex 布局，最小高度占满视口
 */
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--background-light);
  transition: background-color 0.3s ease;
}

/**
 * 导航栏样式
 * 固定在顶部，带有阴影效果
 */
.navbar {
  background-color: var(--card-background);
  box-shadow: var(--shadow-md);
  position: sticky;
  top: 0;
  z-index: 100;
  transition: all 0.3s ease;
}

.navbar-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.navbar-brand {
  display: flex;
  align-items: center;
}

.brand-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.brand-text {
  color: var(--primary-color);
}

.brand-subtitle {
  font-size: 1rem;
  font-weight: 400;
  color: var(--text-secondary);
}

/**
 * 导航菜单样式
 */
.navbar-nav {
  display: flex;
  gap: 2rem;
  align-items: center;
}

.nav-link {
  color: var(--text-primary);
  font-weight: 500;
  text-decoration: none;
  padding: 0.5rem 0;
  position: relative;
  transition: color 0.2s ease;
}

.nav-link:hover {
  color: var(--primary-color);
}

.nav-link.active {
  color: var(--primary-color);
}

.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background-color: var(--primary-color);
  border-radius: 2px;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    width: 0;
    left: 50%;
  }
  to {
    width: 100%;
    left: 0;
  }
}

/**
 * 导航栏操作区样式
 */
.navbar-actions {
  display: flex;
  align-items: center;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-name {
  font-weight: 500;
  color: var(--text-primary);
}

.logout-button {
  color: var(--text-secondary);
}

.logout-button:hover {
  color: var(--primary-color);
}

.login-button {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
  border-radius: 0.5rem;
  padding: 0.5rem 1.25rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.login-button:hover {
  background-color: var(--primary-hover);
  border-color: var(--primary-hover);
}

/**
 * 主内容区样式
 */
.main-content {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem;
}

/**
 * 登录页面特殊样式
 */
.main-content.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  background-color: var(--background-light);
  max-width: none;
  width: 100%;
  height: 100%;
  margin: 0;
  overflow: hidden;
}

/**
 * 页脚样式
 */
.footer {
  background-color: var(--card-background);
  border-top: 1px solid var(--border-color);
  margin-top: auto;
  transition: all 0.3s ease;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 2rem;
  margin-bottom: 2rem;
}

.footer-section h3 {
  font-size: 1.25rem;
  margin-bottom: 1rem;
  color: var(--text-primary);
}

.footer-section h4 {
  font-size: 1rem;
  margin-bottom: 1rem;
  color: var(--text-primary);
}

.footer-section p {
  color: var(--text-secondary);
  margin-bottom: 0.5rem;
}

.footer-links {
  list-style: none;
}

.footer-links li {
  margin-bottom: 0.5rem;
}

.footer-links a {
  color: var(--text-secondary);
  text-decoration: none;
  transition: color 0.2s ease;
}

.footer-links a:hover {
  color: var(--primary-color);
}

.footer-bottom {
  border-top: 1px solid var(--border-color);
  padding-top: 1rem;
  text-align: center;
  color: var(--text-secondary);
  font-size: 0.875rem;
}

/**
 * 响应式设计
 */
@media (max-width: 768px) {
  .navbar-container {
    padding: 0 1rem;
    height: 70px;
  }
  
  .brand-title {
    font-size: 1.25rem;
  }
  
  .navbar-nav {
    gap: 1rem;
  }
  
  .main-content {
    padding: 1rem;
  }
  
  .footer-container {
    padding: 1.5rem 1rem;
  }
  
  .footer-content {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }
}

/**
 * 页面切换动画
 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>