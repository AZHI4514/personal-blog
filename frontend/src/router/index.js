import { createRouter, createWebHistory } from 'vue-router'

/**
 * Vue Router 实例。
 * - 使用 HTML5 History 模式（createWebHistory），URL 干净无 hash。
 * - 所有页面组件均为懒加载（动态 import），按需分包。
 * - scrollBehavior 始终滚动到页面顶部。
 * - 最后一条为 catch-all 路由，匹配所有未定义路径 → 404 页面。
 */
const router = createRouter({
  // HTML5 History 模式，基础路径来自 Vite 环境变量 BASE_URL
  history: createWebHistory(import.meta.env.BASE_URL),

  // 每次路由切换自动滚动到页面顶部
  scrollBehavior() {
    return { top: 0 }
  },

  // 路由表：全部使用懒加载，减少首屏打包体积
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/HomePage.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/pages/ProfilePage.vue')
    },
    {
      path: '/gallery',
      name: 'gallery',
      component: () => import('@/pages/GalleryPage.vue')
    },
    {
      path: '/bbs',
      name: 'bbs',
      component: () => import('@/pages/BbsPage.vue')
    },
    {
      path: '/rules',
      name: 'rules',
      component: () => import('@/pages/RulesPage.vue')
    },
    {
      path: '/games',
      name: 'games',
      component: () => import('@/pages/GamesPage.vue')
    },
    {
      path: '/music',
      name: 'music',
      component: () => import('@/pages/MusicPage.vue')
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/pages/AdminPage.vue')
    },
    {
      path: '/links',
      name: 'links',
      component: () => import('@/pages/LinksPage.vue')
    },
    // Catch-all：匹配所有未定义的路径，展示 404 页面
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/pages/NotFoundPage.vue')
    }
  ],
})

export default router
