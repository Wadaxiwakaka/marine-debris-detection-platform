<template>
  <!-- 登录页面容器 -->
  <div class="login-container">
    <div class="login-wrapper">
      <!-- 登录头部 -->
      <div class="login-header">
        <div class="login-logo">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
          </svg>
          <h1>OceanGuard YOLO</h1>
        </div>
        <p class="login-subtitle">海漂垃圾识别与可视化系统</p>
      </div>
      
      <!-- 登录卡片 -->
      <n-card class="login-card">
        <n-form :model="loginForm" :rules="rules" ref="formRef" class="login-form">
          <!-- 用户名输入 -->
          <n-form-item label="用户名" path="username" class="form-item">
            <n-input 
              v-model:value="loginForm.username" 
              placeholder="请输入用户名" 
              class="form-input"
              :focusable="true"
            >
              <template #prefix>
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
              </template>
            </n-input>
          </n-form-item>
          
          <!-- 密码输入 -->
          <n-form-item label="密码" path="password" class="form-item">
            <n-input 
              type="password" 
              v-model:value="loginForm.password" 
              placeholder="请输入密码" 
              class="form-input"
              :focusable="true"
            >
              <template #prefix>
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
              </template>
            </n-input>
          </n-form-item>
          
          <!-- 登录按钮 -->
          <n-form-item class="form-item">
            <n-button 
              type="primary" 
              :loading="loading" 
              @click="handleLogin" 
              block
              class="login-button"
            >
              登录
            </n-button>
          </n-form-item>
          
          <!-- 注册链接 -->
          <div class="register-link">
            <router-link to="/register">还没有账号？立即注册</router-link>
          </div>
        </n-form>
        
        <!-- 错误提示 -->
        <div class="error-message" v-if="error">{{ error }}</div>
      </n-card>
      
      <!-- 登录页脚 -->
      <div class="login-footer">
        <p>© 2026 OceanGuard YOLO. 保留所有权利。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Login.vue - 登录页面组件
 * 
 * 此组件提供用户登录功能：
 * 1. 用户名和密码输入表单
 * 2. 表单验证（必填项检查）
 * 3. 登录请求处理
 * 4. 错误提示展示
 * 5. 跳转注册页面链接
 */

import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'

// 初始化路由和状态管理
const router = useRouter()
const userStore = useUserStore()

// 表单引用
const formRef = ref()

// 加载状态
const loading = ref(false)

// 错误信息
const error = ref('')

// 登录表单数据（默认填充测试账号）
const loginForm = ref({
  username: 'admin',
  password: '123123'
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名' }
  ],
  password: [
    { required: true, message: '请输入密码' }
  ]
}

/**
 * 处理登录请求
 */
const handleLogin = async () => {
  if (!formRef.value) return

  loading.value = true
  error.value = ''

  try {
    console.log('Attempting login with:', loginForm.value)
    
    // 先验证表单
    try {
      await formRef.value.validate()
    } catch (validationError) {
      console.log('Form validation failed:', validationError)
      error.value = '请输入用户名和密码'
      return
    }

    // 执行登录
    await userStore.login(loginForm.value.username, loginForm.value.password)
    console.log('Login result:', userStore.isLoggedIn)
    
    if (userStore.isLoggedIn) {
      // 登录成功，跳转到首页
      router.push('/home')
    } else {
      error.value = '登录失败，请检查用户名和密码'
    }
  } catch (err) {
    console.error('Login error:', err)
    error.value = userStore.error || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<style>
/* 隐藏滚动条 */
.login-container::-webkit-scrollbar {
  display: none;
}

.login-container {
  scrollbar-width: none;
  overflow: hidden;
}
</style>

<style scoped>
/* 登录页面容器 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--background-light) 0%, #e8f0fe 100%);
  position: relative;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

/* 背景装饰图案 */
.login-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(100, 108, 255, 0.05) 1px, transparent 1px);
  background-size: 20px 20px;
  animation: float 20s linear infinite;
}

@keyframes float {
  0% { transform: translate(0, 0) rotate(0deg); }
  100% { transform: translate(-20px, -20px) rotate(360deg); }
}

/* 登录包装器 */
.login-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 2rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
}

/* 登录头部 */
.login-header {
  text-align: center;
  animation: slide-up 0.5s ease-out;
}

.login-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.login-logo svg {
  color: var(--primary-color);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.login-logo h1 {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.login-subtitle {
  color: var(--text-secondary);
  font-size: 1rem;
  margin: 0;
}

/* 登录卡片 */
.login-card {
  background: var(--card-background);
  border-radius: 1rem;
  box-shadow: var(--shadow-lg);
  padding: 1.5rem;
  width: 100%;
  max-width: 450px;
  animation: slide-up 0.5s ease-out 0.1s both;
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

.login-card:hover {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
}

/* 登录表单 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-item {
  margin-bottom: 0;
}

.form-input {
  border-radius: 0.5rem;
  transition: all 0.3s ease;
}

.form-input:hover {
  border-color: var(--primary-color);
}

/* 登录按钮 */
.login-button {
  margin-top: 0.5rem;
  height: 2.5rem;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 0.5rem;
  transition: all 0.3s ease;
}

.login-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 6px -1px rgba(100, 108, 255, 0.2);
}

/* 注册链接 */
.register-link {
  margin-top: 1rem;
  text-align: center;
  animation: fade-in 0.3s ease;
}

.register-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 0.875rem;
  transition: color 0.3s ease;
}

.register-link a:hover {
  color: #7c3aed;
  text-decoration: underline;
}

/* 错误信息 */
.error-message {
  color: #ef4444;
  margin-top: 1rem;
  text-align: center;
  font-size: 0.875rem;
  animation: fade-in 0.3s ease;
}

/* 登录页脚 */
.login-footer {
  text-align: center;
  animation: slide-up 0.5s ease-out 0.2s both;
}

.login-footer p {
  color: var(--text-secondary);
  font-size: 0.875rem;
  margin: 0;
}

/* 动画效果 */
@keyframes slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes fade-in {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-wrapper {
    padding: 1.5rem;
    max-width: 90%;
  }
  
  .login-card {
    padding: 1.5rem;
  }
  
  .login-logo h1 {
    font-size: 1.5rem;
  }
}
</style>