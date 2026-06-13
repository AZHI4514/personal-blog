import { ref, onMounted, onBeforeUnmount, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recordVisitor, getTotalVisitors } from '@/api/visitor'
import { sendClap } from '@/api/clap'
import { createImage, deleteImage, getImages } from '@/api/gallery'
import { createMusic, deleteMusic, getMusics } from '@/api/music'
import { createPost, deletePost, getPosts, updatePost } from '@/api/post'
import { uploadImageFile, uploadMusicFile } from '@/api/upload'
import { loginUser, logoutUser, registerUser } from '@/api/user'
import { readJson, removeItem, writeJson } from '@/utils/storage'

const currentPage = ref('home')

const validPageNames = new Set([
  'home',
  'profile',
  'gallery',
  'bbs',
  'rules',
  'games',
  'music',
  'admin',
  'links'
])

let appRouter = null
let routeSyncStarted = false
const isSidebarOpen = ref(false)
const visitorCount = ref(0)
const galleryImages = ref([
  { path: 'https://picsum.photos/id/104/200/150', author: '张三' },
  { path: 'https://picsum.photos/id/15/200/150', author: '李四' },
  { path: 'https://picsum.photos/id/13/200/150', author: '宋江' }
])
const currentUser = ref(readJson('currentUser', null))
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
const live2dCanvas = ref(null)
const live2dError = ref('')
const live2dLoading = ref(false)
const gameActivePanel = ref('chat')
const gameMessageListRef = ref(null)
const gameSending = ref(false)
const gameInput = ref('')
const gameMessages = ref([])
const gameStreamingMessageId = ref('')
const startupErrors = ref([])

let live2dSdk = null
let live2dSubdelegate = null
let live2dAnimationFrame = 0
let live2dPointerHandlers = null
let live2dFrameworkReady = false
let gameChatEventSource = null

const collectStartupError = (label, error) => {
  const message = error?.stack || error?.message || String(error)
  console.error(`${label} failed`, error)
  startupErrors.value.push(`${label}: ${message}`)
}

const runStartupTask = async (label, task) => {
  try {
    await task()
  } catch (error) {
    collectStartupError(label, error)
  }
}

