<script setup>
import { RouterView } from 'vue-router'
import { useBlogApp, useBlogAppLifecycle } from '@/composables/useBlogApp'

const {
  isSidebarOpen,
  startupErrors,
  isAuthMenuOpen,
  isLoggedIn,
  currentUser,
  isAdmin,
  toggleSidebar,
  closeSidebar,
  openAuthPage,
  toggleAuthMenu,
  logout,
  showPage
} = useBlogApp()

useBlogAppLifecycle()
</script>

<template>
  <div id="app">
    <div class="mobile-top-bar">
      <button class="mobile-menu-btn" type="button" @click="toggleSidebar" aria-label="展开菜单">☰</button>
      <div class="mobile-top-title">☽星尘观测站☾</div>
      <div class="mobile-top-links">
        <a href="#" @click.prevent="openAuthPage('login')">登录</a>
        <span>|</span>
        <a href="#" @click.prevent="openAuthPage('register')">注册</a>
        <span>|</span>
        <button
          class="mobile-user-link"
          type="button"
          @click="toggleAuthMenu"
          :aria-expanded="isAuthMenuOpen"
        >
          {{ isLoggedIn ? currentUser.username : '未登录' }}
        </button>
      </div>
      <div v-if="startupErrors.length" class="startup-error-banner">
        {{ startupErrors[0] }}
      </div>
      <div v-if="isAuthMenuOpen" class="mobile-auth-popover">
        <div class="auth-status mobile-auth-status">
          <template v-if="isLoggedIn">
            当前登录：{{ currentUser.username }}（{{ currentUser.email }}）
            <button type="button" @click="logout">退出登录</button>
          </template>
          <template v-else>
            当前登录：未登录
          </template>
        </div>
      </div>
    </div>
    <div class="mobile-overlay" :class="{ active: isSidebarOpen }" @click="closeSidebar"></div>

    <!-- 侧边导航栏-->
    <aside class="sidebar" :class="{ 'sidebar-open': isSidebarOpen }">
      <div class="menu-header">☽星尘观测站☾</div>
      <ul class="menu-list">
        <li><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('home')">首页</a></li>
        <li><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('profile')">个人资料</a> · 100问100答</li>
        <li><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('gallery')">画廊</a></li>
        <li><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('bbs')">BBS/留言板</a></li>
        <li><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('rules')">※使用规定</a><span>←必读</span></li>
        <li><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('games')">游戏角</a>&nbsp;制作中</li>
        <li><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('music')">音乐</a></li>
        <li v-if="isAdmin"><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('admin')">管理员</a></li>
        <li><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('links')">链接集</a></li>
      </ul>
      <div class="menu-notice">(个人博客同好站)</div>
    </aside>

    <!-- 主内容区域：页面的结构-->
    <main class="main-content">
      <RouterView />

      <div class="footer-link footer-pad last">
        <p>
          <span>
            <img alt="" src="@/assets/images/foot-icp.png" style="margin-right:5px;"><a href="https://beian.miit.gov.cn/#/Integrated/index" target="_blank">闽ICP备2026020308号</a>
          </span>
        </p>
        <p>
          <span>
            <img alt="" src="@/assets/images/foot-ga.png" style="margin-right:5px;"><a href="https://beian.mps.gov.cn/#/query/webSearch?code=11010102000001" target="_blank">闽公网安备35082102000227号</a>
          </span>
        </p>
      </div>
    </main>
  </div>
</template>
