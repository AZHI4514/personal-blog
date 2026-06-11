<script setup>
import { useBlogApp } from '@/composables/useBlogApp'

const {
  galleryImages
} = useBlogApp()
</script>

<template>
  <div>
          <h1 class="gallery-title"><span class="gallery-icon">◆&nbsp;&nbsp;</span>画廊<span class="gallery-icon"></span>&nbsp;&nbsp;◆</h1>
          <p>感谢各位神明大人送来的超棒插画！今后也请多多关照～♪</p>
          <div v-for="(image, index) in galleryImages" :key="index" class="artwork-card">
            <img :src="image.path" :alt="image.title" class="artwork-img" />
            <div class="artwork-info">
              <p class="artwork-author">作者：{{ image.author }}</p>
            </div>
          </div>
        </div>
  
        <!-- 管理员结构 -->
</template>
