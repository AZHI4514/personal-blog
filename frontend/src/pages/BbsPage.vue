<script setup>
import { useBlogApp } from '@/composables/useBlogApp'

const {
  currentUser,
  authMode,
  authForm,
  authSubmitting,
  isLoggedIn,
  submitAuth,
  postForm,
  submitting,
  uploading,
  replyingToPostId,
  editingPostId,
  posts,
  handleFileUpload,
  submitPostOrReply,
  resetForm,
  startReply,
  startEdit,
  deletePostHandler
} = useBlogApp()
</script>

<template>
  <!-- 论坛结构 -->
  <div class="bbs-container">
          <h1 class="bbs-title"><span class="bbs-icon">◆&nbsp;&nbsp;</span>☽AZHI☾的小屋<span class="bbs-icon"></span>&nbsp;&nbsp;◆</h1>
  
          <div v-if="!isLoggedIn" class="post-form">
            <div class="post-form-title">登录与注册</div>
            <form @submit.prevent="submitAuth">
              <table class="form-table">
                <tr>
                  <th class="form-label">模式</th>
                  <td>
                    <label class="auth-radio"><input type="radio" value="login" v-model="authMode"> 登录</label>
                    <label class="auth-radio"><input type="radio" value="register" v-model="authMode"> 注册</label>
                  </td>
                </tr>
                <tr>
                  <th class="form-label">用户名</th>
                  <td><input type="text" v-model="authForm.username" required class="form-input" placeholder="请输入用户名"></td>
                </tr>
                <tr v-if="authMode === 'register'">
                  <th class="form-label">邮箱</th>
                  <td><input type="email" v-model="authForm.email" required class="form-input" placeholder="your@email.com"></td>
                </tr>
                <tr>
                  <th class="form-label">密码</th>
                  <td class="form-title-cell">
                    <input type="password" v-model="authForm.password" required class="form-input form-input-title" placeholder="请输入密码">
                    <input type="submit" :value="authMode === 'login' ? '登录' : '注册'" class="form-submit form-submit-top" :disabled="authSubmitting">
                  </td>
                </tr>
              </table>
            </form>
            <div v-if="isLoggedIn" class="auth-status">
              当前登录：{{ currentUser.username }}（{{ currentUser.email }}）
              <button type="button" @click="logout">退出登录</button>
            </div>
          </div>
  
          <div v-if="!isLoggedIn" class="login-required">登陆后体验更多功能~~~</div>
  
          <template v-else>
            <div class="post-form">
              <div class="post-form-title">
                {{ editingPostId ? '编辑帖子' : (replyingToPostId ? '回复帖子' : '发布新帖') }}
              </div>
  
              <form @submit.prevent="submitPostOrReply">
                <table class="form-table">
                  <tr>
                    <th class="form-label">发帖人</th>
                    <td>
                      <span class="form-hint">{{ currentUser.username }} / {{ currentUser.email }}</span>
                    </td>
                  </tr>
  
                  <tr>
                    <th class="form-label">标题</th>
                    <td class="form-title-cell">
                      <input type="text" v-model="postForm.title" :required="!replyingToPostId && !editingPostId" class="form-input form-input-title" placeholder="帖子标题">
                      <input type="submit" :value="editingPostId ? '保存修改' : (replyingToPostId ? '提交回复' : '发布新贴')" class="form-submit form-submit-top" :disabled="submitting">
                    </td>
                  </tr>
  
                  <tr>
                    <th class="form-label form-label-comment">内容</th>
                    <td>
                      <textarea v-model="postForm.content" rows="5" required class="form-textarea" placeholder="想说些什么？"></textarea>
                    </td>
                  </tr>
  
                  <tr>
                    <th class="form-label">附件</th>
                    <td>
                      <input type="file" @change="handleFileUpload" accept="image/*" class="form-file">
                      <span v-if="uploading" class="form-hint">上传中...</span>
                      <span v-if="postForm.imagePath" class="form-hint">已上传: {{ postForm.imagePath }}</span>
                    </td>
                  </tr>
  
                  <tr>
                    <th class="form-label">删除钥匙</th>
                    <td>
                      <input type="text" v-model="postForm.deleteKey" maxlength="8" pattern="[A-Za-z0-9]{1,8}" required class="form-input-small">
                      <span class="form-hint">(用于删帖，8 位以内字母数字)</span>
                    </td>
                  </tr>
  
                  <tr v-if="replyingToPostId || editingPostId">
                    <th class="form-label"></th>
                    <td class="form-cancel-cell">
                      <button type="button" class="form-cancel-btn" @click="resetForm">
                        {{ editingPostId ? '取消编辑' : '取消回复' }}
                      </button>
                    </td>
                  </tr>
                </table>
              </form>
  
              <ul class="form-notes">
                <li>可上传附件最大3MB。</li>
              </ul>
            </div>
  
            <hr>
  
            <!-- 帖子列表 -->
            <div class="post-list">
              <h3>所有帖子</h3>
              <div v-for="post in posts" :key="post.postId" class="post-item">
                <div class="post-header">
                  <strong class="post-title">{{ post.title }}</strong>
                  <span class="post-author">作者：{{ post.username }}</span>
                  <span class="post-email">邮箱：{{ post.email || '未提供' }}</span>
                </div>
                <div class="post-content">{{ post.content }}</div>
                <div v-if="post.imagePath" class="post-image"><img :src="post.imagePath" width="200"></div>
                <div class="post-meta">发布时间: {{ post.createTime }} | 回复数: {{ post.replyCount || 0 }}</div>
                <button @click="startReply(post.postId)" :disabled="replyingToPostId !== null || editingPostId !== null">回复</button>
                <button @click="startEdit(post)" :disabled="editingPostId !== null || replyingToPostId !== null">编辑</button>
                <button @click="deletePostHandler(post.postId)" class="delete-btn">删除</button>
  
                <!-- 回复列表 -->
                <div v-if="post.replies && post.replies.length" class="replies">
                  <div v-for="reply in post.replies" :key="reply.postId" class="reply-item">
                    <div class="post-header">
                      <strong class="post-title">{{ reply.title || '(无标题)' }}</strong>
                      <span class="post-author">作者：{{ reply.username }}</span>
                      <span class="post-email">邮箱：{{ reply.email || '未提供' }}</span>
                    </div>
                    <div class="post-content">{{ reply.content }}</div>
                    <div v-if="reply.imagePath" class="post-image"><img :src="reply.imagePath" width="200"></div>
                    <div class="post-meta">回复时间: {{ reply.createTime }}</div>
                    <button @click="startEdit(reply)" :disabled="editingPostId !== null || replyingToPostId !== null">编辑</button>
                    <button @click="deletePostHandler(reply.postId)" class="delete-btn">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
</template>
