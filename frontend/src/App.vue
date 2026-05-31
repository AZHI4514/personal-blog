<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { recordVisitor, getTotalVisitors } from '@/api/visitor'
import { sendClap } from '@/api/clap'
import { createImage, deleteImage, getImages } from '@/api/gallery'
import { createMusic, deleteMusic, getMusics } from '@/api/music'
import { deletePost } from '@/api/post'
import { uploadImageFile, uploadMusicFile } from '@/api/upload'
import { loginUser, logoutUser, registerUser } from '@/api/user'

const currentPage = ref('home')
const isSidebarOpen = ref(false)
const visitorCount = ref(0)
const galleryImages = ref([
  { path: 'https://picsum.photos/id/104/200/150', author: '张三' },
  { path: 'https://picsum.photos/id/15/200/150', author: '李四' },
  { path: 'https://picsum.photos/id/13/200/150', author: '宋江' }
])
const currentUser = ref(JSON.parse(localStorage.getItem('currentUser') || 'null'))
const authMode = ref('login')
const isAuthMenuOpen = ref(false)
const authForm = ref({
  username: '',
  password: '',
  email: ''
})
const authSubmitting = ref(false)
const isLoggedIn = computed(() => !!currentUser.value)
const isAdmin = computed(() => currentUser.value?.username === 'AZHI4514')
const canDeleteWithoutKey = computed(() => currentUser.value?.username === 'AZHI4514')
const adminMusicForm = ref({
  title: '',
  artist: '',
  filePath: '',
  coverPath: ''
})
const adminImageForm = ref({
  path: '',
  author: 'AZHI4514'
})
const adminUploading = ref({
  music: false,
  cover: false,
  image: false
})
const adminSubmitting = ref({
  music: false,
  image: false
})
const adminDeleting = ref({
  musicId: null,
  imageId: null
})

const handleClap = async () => {
  try {
    await sendClap()
    alert('感谢你的拍手！')
  } catch (err) {
    console.error('拍手失败', err)
    alert('拍手失败，请稍后再试')
  }
}

const submitAuth = async () => {
  if (!authForm.value.username.trim() || !authForm.value.password.trim()) {
    alert('用户名和密码不能为空')
    return
  }
  if (authMode.value === 'register' && !authForm.value.email.trim()) {
    alert('邮箱不能为空')
    return
  }

  authSubmitting.value = true
  try {
    const user = authMode.value === 'login'
      ? await loginUser({ username: authForm.value.username, password: authForm.value.password })
      : await registerUser(authForm.value)
    currentUser.value = user
    localStorage.setItem('currentUser', JSON.stringify(user))
    authForm.value = { username: '', password: '', email: '' }
    alert(authMode.value === 'login' ? '登录成功' : '注册成功，已自动登录')
    await loadPosts()
  } catch (err) {
    console.error(authMode.value === 'login' ? '登录失败' : '注册失败', err)
    alert(err.message || (authMode.value === 'login' ? '登录失败' : '注册失败'))
  } finally {
    authSubmitting.value = false
  }
}

const logout = async () => {
  try {
    await logoutUser()
  } catch (err) {
    console.error('退出登录失败', err)
  }
  currentUser.value = null
  isAuthMenuOpen.value = false
  localStorage.removeItem('currentUser')
  posts.value = []
  resetForm()
  resetAdminForms()
  if (currentPage.value === 'admin') {
    currentPage.value = 'home'
  }
}

// ==================== BBS 统一表单 ====================
const postForm = ref({
  username: '',
  email: '',
  title: '',
  content: '',
  imagePath: '',
  deleteKey: ''
})
const submitting = ref(false)
const uploading = ref(false)
const replyingToPostId = ref(null)
const editingPostId = ref(null)

// 帖子列表（不再分页）
const posts = ref([])

// ---------- 统一附件上传函数 ----------
const uploadImage = async (event, targetForm) => {
  const file = event.target.files[0]
  if (!file) return
  uploading.value = true
  try {
    const res = await uploadImageFile(file)
    targetForm.imagePath = res.filePath
  } catch (err) {
    console.error('上传失败', err)
    alert('图片上传失败')
  } finally {
    uploading.value = false
  }
}

const handleFileUpload = (event) => uploadImage(event, postForm.value)

const resetAdminForms = () => {
  adminMusicForm.value = {
    title: '',
    artist: '',
    filePath: '',
    coverPath: ''
  }
  adminImageForm.value = {
    path: '',
    author: currentUser.value?.username || 'AZHI4514'
  }
}

const handleAdminMusicUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  adminUploading.value.music = true
  try {
    const res = await uploadMusicFile(file)
    adminMusicForm.value.filePath = res.filePath
  } catch (err) {
    console.error('音乐上传失败', err)
    alert('音乐上传失败，请稍后再试')
  } finally {
    adminUploading.value.music = false
    event.target.value = ''
  }
}

const handleAdminCoverUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  adminUploading.value.cover = true
  try {
    const res = await uploadImageFile(file)
    adminMusicForm.value.coverPath = res.filePath
  } catch (err) {
    console.error('封面上传失败', err)
    alert('封面上传失败，请稍后再试')
  } finally {
    adminUploading.value.cover = false
    event.target.value = ''
  }
}

const handleAdminImageUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  adminUploading.value.image = true
  try {
    const res = await uploadImageFile(file)
    adminImageForm.value.path = res.filePath
  } catch (err) {
    console.error('图片上传失败', err)
    alert('图片上传失败，请稍后再试')
  } finally {
    adminUploading.value.image = false
    event.target.value = ''
  }
}

const submitAdminMusic = async () => {
  if (!isAdmin.value) {
    alert('只有管理员可以使用这个页面')
    return
  }
  if (!adminMusicForm.value.title.trim() || !adminMusicForm.value.filePath.trim()) {
    alert('请先填写歌曲名并上传音乐文件')
    return
  }

  adminSubmitting.value.music = true
  try {
    await createMusic({
      title: adminMusicForm.value.title.trim(),
      artist: adminMusicForm.value.artist.trim(),
      filePath: adminMusicForm.value.filePath.trim(),
      coverPath: adminMusicForm.value.coverPath.trim()
    })
    alert('音乐已添加到列表')
    adminMusicForm.value = {
      title: '',
      artist: '',
      filePath: '',
      coverPath: ''
    }
    await loadMusicList()
  } catch (err) {
    console.error('添加音乐失败', err)
    alert(err.message || '添加音乐失败')
  } finally {
    adminSubmitting.value.music = false
  }
}

const submitAdminImage = async () => {
  if (!isAdmin.value) {
    alert('只有管理员可以使用这个页面')
    return
  }
  if (!adminImageForm.value.path.trim() || !adminImageForm.value.author.trim()) {
    alert('请先上传图片并填写作者')
    return
  }

  adminSubmitting.value.image = true
  try {
    await createImage({
      path: adminImageForm.value.path.trim(),
      author: adminImageForm.value.author.trim()
    })
    alert('图片已添加到画廊')
    adminImageForm.value = {
      path: '',
      author: currentUser.value?.username || 'AZHI4514'
    }
    await loadGallery()
  } catch (err) {
    console.error('添加图片失败', err)
    alert(err.message || '添加图片失败')
  } finally {
    adminSubmitting.value.image = false
  }
}

const deleteAdminMusic = async (music) => {
  if (!isAdmin.value) {
    alert('只有管理员可以使用这个页面')
    return
  }
  if (!confirm(`确定删除音乐《${music.title}》吗？`)) {
    return
  }

  adminDeleting.value.musicId = music.musicId
  try {
    await deleteMusic(music.musicId)
    if (musicList.value[currentMusicIndex.value]?.musicId === music.musicId) {
      releaseAudio()
      currentMusicIndex.value = -1
      currentTime.value = 0
      duration.value = 0
      isPlaying.value = false
    }
    await loadMusicList()
    alert('音乐已删除')
  } catch (err) {
    console.error('删除音乐失败', err)
    alert(err.message || '删除音乐失败')
  } finally {
    adminDeleting.value.musicId = null
  }
}

const deleteAdminImage = async (image) => {
  if (!isAdmin.value) {
    alert('只有管理员可以使用这个页面')
    return
  }
  if (!confirm(`确定删除作者为 ${image.author} 的这张图片吗？`)) {
    return
  }

  adminDeleting.value.imageId = image.id
  try {
    await deleteImage(image.id)
    await loadGallery()
    alert('图片已删除')
  } catch (err) {
    console.error('删除图片失败', err)
    alert(err.message || '删除图片失败')
  } finally {
    adminDeleting.value.imageId = null
  }
}