const gameConfigItems = [
  { label: '使用模型', value: 'Mimo-v2.5' },
  { label: '是否启用长期记忆', value: '是' },
  { label: '是否启用 MCP 服务', value: '是' },
  { label: 'MCP 服务模型', value: 'Dashscope' },
  { label: '调用 MCP 服务工具', value: 'web_search' }
]

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
    writeJson('currentUser', user)
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
  removeItem('currentUser')
  posts.value = []
  resetForm()
  resetAdminForms()
  authMode.value = 'login'
  navigateToPage('bbs')
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
    alert(err.message || '音乐上传失败，请稍后再试')
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
      const payload = {
        deleteKey: postForm.value.deleteKey,
        title: postForm.value.title,
        content: postForm.value.content,
        imagePath: postForm.value.imagePath
      }
      await updatePost(editingPostId.value, payload)
      alert('修改成功')
    } else {
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
const readGameJson = (key, fallback) => {
  return readJson(key, fallback)
}

const writeGameJson = (key, value) => {
  writeJson(key, value)
}

const getGameUserId = () => currentUser.value?.userId || currentUser.value?.id || currentUser.value?.username || 'guest'

const getGameScopedKey = (key) => `${key}:${getGameUserId()}`

const readScopedGameJson = (key, fallback) => readGameJson(getGameScopedKey(key), fallback)

const writeScopedGameJson = (key, value) => writeGameJson(getGameScopedKey(key), value)

const createGameMemoryId = () => Math.floor(Date.now() % 2147483647)

const getGameMemoryId = () => {
  const stored = Number(readScopedGameJson('roomChatMemoryId', 0))
  if (Number.isInteger(stored) && stored > 0) {
    return stored
  }
  const nextId = createGameMemoryId()
  writeScopedGameJson('roomChatMemoryId', nextId)
  return nextId
}

const createGameMessage = (role, content, options = {}) => ({
  id: `msg-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`,
  role,
  content: String(content || ''),
  image: options.image || null,
  createdAt: Date.now()
})

const addGameMessage = (role, content, options = {}) => {
  gameMessages.value.push(createGameMessage(role, content, options))
  nextTick(() => {
    if (gameMessageListRef.value) gameMessageListRef.value.scrollTop = gameMessageListRef.value.scrollHeight
  })
}

const loadGameChatHistory = () => {
  const history = readScopedGameJson('roomChatHistory', [])
  if (!Array.isArray(history) || !history.length) {
    gameMessages.value = [
      createGameMessage('assistant', '欢迎来到游戏角。这里已经接入了 Yachiyo 的聊天助手')
    ]
    return
  }
  gameMessages.value = history.map(item => createGameMessage(item.role, item.content))
}

const persistGameChatHistory = () => {
  writeScopedGameJson('roomChatHistory', gameMessages.value.slice(-24).map(item => ({
    role: item.role,
    content: item.content
  })))
}

const closeGameEventSource = () => {
  if (gameChatEventSource) {
    gameChatEventSource.close()
    gameChatEventSource = null
  }
}

const updateGameMessageContent = (messageId, updater) => {
  const index = gameMessages.value.findIndex(item => item.id === messageId)
  if (index < 0) return
  const current = gameMessages.value[index]
  gameMessages.value[index] = {
    ...current,
    content: updater(current.content || '')
  }
  nextTick(() => {
    if (gameMessageListRef.value) gameMessageListRef.value.scrollTop = gameMessageListRef.value.scrollHeight
  })
}

const streamGameChatByGet = async (memoryId, userMessage, assistantMessageId) => new Promise((resolve, reject) => {
  const params = new URLSearchParams({
    memoryId: String(memoryId),
    message: userMessage
  })
  const eventSource = new EventSource(`/ai/chat?${params.toString()}`)
  gameChatEventSource = eventSource

  eventSource.onmessage = (event) => {
    if (!event.data) return
    updateGameMessageContent(assistantMessageId, (content) => `${content}${event.data}`)
  }

  eventSource.addEventListener('done', () => {
    closeGameEventSource()
    resolve()
  })

  eventSource.addEventListener('error', (event) => {
    const errorMessage = event?.data || '对话服务连接失败'
    closeGameEventSource()
    reject(new Error(errorMessage))
  })

  eventSource.onerror = () => {
    if (!gameChatEventSource) return
    closeGameEventSource()
    reject(new Error('对话服务连接中断'))
  }
})

const clearGameConversationAndMemory = async () => {
  const confirmed = window.confirm('确定要清空当前对话和长期记忆吗？')
  if (!confirmed) return
  try {
    closeGameEventSource()
    gameMessages.value = [
      createGameMessage('assistant', '欢迎回到游戏角。之前的对话和记忆已经清空，我们可以重新开始。')
    ]
    writeScopedGameJson('roomChatHistory', [])
    writeScopedGameJson('roomChatMemoryId', createGameMemoryId())
    gameStreamingMessageId.value = ''
    nextTick(() => {
      if (gameMessageListRef.value) gameMessageListRef.value.scrollTop = gameMessageListRef.value.scrollHeight
    })
  } catch (error) {
    console.error('clear game conversation failed', error)
    alert(error.message || '清空对话失败')
  }
}

const sendGameMessage = async () => {
  const message = gameInput.value.trim()
  if (!message || gameSending.value) return

  closeGameEventSource()
  addGameMessage('user', message)
  gameInput.value = ''
  gameSending.value = true

  try {
    const assistantMessage = createGameMessage('assistant', '')
    gameMessages.value.push(assistantMessage)
    gameStreamingMessageId.value = assistantMessage.id
    const memoryId = getGameMemoryId()
    await streamGameChatByGet(memoryId, message, assistantMessage.id)
    persistGameChatHistory()
    gameSending.value = false
    gameStreamingMessageId.value = ''
  } catch (error) {
    console.error('send game message failed', error)
    const currentContent = gameMessages.value.find(item => item.id === gameStreamingMessageId.value)?.content?.trim()
    if (!currentContent && gameStreamingMessageId.value) {
      updateGameMessageContent(gameStreamingMessageId.value, () => `发送失败：${error.message}`)
    } else {
      addGameMessage('system', `发送失败：${error.message}`)
    }
    persistGameChatHistory()
    gameSending.value = false
    gameStreamingMessageId.value = ''
  }
}

const ensureLive2dCoreLoaded = async () => {
  if (window.Live2DCubismCore) {
    return
  }

  const currentScript = document.querySelector('script[data-live2d-core="true"]')
  if (currentScript) {
    await new Promise((resolve, reject) => {
      currentScript.addEventListener('load', resolve, { once: true })
      currentScript.addEventListener('error', reject, { once: true })
    })
    return
  }

  await new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = '/Core/live2dcubismcore.js'
    script.async = true
    script.dataset.live2dCore = 'true'
    script.addEventListener('load', resolve, { once: true })
    script.addEventListener('error', () => reject(new Error('Live2D Core load failed')), { once: true })
    document.head.appendChild(script)
  })
}

