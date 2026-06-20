<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapsed ? '64px' : '240px'" class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <span v-if="!isCollapsed" class="logo-text">用户管理</span>
      </div>
      <el-menu 
        :default-active="route.path" 
        router 
        :collapse="isCollapsed"
        class="sidebar-menu"
      >
        <el-menu-item index="/users" v-if="userStore.user?.role === 'ADMIN'">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><Setting /></el-icon>
          <template #title>个人中心</template>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button 
          :icon="isCollapsed ? Expand : Fold" 
          @click="isCollapsed = !isCollapsed"
          text
          class="collapse-btn"
        />
      </div>
    </el-aside>
    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="36" class="avatar">
                {{ userStore.user?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-detail">
                <span class="username">{{ userStore.user?.nickname }}</span>
                <span class="role">{{ userStore.user?.role === 'ADMIN' ? '管理员' : '用户' }}</span>
              </div>
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
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { User, Setting, Fold, Expand, ArrowDown, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapsed = ref(false)

const currentTitle = computed(() => {
  const titles = {
    '/users': '用户管理',
    '/profile': '个人中心'
  }
  return titles[route.path] || '首页'
})

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: #f0f2f5;
}

.sidebar {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.logo svg {
  width: 20px;
  height: 20px;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-menu {
  flex: 1;
  border: none;
  background: transparent;
  padding: 12px 8px;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin-bottom: 4px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s ease;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.collapse-btn {
  width: 100%;
  color: rgba(255, 255, 255, 0.6);
}

.collapse-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.main-container {
  display: flex;
  flex-direction: column;
}

.header {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  padding: 10px 16px;
  border-radius: 12px;
  transition: background 0.3s ease;
  min-width: 180px;
}

.user-info:hover {
  background: #f5f7fa;
}

.avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

.user-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  line-height: 1.4;
}

.role {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.3;
}

.arrow {
  color: #9ca3af;
  font-size: 14px;
  flex-shrink: 0;
  margin-left: 4px;
}

.main-content {
  padding: 24px;
  overflow-y: auto;
}
</style>
