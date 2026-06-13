<script setup>
import { useBlogApp } from '@/composables/useBlogApp'

const {
  isAdmin,
  adminMusicForm,
  adminImageForm,
  adminUploading,
  adminSubmitting,
  adminDeleting,
  handleAdminMusicUpload,
  handleAdminCoverUpload,
  handleAdminImageUpload,
  submitAdminMusic,
  submitAdminImage,
  deleteAdminMusic,
  deleteAdminImage,
  musicList,
  playMusic,
  galleryImages
} = useBlogApp()
</script>

<template>
  <!-- 管理员结构 -->
  <div v-if="isAdmin" class="bbs-container">
          <h1 class="bbs-title"><span class="bbs-icon">◆&nbsp;&nbsp;</span>管理员<span class="bbs-icon"></span>&nbsp;&nbsp;◆</h1>
          <p class="admin-intro">仅管理员可见。这里可以维护音乐和画廊内容，表单格式分别对应 `Music.java` 与 `Image.java`。</p>
  
          <div class="post-form">
            <div class="post-form-title">上传音乐</div>
            <form @submit.prevent="submitAdminMusic">
              <table class="form-table">
                <tr>
                  <th class="form-label">歌曲名</th>
                  <td class="form-title-cell">
                    <input type="text" v-model="adminMusicForm.title" required class="form-input form-input-title" placeholder="对应 Music.title">
                    <input type="submit" value="保存音乐" class="form-submit form-submit-top" :disabled="adminSubmitting.music">
                  </td>
                </tr>
                <tr>
                  <th class="form-label">艺术家</th>
                  <td>
                    <input type="text" v-model="adminMusicForm.artist" class="form-input" placeholder="对应 Music.artist">
                  </td>
                </tr>
                <tr>
                  <th class="form-label">音乐文件</th>
                  <td>
                    <input type="file" @change="handleAdminMusicUpload" accept=".mp3,.wav,.ogg,.flac,.m4a,audio/*" class="form-file">
                    <span v-if="adminUploading.music" class="form-hint">上传中...</span>
                    <span v-if="adminMusicForm.filePath" class="form-hint">已上传: {{ adminMusicForm.filePath }}</span>
                  </td>
                </tr>
                <tr>
                  <th class="form-label">封面图</th>
                  <td>
                    <input type="file" @change="handleAdminCoverUpload" accept="image/*" class="form-file">
                    <span v-if="adminUploading.cover" class="form-hint">上传中...</span>
                    <span v-if="adminMusicForm.coverPath" class="form-hint">已上传: {{ adminMusicForm.coverPath }}</span>
                  </td>
                </tr>
              </table>
            </form>
            <ul class="form-notes">
              <li>提交字段：`title`、`artist`、`filePath`、`coverPath`。</li>
              <li>音乐文件会先上传，再按 `Music.java` 的字段结构写入列表。</li>
            </ul>
          </div>
  
          <hr>
  
          <div class="post-form">
            <div class="post-form-title">上传图片</div>
            <form @submit.prevent="submitAdminImage">
              <table class="form-table">
                <tr>
                  <th class="form-label">图片文件</th>
                  <td>
                    <input type="file" @change="handleAdminImageUpload" accept="image/*" class="form-file">
                    <span v-if="adminUploading.image" class="form-hint">上传中...</span>
                    <span v-if="adminImageForm.path" class="form-hint">已上传: {{ adminImageForm.path }}</span>
                  </td>
                </tr>
                <tr>
                  <th class="form-label">作者</th>
                  <td class="form-title-cell">
                    <input type="text" v-model="adminImageForm.author" required class="form-input form-input-title" placeholder="对应 Image.author">
                    <input type="submit" value="保存图片" class="form-submit form-submit-top" :disabled="adminSubmitting.image">
                  </td>
                </tr>
              </table>
            </form>
            <ul class="form-notes">
              <li>提交字段：`path`、`author`。</li>
              <li>图片文件会先上传，再按 `Image.java` 的字段结构写入画廊。</li>
            </ul>
          </div>
  
          <hr>
  
          <div class="music-list admin-music-list">
            <h3>曲目列表</h3>
            <table class="music-table">
              <thead>
                <tr><th>序号</th><th>封面</th><th>曲名</th><th>艺术家</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="(music, idx) in musicList" :key="music.musicId" @click="playMusic(music)" class="music-item">
                  <td>{{ idx + 1 }}</td>
                  <td><img :src="music.coverPath" class="list-cover" alt="封面"></td>
                  <td>{{ music.title }}</td>
                  <td>{{ music.artist }}</td>
                  <td class="admin-actions-cell">
                    <button class="play-btn" @click.stop="playMusic(music)">▶ 播放</button>
                    <button
                      class="delete-btn admin-delete-btn"
                      type="button"
                      @click.stop="deleteAdminMusic(music)"
                      :disabled="adminDeleting.musicId === music.musicId"
                    >
                      {{ adminDeleting.musicId === music.musicId ? '删除中...' : '删除音乐' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
  
          <hr>
  
          <div class="admin-gallery-list">
            <h3>画廊</h3>
            <div v-for="(image, index) in galleryImages" :key="image.id || index" class="artwork-card">
              <img :src="image.path" :alt="image.title" class="artwork-img" />
              <div class="artwork-info">
                <p class="artwork-author">作者：{{ image.author }}</p>
                <button
                  class="delete-btn admin-delete-btn"
                  type="button"
                  @click="deleteAdminImage(image)"
                  :disabled="adminDeleting.imageId === image.id"
                >
                  {{ adminDeleting.imageId === image.id ? '删除中...' : '删除图片' }}
                </button>
              </div>
            </div>
          </div>
        </div>
</template>