const loadLive2dSdk = async () => {
  if (live2dSdk) {
    return live2dSdk
  }

  await ensureLive2dCoreLoaded()

  const [
    { CubismFramework, Option },
    { LAppPal },
    live2dDefine,
    { LAppSubdelegate },
    { LAppView },
    { LAppLive2DManager }
  ] = await Promise.all([
    import('@framework/live2dcubismframework'),
    import('@live2d-demo/lapppal'),
    import('@live2d-demo/lappdefine'),
    import('@live2d-demo/lappsubdelegate'),
    import('@live2d-demo/lappview'),
    import('@live2d-demo/lapplive2dmanager')
  ])

  if (!LAppView.prototype.__appLive2dPatched) {
    LAppView.prototype.initializeSprite = function initializeSprite() {
      if (this._programId == null) {
        this._programId = this._subdelegate.createShader()
      }
    }
    LAppView.prototype.__appLive2dPatched = true
  }

  if (!LAppSubdelegate.prototype.__appLive2dPatched) {
    LAppSubdelegate.prototype.update = function update() {
      if (this._glManager.getGl().isContextLost()) {
        return
      }

      if (this._needResize) {
        this.onResize()
        this._needResize = false
      }

      const gl = this._glManager.getGl()

      gl.clearColor(0.0, 0.0, 0.0, 0.0)
      gl.enable(gl.DEPTH_TEST)
      gl.depthFunc(gl.LEQUAL)
      gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
      gl.clearDepth(1.0)
      gl.enable(gl.BLEND)
      gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)

      this._view.render()
    }
    LAppSubdelegate.prototype.__appLive2dPatched = true
  }

  if (!LAppLive2DManager.prototype.__appLive2dTapPatched) {
    LAppLive2DManager.prototype.onTap = function onTap() {
      const model = this._models[0]
      if (model) {
        // 调试：检查动作组是否被正确加载
        const motionCount = model._modelSetting.getMotionCount('Tap')
        console.log('[Live2D Tap] Motion count for Tap group:', motionCount)
        // 播放"招右手"动作
        const handle = model.startMotion('Tap', 0, 3)
        console.log('[Live2D Tap] startMotion result:', handle)
        // 同时随机播放一个表情
        model.setRandomExpression()
      }
    }
    LAppLive2DManager.prototype.__appLive2dTapPatched = true
  }

  live2dDefine.ModelDir.splice(0, live2dDefine.ModelDir.length, 'Yachiyo')

  live2dSdk = {
    CubismFramework,
    Option,
    LAppPal,
    live2dDefine,
    LAppSubdelegate
  }

  return live2dSdk
}

const detachLive2dPointerEvents = () => {
  if (!live2dPointerHandlers) {
    return
  }

  const { canvas, handlePointerDown, handlePointerMove, handlePointerUp, handlePointerCancel } = live2dPointerHandlers
  canvas.removeEventListener('pointerdown', handlePointerDown)
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', handlePointerUp)
  window.removeEventListener('pointercancel', handlePointerCancel)
  live2dPointerHandlers = null
}

