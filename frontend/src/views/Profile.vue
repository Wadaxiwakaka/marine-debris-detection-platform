<template>
  <div class="profile-container">
    <div class="profile-card">
      <div class="profile-header">
        <h2>个人资料</h2>
      </div>
      
      <div class="profile-content">
        <!-- 头像上传部分 -->
        <div class="avatar-section">
          <div class="avatar-container">
            <img 
              :src="avatarUrl" 
              alt="头像" 
              class="avatar"
            />
            <div class="avatar-upload-overlay">
              <input 
                type="file" 
                accept="image/*" 
                @change="handleAvatarUpload"
                class="avatar-upload-input"
              />
              <span class="avatar-upload-text">更换头像</span>
            </div>
          </div>
          <div class="user-info">
            <h3>{{ user?.username }}</h3>
            <p>{{ user?.role === 'admin' ? '管理员' : '普通用户' }}</p>
          </div>
        </div>
        
        <!-- 修改密码部分 -->
        <div class="password-section">
          <h3>修改密码</h3>
          <n-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" class="password-form">
            <n-form-item label="旧密码" path="oldPassword">
              <n-input 
                type="password" 
                v-model:value="passwordForm.oldPassword" 
                placeholder="请输入旧密码"
              />
            </n-form-item>
            <n-form-item label="新密码" path="newPassword">
              <n-input 
                type="password" 
                v-model:value="passwordForm.newPassword" 
                placeholder="请输入新密码"
              />
            </n-form-item>
            <n-form-item label="确认新密码" path="confirmPassword">
              <n-input 
                type="password" 
                v-model:value="passwordForm.confirmPassword" 
                placeholder="请确认新密码"
              />
            </n-form-item>
            <n-form-item>
              <n-button 
                type="primary" 
                :loading="passwordLoading" 
                @click="handlePasswordChange"
              >
                修改密码
              </n-button>
            </n-form-item>
          </n-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Profile.vue - 用户个人资料页面组件
 * 
 * 此组件提供用户个人资料管理功能：
 * 1. 头像上传和更新
 * 2. 个人信息展示（用户名、角色）
 * 3. 密码修改
 */

import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../stores/userStore'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import request from '../utils/request'

// 消息通知实例
const message = useMessage()

const userStore = useUserStore()
const router = useRouter()
const passwordFormRef = ref()
const passwordLoading = ref(false)
const avatarLoading = ref(false)

// 密码表单
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 6, message: '密码长度至少6位' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码' },
    {
      validator: (rule: any, value: string) => {
        return value === passwordForm.value.newPassword
      },
      message: '两次输入的密码不一致'
    }
  ]
}

// 计算属性：用户信息
const user = computed(() => userStore.user)

// 计算属性：头像URL
const avatarUrl = computed(() => {
  console.log('Current user avatar:', user.value?.avatar)
  if (user.value?.avatar) {
    // 检查头像URL是否已经是完整的URL
    if (user.value.avatar.startsWith('http://') || user.value.avatar.startsWith('https://')) {
      console.log('Using full URL:', user.value.avatar)
      return user.value.avatar + '?t=' + Date.now()
    }
    const constructedUrl = `/api${user.value.avatar}`
    console.log('Relative URL:', constructedUrl)
    return constructedUrl + '?t=' + Date.now()
  }
  // 默认头像
  const defaultUrl = `https://picsum.photos/200/200?random=${user.value?.id || 1}`
  console.log('Using default URL:', defaultUrl)
  return defaultUrl
})

// 处理头像上传
const handleAvatarUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file || !user.value?.id) return

  avatarLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('userId', user.value.id.toString())

    console.log('User ID:', user.value.id)
    console.log('Token in localStorage:', localStorage.getItem('token'))
    
    // 调用新的头像上传接口，使用request实例添加认证头
    // 注意：不要手动设置Content-Type，让axios自动处理multipart/form-data
    const avatarUrl = await request.post('/user/avatar', formData)

    console.log('Received avatar URL from server:', avatarUrl)
    // 使用userStore的updateAvatar方法更新头像，确保响应式更新
    await userStore.updateAvatar(avatarUrl)
    console.log('Updated user avatar in local storage:', userStore.user?.avatar)
    message.success('头像更新成功')
  } catch (error: any) {
    console.error('头像上传失败:', error)
    console.error('Error details:', error.message, error.response?.data)
    message.error(error.message || '头像上传失败')
  } finally {
    avatarLoading.value = false
    // 重置文件输入
    target.value = ''
  }
}

// 处理密码修改
const handlePasswordChange = async () => {
  if (!passwordFormRef.value) return

  passwordLoading.value = true
  try {
    // 验证表单
    await passwordFormRef.value.validate()

    // 调用修改密码接口，使用JSON体传递参数
    const response = await request.put('/user/password', {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    console.log('修改密码的接口响应', response)
    // 响应拦截器已经处理了错误情况，成功时直接返回结果
    message.success('密码修改成功')
    // 清除token，返回登录页面
    await userStore.logout()
    router.push('/login')
  } catch (error: any) {
    console.error('修改密码失败:', error)
    message.error(error.message || '修改密码失败')
  } finally {
    passwordLoading.value = false
  }
}

// 初始化用户信息
onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
  }
})
</script>

<style scoped>
.profile-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, var(--background-light) 0%, #e8f0fe 100%);
  padding: 2rem;
}

.profile-card {
  background: var(--card-background);
  border-radius: 1rem;
  box-shadow: var(--shadow-lg);
  padding: 2rem;
  width: 100%;
  max-width: 600px;
  border: 1px solid var(--border-color);
}

.profile-header {
  margin-bottom: 2rem;
  text-align: center;
}

.profile-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.avatar-container {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid var(--primary-color);
  cursor: pointer;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-container:hover .avatar-upload-overlay {
  opacity: 1;
}

.avatar-upload-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.avatar-upload-text {
  color: white;
  font-size: 0.875rem;
  text-align: center;
}

.user-info {
  flex: 1;
}

.user-info h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 0.5rem 0;
}

.user-info p {
  color: var(--text-secondary);
  margin: 0;
}

.password-section h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 1rem 0;
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-container {
    padding: 1rem;
  }

  .profile-card {
    padding: 1.5rem;
  }

  .avatar-section {
    flex-direction: column;
    text-align: center;
    gap: 1rem;
  }

  .user-info {
    order: -1;
  }
}
</style>