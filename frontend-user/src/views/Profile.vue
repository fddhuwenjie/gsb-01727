<template>
  <div class="profile-page">
    <!-- 用户信息卡片 -->
    <div class="user-banner">
      <div class="banner-bg"></div>
      <div class="user-info-section">
        <div class="avatar-wrapper">
          <el-avatar :size="88" class="user-avatar">
            {{ userStore.user?.nickname?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="avatar-badge">
            <el-icon><Check /></el-icon>
          </div>
        </div>
        <div class="user-meta">
          <h1>{{ userStore.user?.nickname || '用户' }}</h1>
          <div class="user-tags">
            <span class="tag username-tag">@{{ userStore.user?.username }}</span>
            <span class="tag id-tag">ID: {{ userStore.user?.id }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 设置区域 -->
    <div class="settings-section">
      <div class="settings-nav">
        <div 
          class="nav-item" 
          :class="{ active: activeTab === 'profile' }"
          @click="activeTab = 'profile'"
        >
          <el-icon><User /></el-icon>
          <span>基本信息</span>
        </div>
        <div 
          class="nav-item" 
          :class="{ active: activeTab === 'security' }"
          @click="activeTab = 'security'"
        >
          <el-icon><Lock /></el-icon>
          <span>安全设置</span>
        </div>
      </div>

      <div class="settings-content">
        <!-- 基本信息 -->
        <div v-show="activeTab === 'profile'" class="tab-content">
          <div class="content-header">
            <h3>基本信息</h3>
            <p>管理您的个人资料信息</p>
          </div>
          <el-form :model="profileForm" :rules="profileRules" ref="profileRef" label-position="top" class="settings-form">
            <div class="form-row">
              <el-form-item label="用户名" class="form-item-half">
                <el-input :value="userStore.user?.username" disabled>
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="昵称" prop="nickname" class="form-item-half">
                <el-input v-model="profileForm.nickname" placeholder="请输入昵称">
                  <template #prefix><el-icon><EditPen /></el-icon></template>
                </el-input>
              </el-form-item>
            </div>
            <div class="form-row">
              <el-form-item label="手机号" prop="phone" class="form-item-half">
                <el-input v-model="profileForm.phone" placeholder="请输入手机号">
                  <template #prefix><el-icon><Phone /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="邮箱" prop="email" class="form-item-half">
                <el-input v-model="profileForm.email" placeholder="请输入邮箱">
                  <template #prefix><el-icon><Message /></el-icon></template>
                </el-input>
              </el-form-item>
            </div>
            <div class="form-actions">
              <el-button type="primary" @click="updateProfile" :loading="profileLoading" class="save-btn">
                <el-icon><Check /></el-icon>
                保存修改
              </el-button>
            </div>
          </el-form>
        </div>

        <!-- 安全设置 -->
        <div v-show="activeTab === 'security'" class="tab-content">
          <div class="content-header">
            <h3>安全设置</h3>
            <p>修改您的登录密码</p>
          </div>
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordRef" label-position="top" class="settings-form">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <div class="form-row">
              <el-form-item label="新密码" prop="newPassword" class="form-item-half">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码">
                  <template #prefix><el-icon><Key /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword" class="form-item-half">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码">
                  <template #prefix><el-icon><Key /></el-icon></template>
                </el-input>
              </el-form-item>
            </div>
            <div class="password-tips">
              <div class="tip-item">
                <el-icon><InfoFilled /></el-icon>
                <span>密码长度至少6位</span>
              </div>
              <div class="tip-item">
                <el-icon><InfoFilled /></el-icon>
                <span>建议使用字母、数字和符号的组合</span>
              </div>
            </div>
            <div class="form-actions">
              <el-button type="primary" @click="changePassword" :loading="passwordLoading" class="save-btn">
                <el-icon><Check /></el-icon>
                修改密码
              </el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock, Check, EditPen, Phone, Message, Key, InfoFilled } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import api from '../api'

const userStore = useUserStore()
const profileRef = ref()
const passwordRef = ref()
const profileLoading = ref(false)
const passwordLoading = ref(false)
const activeTab = ref('profile')

const profileForm = reactive({ nickname: '', phone: '', email: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const profileRules = {
  phone: [{ pattern: /^1[3-9]\d{9}$|^$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (value !== passwordForm.newPassword) callback(new Error('两次密码不一致'))
      else callback()
    }, trigger: 'blur' }
  ]
}

const loadProfile = async () => {
  const res = await api.get('/user/profile')
  const data = res.data.data
  profileForm.nickname = data.nickname || ''
  profileForm.phone = data.phone || ''
  profileForm.email = data.email || ''
}

const updateProfile = async () => {
  await profileRef.value.validate()
  profileLoading.value = true
  try {
    const res = await api.put('/user/profile', profileForm)
    userStore.updateProfile(res.data.data)
    ElMessage.success('修改成功')
  } finally { profileLoading.value = false }
}

const changePassword = async () => {
  await passwordRef.value.validate()
  passwordLoading.value = true
  try {
    await api.put('/user/password', passwordForm)
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } finally { passwordLoading.value = false }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  max-width: 900px;
  width: 100%;
}

.user-banner {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 24px;
}

.banner-bg {
  height: 120px;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.user-info-section {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding: 0 32px 24px;
  background: #fff;
  margin-top: -1px;
}

.avatar-wrapper {
  position: relative;
  margin-top: -44px;
}

.user-avatar {
  border: 4px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: #fff;
  font-size: 36px;
  font-weight: 600;
}

.avatar-badge {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  background: #10b981;
  border-radius: 50%;
  border: 2px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
}

.user-meta {
  padding-bottom: 4px;
}

.user-meta h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px 0;
}

.user-tags {
  display: flex;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.username-tag {
  background: linear-gradient(135deg, rgba(17, 153, 142, 0.1) 0%, rgba(56, 239, 125, 0.1) 100%);
  color: #11998e;
}

.id-tag {
  background: #f3f4f6;
  color: #6b7280;
}

.settings-section {
  display: flex;
  gap: 24px;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
}

.settings-nav {
  width: 200px;
  padding: 24px 16px;
  background: #f9fafb;
  border-right: 1px solid #f0f0f0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 12px;
  cursor: pointer;
  color: #6b7280;
  font-weight: 500;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.nav-item:hover {
  background: #fff;
  color: #374151;
}

.nav-item.active {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(17, 153, 142, 0.3);
}

.settings-content {
  flex: 1;
  padding: 32px;
}

.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.content-header {
  margin-bottom: 32px;
}

.content-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 4px 0;
}

.content-header p {
  color: #9ca3af;
  margin: 0;
  font-size: 14px;
}

.settings-form {
  max-width: 560px;
}

.form-row {
  display: flex;
  gap: 20px;
}

.form-item-half {
  flex: 1;
}

.settings-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  padding-bottom: 8px;
}

.settings-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e5e7eb;
  padding: 4px 12px;
}

.settings-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #11998e;
}

.settings-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(17, 153, 142, 0.2), 0 0 0 1px #11998e;
}

