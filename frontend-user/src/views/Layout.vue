<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <span class="logo-text">用户中心</span>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click" @command="handleCommand">
          <div class="user-info">
            <el-avatar :size="36" class="avatar">
              {{ userStore.user?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <span class="username">{{ userStore.user?.nickname }}</span>
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { User, ArrowDown, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}

.header {
  height: 64px;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo svg { width: 20px; height: 20px; }

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  padding: 10px 16px;
  border-radius: 12px;
  transition: background 0.3s ease;
}

.user-info:hover { background: #f5f7fa; }

.avatar {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

.username { 
  font-size: 14px; 
  font-weight: 600; 
  color: #1a1a2e;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow { 
  color: #9ca3af; 
  font-size: 14px;
  flex-shrink: 0;
}

.main-content {
  padding: 40px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .header {
    padding: 0 16px;
    height: 56px;
  }
  .logo-text {
    font-size: 16px;
  }
  .username {
    display: none;
  }
  .user-info {
    padding: 8px;
    gap: 8px;
  }
  .main-content {
    padding: 16px;
  }
}
</style>
