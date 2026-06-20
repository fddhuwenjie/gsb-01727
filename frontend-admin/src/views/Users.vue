<template>
  <div class="users-page">
    <div class="page-header">
      <div class="header-info">
        <h1>用户管理</h1>
        <p>管理系统中的所有用户账号</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="showAddDialog" class="add-btn">
        添加用户
      </el-button>
    </div>

    <el-card class="table-card" shadow="never">
      <el-table :data="users" v-loading="loading" stripe class="user-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" class="user-avatar">
                {{ row.nickname?.charAt(0) || row.username?.charAt(0) }}
              </el-avatar>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">
            {{ row.email || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="row.role === 'ADMIN' ? 'danger' : 'primary'" 
              effect="light"
              round
            >
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="90">
          <template #default="{ row }">
            <el-tag 
              :type="row.enabled ? 'success' : 'info'" 
              effect="light"
              round
            >
              {{ row.enabled ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="showEditDialog(row)">
              编辑
            </el-button>
            <el-button size="small" text type="warning" @click="resetPassword(row.id)">
              重置密码
            </el-button>
            <el-button 
              size="small" 
              text 
              :type="row.enabled ? 'danger' : 'success'" 
              @click="toggleStatus(row.id)"
            >
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑用户' : '添加用户'" 
      width="480px"
      class="user-dialog"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" label-position="right">
        <el-form-item label="用户名" prop="username" v-if="!isEdit">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone" v-if="isEdit">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email" v-if="isEdit">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" v-if="isEdit">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ isEdit ? '保存修改' : '创建用户' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api'

const users = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  role: 'USER'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await api.get('/admin/users')
    users.value = res.data.data
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  Object.assign(form, { username: '', password: '', nickname: '', phone: '', email: '', role: 'USER' })
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, { nickname: row.nickname, phone: row.phone || '', email: row.email || '', role: row.role })
  dialogVisible.value = true
}

const submitForm = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await api.put(`/admin/users/${editId.value}`, form)
      ElMessage.success('修改成功')
    } else {
      await api.post('/admin/users', form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchUsers()
  } finally {
    submitting.value = false
  }
}

const resetPassword = async (id) => {
  await ElMessageBox.confirm('确定要重置该用户的密码吗？密码将被重置为 123456', '重置密码', { 
    type: 'warning',
    confirmButtonText: '确定重置',
    cancelButtonText: '取消'
  })
  await api.post(`/admin/users/${id}/reset-password`)
  ElMessage.success('密码已重置')
}

const toggleStatus = async (id) => {
  await api.post(`/admin/users/${id}/toggle-status`)
  ElMessage.success('状态已更新')
  fetchUsers()
}

onMounted(fetchUsers)
</script>

<style scoped>
.users-page {
  max-width: 1400px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-info h1 {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 4px 0;
}

.header-info p {
  color: #6b7280;
  margin: 0;
  font-size: 14px;
}

.add-btn {
  height: 40px;
  padding: 0 20px;
  border-radius: 10px;
  font-weight: 500;
}

.table-card {
  border-radius: 16px;
  border: none;
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.user-table {
  border-radius: 16px;
}

.user-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 500;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 14px;
}

.user-dialog :deep(.el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.user-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.user-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
}
</style>