.settings-form :deep(.el-input__prefix) {
  color: #9ca3af;
}

.password-tips {
  background: #f9fafb;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 24px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 8px;
}

.tip-item:last-child {
  margin-bottom: 0;
}

.tip-item .el-icon {
  color: #9ca3af;
}

.form-actions {
  padding-top: 8px;
}

.save-btn {
  height: 44px;
  padding: 0 28px;
  border-radius: 10px;
  font-weight: 500;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.save-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(17, 153, 142, 0.4);
}

@media (max-width: 768px) {
  .profile-page {
    max-width: 100%;
  }
  
  .user-banner {
    border-radius: 0;
    margin-bottom: 16px;
  }
  
  .banner-bg {
    height: 100px;
  }
  
  .user-info-section {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0 16px 20px;
    gap: 12px;
  }
  
  .avatar-wrapper {
    margin-top: -40px;
  }
  
  .user-avatar {
    width: 72px !important;
    height: 72px !important;
    font-size: 28px !important;
  }
  
  .user-meta h1 {
    font-size: 20px;
  }
  
  .user-tags {
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .settings-section {
    flex-direction: column;
    border-radius: 16px;
    gap: 0;
  }
  
  .settings-nav {
    width: 100%;
    display: flex;
    padding: 12px;
    gap: 8px;
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
  }
  
  .nav-item {
    flex: 1;
    justify-content: center;
    padding: 12px;
    margin-bottom: 0;
  }
  
  .nav-item span {
    display: none;
  }
  
  .settings-content {
    padding: 20px 16px;
  }
  
  .content-header {
    margin-bottom: 24px;
  }
  
  .content-header h3 {
    font-size: 18px;
  }
  
  .form-row {
    flex-direction: column;
    gap: 0;
  }
  
  .form-item-half {
    width: 100%;
  }
  
  .settings-form {
    max-width: 100%;
  }
  
  .save-btn {
    width: 100%;
    justify-content: center;
  }
  
  .password-tips {
    padding: 12px;
  }
  
  .tip-item {
    font-size: 12px;
  }
}
</style>