const attachLive2dPointerEvents = (subdelegate, canvas) => {
  const updateLive2dCursor = (event) => {
    const view = subdelegate._view

    if (!view) {
      return
    }

    const viewportWidth = Math.max(window.innerWidth, 1)
    const viewportHeight = Math.max(window.innerHeight, 1)
    const posX = (event.clientX / viewportWidth) * canvas.width
    const posY = (event.clientY / viewportHeight) * canvas.height
    const viewX = view.transformViewX(posX)
    const viewY = view.transformViewY(posY)
    subdelegate.getLive2DManager().onDrag(viewX, viewY)
  }

  const handlePointerDown = (event) => {
    subdelegate.onPointBegan(event.pageX, event.pageY)
    updateLive2dCursor(event)
  }
  const handlePointerMove = (event) => {
    updateLive2dCursor(event)
  }
  const handlePointerUp = (event) => {
    subdelegate.onPointEnded(event.pageX, event.pageY)
  }
  const handlePointerCancel = (event) => {
    subdelegate.getLive2DManager().onDrag(0.0, 0.0)
    if (event?.pageX != null && event?.pageY != null) {
      subdelegate.onTouchCancel(event.pageX, event.pageY)
    }
  }

  canvas.addEventListener('pointerdown', handlePointerDown, { passive: true })
  window.addEventListener('pointermove', handlePointerMove, { passive: true })
  window.addEventListener('pointerup', handlePointerUp, { passive: true })
  window.addEventListener('pointercancel', handlePointerCancel, { passive: true })

  live2dPointerHandlers = {
    canvas,
    handlePointerDown,
    handlePointerMove,
    handlePointerUp,
    handlePointerCancel
  }
}

const destroyLive2dInstance = () => {
  stopLive2dRenderLoop()
  detachLive2dPointerEvents()

  if (live2dSubdelegate) {
    try {
      live2dSubdelegate.release()
    } catch (e) {
      // WebGL 上下文可能已被浏览器清理，忽略即可
    }
    live2dSubdelegate = null
  }
}

const stopLive2dRenderLoop = () => {
  if (live2dAnimationFrame) {
    cancelAnimationFrame(live2dAnimationFrame)
    live2dAnimationFrame = 0
  }
}

const startLive2dRenderLoop = (subdelegate) => {
  if (!subdelegate || live2dAnimationFrame || !live2dSdk?.LAppPal) {
    return
  }

  const render = () => {
    if (live2dSubdelegate !== subdelegate || currentPage.value !== 'games') {
      live2dAnimationFrame = 0
      return
    }

    live2dSdk.LAppPal.updateTime()
    subdelegate.update()
    live2dAnimationFrame = requestAnimationFrame(render)
  }

  render()
}

const destroyLive2d = () => {
  destroyLive2dInstance()
  if (live2dSdk?.CubismFramework) {
    live2dSdk.CubismFramework.dispose()
    live2dSdk.CubismFramework.cleanUp()
  }
  live2dFrameworkReady = false
  live2dSdk = null
  live2dLoading.value = false
}

const mountLive2d = async () => {
  if (currentPage.value !== 'games') {
    live2dLoading.value = false
    return
  }

  await nextTick()

  if (!live2dCanvas.value) {
    live2dLoading.value = true
    return
  }

  if (live2dSubdelegate) {
    destroyLive2dInstance()
  }

  live2dLoading.value = true
  live2dError.value = ''

  try {
    const { CubismFramework, Option, LAppPal, live2dDefine, LAppSubdelegate } = await loadLive2dSdk()
    if (!live2dFrameworkReady) {
      const option = new Option()
      option.logFunction = LAppPal.printMessage
      option.loggingLevel = live2dDefine.CubismLoggingLevel

      CubismFramework.startUp(option)
      CubismFramework.initialize()
      live2dFrameworkReady = true
    }

    const subdelegate = new LAppSubdelegate()
    const initialized = subdelegate.initialize(live2dCanvas.value)

    if (!initialized) {
      throw new Error('Live2D WebGL initialization failed')
    }

    live2dSubdelegate = subdelegate
    attachLive2dPointerEvents(subdelegate, live2dCanvas.value)
    live2dLoading.value = false
    startLive2dRenderLoop(subdelegate)
  } catch (error) {
    console.error('Live2D initialization failed', error)
    live2dError.value = 'Live2D load failed.'
    live2dLoading.value = false
    destroyLive2d()
  }
}



// ---------- 其他页面函数 ----------
function toggleSidebar() {
  isSidebarOpen.value = !isSidebarOpen.value
}

function closeSidebar() {
  isSidebarOpen.value = false
}

function openSidebar() {
  isSidebarOpen.value = true
}

