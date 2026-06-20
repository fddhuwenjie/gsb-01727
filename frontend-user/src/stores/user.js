import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const setAuth = (data) => {
    token.value = data.token
    user.value = data.user
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
  }

  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const login = async (username, password) => {
    const res = await api.post('/auth/login', { username, password })
    if (res.data.code === 200) {
      setAuth(res.data.data)
      return true
    }
    throw new Error(res.data.message)
  }

  const register = async (data) => {
    const res = await api.post('/auth/register', data)
    if (res.data.code === 200) {
      setAuth(res.data.data)
      return true
    }
    throw new Error(res.data.message)
  }

  const updateProfile = (userData) => {
    user.value = userData
    localStorage.setItem('user', JSON.stringify(userData))
  }

  return { token, user, login, register, logout, updateProfile }
})
