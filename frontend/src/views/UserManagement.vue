<template>
  <div class="user-management-container">
    <h1 class="page-title">用户管理</h1>
    
    <!-- 操作栏 -->
    <div class="action-bar">
      <n-button type="primary" @click="handleAddUser">
        <template #icon>
          <n-icon>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
            </svg>
          </n-icon>
        </template>
        添加用户
      </n-button>
    </div>
    
    <!-- 用户列表 -->
    <div class="user-list-card">
      <n-data-table
        :columns="columns"
        :data="users"
        :loading="loading"
        bordered
        class="user-table"
      >
      </n-data-table>
    </div>
    
    <!-- 添加/编辑用户对话框 -->
    <n-modal
      v-model:show="userModalVisible"
      :title="isEditMode ? '编辑用户' : '添加用户'"
      preset="card"
    >
      <n-form :model="formData" label-placement="top">
        <n-form-item label="用户名">
          <n-input v-model:value="formData.username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item label="密码" v-if="!isEditMode">
          <n-input
            v-model:value="formData.password"
            type="password"
            placeholder="请输入密码"
          />
        </n-form-item>
        <n-form-item label="角色">
          <n-select
            v-model:value="formData.role"
            :options="roleOptions"
            placeholder="请选择角色"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <div class="modal-footer">
          <n-button @click="userModalVisible = false">取消</n-button>
          <n-button type="primary" @click="handleSubmitUser">
            {{ isEditMode ? '更新' : '创建' }}
          </n-button>
        </div>
      </template>
    </n-modal>
    
    <!-- 修改密码对话框 -->
    <n-modal
      v-model:show="passwordModalVisible"
      title="修改密码"
      preset="card"
    >
      <n-form :model="passwordForm" label-placement="top">
        <n-form-item label="新密码">
          <n-input
            v-model:value="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
          />
        </n-form-item>
        <n-form-item label="确认密码">
          <n-input
            v-model:value="passwordForm.confirmPassword"
            type="password"
            placeholder="请确认新密码"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <div class="modal-footer">
          <n-button @click="passwordModalVisible = false">取消</n-button>
          <n-button type="primary" @click="handleSubmitPassword">
            确认修改
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/**
 * UserManagement.vue - 用户管理页面组件（管理员专用）
 * 
 * 此组件提供系统用户管理功能（仅管理员可见）：
 * 1. 用户列表展示
 * 2. 添加新用户
 * 3. 编辑用户信息
 * 4. 修改用户密码
 * 5. 删除用户
 */

import { ref, reactive, onMounted, h } from 'vue'
import request from '../utils/request'
import { useMessage, useDialog } from 'naive-ui'

// 消息通知实例
const message = useMessage()
const dialog = useDialog()

// const router = useRouter()
const loading = ref(false)
const users = ref<any[]>([])

const userModalVisible = ref(false)
const passwordModalVisible = ref(false)
const isEditMode = ref(false)
const currentUserId = ref<number | null>(null)

const roleOptions = [
  { label: '管理员', value: 'admin' },
  { label: '普通用户', value: 'user' }
]

const formData = reactive({
  username: '',
  password: '',
  role: 'user'
})

const passwordForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const columns = [
  {
    title: 'ID',
    key: 'id'
  },
  {
    title: '用户名',
    key: 'username'
  },
  {
    title: '角色',
    key: 'role',
    render: (row: any) => {
      return row.role === 'admin' ? '管理员' : '普通用户'
    }
  },
  {
    title: '创建时间',
    key: 'createTime'
  },
  {
    title: '操作',
    key: 'actions',
    width: 250,
    render: (row: any) => {
      return h('div', { style: { display: 'flex', gap: '8px' } }, [
        h('button', {
          style: {
            padding: '4px 12px',
            backgroundColor: '#1890ff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '12px',
            whiteSpace: 'nowrap',
            minWidth: '60px'
          },
          onClick: () => handleEditUser(row)
        }, '编辑'),
        h('button', {
          style: {
            padding: '4px 12px',
            backgroundColor: '#faad14',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '12px',
            whiteSpace: 'nowrap',
            minWidth: '80px'
          },
          onClick: () => handleChangePassword(row)
        }, '修改密码'),
        h('button', {
          style: {
            padding: '4px 12px',
            backgroundColor: '#ff4d4f',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '12px',
            whiteSpace: 'nowrap',
            minWidth: '60px'
          },
          onClick: () => handleDeleteUser(row)
        }, '删除')
      ])
    }
  }
]

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const response = await request.get('/user')
    // 处理后端返回的数据，添加缺失的字段
    users.value = response.map((user: any) => ({
      ...user,
      role: user.role,
      createTime: user.createTime
    }))
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAddUser = () => {
  isEditMode.value = false
  formData.username = ''
  formData.password = ''
  formData.role = 'user'
  userModalVisible.value = true
}

const handleEditUser = (row: any) => {
  isEditMode.value = true
  currentUserId.value = row.id
  formData.username = row.username
  formData.role = row.role
  userModalVisible.value = true
}

const handleChangePassword = (row: any) => {
  currentUserId.value = row.id
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordModalVisible.value = true
}

const handleDeleteUser = async (row: any) => {
  dialog.warning({
    title: '删除确认',
    content: `确定要删除用户 "${row.username}" 吗？此操作不可撤销。`,
    positiveText: '确认删除',
    negativeText: '取消',
    type: 'error',
    onPositiveClick: async () => {
      try {
        await request.delete(`/user/${row.id}`)
        message.success('用户删除成功')
        // 重新获取用户列表
        await fetchUsers()
      } catch (error) {
        console.error('删除用户失败:', error)
        message.error('删除用户失败')
      }
    }
  })
}

const handleSubmitUser = async () => {
  try {
    if (isEditMode.value && currentUserId.value) {
      // 更新用户
      await request.put('/user', {
        id: currentUserId.value,
        username: formData.username,
        role: formData.role
      })
    } else {
      // 添加用户
      await request.post('/user/register', {
        username: formData.username,
        password: formData.password,
        role: formData.role
      })
    }
    // 重新获取用户列表
    await fetchUsers()
    userModalVisible.value = false
  } catch (error: any) {
    console.error('保存用户失败:', error)
    message.error(error.message || '保存用户失败')
  }
}

const handleSubmitPassword = async () => {
  if (!currentUserId.value) return
  
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  
  try {
    await request.put('/user', {
      id: currentUserId.value,
      password: passwordForm.newPassword
    })
    passwordModalVisible.value = false
    message.success('密码修改成功')
  } catch (error: any) {
    console.error('修改密码失败:', error)
    message.error(error.message || '修改密码失败')
  }
}

onMounted(() => {
  // 获取用户列表
  fetchUsers()
})
</script>

<style scoped>
.user-management-container {
  padding: 2rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.action-bar {
  display: flex;
  justify-content: flex-start;
}

.user-list-card {
  background-color: var(--card-background);
  border-radius: 1rem;
  box-shadow: var(--shadow-md);
  overflow: hidden;
  transition: all 0.3s ease;
}

.user-list-card:hover {
  box-shadow: var(--shadow-lg);
}

.user-table {
  width: 100%;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 70px;
  text-align: center;
}

.action-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.edit-btn {
  background-color: #1890ff;
  color: white;
}

.password-btn {
  background-color: #faad14;
  color: white;
}

.delete-btn {
  background-color: #f5222d;
  color: white;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-management-container {
    padding: 1rem;
  }
  
  .page-title {
    font-size: 1.5rem;
  }
  
  .action-buttons {
    flex-direction: column;
  }
}
</style>