function showPage(pageName) {
  if (!validPageNames.has(pageName)) {
    return
  }
  if (pageName === 'admin' && !isAdmin.value) {
    return
  }
  if (pageName === 'games' && !isLoggedIn.value) {
    authMode.value = 'login'
    isAuthMenuOpen.value = false
    navigateToPage('bbs')
    closeSidebar()
    return
  }

  isAuthMenuOpen.value = false
  navigateToPage(pageName)
  closeSidebar()
}

function navigateToPage(pageName) {
  currentPage.value = pageName

  if (!appRouter) {
    return
  }

  const navigation = appRouter.push({ name: pageName })
  if (navigation && typeof navigation.catch === 'function') {
    navigation.catch((error) => {
      if (error?.name !== 'NavigationDuplicated') {
        console.warn('Page navigation failed', error)
      }
    })
  }
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

function syncCurrentPageFromRoute(route) {
  const pageName = typeof route.name === 'string' ? route.name : 'home'
  if (validPageNames.has(pageName)) {
    currentPage.value = pageName
  }
}

const blogAppState = {
  currentPage,
  isSidebarOpen,
  visitorCount,
  galleryImages,
  currentUser,
  authMode,
  isAuthMenuOpen,
  authForm,
  authSubmitting,
  isLoggedIn,
  isAdmin,
  canDeleteWithoutKey,
  adminMusicForm,
  adminImageForm,
  adminUploading,
  adminSubmitting,
  adminDeleting,
  live2dCanvas,
  live2dError,
  live2dLoading,
  gameActivePanel,
  gameMessageListRef,
  gameSending,
  gameInput,
  gameMessages,
  gameStreamingMessageId,
  startupErrors,
  gameConfigItems,
  handleClap,
  submitAuth,
  logout,
  postForm,
  submitting,
  uploading,
  replyingToPostId,
  editingPostId,
  posts,
  uploadImage,
  handleFileUpload,
  resetAdminForms,
  handleAdminMusicUpload,
  handleAdminCoverUpload,
  handleAdminImageUpload,
  submitAdminMusic,
  submitAdminImage,
  deleteAdminMusic,
  deleteAdminImage,
  submitPostOrReply,
  resetForm,
  startReply,
  startEdit,
  deletePostHandler,
  loadPosts,
  musicList,
  currentMusicIndex,
  audio,
  isPlaying,
  currentTime,
  duration,
  searchKeyword,
  filteredMusicList,
  releaseAudio,
  playByIndex,
  playMusic,
  togglePlay,
  prevTrack,
  nextTrack,
  formatTime,
  loadMusicList,
  clearGameConversationAndMemory,
  sendGameMessage,
  toggleSidebar,
  closeSidebar,
  openSidebar,
  showPage,
  openAuthPage,
  toggleAuthMenu,
  recordAndGetVisitor,
  loadGallery
}

export function useBlogApp() {
  const router = useRouter()
  const route = useRoute()

  appRouter = router

  if (!routeSyncStarted) {
    routeSyncStarted = true
    syncCurrentPageFromRoute(route)
    watch(() => route.name, () => syncCurrentPageFromRoute(route))
  }

  return blogAppState
}

export function useBlogAppLifecycle() {
  onBeforeUnmount(() => {
    closeGameEventSource()
    destroyLive2d()
    releaseAudio()
  })

  onMounted(async () => {
    resetAdminForms()
  
    await runStartupTask('loadGameChatHistory', async () => {
      loadGameChatHistory()
    })
    await runStartupTask('recordAndGetVisitor', recordAndGetVisitor)
    await runStartupTask('loadGallery', loadGallery)
    await runStartupTask('loadPosts', loadPosts)
    await runStartupTask('loadMusicList', loadMusicList)
  
    if (currentPage.value === 'games') {
      await runStartupTask('mountLive2d', mountLive2d)
    }
  })

  watch(currentPage, async (pageName) => {
    if (pageName === 'games') {
      await nextTick()
      await runStartupTask('mountLive2d', mountLive2d)
      return
    }
  
    destroyLive2dInstance()
  })

  watch(live2dCanvas, async (canvas) => {
    if (!canvas || currentPage.value !== 'games') {
      return
    }
    await runStartupTask('mountLive2d', mountLive2d)
  })

  watch(currentUser, () => {
    loadGameChatHistory()
  })
}
