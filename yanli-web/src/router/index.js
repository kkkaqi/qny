import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../components/HomePage.vue'
import LoginPage from '../components/LoginPage.vue'
import axios from 'axios'

const routes = [
  { path: '/', component: HomePage, meta: { requiresAuth: true } },
  { path: '/login', component: LoginPage }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth) {
    try { await axios.get('/api/auth/me'); return true } catch { return '/login' }
  }
  if (to.path === '/login') {
    try { await axios.get('/api/auth/me'); return '/' } catch { return true }
  }
  return true
})

export default router
