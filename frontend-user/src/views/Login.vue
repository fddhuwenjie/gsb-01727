<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
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
        <div class="illustration">
          <div class="floating-card card-1">
            <div class="card-icon">👤</div>
            <span>个人信息</span>
          </div>
          <div class="floating-card card-2">
            <div class="card-icon">🔒</div>
            <span>安全设置</span>
          </div>
          <div class="floating-card card-3">
            <div class="card-icon">⚡</div>
            <span>快速便捷</span>
          </div>
        </div>
        <p class="tagline">安全、便捷的个人账号管理平台</p>
      </div>
      <div class="login-right">
        <div class="login-form-wrapper">
          <div class="form-header">
            <h2>欢迎登录</h2>
            <p>登录您的账号，管理个人信息</p>
          </div>
          <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" native-type="submit" :loading="loading" size="large" class="login-btn">
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>还没有账号？</span>
            <router-link to="/register" class="register-link">立即注册</router-link>
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

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-container {
  display: flex;
  width: 100%;
  max-width: 1000px;
  min-height: 560px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
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

.brand h1 {
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.illustration {
  position: relative;
  height: 200px;
  margin-bottom: 40px;
}

.floating-card {
  position: absolute;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
  font-weight: 500;
  animation: float 3s ease-in-out infinite;
}

.card-icon { font-size: 24px; }
.card-1 { top: 0; left: 0; animation-delay: 0s; }
.card-2 { top: 60px; right: 0; animation-delay: 0.5s; }
.card-3 { bottom: 0; left: 40px; animation-delay: 1s; }

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.tagline {
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  margin: 0;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

.login-form-wrapper { width: 100%; max-width: 360px; }

.form-header { margin-bottom: 40px; }
.form-header h2 { font-size: 28px; font-weight: 600; color: #1a1a2e; margin: 0 0 8px 0; }
.form-header p { color: #6b7280; margin: 0; font-size: 15px; }

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e5e7eb;
  padding: 4px 12px;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #11998e;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(17, 153, 142, 0.2), 0 0 0 1px #11998e;
}

.login-form :deep(.el-form-item) { margin-bottom: 24px; }

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  border: none;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(17, 153, 142, 0.4);
}

.form-footer {
  text-align: center;
  margin-top: 24px;
  color: #6b7280;
}

.register-link {
  color: #11998e;
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.register-link:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .login-left { display: none; }
  .login-container { 
    max-width: 100%; 
    min-height: auto;
    border-radius: 0;
    box-shadow: none;
  }
  .login-page {
    padding: 0;
    background: #fff;
  }
  .login-right {
    padding: 40px 24px;
  }
  .login-form-wrapper {
    max-width: 100%;
  }
  .form-header h2 {
    font-size: 24px;
  }
  .login-btn {
    height: 44px;
  }
}
</style>
