<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-left">
        <div class="brand">
          <div class="logo">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h1>用户中心</h1>
        </div>
        <div class="welcome-text">
          <h2>加入我们</h2>
          <p>创建账号，开启您的个人管理之旅</p>
        </div>
        <div class="features">
          <div class="feature-item">
            <span class="feature-icon">✓</span>
            <span>安全可靠的账号体系</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✓</span>
            <span>便捷的个人信息管理</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✓</span>
            <span>完善的隐私保护机制</span>
          </div>
        </div>
      </div>
      <div class="register-right">
        <div class="register-form-wrapper">
          <div class="form-header">
            <h2>创建账号</h2>
            <p>填写以下信息完成注册</p>
          </div>
          <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleRegister" class="register-form">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" placeholder="请确认密码" size="large" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称（选填）" size="large" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" native-type="submit" :loading="loading" size="large" class="register-btn">
                {{ loading ? '注册中...' : '立即注册' }}
              </el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>已有账号？</span>
            <router-link to="/login" class="login-link">立即登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '', confirmPassword: '', nickname: '' })
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在3-50之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (value !== form.password) callback(new Error('两次密码不一致'))
      else callback()
    }, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.register(form)
    ElMessage.success('注册成功')
    router.push('/')
  } catch (e) {} finally { loading.value = false }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-container {
  display: flex;
  width: 100%;
  max-width: 1000px;
  min-height: 600px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.register-left {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 60px;
}

.logo {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo svg { width: 28px; height: 28px; }
.brand h1 { color: #fff; font-size: 24px; font-weight: 600; margin: 0; }

.welcome-text { margin-bottom: 40px; }
.welcome-text h2 { color: #fff; font-size: 32px; font-weight: 600; margin: 0 0 12px 0; }
.welcome-text p { color: rgba(255, 255, 255, 0.8); font-size: 16px; margin: 0; }

.features { display: flex; flex-direction: column; gap: 16px; }
.feature-item { display: flex; align-items: center; gap: 12px; color: rgba(255, 255, 255, 0.9); }
.feature-icon { width: 24px; height: 24px; background: rgba(255,255,255,0.2); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; }

.register-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

.register-form-wrapper { width: 100%; max-width: 360px; }
.form-header { margin-bottom: 32px; }
.form-header h2 { font-size: 28px; font-weight: 600; color: #1a1a2e; margin: 0 0 8px 0; }
.form-header p { color: #6b7280; margin: 0; font-size: 15px; }

.register-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e5e7eb;
  padding: 4px 12px;
}

.register-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #667eea; }
.register-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2), 0 0 0 1px #667eea; }
.register-form :deep(.el-form-item) { margin-bottom: 20px; }

.register-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.register-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4); }

.form-footer { text-align: center; margin-top: 24px; color: #6b7280; }
.login-link { color: #667eea; text-decoration: none; font-weight: 500; margin-left: 4px; }
.login-link:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .register-left { display: none; }
  .register-container { 
    max-width: 100%;
    min-height: auto;
    border-radius: 0;
    box-shadow: none;
  }
  .register-page {
    padding: 0;
    background: #fff;
  }
  .register-right {
    padding: 40px 24px;
  }
  .register-form-wrapper {
    max-width: 100%;
  }
  .form-header h2 {
    font-size: 24px;
  }
  .register-btn {
    height: 44px;
  }
}
</style>