// ---------- 统一提交：发帖、回复或编辑 ----------
const submitPostOrReply = async () => {
  if (!isLoggedIn.value) {
    alert('请先登录')
    return
  }
  if (!postForm.value.content.trim()) {
    alert('内容不能为空')
    return
  }
  submitting.value = true
  try {
    if (editingPostId.value) {
      // 编辑模式
      const { updatePost } = await import('@/api/post')
      const payload = {
        deleteKey: postForm.value.deleteKey,
        title: postForm.value.title,
        content: postForm.value.content,
        imagePath: postForm.value.imagePath
      }
      await updatePost(editingPostId.value, payload)
      alert('修改成功')
    } else {
      // 发帖或回复模式
      const { createPost } = await import('@/api/post')
      const payload = {
        username: currentUser.value.username,
        email: currentUser.value.email,
        title: postForm.value.title,
        content: postForm.value.content,
        parentId: replyingToPostId.value,
        imagePath: postForm.value.imagePath,
        deleteKey: postForm.value.deleteKey
      }
      await createPost(payload)
      alert(replyingToPostId.value ? '回复成功' : '发布成功')
    }
    resetForm()
    loadPosts()
  } catch (err) {
    console.error(editingPostId.value ? '修改失败' : '发布失败', err)
    const msg = err.response?.data?.message || (editingPostId.value ? '修改失败，请检查删除钥匙是否正确' : '发布失败')
    alert(msg)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  postForm.value = {
    username: '',
    email: '',
    title: '',
    content: '',
    imagePath: '',
    deleteKey: ''
  }
  replyingToPostId.value = null
  editingPostId.value = null
  uploading.value = false
}

const startReply = (postId) => {
  replyingToPostId.value = postId
  editingPostId.value = null
  postForm.value = {
    username: '',
    email: '',
    title: '',
    content: '',
    imagePath: '',
    deleteKey: ''
  }
}

const startEdit = (post) => {
  editingPostId.value = post.postId
  replyingToPostId.value = null
  postForm.value = {
    username: '',
    email: '',
    title: post.title || '',
    content: post.content || '',
    imagePath: post.imagePath || '',
    deleteKey: ''
  }
}

const deletePostHandler = async (postId) => {
  if (canDeleteWithoutKey.value) {
    const confirmed = confirm('是否要删除')
    if (!confirmed) return
    try {
      await deletePost(postId)
      alert('删除成功')
      await loadPosts()
    } catch (err) {
      console.error('删除失败', err)
      const msg = err.response?.data?.message || '删除失败，请稍后再试'
      alert(msg)
    }
    return
  }

  const deleteKey = prompt('请输入该帖子的删除钥匙（发帖时填写的）')
  if (deleteKey === null) return
  try {
    await deletePost(postId, deleteKey)
    alert('删除成功')
    await loadPosts()
  } catch (err) {
    console.error('删除失败', err)
    const msg = err.response?.data?.message || '删除失败，请检查删除钥匙是否正确'
    alert(msg)
  }
}

// ---------- 加载帖子列表（不分页，直接返回数组） ----------
const loadPosts = async () => {
  if (!isLoggedIn.value) {
    posts.value = []
    return
  }
  try {
    const { getPosts } = await import('@/api/post')
    // 假设 getPosts 不再需要分页参数，直接返回所有帖子数组
    const res = await getPosts()
    posts.value = res  // res 是数组
  } catch (err) {
    console.error('加载帖子失败，使用模拟数据', err)
    posts.value = [
      {
        postId: 1,
        title: '【置顶】欢迎来到星尘观测站！',
        username: 'AZHI',
        email: 'azhi@example.com',
        content: '各位神明大人好！这里是站主 AZHI。\n网站刚刚重建完成，还有很多功能在完善中。\n希望大家在这里玩得开心！如果有BUG请留言告诉我哦～ (´∀`)',
        createTime: '2026-04-05 10:00:00',
        imagePath: '',
        replies: [
          {
            postId: 101,
            title: '沙发！',
            username: '路人A',
            email: 'guestA@test.com',
            content: '前排围观！网站风格好复古，喜欢！',
            createTime: '2026-04-05 10:05:00',
            imagePath: ''
          },
          {
            postId: 102,
            title: '',
            username: '小透明',
            email: 'guestB@test.com',
            content: '支持站长！加油更新呀～',
            createTime: '2026-04-05 10:15:00',
            imagePath: ''
          }
        ]
      },
      {
        postId: 2,
        title: '分享一张最近画的摸鱼图',
        username: '画师喵',
        email: 'artist@cat.com',
        content: '最近天气不错，画了一张海边的风景。\n大家觉得怎么样？\n(图片见附件)',
        createTime: '2026-04-06 14:20:00',
        imagePath: 'https://picsum.photos/id/10/300/200',
        replies: [
          {
            postId: 201,
            title: '好好看！',
            username: 'AZHI',
            email: 'azhi@example.com',
            content: '哇！色彩搭配真棒！可以收录进画廊吗？',
            createTime: '2026-04-06 15:00:00',
            imagePath: ''
          }
        ]
      },
      {
        postId: 3,
        title: '关于背景音乐的建议',
        username: '音乐爱好者',
        email: 'music@fan.com',
        content: '站长，现在的背景音乐很好听，但是能不能加一个音量调节按钮呀？\n有时候稍微有点大声了嘿嘿。',
        createTime: '2026-04-07 09:30:00',
        replyCount: 0,
        imagePath: '',
        replies: []
      },
      {
        postId: 4,
        title: '测试一下删除钥匙功能',
        username: '测试员007',
        email: 'test@007.com',
        content: '这是一条测试帖子，用来验证删除钥匙是否有效。\n请勿回复。',
        createTime: '2026-04-08 11:11:11',
        replyCount: 0,
        imagePath: '',
        replies: []
      }
    ]
    posts.value.forEach(post => {
      post.replyCount = post.replies.length
    })
  }
}

// ==================== 音乐模块（完全重写，上下曲稳定） ====================
const musicList = ref([])
const currentMusicIndex = ref(-1)        // 当前播放索引，-1 表示未播放
const audio = ref(null)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const searchKeyword = ref('')

// 过滤后的音乐列表（根据歌曲名或艺术家）
const filteredMusicList = computed(() => {
  if (!searchKeyword.value.trim()) {
    return musicList.value
  }
  const keyword = searchKeyword.value.trim().toLowerCase()
  return musicList.value.filter(music => 
    music.title.toLowerCase().includes(keyword) || 
    music.artist.toLowerCase().includes(keyword)
  )
})

// 释放旧音频资源
const releaseAudio = () => {
  if (audio.value) {
    audio.value.pause()
    audio.value.src = ''
    audio.value.onloadedmetadata = null
    audio.value.ontimeupdate = null
    audio.value.onended = null
    audio.value = null
  }
}

// 核心播放函数：根据索引播放
const playByIndex = (index) => {
  if (!musicList.value.length) return
  if (index < 0) index = 0
  if (index >= musicList.value.length) index = musicList.value.length - 1

  const music = musicList.value[index]
  if (!music) return

  // 如果已经是同一首，则切换播放/暂停
  if (currentMusicIndex.value === index && audio.value) {
    if (isPlaying.value) {
      audio.value.pause()
      isPlaying.value = false
    } else {
      audio.value.play().catch(e => console.warn('播放失败', e))
      isPlaying.value = true
    }
    return
  }

  // 切换新歌曲
  releaseAudio()
  currentMusicIndex.value = index
  audio.value = new Audio(music.filePath)

  audio.value.onloadedmetadata = () => {
    duration.value = audio.value?.duration || 0
  }
  audio.value.ontimeupdate = () => {
    if (audio.value) currentTime.value = audio.value.currentTime
  }
  audio.value.onended = () => {
    isPlaying.value = false
    nextTrack()   // 自动下一首
  }

  audio.value.play()
    .then(() => { isPlaying.value = true })
    .catch(err => {
      console.warn('自动播放被阻止，需要用户交互', err)
      isPlaying.value = false
    })
}

// 供模板调用的播放函数（兼容原有调用方式）
const playMusic = (music) => {
  const idx = musicList.value.findIndex(m => m.musicId === music.musicId)
  if (idx !== -1) playByIndex(idx)
}

const togglePlay = () => {
  if (currentMusicIndex.value === -1 && musicList.value.length) {
    // 没有选中任何歌曲时，默认播放第一首
    playByIndex(0)
  } else if (audio.value) {
    if (isPlaying.value) {
      audio.value.pause()
      isPlaying.value = false
    } else {
      audio.value.play().catch(e => console.warn('播放失败', e))
      isPlaying.value = true
    }
  }
}

const prevTrack = () => {
  if (!musicList.value.length) return
  let newIndex = currentMusicIndex.value - 1
  if (newIndex < 0) newIndex = musicList.value.length - 1
  playByIndex(newIndex)
}

const nextTrack = () => {
  if (!musicList.value.length) return
  let newIndex = currentMusicIndex.value + 1
  if (newIndex >= musicList.value.length) newIndex = 0
  playByIndex(newIndex)
}

const formatTime = (seconds) => {
  if (isNaN(seconds) || seconds === undefined) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 加载音乐列表（已有的 mock 或 API）
const loadMusicList = async () => {
  try {
    const res = await getMusics()
    musicList.value = res
  } catch (err) {
    console.error('加载音乐失败', err)
    musicList.value = [
      { musicId: 1, title: '転がる岩、君に朝が降る', artist: '結束バンド', filePath: '/music/転がる岩、君に朝が降る.mp3', coverPath: '/image/転がる岩、君に朝が降る.jpg' },
      { musicId: 2, title: '稻香', artist: '周杰伦', filePath: '/demo2.mp3', coverPath: '/demo2.jpg' }
    ]
  }
}

// 组件卸载前释放音频
onBeforeUnmount(() => {
  releaseAudio()
})

// ---------- 其他页面函数 ----------
function toggleSidebar() {
  isSidebarOpen.value = !isSidebarOpen.value
}

function closeSidebar() {
  isSidebarOpen.value = false
}

function showPage(pageName) {
  if (pageName === 'admin' && !isAdmin.value) {
    return
  }
  currentPage.value = pageName
  isAuthMenuOpen.value = false
  closeSidebar()
}

function openAuthPage(mode) {
  authMode.value = mode
  showPage('bbs')
}

function toggleAuthMenu() {
  isAuthMenuOpen.value = !isAuthMenuOpen.value
}

async function recordAndGetVisitor() {
  try {
    await recordVisitor()
    const result = await getTotalVisitors()
    visitorCount.value = result.totalVisitors
  } catch (error) {
    console.error('访客统计失败', error)
    visitorCount.value = 168
  }
}

async function loadGallery() {
  try {
    const result = await getImages()
    galleryImages.value = result
  } catch (error) {
    console.error('获取画廊数据失败', error)
  }
}

onMounted(() => {
  resetAdminForms()
  recordAndGetVisitor()
  loadGallery()
  loadPosts()
  loadMusicList()
})
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
        <li><span class="menu-icon">◆</span> <span href="#"><del>游戏角</del></span>&nbsp;制作中</li>
        <li><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('music')">音乐</a></li>
        <li v-if="isAdmin"><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('admin')">管理员</a></li>
        <li><span class="menu-icon">◆</span> <a href="#" @click.prevent="showPage('links')">链接集</a></li>
      </ul>
      <div class="menu-notice">(个人博客同好站)</div>
    </aside>

    <!-- 主内容区域：页面的结构-->
    <main class="main-content">
      <!-- 首页结构 -->
      <div v-if="currentPage === 'home'">
        <div class="header-section">
          <div class="header-top">
            <img src="./assets/images/title_text.png" style="width: 1000px; height: 150px;" alt="标题文本图" class="title-img">
          </div>
          <p class="welcome-msg">★★★ 欢迎光临！本网站是以anime为主的插画交流网站 ★★★</p>
        </div>
        <div class="warning-bar">
            <div class="warning-bar-marquee-inner">
                <span class="warning-bar-text">
                    <span class="star-orange">★</span>&thinsp;<span class="star-green">★</span>&thinsp;<span class="star-purple">★</span>
                    &thinsp;&thinsp;<strong>未经许可禁止转载/复制</strong>&thinsp;&thinsp;
                    <span class="star-purple">★</span>&thinsp;<span class="star-green">★</span>&thinsp;<span class="star-orange">★</span>
                </span>
            </div>
        </div>
        <div class="counter-section">
          <div>
            您是第
              <span class="counter-digital">{{ visitorCount }}
              </span>
            位到访的客人！
          </div>
          <div class="counter-info">
            若踩到整数号，请通过 <a href="#" @click="handleClap">拍手</a>
            或 <a href="#" @click.prevent="showPage('bbs')">BBS留言</a> 告诉我～
          </div>
        </div>
        <div class="center-area">
          <div class="illust-container">
            <img src="./assets/images/main_illust.jpg" alt="本月插画" class="main-illust" width="650" height="488" fetchpriority="high">
            <div class="illust-credit">本月插画 / 头像：站主</div>
          </div>
          <div class="log-box">
            <div class="log-content">
              <ul>
                <li><span class="log">2026-5-30</span> 更新了音乐与画廊样式~</li>
                <li><span class="log">2026-5-29</span> 更新了☽AZHI☾的小屋</li>
                <li><span class="log">2026-4-19</span> 已修复Links☆</li>
                <li><span class="log">2026-4-19</span> 留言板垃圾信息过滤加强！</li>
                <li><span class="log">2026-4-17</span> 更新了个人资料100问100答♪</li>
                <li><span class="log">2026-3-16</span> 更换了网站背景 (>_<)</li>
                <li><span class="log">2026-3-15</span> 已把画作全部备份完成！
                </li>
              </ul>
            </div>
              <div class="clap-section" style="padding-top:15px;">
                <div class="tooltip-container btn-mode">
                  <button class="btn-clap" @click="handleClap">拍手!</button>
                  <div class="clap-info">★什么是拍手？<br>只要按一下按钮，就可以把应援讯息发送给站长!♪</div>
                </div>
                <div class="clap-sub-info">★非常期待大家的应援与留言★</div>
              </div>
          </div>
        </div>
        <div class="notice-box">
          <div class="notice-title">◆ 公告板 ◆</div>
            <div class="notice-content">
              天气渐渐变热了，神明大人们最近还过得好吗？<br>
              （站长因为是重度家里蹲，所以完全不知道外面的气温呢＞＜；）<br>
              近期计划对网站进行改版！<br>
              时间尚未确定，临近时会再来这里通知大家。<br>
              随时欢迎到BBS留言♪
            </div>
        </div>
          <table class="info-table">
            <thead>
              <tr>
                <th>页面</th>
                <th>说明</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><a href="#" @click.prevent="showPage('home')">首页</a></td>
                <td>就是这里～♪</td>
              </tr>
              <tr>
                <td><a href="#" @click.prevent="showPage('profile')">个人资料</a></td>
                <td>看完这里，你就能了解站长的一切！？ 一定要看☆</td>
              </tr>
              <tr>
                <td><a href="#" @click.prevent="showPage('gallery')">画廊</a><span class="new-tag">new</span></td>
                <td>展示着各位神明大人送来的超棒插画！一定要来看看哦</td>
              </tr>
              <tr>
                <td><a href="#" @click.prevent="showPage('bbs')">BBS/留言板</a></td>
                <td>意见、感想请到留言板♪ 在涂鸦留言板也在征集插画中！一直以来谢谢大家＞＜</td>
              </tr>
              <tr>
                <td><a href="#" @click.prevent="showPage('rules')">使用规定</a></td>
                <td>一定要先读哦☆</td>
              </tr>
              <tr>
                <td><span href="#"><del>游戏角</del></span> </td>
                <td>放着站主自己做的游戏，以及觉得很有趣的游戏介绍～♪ &nbsp;正在制作</td>
              </tr>
              <tr>
                <td><a href="#" @click.prevent="showPage('music')">音乐</a><span class="new-tag">new</span></td>
                <td>收录二次元相关曲目，点击后就能直接播放♪</td>
              </tr>
              <tr>
                <td><a href="#" @click.prevent="showPage('links')">链接集</a><span class="new-tag">new</span></td>
                <td>非常欢迎互换链接☆<br><img src="./assets/images/banner.png" style="width: 10%" alt="网站横幅""></td>
              </tr>
            </tbody>
          </table>   
      </div>
      
      <!-- 个人资料页结构 -->
      <div v-if="currentPage === 'profile'" class="profile-container">
        <h1 class="profile-title"><span class="profile-icon-left">◆&nbsp;&nbsp;</span>个人资料<span class="profile-icon-right"></span>&nbsp;&nbsp;◆</h1>
        <p class="subtitle">—— AZHI的100问100答 ——</p>
        <div class="qa-list">
          <p><strong>Q1. 你的名字是？</strong><br>☽AZHI☾！</p>
          <p><strong>Q2. 你的网名是什么？</strong><br>azhi4514～</p>
          <p><strong>Q3. 这是什么类型的网站？</strong><br>插画！还会分享一些自己制作的喜欢的游戏！</p>
          <p><strong>Q4. 是什么促使你创建这个网站？</strong><br>因为想要一个能和神明大人们开心聊天的地方☆ 一个谁都不会感到寂寞的地方！</p>
          <p><strong>Q5. 你运营这个网站多久了？</strong><br>从26年开始！时间不久，我自己有时会把年数写错（笑）。</p>
          <p><strong>Q6. 网站名的由来？</strong><br>二次元聚集地「☽星尘观测站☾」♪ 月亮符号是重点！</p>
          <p><strong>Q7. 你的网站对你身边的人来说是秘密吗？</strong><br>不是的！</p>
          <p><strong>Q8. 你会为达到一定访问量的人做些什么吗？</strong><br>目前还没有想好哦... 也许会做个特别的感谢页面？</p>
          <p><strong>Q9. 你多久查看一次自己的网站？</strong><br>每天都看！每时每刻！</p>
          <p><strong>Q10. 你目前使用的服务器好用吗？</strong><br>倒不是很讨厌它。</p>
          <p><strong>Q11. 你的服务器被封过吗？</strong><br>没有</p>
          <p><strong>Q12. 你是自己敲HTML标签，还是用AI？</strong><br>大部分自己输入的！</p>
          <p><strong>Q13. 你会参考哪些网站？它们叫什么名字？</strong><br>参考了yachiyo！上面有很多有趣的内容呢（笑）</p>
          <p><strong>Q14. 你会担心访问量吗？</strong><br>如果能让大家都看到的话就最好了... 访问量什么的其实无所谓的！</p>
          <p><strong>Q15. 你单日访问量最高是多少？</strong><br>大概只有我自己（？</p>
          <p><strong>Q16. 你认为个人网站无法与企业或商业网站竞争吗？</strong><br>我不认为这是竞争关系。首先，目的不同。</p>
          <p><strong>Q17. 个人网站的优势是什么？</strong><br>可以随心所欲！</p>
          <p><strong>Q18. 个人网站的劣势是什么？</strong><br>开发压力有时候会很大（？</p>
          <p><strong>Q19. 你有没有想过关闭你的网站？如果有，为什么？</strong><br>没有想过！</p>
          <p><strong>Q20. 关于互链？</strong><br>非常欢迎！请通过BBS，web拍手，或邮件联系♪</p>
          <p><strong>Q21. 让你开心的留言？</strong><br>每一条留言都让我开心！ 全部都有看哦☆</p>
          <p><strong>Q22. 你多久更新一次网站？</strong><br>每天！</p>
          <p><strong>Q23. 当你在某个自动链接目录网站上注册你的网站时，你还会选择"个人网站"作为类别吗？</strong><br>有时候会选择类型吧...？</p>
          <p><strong>Q24. 网站的讲究？</strong><br>让来访的人能感到温馨☆ 还有计数器！计数器是必须的！</p>
          <p><strong>Q25. 有计划在谷歌注册吗？</strong><br>如果能注册会很高兴！不过现在这样也挺好。</p>
          <p><strong>Q26. 你的网站是否曾在任何新闻简报、媒体或主流网站上被报道过？</strong><br>没有捏...</p>
          <p><strong>Q27. 运营个人网站时，你会感到孤独吗？</strong><br>偶尔会＞＜</p>
          <p><strong>Q28. 你和朋友的网站有互推链接吗？</strong><br>以前有，但最近朋友们的网站好像正在休息~ 如果有新消息会立刻告诉大家的！</p>
          <p><strong>Q29. 你私下里觉得自己的网站是最好的吗？</strong><br>是秘密！</p>
          <p><strong>Q30. 你希望你的网站比大型企业或商业网站更受欢迎吗？</strong><br>希望有一天能做到吧（？</p>
          <p><strong>Q31. 你觉得你的网站和其他个人网站相比做得好吗？</strong><br>这是追求的目标！</p>
          <p><strong>Q32. 有其他管理员和你一起管理网站吗？</strong><br>虽然听起来很有趣，但还是希望能按照自己的节奏工作呢！</p>
          <p><strong>Q33. 你会在网上写日记吗？</strong><br>会在bbs上发帖子！</p>
          <p><strong>Q34. 你如何应对留言板上的喷子？</strong><br>删除他们。</p>
          <p><strong>Q35. 运营网站最难的是什么？</strong><br>得不到回复。</p>
          <p><strong>Q36. 运营网站最令人高兴的是什么？</strong><br>收到回复。</p>
          <p><strong>Q37. 你会做访问量分析吗？如果会，哪些信息最有用？</strong><br>会。链接来源信息非常有用。</p>
          <p><strong>Q38. 哪个更理想：流量巨大的大型网站，还是只供朋友使用的小网站？</strong><br>假设有很多朋友……朋友的网站更好＞＜</p>
          <p><strong>Q39. 你会收到垃圾邮件吗？自从你开始运营网站以来，垃圾邮件数量增加了吗？</strong><br>是的，增加了。很烦人。</p>
          <p><strong>Q40. 自从运营网站以来，你结交了新朋友吗？</strong><br>认识了很多人！能在网上和很多人聊天真的很幸福☆</p>
          <p><strong>Q41. 在网上和朋友经常做的事？</strong><br>聊天！ 在留言板和聊天室熬夜♪</p>
          <p><strong>Q42. 你做搜索引擎优化吗？</strong><br>不做。太麻烦了~</p>
          <p><strong>Q43. 个人网站和公司或企业网站最大的区别是什么？</strong><br>要保持不让网站关闭！</p>
          <p><strong>Q44. 网站有英文版吗？</strong><br>没有，不过也许之后会做哦！</p>
          <p><strong>Q45. 你使用AI吗？如果是，你用它们做什么？</strong><br>会用哦~ 在计划一个新项目... 不过这是秘密！</p>
          <p><strong>Q46. 网站上有什么访客可以参与的内容吗？</strong><br>BBS留言板！还有web拍手！</p>
          <p><strong>Q47. 你觉得自从开始运营这个网站以来，你的日常生活发生了变化吗？</strong><br>变得不寂寞了！＞＜</p>
          <p><strong>Q48. 你有自己的域名吗？</strong><br>没有</p>
          <p><strong>Q49. 最用心做的页面？</strong><br>首页！ 背景的图案什么的重做了好多次…</p>
          <p><strong>Q50. 对留言板的期望？</strong><br>大家能友好热闹地交流就最好了~！</p>
          <p><strong>Q51. 你有广告收入吗？</strong><br>没有</p>
          <p><strong>Q52. 你觉得自己有运营网站的天赋吗？</strong><br>每天高强度上网可以算嘛（？</p>
          <p><strong>Q53. 哪个更好：流量很大但没有评论的网站，还是流量很小但每天都有稳定评论的网站？</strong><br>流量很小但每天都有稳定评论的网站！前者的话，你最终可能会失去运营网站的动力＞＜</p>
          <p><strong>Q54. 你认为你的网站哪一点绝对优于其他网站？</strong><br>有最可爱的插画。二次元什么的最可爱了不是嘛？＞＜</p>
          <p><strong>Q55. 你觉得你的个性体现在你的网站设计中吗？</strong><br>我觉得在某种程度上是的（笑）</p>
          <p><strong>Q56. 你觉得你的个性体现在你的网站内容中吗？</strong><br>我觉得在某种程度上是的...（？</p>
          <p><strong>Q57. 运营网站是你的爱好，还是有其他目的？</strong><br>爱好！</p>
          <p><strong>Q58. 你是否收到过大型公司或企业网站的互推链接请求？</strong><br>还没有呢＞＜</p>
          <p><strong>Q59. 你的网站注重质量还是数量？</strong><br>质量优先于数量！</p>
          <p><strong>Q60. 如果有人在留言板上批评你或你的网站，你会回复吗？还是会删除？</strong><br>这取决于批评的内容吧...</p>
          <p><strong>Q61. 你的网站主要关注什么？</strong><br>画画！还有聊天和分享！</p>
          <p><strong>Q62. 你是否重新设计过你的网站？如果有，原因是什么？</strong><br>目前还没有哦</p>
          <p><strong>Q63. 你网站的主要搜索关键词是什么？</strong><br>"画廊"...？</p>
          <p><strong>Q64. 你每天花多少时间运营你的网站？</strong><br>会花很——长——很——长——的时间~</p>
          <p><strong>Q65. 你有没有发现有人搜索过你认为与你的网站无关的关键词？</strong><br>不，不，绝对没有！</p>
          <p><strong>Q66. 最后，你庆幸自己运营了这个网站吗？</strong><br>非常庆幸!</p>
          <p><strong>Q67. 它真的只是一个自我满足的网站吗？</strong><br>我也想知道有没有完全不包含这部分内容的个人网站</p>
          <p><strong>Q68. 你有没有梦到过你的网站？如果有，梦到了什么？</strong><br>梦到了一个奇怪的人打开了网站</p>
          <p><strong>Q69. 如果把你的网站比作音乐，你会把它比作哪种音乐？一首歌？</strong><br>钢琴曲！但是柔和而甜美的钢琴曲！</p>
          <p><strong>Q70. 如果把你的网站比作一种植物？</strong><br>草~！</p>
          <p><strong>Q71. 你的网站有配色方案吗？如果有，是什么颜色？为什么？</strong><br>像海一样的蓝色！原因是秘密哦~</p>
          <p><strong>Q72. 你最长多久没更新网站？</strong><br>每天都有在更新！</p>
          <p><strong>Q73. 你收到的访客反馈或评论中最有帮助的是什么？</strong><br>关于失效链接的提示。之前完全没注意到呢~</p>
          <p><strong>Q74. 你对网站运营有什么担忧吗？如果有，是什么？</strong><br>很多人使用我不太理解的搜索词访问我的网站。</p>
          <p><strong>Q75. 如果你的网站访问量一直很稳定，但突然骤降，你会怎么做？</strong><br>即便如此，还是会继续每天更新！</p>
          <p><strong>Q76. 在运营网站的过程中，你是否曾经做出过任何妥协？</strong><br>还没有发生什么哦</p>
          <p><strong>Q77. 在运营网站的过程中，你是否丢失过任何东西？</strong><br>没有</p>
          <p><strong>Q78. 你有没有因为拥有个人网站而被嘲笑过？如果有，你是怎么做的？</strong><br>没有！网站是最快乐的地方了！</p>
          <p><strong>Q79. 如果一切都回到你网站上线之前的状态，你会怎么做？</strong><br>当然会再做一次！</p>
          <p><strong>Q80. 你有没有经常访问的常用网站？</strong><br>太多了，我列不完！</p>
          <p><strong>Q81. 你未来想为公司或企业运营网站吗？</strong><br>不知道哦</p>
          <p><strong>Q82. 你觉得你的网站有什么可以改进的地方吗？</strong><br>正在努力补全游戏页！</p>
          <p><strong>Q83. 如果你的网站成为一个超级热门网站，你会怎么做？</strong><br>会很高兴！＞＜</p>
          <p><strong>Q84. 到目前为止，你在运营网站过程中犯过的最大错误是什么？</strong><br>使用错了项目结构……！</p>
          <p><strong>Q85. 运营网站有什么值得炫耀的吗？</strong><br>能够让神明大人们都感到幸福快乐！</p>
          <p><strong>Q86. 你的网站有手机版吗？</strong><br>很久以前就做好了，但是一直没有测试（笑）</p>
          <p><strong>Q87. 如果你出生在没有互联网的时代，你觉得你会做什么来代替运营网站？</strong><br>会一直等到互联网到来的那一天！</p>
          <p><strong>Q88. 运营网站在学校或工作中对你有帮助吗？</strong><br>是的！</p>
          <p><strong>Q89. 运营网站的经验？</strong><br>还只是个新手呢♪ 以后会更加努力的☆</p>
          <p><strong>Q90. 如果你将来要停止运营网站，你会给出什么理由？</strong><br>不会停止的！</p>
          <p><strong>Q91. 如果你有一天发现你网站上的一些内容未经许可被转载到其他网站，你会怎么做？</strong><br>去那个网站的bbs上留言...（？</p>
          <p><strong>Q92. 如果你的另一半问你："你会选择我还是运营这个网站？"，你会怎么回答？</strong><br>啊！有趣的问题...</p>
          <p><strong>Q93. 如果我现在把你的网站拿走，会发生什么？</strong><br>大概会感到寂寞吧...＞＜</p>
          <p><strong>Q94. 你运营这个网站的未来目标是什么？</strong><br>继续更新！让更多的神明大人幸福快乐地生活！</p>
          <p><strong>Q95. 如果用一个词来形容你的网站，你会用哪个？</strong><br>海边！</p>
          <p><strong>Q96. 如果运营网站必须花钱，你每月愿意支付多少钱？</strong><br>0.168元左右...？</p>
          <p><strong>Q97. 你认为有哪些网站是竞争对手？</strong><br>目前还没有呢~</p>
          <p><strong>Q98. 除了运营网站，你还想做什么？</strong><br>在实现一个很疯狂的计划呢！是秘密~</p>
          <p><strong>Q99. 你会一直运营这个网站吗？</strong><br>直到那一天来临！</p>
          <p><strong>Q100. 对读到这里的人说一句话！</strong><br>谢谢你读到最后～＞＜☆ 如果能让你多了解AZHI一点点的话就太开心了♪ 以后也请多多关照！</p>
        </div>
      </div>

      <!-- 画廊页结构 -->
      <div v-if="currentPage === 'gallery'">
        <h1 class="gallery-title"><span class="gallery-icon">◆&nbsp;&nbsp;</span>画廊<span class="gallery-icon"></span>&nbsp;&nbsp;◆</h1>
        <p>感谢各位神明大人送来的超棒插画！今后也请多多关照～♪</p>
        <div v-for="(image, index) in galleryImages" :key="index" class="artwork-card">
          <img :src="image.path" :alt="image.title" class="artwork-img" />
          <div class="artwork-info">
            <p class="artwork-author">作者：{{ image.author }}</p>
          </div>
        </div>
      </div>

      <div v-if="currentPage === 'admin' && isAdmin" class="bbs-container">
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

      <!-- BBS/留言板结构 -->
      <div v-if="currentPage === 'bbs'" class="bbs-container">
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

        <div v-if="!isLoggedIn" class="login-required">登陆后查看帖子~~~</div>

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

      <!-- 使用规定 -->
      <div v-if="currentPage === 'rules'" class="rules-container">
        <h1 class="rules-title"><span class="rules-icon">☆&nbsp;&nbsp;</span>星尘观测站&nbsp;使用规约<span class="rules-icon"></span>&nbsp;&nbsp;☆</h1>
        <p>最终更新 / 最后更新：2026.03.16</p>
        <p>神明大人们好~！这里是☽AZHI☾~<br>
        欢迎来到聊天室！<br>
        为了让神明大人们都能愉快地画画，请遵守以下规则。<br>
        进入房间即视为同意本规约。</p>
        <p>※请务必读到最后哦！※</p>

        <p>■ 1. 禁止事项</p>
        <p>　以下行为是被禁止的，发现后将立即处理。</p>
        <p>　◆ 禁止投稿R18、色情、血腥暴力等内容<br>
        　　→ 本站面向全年龄用户。擦边内容也不行哦。</p>
        <p>　◆ 禁止刷屏、发垃圾信息、恶意捣乱<br>
        　　→ 无论是聊天还是画布上的捣乱行为都会被立即处理。</p>
        <p>　◆ 禁止人身攻击、骚扰、恶意中伤他人<br>
        　　→ 对画作提意见可以，但请不要进行人格侮辱。<br>
        　　　让这里成为一个愉快的地方吧。</p>
        <p>　◆ 禁止发布个人信息<br>
        　　→ 禁止公开自己或他人的真实姓名、地址、联系方式等。</p>
        <p>　◆ 禁止用于广告宣传、拉人等目的<br>
        　　→ 这里是画画的地方，不是打广告的地方。</p>

        <p>■ 2. 关于作品版权</p>
        <p>　◆ 投稿作品的版权归画师本人所有。<br>
        　　→ 管理员不会主张任何版权。</p>
        <p>　◆ 禁止将投稿作品用于AI训练或商业用途。<br>
        　　→ 请不要把这里投稿的作品用作AI的训练数据，<br>
        　　　也不要用于任何商业目的。</p>

        <p>■ 3. 关于管理员权限</p>
        <p>　◆ 管理员有权在不事先通知的情况下<br>
        　　→ 删除违反规约的作品和发言。</p>
        <p>　◆ 情节严重的情况下，可能会进行封禁（BAN）处理。</p>
        <p>　◆ 因网站运营需要，规约内容可能会进行修改。<br>
        　　→ 修改时会在网站上进行公告。</p>

        <p>■ 4. 免责声明</p>
        <p>　◆ 本站为个人运营的同好网站。<br>
        　　→ 因服务器故障等原因导致的数据丢失，<br>
        　　　管理员无法承担责任。重要的作品请自行保存备份。</p>
        <p>　◆ 对于用户之间的纠纷，管理员没有调解义务。<br>
        　　→ 但如果存在明显的违规行为，将会进行处理。</p>

        <p>■ 5. 拜托大家</p>
        <p>　◆ 让我们一起维护这个大家都能舒适使用的地方吧！<br>
        　◆ 欢迎新手！画得好不好完全不重要～<br>
        　◆ 如果遇到任何问题，请联系管理员。<br>
        　◆ 开开心心画画吧！ (ﾉ´∀`)ﾉ</p>

        <p>感谢神明大人读完以上规约，祝神明大人们聊天愉快！</p>
      </div>

      <!-- 音乐页结构 -->
      <div v-if="currentPage === 'music'">
        <h1 class="music-title"><span class="music-icon">◆&nbsp;&nbsp;</span>音乐放置处<span class="music-icon"></span>&nbsp;&nbsp;◆</h1>
        <p>收录站长喜欢的曲目，点击列表即可播放。</p>

        <!-- 搜索框 -->
        <div class="music-search">
          <input 
            type="text" 
            v-model="searchKeyword" 
            placeholder="请输入歌曲名或艺术家" 
            class="search-input"
          >
        </div>

        <!-- 播放器控制栏 - 仅当有歌曲被选中时显示 -->
        <div class="music-player" v-if="currentMusicIndex !== -1">
          <div class="player-controls">
            <button @click="prevTrack" class="ctrl-btn">⏮ 上一首</button>
            <button @click="togglePlay" class="ctrl-btn" :class="{ 'playing': isPlaying }">{{ isPlaying ? '⏸ 暂停' : '▶ 播放' }}</button>
            <button @click="nextTrack" class="ctrl-btn">下一首 ⏭</button>
          </div>
          <div class="player-info">
            <!-- 注意：当前歌曲信息从 musicList 中根据索引获取 -->
            <img :src="musicList[currentMusicIndex]?.coverPath" class="player-cover" alt="封面">
            <div class="player-details">
              <div class="player-title">{{ musicList[currentMusicIndex]?.title }} - {{ musicList[currentMusicIndex]?.artist }}</div>
              <div class="player-progress">
                <span>{{ formatTime(currentTime) }}</span>
                <progress :value="currentTime" :max="duration" class="progress-bar"></progress>
                <span>{{ formatTime(duration) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="no-music">
          <p>暂未选择音乐，请从下方列表点击播放～</p>
        </div>

        <!-- 曲目列表（不变） -->
        <div class="music-list">
          <h3>曲目列表</h3>
          <table class="music-table">
            <thead>
              <tr><th>序号</th><th>封面</th><th>曲名</th><th>艺术家</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="(music, idx) in filteredMusicList" :key="music.musicId" @click="playMusic(music)" class="music-item">
                <td>{{ idx + 1 }}</td>
                <td><img :src="music.coverPath" class="list-cover" alt="封面"></td>
                <td>{{ music.title }}</td>
                <td>{{ music.artist }}</td>
                <td><button class="play-btn">▶ 播放</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <p>点击曲目即可开始播放♪</p>
      </div>

      <!-- 链接集结构 -->
      <div v-if="currentPage === 'links'" class="links-container">
        <h1 class="links-title"><span class="links-icon">☆&nbsp;&nbsp;</span>Links<span class="links-icon"></span>&nbsp;&nbsp;☆</h1>
        <p class="links-intro">这些都是站长非常珍惜的宝藏链接。<br>要不要一起出发，去探索充满回忆与灵感的地方呢？☆</p>

        <!-- 好友网站 -->
        <div class="links-category">
          <h2>☆ 好友网站 ☆</h2>
          <p class="maintaining">◆ 维护中 ◆<br>朋友们的网站好像正在休息中呢…… (´;ω;｀)<br>站长偶尔也会去串门，一直期待着它们回归的那天。<br>如果有新消息会立刻告诉大家的！</p>
        </div>

        <!-- 资料室 / 实用工具 -->
        <div class="links-category">
          <h2>☆ 资料室 / 实用工具 ☆</h2>
          <ul class="links-list">
            <li><a href="https://gifcities.org/" target="_blank" rel="noopener noreferrer">素材天堂 (GIFCities)</a> – 闪闪发光的图标、可爱的背景、动感十足的 GIF 应有尽有！是装扮主页的宝库哦~</li>
            <li><a href="https://www.w3schools.com/" target="_blank" rel="noopener noreferrer">HTML/CSS 基础讲座 (W3Schools)</a> – 想要亲手制作网页的话就从这里开始吧！站长也在这里学到了很多知识呢。</li>
            <li><a href="https://www.dafont.com/" target="_blank" rel="noopener noreferrer">精美字体馆 (DaFont)</a> – 能瞬间改变网站氛围的个性字体！记得查看使用说明，给主页换个漂亮新装吧！</li>
            <li><a href="https://www.cursors-4u.com/" target="_blank" rel="noopener noreferrer">鼠标指针资料站 (Cursors-4u.com)</a> – 让鼠标也变得闪闪发光或可爱跳动！超多动画指针等你来领走。</li>
          </ul>
        </div>

        <!-- 插画同好 & 素材参考 -->
        <div class="links-category">
          <h2>☆ 插画同好 & 素材参考 ☆</h2>
          <ul class="links-list">
            <li><a href="https://www.tinami.com/" target="_blank" rel="noopener noreferrer">★ TINAMI</a> – 知名的动画/漫画插画投稿站！超多精美画作，非常有参考价值。</li>
            <li><a href="https://www.deviantart.com/" target="_blank" rel="noopener noreferrer">DeviantArt</a> – 可以看到全世界画师的各种创作！在这里还能和国外的画友交流呢。</li>
            <li><a href="https://www.bilibili.com/" target="_blank" rel="noopener noreferrer">Bilibili</a> – 搜索新事物、寻找画友主页，是站长互联网生活的中心站！</li>
          </ul>
        </div>
      </div>

    </main>
  </div>
</template>

<style>
/* ===== 1. 全局基础样式 ===== */
body {
  margin: 0;
  padding: 0;
  background-color: #87D4DA;
  font-family: "SimSun", "宋体", "PMingLiU", sans-serif;
  -webkit-font-smoothing: none;
  font-size: 15.5px;
  color: #444;
  line-height: 1.5;
  overflow-x: auto;
}

a {
  text-decoration: none;
}

/* ===== 2. 主布局结构 ===== */
#app {
  width: 100%;
  min-width: 1350px;
  display: flex;
  min-height: 100vh;
  border-left: 1px solid #fff;
  border-right: 1px solid #fff;
  box-sizing: border-box;
}

/* ===== 3. 侧边栏样式 ===== */
.mobile-top-bar {
  display: flex;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 40px;
  z-index: 9999;
  background-color: #0f5e6d;
  background-image:
    linear-gradient(rgba(255,255,255,0.12) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.12) 1px, transparent 1px);
  background-size: 4px 4px;
  border-bottom: 2px solid #0a4550;
  align-items: center;
  padding: 0 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.25);
  box-sizing: border-box;
}

.mobile-menu-btn {
  background: none;
  border: 1px solid rgba(255,255,255,0.35);
  color: #fff;
  font-size: 20px;
  padding: 1px 7px 3px;
  font-weight: bold;
  line-height: 1;
  flex-shrink: 0;
  font-family: sans-serif;
  cursor: pointer;
}

.mobile-menu-btn:active {
  background: rgba(255,255,255,0.15);
}

.mobile-top-title {
  color: #fff;
  font-weight: bold;
  font-size: 14px;
  text-shadow: 1px 1px 3px rgba(0,0,0,0.5);
  flex: 1;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 4px;
  letter-spacing: 0.5px;
}

.mobile-top-links {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  color: #fff;
  font-size: 12px;
}

.mobile-top-links a,
.mobile-user-link {
  color: #fff;
  text-decoration: underline;
  background: none;
  border: 0;
  padding: 0;
  font: inherit;
  cursor: pointer;
  text-decoration: none;
}

.mobile-top-links a:hover,
.mobile-user-link:hover {
  color: #fff6b3;
}

.mobile-auth-popover {
  position: absolute;
  top: calc(100% + 4px);
  right: 8px;
  z-index: 10000;
  width: min(320px, calc(100vw - 16px));
}

.mobile-auth-status {
  margin-top: 0;
  text-align: left;
  box-shadow: 0 2px 6px rgba(0,0,0,0.18);
}

.mobile-overlay {
  display: none;
  position: fixed;
  top: 40px;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.35);
  z-index: 9997;
}

.mobile-overlay.active {
  display: block;
}

.sidebar {
  position: fixed;
  top: 40px;
  left: 0;
  width: 260px;
  height: calc(100% - 40px);
  z-index: 9998;
  transform: translateX(-100%);
  transition: transform 0.2s;
  overflow-y: auto;
  background-color: #FDFAC8;
  background-image: url('@/assets/images/bg_yellow.jpg');
  background-repeat: repeat;
  padding: 20px;
  border-right: 2px solid #808080;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.sidebar.sidebar-open {
  transform: translateX(0);
}

.menu-header {
  font-weight: bold;
  font-size: 20px;
  color: #333;
  margin-bottom: 3px;
  text-align: left;
}

.menu-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.menu-list li {
  margin-bottom: 22px;
}

.menu-list a {
  color: #0000CD;
  font-weight: bold;
  font-size: 16px;
}
.menu-list a:hover {
  color: #ff6600;
}

.menu-icon {
  color: #D26432;
  font-size: 18px;
  vertical-align: middle;
  margin-right: 3px;
}

.new-tag {
  color: red;
  font-size: 12px;
  font-weight: bold;
  vertical-align: top;
  animation: blink 1s step-end infinite;
}

.menu-notice {
  font-size: 10px;
  color: #666;
  margin-top: 6px;
  text-align: center;
  animation: fanNoticeSlide 10s ease-in-out infinite alternate;
}

@keyframes fanNoticeSlide {
  0% { transform: translateX(-6px); }
  50% { transform: translateX(6px); }
  100% { transform: translateX(-6px); }
}

@keyframes blink {
  50% { opacity: 0; }
}

/* ===== 4. 主内容区通用 ===== */
.main-content {
  flex: 1;
  padding: 70px 40px 30px;
  position: relative;
  background-color: #D0F0F5;
  box-shadow: inset 2px 0 0 #808080;
  overflow: hidden;
}
/* 主内容背景装饰 */
.main-content::before {
  content: "";
  position: absolute;
  left: -10px;
  right: 0;
  top: -306px;
  height: calc(100% + 800px);
  background-color: #D0F0F5;
  background-image: url('@/assets/images/bg_blue.png');
  background-repeat: repeat;
  background-attachment: fixed;
  z-index: 0;
  pointer-events: none;
}
.main-content > * {
  position: relative;
  z-index: 1;
}

/* ===== 5. 首页模块 ===== */
.header-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 2px;
  text-align: center;
}
.header-top {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  margin-bottom: 28px;
}
.title-img {
  width: 505px;
  height: auto;
}
.welcome-msg {
  font-weight: bold;
  margin: 0 0 10px 0;
  color: #333;
  font-size: 16px;
  text-shadow: 1px 1px 0 #fff;
}
.warning-bar {
    margin-left: -40px; margin-right: -40px; width: calc(100% + 80px); box-sizing: border-box;
    background-color: #0f5e6d;
    /* 背景由两个线性渐变叠加而成：第一个绘制水平网格线，第二个绘制垂直网格线 */
    background-image:
        /* 水平线渐变：从上到下，在1px处绘制半透明白线，其余透明 */
        linear-gradient(rgba(255,255,255,0.15) 1px, transparent 1px),
        /* 垂直线渐变：从左到右（90deg），在1px处绘制半透明白线，其余透明 */
        linear-gradient(90deg, rgba(255,255,255,0.15) 1px, transparent 1px);
    /* 设置每个渐变背景单元的尺寸为4px×4px，形成重复的网格 */
    background-size: 4px 4px; color: #fff; font-weight: bold; font-size: 16px;
    padding: 1px 40px; text-align: center; border-top: 1px solid #0a4550;
    border-bottom: 1px solid #0a4550; text-shadow: 1px 1px 3px rgba(0,0,0,0.5); letter-spacing: 1px;
    overflow: hidden;
    white-space: nowrap;
    position: relative;
}
.warning-bar-marquee-inner {
  display: inline-block;
  white-space: nowrap;
  animation: scrollLeftToRight 10s linear infinite;
  padding-left: 0;
}
@keyframes scrollLeftToRight {
  0% {
    transform: translateX(-200%);
  }
  100% {
    transform: translateX(200%);
  }
}
.warning-bar-text { 
  display: inline-block;
  margin: 0; padding: 0;
}
.star-orange { 
  color: #ffad33;
  text-shadow: 1px 1px 0 #333;
}
.star-green  { 
  color: #00cc66;
  text-shadow: 1px 1px 0 #333;
}
.star-purple { 
  color: #aa44dd;
  text-shadow: 1px 1px 0 #333;
}
.counter-section {
  text-align: center;
  margin: 20px 0 10px;
  font-weight: bold;
  font-size: 18px;
}
.counter-digital {
  font-family: 'DSEG7-Classic', monospace;
  font-size: 28px;
  letter-spacing: 2px;
  background: #0a0a1a;
  color: #32ff32;
  padding: 3px 6px;
  border-radius: 4px;
  text-shadow: 0 0 4px #00ff00;
  display: inline-block;
  margin: 0 4px;
}
.counter-info {
  font-size: 14px;
  margin-top: 0px;
  font-weight: normal;
}
.center-area {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  gap: 5px;
}
.illust-container {
  flex: 0 0 650px;
  text-align: center;
}
.main-illust {
  width: 35%;
  height: 100%;
  box-shadow: 4px 4px 10px rgba(0,0,0,0.1);
}
.illust-credit {
  font-size: 13px;
  color: #555;
  text-align: center;
  margin-top: 2px;
}
.log-box {
  margin: 20px 0;
}
.log-content {
  background-color: #f7f7f7;
  border: 6px solid #ccc;
  padding: 10px 15px;
  height: 160px;
  overflow-y: auto;
}
.log-content ul {
  padding-left: 10px;
  margin: 0;
  list-style-position: inside;
}
.log-content li {
  white-space: nowrap;
  margin-bottom: 5px;
}
.log {
  color: #333;
  font-family: monospace;
  margin-right: 15px;
}
.clap-section {
  text-align: center;
  margin-top: 15px;
}
.btn-clap {
  background: #fff;
  color: #FF9966;
  padding: 4px 12px;
  border: 2px solid #FF9966;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.2s;
}
.btn-clap:hover + .clap-info {
  background: #FFF5EE;
  display: block;
}
.clap-info {
  width: 100px;
  background: #FFF5EE;
  border: 2px solid #FF9966;
  display: none;
  position: absolute;
  left: 80%;
  font-size: 12px;
  color: #555;
  margin-top: 5px;
}
.clap-sub-info {
  font-size: 12px;
  color: #444;
  margin-top: 5px;
}
.notice-box {
  text-align: center;
  margin: 20px 0;
}
.notice-title {
  font-size: 24px;
  font-weight: bold;
  color: black;
}
.notice-content {
  font-size: 14px;
  margin-top: 5px;
  color: black;
}
.info-table {
  width: 60%;
  margin: 20px auto;
  border-collapse: separate;
  border-spacing: 2px;
  border: 2px solid #487A8A;
  font-size: 13px;
}
.info-table th,
.info-table td {
  border: 1px solid #487A8A;
  padding: 4px 6px;
  color: #000;
  font-weight: bold;
}
.info-table a {
  color: #0000CD;
  text-decoration: underline;
}
.info-table a:hover {
  color: #ff6600;
}

/* ===== 6. 个人资料页 ===== */
.profile-container {
  max-width: 960px; margin: 0 auto;
  background: rgba(255,255,255,0.88);
  border: 2px solid #79ACC5;
  padding: 20px 26px 24px;
  font-size: 13px; color: #333;
  box-shadow: 2px 2px 6px rgba(0,0,0,0.12);
}
.profile-title {
  text-align: center;
}
.subtitle {
  text-align: center;
  font-size: 14px;
  color: #444;
  margin-bottom: 20px;
}
.qa-list p {
  margin: 12px 0;
  line-height: 1.5;
  border-left: 3px solid #7fdbff;
  padding-left: 15px;
}
.qa-list strong {
  color: #0f5e6d;
}

/* ===== 7. 画廊页 ===== */
.gallery-title {
  text-align: center;
  margin: 0 0 16px;
  padding: 5px 10px;
  background: #e3f4f7;
  border: 1px solid #79ACC5;
  color: #800000;
  font-size: 24px;
  letter-spacing: 1px;
  text-shadow: 1px 1px 0 #fff;
}
.artwork-card {
  display: inline-block;
  width: 210px;
  margin: 10px;
  padding: 8px;
  background: #fffff3;
  border: 1px solid #b9a982;
  box-shadow: inset 0 0 0 1px #fff, 2px 2px 0 rgba(0,0,0,0.12);
  text-align: center;
  vertical-align: top;
}
.artwork-img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  display: block;
  border: 1px solid #777;
  background: #fff;
}
.artwork-info {
  margin-top: 8px;
  padding: 6px 8px 8px;
  background: #fffef9;
  border: 1px dotted #b9a982;
}
.artwork-author {
  font-weight: bold;
  font-size: 13px;
  color: #0f5e6d;
  margin: 0;
  line-height: 1.5;
}

/* ===== 8. BBS/留言板页 ===== */
.bbs-container {
  max-width: 980px;
  margin: 0 auto;
  padding: 18px 22px 28px;
  background-color: #fff8df;
  background-image:
    linear-gradient(rgba(121,172,197,0.16) 1px, transparent 1px),
    linear-gradient(90deg, rgba(121,172,197,0.16) 1px, transparent 1px);
  background-size: 16px 16px;
  border: 3px double #79ACC5;
  box-shadow: 4px 4px 0 rgba(72,122,138,0.22);
  color: #333;
}

.bbs-title {
  text-align: center;
  margin: 0 0 16px;
  padding: 5px 10px;
  background: #e3f4f7;
  border: 1px solid #79ACC5;
  color: #800000;
  font-size: 24px;
  letter-spacing: 1px;
  text-shadow: 1px 1px 0 #fff;
}

.bbs-container hr {
  border: none;
  border-top: 1px dashed #79ACC5;
  margin: 18px 0;
}

.admin-intro {
  max-width: 720px;
  margin: 0 auto 14px;
  padding: 8px 10px;
  background: #fffef7;
  border: 1px dotted #b9a982;
  color: #6b4f1d;
  line-height: 1.6;
}

.admin-music-list,
.admin-gallery-list {
  max-width: 900px;
  margin: 0 auto;
}

.admin-gallery-list h3 {
  font-size: 18px;
  font-weight: bold;
  color: #0f5e6d;
  margin: 0 0 10px;
  padding: 4px 8px;
  background: #e3f4f7;
  border: 1px solid #79ACC5;
}

.admin-actions-cell {
  white-space: nowrap;
}

.admin-delete-btn {
  margin-left: 8px;
}

.post-form {
  max-width: 720px;
  margin: 0 auto 10px;
  padding: 10px 12px 8px;
  background: #fffff3;
  border: 1px solid #b9a982;
  box-shadow: inset 0 0 0 1px #fff, 2px 2px 0 rgba(0,0,0,0.12);
  box-sizing: border-box;
}

.post-form-title {
  margin: -10px -12px 8px;
  padding: 4px 10px;
  background: #79ACC5;
  border-bottom: 1px solid #487A8A;
  color: #fff;
  font-weight: bold;
  font-size: 14px;
  text-shadow: 1px 1px 0 #487A8A;
}

.form-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 1px;
  background: #b9a982;
  font-size: 13px;
}

.form-label {
  width: 86px;
  background-color: #e3f4f7;
  color: #0f5e6d;
  font-weight: bold;
  font-size: 13px;
  padding: 4px 8px;
  text-align: right;
  vertical-align: middle;
  white-space: nowrap;
}

.form-label-comment {
  vertical-align: top;
  padding-top: 7px;
}

.form-table td {
  padding: 4px 6px;
  background: #fffef7;
}

.form-input,
.form-textarea,
.form-input-small {
  border: 1px solid #8aa9b3;
  background: #fff;
  color: #333;
  padding: 3px 5px;
  font-size: 13px;
  font-family: "MS PGothic", "SimSun", "宋体", monospace;
  box-sizing: border-box;
  box-shadow: inset 1px 1px 2px rgba(0,0,0,0.15);
}

.form-input {
  width: 320px;
}

.form-input-title {
  width: 260px;
}

.form-textarea {
  width: 100%;
  min-height: 110px;
  resize: vertical;
}

.form-input-small {
  width: 120px;
}

.form-file {
  font-size: 12px;
  font-family: inherit;
}

.form-hint {
  font-size: 12px;
  color: #666;
  margin-left: 6px;
}

.form-notes {
  font-size: 12px;
  color: #800000;
  margin: 6px 0 0;
  padding-left: 18px;
  line-height: 1.5;
}

.form-notes li {
  margin-bottom: 1px;
}

.auth-radio {
  margin-right: 18px;
  color: #0f5e6d;
  font-weight: bold;
}

.auth-status,
.login-required {
  margin-top: 10px;
  padding: 8px 10px;
  background: #fffef7;
  border: 1px dotted #b9a982;
  color: #800000;
  font-weight: bold;
  text-align: center;
}

.auth-status button {
  margin-left: 10px;
}

.login-required {
  max-width: 720px;
  margin: 12px auto 0;
  font-size: 16px;
}

.form-title-cell {
  position: relative;
  padding-right: 120px !important;
}

.form-submit,
.form-cancel-btn,
.bbs-container button {
  background: #eee;
  border: 2px outset #fff;
  padding: 2px 10px;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
  font-family: "MS PGothic", "SimSun", "宋体", sans-serif;
  color: #333;
}

.form-submit:hover,
.form-cancel-btn:hover,
.bbs-container button:hover {
  background: #fff6cc;
  color: #800000;
}

.form-submit:active,
.form-cancel-btn:active,
.bbs-container button:active {
  border-style: inset;
}

.form-submit-top {
  position: absolute;
  top: 4px;
  right: 6px;
  line-height: 1.2;
}

.form-submit:disabled,
.bbs-container button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.form-cancel-cell {
  padding: 6px !important;
}

.delete-btn {
  color: #800000 !important;
}

.post-list {
  max-width: 900px;
  margin: 0 auto;
  padding: 0;
  font-family: "MS PGothic", "SimSun", "宋体", sans-serif;
}

.post-list h3 {
  font-size: 18px;
  font-weight: bold;
  color: #0f5e6d;
  margin: 0 0 10px;
  padding: 4px 8px;
  background: #e3f4f7;
  border: 1px solid #79ACC5;
}

.post-item {
  background: #fffff3;
  border: 1px solid #b9a982;
  border-left: 6px solid #79ACC5;
  padding: 10px 12px;
  margin-bottom: 12px;
  box-shadow: 2px 2px 0 rgba(0,0,0,0.1);
  clear: both;
}

.post-item:last-child {
  margin-bottom: 0;
}

.post-header {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 6px 10px;
  align-items: baseline;
  margin: -10px -12px 10px;
  padding: 5px 8px;
  background: #f2ead2;
  border-bottom: 1px dotted #b9a982;
}

.post-title {
  font-size: 16px;
  font-weight: bold;
  color: #800000;
}

.post-author,
.post-email {
  font-size: 12px;
  color: #555;
  font-family: "Courier New", monospace;
  white-space: nowrap;
}

.post-content {
  font-size: 14px;
  line-height: 1.65;
  color: #222;
  margin: 8px 0;
  padding: 8px 10px;
  background: #fffef9;
  border: 1px solid #eadfbd;
  white-space: pre-wrap;
}

.post-image {
  margin: 10px 0;
  text-align: left;
  padding: 5px;
  background: #fff;
  border: 1px dotted #b9a982;
  display: inline-block;
}

.post-image img {
  max-width: 100%;
  height: auto;
  display: block;
  border: 1px solid #777;
}

.post-meta {
  font-size: 11px;
  color: #666;
  margin: 8px 0;
  padding: 3px 6px;
  background: #f7f1dc;
  border-top: 1px dotted #d0c49f;
  border-bottom: 1px dotted #d0c49f;
  font-family: "Courier New", monospace;
}

.replies {
  margin-top: 12px;
  padding: 8px 0 0 14px;
  border-top: 1px dashed #79ACC5;
}

.reply-item {
  background: #f7fcff;
  border: 1px solid #b8dce8;
  border-left: 5px solid #b8dce8;
  padding: 8px 10px;
  margin: 8px 0 0;
}

.reply-item .post-header {
  margin: -8px -10px 8px;
  background: #e9f6fa;
}

.reply-item .post-title {
  font-size: 14px;
}

.reply-item .post-content {
  font-size: 13px;
  margin: 6px 0;
  background: #fff;
}

.reply-item .post-meta {
  font-size: 10px;
  margin: 6px 0;
}

/* ===== 9. 使用规定页 ===== */
.rules-container {
  max-width: 960px; margin: 0 auto;
  background: rgba(255,255,255,0.88);
  border: 2px solid #79ACC5;
  padding: 20px 26px 24px;
  font-size: 13px; color: #333;
  box-shadow: 2px 2px 6px rgba(0,0,0,0.12);
}
.rules-title {
  text-align: center;
}
.rules-page p {
  margin: 8px 0;
  line-height: 1.5;
}
.rules-page p:first-of-type {
  margin-top: 0;
}

/* ===== 10. 音乐页 ===== */
/* 音乐搜索框样式 */
.music-search {
  margin: 18px auto 12px;
  text-align: center;
  max-width: 900px;
  padding: 10px 12px;
  background: #fffff3;
  border: 1px solid #b9a982;
  box-shadow: inset 0 0 0 1px #fff, 2px 2px 0 rgba(0,0,0,0.12);
}
.search-input {
  width: 320px;
  padding: 5px 8px;
  font-size: 13px;
  border: 1px solid #8aa9b3;
  background: #fff;
  color: #333;
  font-family: "MS PGothic", "SimSun", "宋体", monospace;
  outline: none;
  box-shadow: inset 1px 1px 2px rgba(0,0,0,0.15);
}
.search-input:focus {
  border-color: #79ACC5;
  box-shadow: inset 1px 1px 2px rgba(0,0,0,0.15), 0 0 0 1px #d8eef5;
}
.music-title {
  text-align: center;
  margin: 0 0 16px;
  padding: 5px 10px;
  background: #e3f4f7;
  border: 1px solid #79ACC5;
  color: #800000;
  font-size: 24px;
  letter-spacing: 1px;
  text-shadow: 1px 1px 0 #fff;
}
.music-player {
  max-width: 900px;
  margin: 18px auto;
  padding: 16px 18px;
  background: #fff8df;
  background-image:
    linear-gradient(rgba(121,172,197,0.14) 1px, transparent 1px),
    linear-gradient(90deg, rgba(121,172,197,0.14) 1px, transparent 1px);
  background-size: 16px 16px;
  border: 3px double #79ACC5;
  box-shadow: 4px 4px 0 rgba(72,122,138,0.22);
}
.player-controls {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 14px;
}
.ctrl-btn {
  background: #eee;
  border: 2px outset #fff;
  padding: 4px 14px;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
  color: #333;
  font-family: "MS PGothic", "SimSun", "宋体", sans-serif;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
  outline: none;
}

.ctrl-btn:hover {
  background-color: #fff6cc !important;
  border-color: #fff !important;
  color: #800000 !important;
  transform: translateY(-1px);
}

.ctrl-btn:active {
  border-style: inset;
  transform: translateY(0);
  filter: none;
}

.ctrl-btn:not(.playing) {
  background-color: #eee;
  border-color: #fff;
  color: #444;
}
.player-info {
  display: flex;
  gap: 20px;
  align-items: center;
  background: #fffef7;
  padding: 10px 12px;
  border: 1px solid #b9a982;
  box-shadow: inset 0 0 0 1px #fff;
}
.player-cover {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border: 1px solid #777;
  background: #fff;
}
.player-details {
  flex: 1;
}
.player-title {
  font-weight: bold;
  font-size: 18px;
  color: #800000;
}
.player-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  color: #555;
  font-family: "Courier New", monospace;
}
.progress-bar {
  flex: 1;
  height: 10px;
}
.music-list {
  max-width: 900px;
  margin: 0 auto;
}

.music-list h3 {
  font-size: 18px;
  font-weight: bold;
  color: #0f5e6d;
  margin: 0 0 10px;
  padding: 4px 8px;
  background: #e3f4f7;
  border: 1px solid #79ACC5;
}
.music-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 1px;
  background: #b9a982;
  font-size: 13px;
}
.music-table th,
.music-table td {
  border: none;
  padding: 6px 8px;
  text-align: left;
}
.music-table th {
  background: #e3f4f7;
  color: #0f5e6d;
  font-weight: bold;
}

.music-table td {
  background: #fffef7;
  color: #333;
}
tbody tr {
  cursor: pointer;
}
tbody tr:hover {
  background: transparent;
}

.music-table tbody tr:hover td {
  background: #fff6cc;
}
.list-cover {
  width: 46px;
  height: 46px;
  object-fit: cover;
  border: 1px solid #777;
  background: #fff;
}
.play-btn {
  background: #eee;
  border: 2px outset #fff;
  color: #333;
  padding: 2px 10px;
  font-size: 13px;
  font-weight: bold;
  font-family: "MS PGothic", "SimSun", "宋体", sans-serif;
}
.play-btn:hover {
  background: #fff6cc;
  color: #800000;
}
.play-btn:active {
  border-style: inset;
}
.no-music {
  max-width: 900px;
  margin: 18px auto;
  text-align: center;
  color: #800000;
  padding: 16px 18px;
  background: #fffef7;
  border: 1px dotted #b9a982;
  font-weight: bold;
}

/* ===== 11. 链接集页 ===== */
.links-container {
  max-width: 960px; margin: 0 auto;
  background: rgba(255,255,255,0.88);
  border: 2px solid #79ACC5;
  padding: 20px 26px 24px;
  font-size: 13px; color: #333;
  box-shadow: 2px 2px 6px rgba(0,0,0,0.12); 
}
.links-title {
  text-align: center;
}
.links-intro {
  margin: 20px 0;
  line-height: 1.5;
  text-align: center;
}
.links-category {
  margin: 30px 0;
}
.links-category h2 {
  border-left: 5px solid #7fdbff;
  padding-left: 12px;
  margin-bottom: 15px;
  font-size: 18px;
}
.maintaining {
  background-color: #fff5f7;
  border: solid 3px #fe69b4;
  color: #888;
  font-style: italic;
  margin-left: 20px;
}
.links-list {
  list-style: none;
  padding-left: 20px;
}
.links-list li {
  margin: 12px 0;
  line-height: 1.5;
}
.links-list a {
  color: #1abc9c;
  font-weight: bold;
}
.links-list a:hover {
  text-decoration: none;
}
.links-list a:link,
.links-list a:visited {
  color: #1abc9c;
}

@media (max-width: 820px) {
  body {
    overflow-x: hidden;
    font-size: 14px;
  }

  #app {
    width: 100%;
    min-width: 0;
    flex-direction: column;
    border-left: none;
    border-right: none;
  }


  .menu-header {
    text-align: center;
    font-size: 18px;
    margin-bottom: 10px;
  }

  .menu-list {
    display: grid;
    grid-template-columns: 1fr;
    gap: 0;
  }

  .menu-list li {
    margin-bottom: 22px;
    font-size: 13px;
    line-height: 1.35;
  }

  .menu-list a {
    font-size: 14px;
  }

  .menu-icon {
    font-size: 14px;
    margin-right: 1px;
  }

  .main-content {
    padding: 54px 12px 20px;
    border-left: 2px solid #D3D3D3;
    box-shadow: none;
    overflow: hidden;
  }

  .main-content::before {
    top: -100px;
    height: calc(100% + 300px);
    background-attachment: scroll;
  }

  .header-top {
    margin-bottom: 16px;
    gap: 0;
  }

  .title-img {
    width: 82vw !important;
    max-width: 480px;
    height: auto !important;
  }

  .welcome-msg {
    font-size: 12.5px;
    padding: 0 8px;
    margin-bottom: 8px;
  }

  .warning-bar {
    margin-left: -12px;
    margin-right: -12px;
    width: calc(100% + 24px);
    padding: 1px 12px;
    font-size: 13px;
  }

  .warning-bar-marquee-inner {
    animation-duration: 18s;
  }

  .counter-section {
    font-size: 15px;
    margin: 14px 0 8px;
  }

  .counter-digital {
    font-size: 22px;
    letter-spacing: 1px;
    padding: 2px 5px;
  }

  .counter-info {
    font-size: 12px;
  }

  .center-area {
    flex-direction: column;
    align-items: center;
    gap: 0;
  }

  .illust-container {
    flex: none;
    width: 100%;
  }

  .main-illust {
    width: 100%;
    max-width: 650px;
    height: auto;
    display: block;
    margin: 0 auto;
  }

  .log-box {
    width: 100%;
    margin: 8px 0 0;
  }

  .log-content {
    height: auto;
    max-height: 160px;
    padding: 10px;
    box-sizing: border-box;
  }

  .log-content li {
    white-space: normal;
  }

  .clap-info {
    left: 50%;
    transform: translateX(-50%);
    width: 220px;
    z-index: 10;
  }

  .notice-box {
    margin: 16px 0;
  }

  .notice-title {
    font-size: 20px;
  }

  .notice-content {
    font-size: 13px;
  }

  .info-table {
    width: 100%;
    font-size: 12px;
  }

  .info-table th,
  .info-table td {
    padding: 3px 4px;
  }

  .info-table img {
    max-width: 120px;
    height: auto;
  }

  .profile-container,
  .rules-container,
  .links-container {
    padding: 12px 14px 16px;
    font-size: 13px;
  }

  .profile-title,
  .gallery-title,
  .bbs-title,
  .rules-title,
  .music-title,
  .links-title {
    font-size: 20px;
  }

  .qa-list p {
    padding-left: 10px;
  }

  .artwork-card {
    width: calc(50% - 24px);
    margin: 8px;
    box-sizing: border-box;
  }

  .post-form,
  .post-list {
    width: 100%;
    max-width: none;
    box-sizing: border-box;
  }

  .admin-intro {
    max-width: none;
  }

  .admin-music-list,
  .admin-gallery-list {
    max-width: none;
  }

  .admin-delete-btn {
    margin-left: 6px;
    margin-top: 6px;
  }

  .post-list {
    padding: 12px 0;
  }

  .post-item {
    padding: 10px 0;
    margin-bottom: 14px;
  }

  .post-title {
    font-size: 16px;
  }

  .post-content {
    padding: 8px 0;
    overflow-wrap: anywhere;
  }

  .replies {
    padding-left: 8px;
  }

  .reply-item {
    margin-left: 8px;
    padding: 8px 0;
  }

  .form-table,
  .form-table tbody,
  .form-table tr,
  .form-table th,
  .form-table td {
    display: block;
    width: 100%;
    box-sizing: border-box;
  }

  .form-label {
    padding: 4px 8px;
  }

  .form-input,
  .form-input-title,
  .form-textarea,
  .form-input-small {
    width: 100%;
  }

  .form-title-cell {
    padding-top: 4px;
    padding-right: 6px !important;
  }

  .auth-status button {
    display: block;
    margin: 6px auto 0;
  }

  .form-submit-top {
    position: static;
    display: block;
    margin-top: 6px;
    width: 100%;
  }

  .form-file {
    width: 100%;
  }

  .form-hint {
    display: block;
    margin-left: 0;
    margin-top: 4px;
  }

  .music-search {
    margin: 14px 0 10px;
  }

  .search-input {
    width: 100%;
    box-sizing: border-box;
  }

  .music-player {
    padding: 12px;
    margin: 14px 0;
  }

  .player-controls {
    flex-direction: column;
    gap: 8px;
  }

  .ctrl-btn {
    width: 100%;
    padding: 8px 10px;
    font-size: 14px;
  }

  .player-info {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .player-cover {
    width: 96px;
    height: 96px;
    margin: 0 auto;
  }

  .player-title {
    font-size: 15px;
    text-align: center;
  }

  .player-progress {
    gap: 6px;
    font-size: 12px;
  }

  .music-list {
    overflow-x: auto;
  }

  .music-table {
    min-width: 560px;
    font-size: 13px;
  }

  .links-category h2 {
    font-size: 16px;
  }

  .links-list {
    padding-left: 0;
  }

  .maintaining {
    margin-left: 0;
    padding: 8px;
  }
}

@media (max-width: 420px) {
  .title-img {
    width: 78vw !important;
  }

  .welcome-msg {
    font-size: 11px;
  }

  .counter-digital {
    font-size: 18px;
  }

  .menu-list {
    grid-template-columns: 1fr;
  }

  .artwork-card {
    width: 100%;
    margin: 8px 0;
  }

  .info-table {
    font-size: 11px;
  }

  .notice-content,
  .post-content,
  .links-list li {
    font-size: 12px;
  }
}
</style>
