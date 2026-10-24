<template>
  <!-- 注册页面容器 -->
  <div class="register-container">
    <div class="register-wrapper">
      <!-- 注册头部 -->
      <div class="register-header">
        <div class="register-logo">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
          </svg>
          <h1>OceanGuard YOLO</h1>
        </div>
        <p class="register-subtitle">海漂垃圾识别与可视化系统</p>
      </div>
      
      <!-- 注册卡片 -->
      <n-card class="register-card">
        <n-form :model="registerForm" :rules="rules" ref="formRef" class="register-form">
          <!-- 用户名输入 -->
          <n-form-item label="用户名" path="username" class="form-item">
            <n-input 
              v-model:value="registerForm.username" 
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
              v-model:value="registerForm.password" 
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
          
          <!-- 确认密码输入 -->
          <n-form-item label="确认密码" path="confirmPassword" class="form-item">
            <n-input 
              type="password" 
              v-model:value="registerForm.confirmPassword" 
              placeholder="请确认密码" 
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
          
          <!-- 注册按钮 -->
          <n-form-item class="form-item">
            <n-button 
              type="primary" 
              :loading="loading" 
              @click="handleRegister" 
              block
              class="register-button"
            >
              注册
            </n-button>
          </n-form-item>
          
          <!-- 登录链接 -->
          <div class="login-link">
            <router-link to="/login">已有账号？立即登录</router-link>
          </div>
        </n-form>
        
        <!-- 错误提示 -->
        <div class="error-message" v-if="error">{{ error }}</div>
      </n-card>
      
      <!-- 注册页脚 -->
      <div class="register-footer">
        <p>© 2026 OceanGuard YOLO. 保留所有权利。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Register.vue - 注册页面组件
 * 
 * 此组件提供用户注册功能：
 * 1. 用户名、密码、确认密码输入表单
 * 2. 表单验证（必填项、密码长度、密码一致性检查）
 * 3. 注册请求处理
 * 4. 错误提示展示
 * 5. 跳转登录页面链接
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

// 注册表单数据
const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: ''
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名' }
  ],
  password: [
    { required: true, message: '请输入密码' },
    { min: 6, message: '密码长度至少为6位' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码' },
    {
      validator: (rule: any, value: string) => {
        return value === registerForm.value.password
      },
      message: '两次输入的密码不一致'
    }
  ]
}

/**
 * 处理注册请求
 */
const handleRegister = async () => {
  console.log('Register button clicked')
  if (!formRef.value) return

  loading.value = true
  error.value = ''

  try {
    console.log('Attempting register with:', registerForm.value)
    
    // 先验证表单
    try {
      await formRef.value.validate()
    } catch (validationError) {
      console.log('Form validation failed:', validationError)
      error.value = '请检查输入的信息'
      return
    }

    // 执行注册
    await userStore.register(registerForm.value.username, registerForm.value.password)
    console.log('Register result:', userStore.isLoggedIn)
    
    if (userStore.isLoggedIn) {
      // 注册成功后自动登录，跳转到首页
      console.log('Navigating to home')
      router.push('/home')
    } else {
      error.value = '注册失败，请稍后重试'
    }
  } catch (err) {
    console.error('Register error:', err)
    error.value = userStore.error || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style>
/* 隐藏滚动条 */
.register-container::-webkit-scrollbar {
  display: none;
}

.register-container {
  scrollbar-width: none;
  overflow: hidden;
}
</style>

<style scoped>
/* 注册页面容器 */
.register-container {
  display: flex;
  flex-direction: column;
  gap: 2rem;
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
.register-container::before {
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

/* 注册包装器 */
.register-wrapper {
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

/* 注册头部 */
.register-header {
  text-align: center;
  animation: slide-up 0.5s ease-out;
}

.register-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.register-logo svg {
  color: var(--primary-color);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.register-logo h1 {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.register-subtitle {
  color: var(--text-secondary);
  font-size: 1rem;
  margin: 0;
}

/* 注册卡片 */
.register-card {
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

.register-card:hover {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
}

/* 注册表单 */
.register-form {
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

/* 注册按钮 */
.register-button {
  margin-top: 0.5rem;
  height: 2.5rem;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 0.5rem;
  transition: all 0.3s ease;
}

.register-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 6px -1px rgba(100, 108, 255, 0.2);
}

/* 登录链接 */
.login-link {
  margin-top: 1rem;
  text-align: center;
  animation: fade-in 0.3s ease;
}

.login-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 0.875rem;
  transition: color 0.3s ease;
}

.login-link a:hover {
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

/* 注册页脚 */
.register-footer {
  text-align: center;
  animation: slide-up 0.5s ease-out 0.2s both;
}

.register-footer p {
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
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .register-wrapper {
    padding: 1.5rem;
    max-width: 90%;
  }
  
  .register-card {
    padding: 1.5rem;
  }
  
  .register-logo h1 {
    font-size: 1.5rem;
  }
}
</style>