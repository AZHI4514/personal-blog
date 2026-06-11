import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/pages/NotFoundPage.vue')
    }
  ],
})

export default router
