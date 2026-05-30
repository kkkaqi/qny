<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header"><h1>📅 言历</h1><p>{{ isRegister ? '注册新账号' : '登录你的账号' }}</p></div>
      <form @submit.prevent="handleSubmit" class="login-form">
        <div class="form-group"><label>用户名</label><input v-model="username" class="form-input" placeholder="请输入用户名" required /></div>
        <div class="form-group"><label>密码</label><input v-model="password" type="password" class="form-input" placeholder="请输入密码" required /></div>
        <div v-if="isRegister" class="form-group"><label>昵称（可选）</label><input v-model="nickname" class="form-input" placeholder="显示名称" /></div>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <button type="submit" class="btn-submit" :disabled="loading">{{ loading ? '请稍候...' : (isRegister ? '注册' : '登录') }}</button>
      </form>
      <div class="login-footer"><button class="btn-switch" @click="toggleMode">{{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}</button></div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
const router = useRouter()
const isRegister = ref(false), username = ref(''), password = ref(''), nickname = ref(''), error = ref(''), loading = ref(false)
function toggleMode() { isRegister.value = !isRegister.value; error.value = '' }
async function handleSubmit() { error.value = ''; loading.value = true; try { const url = isRegister.value ? '/api/auth/register' : '/api/auth/login'; const body = { username: username.value.trim(), password: password.value }; if (isRegister.value && nickname.value.trim()) body.nickname = nickname.value.trim(); await axios.post(url, body); router.replace('/') } catch (e) { error.value = e.response?.data?.message || '请求失败' } finally { loading.value = false } }
</script>

<style scoped>
.login-page { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: #f5f5f5; }
.login-card { width: 380px; background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); padding: 40px 36px; }
.login-header { text-align: center; margin-bottom: 28px; }
.login-header h1 { font-size: 24px; color: #333; margin-bottom: 6px; }
.login-header p { font-size: 14px; color: #999; }
.login-form { display: flex; flex-direction: column; gap: 14px; }
.form-group label { display: block; font-size: 13px; color: #666; margin-bottom: 4px; }
.form-input { width: 100%; padding: 10px 14px; border: 1px solid #e0e0e0; border-radius: 6px; font-size: 14px; outline: none; }
.form-input:focus { border-color: #c0392b; box-shadow: 0 0 0 3px rgba(192,57,43,0.08); }
.error-msg { background: #fef2f2; color: #e74c3c; padding: 8px 12px; border-radius: 6px; font-size: 13px; }
.btn-submit { padding: 11px; background: #c0392b; color: white; border: none; border-radius: 6px; font-size: 15px; cursor: pointer; font-weight: 500; }
.btn-submit:disabled { opacity: 0.6; cursor: not-allowed; }
.login-footer { text-align: center; margin-top: 18px; }
.btn-switch { background: none; border: none; color: #c0392b; font-size: 13px; cursor: pointer; }
.btn-switch:hover { text-decoration: underline; }
</style>
