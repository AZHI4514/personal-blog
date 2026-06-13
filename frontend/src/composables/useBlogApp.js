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

// ==================== 核心页面状态 ====================

/** 当前活跃的页面名称，与路由 name 保持同步 */
const currentPage = ref('home')

/** 合法页面名称集合，用于校验路由/手动导航的目标页面 */
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

// ==================== 路由同步（模块级单例） ====================

/** Vue Router 实例的模块级缓存，由 useBlogApp() 首次注入 */
let appRouter = null

/** 标记路由同步 watcher 是否已注册，确保只注册一次 */
let routeSyncStarted = false

// ==================== UI 交互状态 ====================

/** 侧边栏是否展开（移动端菜单） */
const isSidebarOpen = ref(false)

/** 网站累计访客数，首页计数器展示 */
const visitorCount = ref(0)

/** 画廊图片列表，初始 mock 数据兜底 */
const galleryImages = ref([
  { path: 'https://picsum.photos/id/104/200/150', author: '张三' },
  { path: 'https://picsum.photos/id/15/200/150', author: '李四' },
  { path: 'https://picsum.photos/id/13/200/150', author: '宋江' }
])

// ==================== 认证状态 ====================

/** 当前登录用户对象，从 localStorage 恢复，未登录时为 null */
const currentUser = ref(readJson('currentUser', null))

/** 认证面板模式：'login' 登录 / 'register' 注册 */
const authMode = ref('login')

/** 认证弹窗是否展开 */
const isAuthMenuOpen = ref(false)

/** 认证表单数据（用户名、密码、邮箱） */
const authForm = ref({
  username: '',
  password: '',
  email: ''
})

/** 认证请求是否正在进行中 */
const authSubmitting = ref(false)

/** 是否已登录（currentUser 不为 null） */
const isLoggedIn = computed(() => !!currentUser.value)

/** 是否为管理员（用户名固定为 AZHI4514） */
const isAdmin = computed(() => currentUser.value?.username === 'AZHI4514')

/** 是否可以免钥匙删除帖子（管理员专属） */
const canDeleteWithoutKey = computed(() => currentUser.value?.username === 'AZHI4514')

// ==================== 管理员表单状态 ====================

/** 管理员音乐表单：标题、艺术家、文件路径、封面路径 */
const adminMusicForm = ref({
  title: '',
  artist: '',
  filePath: '',
  coverPath: ''
})

/** 管理员图片表单：图片路径、作者名 */
const adminImageForm = ref({
  path: '',
  author: 'AZHI4514'
})

/** 管理员上传状态：音乐文件、封面图、图片文件的独立 loading 标记 */
const adminUploading = ref({
  music: false,
  cover: false,
  image: false
})

/** 管理员提交状态：音乐和图片的独立 submitting 标记 */
const adminSubmitting = ref({
  music: false,
  image: false
})

/** 管理员删除状态：记录当前正在删除的 musicId / imageId */
const adminDeleting = ref({
  musicId: null,
  imageId: null
})

// ==================== Live2D 状态 ====================

/** Live2D canvas 元素的模板引用 */
const live2dCanvas = ref(null)

/** Live2D 加载错误信息 */
const live2dError = ref('')

/** Live2D 模型是否正在加载中 */
const live2dLoading = ref(false)

// ==================== 游戏聊天状态 ====================

/** 游戏角当前激活的面板：'chat' 对话 / 'config' 配置 */
const gameActivePanel = ref('chat')

/** 聊天消息列表的 DOM 引用，用于自动滚底 */
const gameMessageListRef = ref(null)

/** 是否正在发送消息 */
const gameSending = ref(false)

/** 聊天输入框内容 */
const gameInput = ref('')

/** 聊天消息列表 */
const gameMessages = ref([])

/** 当前正在流式接收的消息 ID，接收完成后清空 */
const gameStreamingMessageId = ref('')

// ==================== 启动错误收集 ====================

/** 收集 onMounted 期间的启动错误，供 UI 展示 */
const startupErrors = ref([])

// ==================== 非响应式内部变量（模块级单例） ====================

/** Live2D SDK 对象缓存 */
let live2dSdk = null

/** Live2D 子委托实例（管理 WebGL 渲染循环） */
let live2dSubdelegate = null

/** Live2D 渲染循环的 requestAnimationFrame ID */
let live2dAnimationFrame = 0

/** Live2D 指针事件处理器引用（用于解绑） */
let live2dPointerHandlers = null

/** Live2D 框架是否已初始化（startUp + initialize 只执行一次） */
let live2dFrameworkReady = false

/** 游戏聊天 SSE（EventSource）实例，用于流式接收 AI 回复 */
let gameChatEventSource = null

// ==================== 启动任务工具函数 ====================

/**
 * 收集启动错误到 startupErrors 数组，同时打印到控制台。
 * 用于 onMounted 中多个异步任务的统一错误捕获。
 * @param {string} label - 错误标签（任务描述）
 * @param {Error} error - 捕获的错误对象
 */
const collectStartupError = (label, error) => {
  const message = error?.stack || error?.message || String(error)
  console.error(`${label} failed`, error)
  startupErrors.value.push(`${label}: ${message}`)
}

/**
 * 安全执行一个启动任务，异常时收集而不阻断后续任务。
 * @param {string} label - 任务描述标签
 * @param {function} task - 异步任务函数
 */
const runStartupTask = async (label, task) => {
  try {
    await task()
  } catch (error) {
    collectStartupError(label, error)
  }
}

// ==================== 游戏聊天配置常量 ====================

/** 聊天助手的配置信息，在"配置信息"面板中展示 */
const gameConfigItems = [
  { label: '使用模型', value: 'Mimo-v2.5' },
  { label: '是否启用长期记忆', value: '是' },
  { label: '是否启用 MCP 服务', value: '是' },
  { label: 'MCP 服务模型', value: 'Dashscope' },
  { label: '调用 MCP 服务工具', value: 'web_search' }
]

// ==================== 网页拍手 ====================

/**
 * 发送拍手应援。
 * 用户点击首页拍手按钮时调用，无需登录。
 */
const handleClap = async () => {
  try {
    await sendClap()
    alert('感谢你的拍手！')
  } catch (err) {
    console.error('拍手失败', err)
    alert('拍手失败，请稍后再试')
  }
}

// ==================== 认证：登录 / 注册 / 登出 ====================

/**
 * 提交认证表单（登录或注册）。
 * 根据 authMode 决定调用 loginUser 或 registerUser。
 * 成功后将用户信息写入 localStorage 持久化，并重新加载帖子列表。
 */
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

/**
 * 退出登录。
 * 清除 currentUser、localStorage 中的用户数据、帖子缓存，
 * 重置所有表单状态，并导航回 BBS 页面。
 */
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

/** BBS 帖子表单数据：用户名、邮箱、标题、内容、附件图片、删除钥匙 */
const postForm = ref({
  username: '',
  email: '',
  title: '',
  content: '',
  imagePath: '',
  deleteKey: ''
})

/** 帖子提交是否正在进行中 */
const submitting = ref(false)

/** 附件上传是否正在进行中 */
const uploading = ref(false)

/** 当前正在回复的帖子 ID，null 表示不是回复模式 */
const replyingToPostId = ref(null)

/** 当前正在编辑的帖子 ID，null 表示不是编辑模式 */
const editingPostId = ref(null)

/** 帖子列表（不再分页，直接存储完整数组） */
const posts = ref([])

// ---------- 统一附件上传函数 ----------

/**
 * 上传图片文件，并将返回的文件路径写入目标表单的 imagePath 字段。
 * @param {Event} event - 文件选择的 change 事件
 * @param {Object} targetForm - 要更新 imagePath 的表单对象（postForm 或 adminImageForm）
 */
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

/** 封装为事件处理器：BBS 帖子附件上传 */
const handleFileUpload = (event) => uploadImage(event, postForm.value)

// ---------- 管理员表单重置 ----------

/** 重置管理员面板的音乐和图片表单到初始状态 */
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

// ---------- 管理员上传处理 ----------

/**
 * 上传音乐文件，将返回路径写入 adminMusicForm.filePath。
 * @param {Event} event - change 事件
 */
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

/**
 * 上传封面图片，将返回路径写入 adminMusicForm.coverPath。
 * @param {Event} event - change 事件
 */
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

/**
 * 上传画廊图片，将返回路径写入 adminImageForm.path。
 * @param {Event} event - change 事件
 */
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

// ---------- 管理员提交与删除 ----------

/**
 * 提交管理员音乐表单，创建新音乐记录。
 * 需要管理员权限；提交前校验标题和文件路径不为空。
 */
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
    // 重置表单并刷新列表
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

/**
 * 提交管理员图片表单，创建新画廊图片记录。
 * 需要管理员权限；提交前校验路径和作者不为空。
 */
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

/**
 * 删除指定音乐。
 * 需要管理员权限；如果当前正在播放该音乐，会先停止播放再删除。
 * @param {Object} music - 要删除的音乐对象
 */
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
    // 如果删除的恰好是当前播放的歌曲，先释放音频资源再重置状态
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

/**
 * 删除指定画廊图片。
 * 需要管理员权限。
 * @param {Object} image - 要删除的图片对象
 */
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

/**
 * 统一提交入口：根据当前状态执行发帖、回复或编辑操作。
 * - editingPostId 不为空 → 编辑现有帖子
 * - replyingToPostId 不为空 → 发布回复
 * - 两者均为空 → 发布新帖
 * 需要登录。
 */
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
      // 编辑模式：调用 updatePost，需要提供 deleteKey 验证
      const payload = {
        deleteKey: postForm.value.deleteKey,
        title: postForm.value.title,
        content: postForm.value.content,
        imagePath: postForm.value.imagePath
      }
      await updatePost(editingPostId.value, payload)
      alert('修改成功')
    } else {
      // 发帖/回复模式：调用 createPost
      // parentId 为空表示新帖，有值表示回复
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

/** 重置帖子表单和所有编辑/回复状态 */
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

/**
 * 开始回复指定帖子：设置 replyingToPostId 并清空表单。
 * @param {number|string} postId - 要回复的帖子 ID
 */
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

/**
 * 开始编辑指定帖子：设置 editingPostId 并按原有内容预填表单。
 * @param {Object} post - 要编辑的帖子对象
 */
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

/**
 * 删除帖子处理函数。
 * - 管理员：直接 confirm 后删除（无需 deleteKey）
 * - 普通用户：弹出 prompt 要求输入删除钥匙
 * @param {number|string} postId - 要删除的帖子 ID
 */
const deletePostHandler = async (postId) => {
  if (canDeleteWithoutKey.value) {
    // 管理员免钥匙删除
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

  // 普通用户需输入删除钥匙
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

/**
 * 从后端加载帖子列表，失败时使用 mock 模拟数据兜底。
 * 需要已登录状态，未登录时清空帖子列表。
 * 每条帖子的 replyCount 由 replies 数组长度计算。
 */
const loadPosts = async () => {
  if (!isLoggedIn.value) {
    posts.value = []
    return
  }
  try {
    // getPosts 直接返回所有帖子的完整数组（含嵌套 replies）
    const res = await getPosts()
    posts.value = res
  } catch (err) {
    console.error('加载帖子失败，使用模拟数据', err)
    // 网络/后端异常时使用 mock 数据，保证离线演示可用
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
    // 为每条帖子补上 replyCount，方便模板渲染
    posts.value.forEach(post => {
      post.replyCount = post.replies.length
    })
  }
}

// ==================== 音乐模块（完全重写，上下曲稳定） ====================

/** 完整的音乐曲目列表 */
const musicList = ref([])

/** 当前播放的音乐索引，-1 表示尚未选择/播放任何歌曲 */
const currentMusicIndex = ref(-1)

/** 当前 HTMLAudioElement 实例 */
const audio = ref(null)

/** 是否正在播放中 */
const isPlaying = ref(false)

/** 当前播放进度（秒） */
const currentTime = ref(0)

/** 当前歌曲总时长（秒） */
const duration = ref(0)

/** 搜索关键词，用于在音乐列表中按歌名或艺术家搜索 */
const searchKeyword = ref('')

/**
 * 过滤后的音乐列表。
 * 当 searchKeyword 为空时返回全部曲目，否则按歌名/艺术家模糊匹配。
 */
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

/**
 * 释放当前音频资源：暂停、清空 src、移除事件监听，防止内存泄漏。
 * 在切换歌曲或组件卸载时调用。
 */
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

/**
 * 核心播放函数：根据索引播放音乐。
 * - 同一首歌再次点击会切换播放/暂停
 * - 不同歌曲会先释放旧资源再创建新 Audio 实例
 * - 播放结束后自动切换到下一首（通过 onended 回调）
 * @param {number} index - 目标音乐在 musicList 中的索引
 */
const playByIndex = (index) => {
  if (!musicList.value.length) return
  if (index < 0) index = 0
  if (index >= musicList.value.length) index = musicList.value.length - 1

  const music = musicList.value[index]
  if (!music) return

  // 同一首歌曲 → 切换播放/暂停
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

  // 切换为新歌曲 → 释放旧音频，创建新实例
  releaseAudio()
  currentMusicIndex.value = index
  audio.value = new Audio(music.filePath)

  // 元数据加载完成 → 获取总时长
  audio.value.onloadedmetadata = () => {
    duration.value = audio.value?.duration || 0
  }
  // 播放进度更新
  audio.value.ontimeupdate = () => {
    if (audio.value) currentTime.value = audio.value.currentTime
  }
  // 播放结束 → 自动切换下一首
  audio.value.onended = () => {
    isPlaying.value = false
    nextTrack()
  }

  // 开始播放；浏览器可能阻止自动播放，捕获异常并降级
  audio.value.play()
    .then(() => { isPlaying.value = true })
    .catch(err => {
      console.warn('自动播放被阻止，需要用户交互', err)
      isPlaying.value = false
    })
}

/**
 * 供模板调用的播放函数：根据音乐对象查找索引后播放。
 * 兼容原有的按 music 对象播放的调用方式。
 * @param {Object} music - 音乐对象，需包含 musicId
 */
const playMusic = (music) => {
  const idx = musicList.value.findIndex(m => m.musicId === music.musicId)
  if (idx !== -1) playByIndex(idx)
}

/**
 * 切换播放/暂停。
 * 如果还没有选择歌曲，则默认从第一首开始播放。
 */
const togglePlay = () => {
  if (currentMusicIndex.value === -1 && musicList.value.length) {
    // 未选歌曲时默认播放第一首
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

/** 切换到上一首，列表循环（最后一首的上一首是第一首） */
const prevTrack = () => {
  if (!musicList.value.length) return
  let newIndex = currentMusicIndex.value - 1
  if (newIndex < 0) newIndex = musicList.value.length - 1
  playByIndex(newIndex)
}

/** 切换到下一首，列表循环（第一首的下一首是最后一首） */
const nextTrack = () => {
  if (!musicList.value.length) return
  let newIndex = currentMusicIndex.value + 1
  if (newIndex >= musicList.value.length) newIndex = 0
  playByIndex(newIndex)
}

/**
 * 将秒数格式化为 MM:SS 显示格式。
 * @param {number} seconds - 秒数
 * @returns {string} 格式化后的时间字符串，如 "03:45"
 */
const formatTime = (seconds) => {
  if (isNaN(seconds) || seconds === undefined) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

/**
 * 加载音乐列表，失败时使用 mock 数据兜底。
 */
const loadMusicList = async () => {
  try {
    const res = await getMusics()
    musicList.value = res
  } catch (err) {
    console.error('加载音乐失败', err)
    // 网络/后端异常时的 mock 数据
    musicList.value = [
      { musicId: 1, title: '転がる岩、君に朝が降る', artist: '結束バンド', filePath: '/music/転がる岩、君に朝が降る.mp3', coverPath: '/image/転がる岩、君に朝が降る.jpg' },
      { musicId: 2, title: '稻香', artist: '周杰伦', filePath: '/demo2.mp3', coverPath: '/demo2.jpg' }
    ]
  }
}
// ==================== 游戏聊天模块（SSE 流式对话） ====================

/**
 * 从 localStorage 读取游戏相关 JSON 数据。
 * @param {string} key - 存储键名
 * @param {*} fallback - 默认值
 * @returns {*} 解析后的值
 */
const readGameJson = (key, fallback) => {
  return readJson(key, fallback)
}

/**
 * 将游戏相关数据序列化为 JSON 写入 localStorage。
 * @param {string} key - 存储键名
 * @param {*} value - 要存储的值
 */
const writeGameJson = (key, value) => {
  writeJson(key, value)
}

/**
 * 获取当前用户的唯一标识，用于隔离不同用户的游戏数据。
 * 按优先级尝试 userId → id → username，都没有时回退为 'guest'。
 * @returns {string} 用户标识
 */
const getGameUserId = () => currentUser.value?.userId || currentUser.value?.id || currentUser.value?.username || 'guest'

/**
 * 生成按用户隔离的 localStorage 键名。
 * 格式：`原key:用户标识`，确保不同用户的数据互不干扰。
 * @param {string} key - 原始键名
 * @returns {string} 带用户后缀的键名
 */
const getGameScopedKey = (key) => `${key}:${getGameUserId()}`

/** 读取按用户隔离的 JSON 数据 */
const readScopedGameJson = (key, fallback) => readGameJson(getGameScopedKey(key), fallback)

/** 写入按用户隔离的 JSON 数据 */
const writeScopedGameJson = (key, value) => writeGameJson(getGameScopedKey(key), value)

/**
 * 生成一个新的对话记忆 ID。
 * 使用 Date.now() 对 2147483647 取模，确保在 int32 范围内。
 * @returns {number} 新的记忆 ID
 */
const createGameMemoryId = () => Math.floor(Date.now() % 2147483647)

/**
 * 获取或创建当前用户的对话记忆 ID。
 * 优先从 localStorage 读取，不存在时生成新的并持久化。
 * @returns {number} 记忆 ID
 */
const getGameMemoryId = () => {
  const stored = Number(readScopedGameJson('roomChatMemoryId', 0))
  if (Number.isInteger(stored) && stored > 0) {
    return stored
  }
  const nextId = createGameMemoryId()
  writeScopedGameJson('roomChatMemoryId', nextId)
  return nextId
}

/**
 * 创建一条聊天消息对象。
 * @param {string} role - 角色：'user' | 'assistant' | 'system'
 * @param {string} content - 消息内容
 * @param {Object} [options={}] - 可选参数（image 等）
 * @returns {{ id: string, role: string, content: string, image: string|null, createdAt: number }}
 */
const createGameMessage = (role, content, options = {}) => ({
  id: `msg-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`,
  role,
  content: String(content || ''),
  image: options.image || null,
  createdAt: Date.now()
})

/**
 * 向消息列表中添加一条消息，并自动滚动到底部。
 * @param {string} role - 角色
 * @param {string} content - 消息内容
 * @param {Object} [options={}] - 可选参数
 */
const addGameMessage = (role, content, options = {}) => {
  gameMessages.value.push(createGameMessage(role, content, options))
  // nextTick 确保 DOM 更新后再滚动
  nextTick(() => {
    if (gameMessageListRef.value) gameMessageListRef.value.scrollTop = gameMessageListRef.value.scrollHeight
  })
}

/**
 * 从 localStorage 加载聊天历史记录。
 * 无历史记录时显示默认欢迎消息。
 */
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

/**
 * 将当前消息列表写入 localStorage 持久化。
 * 仅保存最近 24 条消息，避免存储空间过大。
 */
const persistGameChatHistory = () => {
  writeScopedGameJson('roomChatHistory', gameMessages.value.slice(-24).map(item => ({
    role: item.role,
    content: item.content
  })))
}

/**
 * 关闭游戏聊天的 SSE 连接。
 */
const closeGameEventSource = () => {
  if (gameChatEventSource) {
    gameChatEventSource.close()
    gameChatEventSource = null
  }
}

/**
 * 更新指定消息的内容（用于流式接收时逐字追加）。
 * @param {string} messageId - 消息 ID
 * @param {function} updater - 更新函数，接收当前 content 返回新 content
 */
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

/**
 * 通过 SSE (Server-Sent Events) 流式获取 AI 回复。
 * 每次收到数据片段时追加到助手消息中，收到 'done' 事件时结束。
 * @param {number} memoryId - 对话记忆 ID，用于后端关联上下文
 * @param {string} userMessage - 用户发送的消息
 * @param {string} assistantMessageId - 助手消息的 ID，用于更新内容
 * @returns {Promise<void>}
 */
const streamGameChatByGet = async (memoryId, userMessage, assistantMessageId) => new Promise((resolve, reject) => {
  const params = new URLSearchParams({
    memoryId: String(memoryId),
    message: userMessage
  })
  const eventSource = new EventSource(`/ai/chat?${params.toString()}`)
  gameChatEventSource = eventSource

  // 收到数据片段 → 追加到助手消息
  eventSource.onmessage = (event) => {
    if (!event.data) return
    updateGameMessageContent(assistantMessageId, (content) => `${content}${event.data}`)
  }

  // 'done' 事件 → 对话完成
  eventSource.addEventListener('done', () => {
    closeGameEventSource()
    resolve()
  })

  // 后端返回的错误事件
  eventSource.addEventListener('error', (event) => {
    const errorMessage = event?.data || '对话服务连接失败'
    closeGameEventSource()
    reject(new Error(errorMessage))
  })

  // 网络层错误
  eventSource.onerror = () => {
    if (!gameChatEventSource) return
    closeGameEventSource()
    reject(new Error('对话服务连接中断'))
  }
})

/**
 * 清空当前对话和长期记忆。
 * 关闭 SSE 连接，重置消息列表为欢迎消息，并生成新的记忆 ID。
 */
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

/**
 * 发送游戏聊天消息。
 * 流程：关闭旧 SSE → 添加用户消息 → 创建空白助手消息 → 发起 SSE 流式请求 → 持久化。
 * 发送失败时在聊天界面显示错误提示。
 */
const sendGameMessage = async () => {
  const message = gameInput.value.trim()
  if (!message || gameSending.value) return

  // 关闭上一次的 SSE 连接（如果存在）
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
    // 如果助手消息仍为空（还没收到任何回复），直接更新其内容为错误提示
    const currentContent = gameMessages.value.find(item => item.id === gameStreamingMessageId.value)?.content?.trim()
    if (!currentContent && gameStreamingMessageId.value) {
      updateGameMessageContent(gameStreamingMessageId.value, () => `发送失败：${error.message}`)
    } else {
      // 已经收到部分内容后又出错，追加一条系统消息
      addGameMessage('system', `发送失败：${error.message}`)
    }
    persistGameChatHistory()
    gameSending.value = false
    gameStreamingMessageId.value = ''
  }
}

// ==================== Live2D 模块 ====================

/**
 * 确保 Live2D Cubism Core 原生库已加载。
 * 通过动态创建 script 标签加载 /Core/live2dcubismcore.js。
 * 如果已有其他页面加载过则会等待其完成，避免重复加载。
 * @returns {Promise<void>}
 */
const ensureLive2dCoreLoaded = async () => {
  if (window.Live2DCubismCore) {
    return
  }

  // 检查是否已有其他脚本正在加载 Core
  const currentScript = document.querySelector('script[data-live2d-core="true"]')
  if (currentScript) {
    await new Promise((resolve, reject) => {
      currentScript.addEventListener('load', resolve, { once: true })
      currentScript.addEventListener('error', reject, { once: true })
    })
    return
  }

  // 动态创建 script 标签加载 Core
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

/**
 * 加载 Live2D SDK（懒加载，首次调用时异步加载并缓存）。
 * 加载后会修补 LAppView、LAppSubdelegate、LAppLive2DManager 的原型方法，
 * 以适配项目的 WebGL 上下文管理和点击交互。
 * @returns {Promise<Object>} 包含 CubismFramework, Option, LAppPal, live2dDefine, LAppSubdelegate 的对象
 */
const loadLive2dSdk = async () => {
  // 已加载则直接返回缓存
  if (live2dSdk) {
    return live2dSdk
  }

  await ensureLive2dCoreLoaded()

  // 并行加载所有 SDK 模块
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

  // 修补 LAppView：延迟创建 shader program，适配透明背景渲染
  if (!LAppView.prototype.__appLive2dPatched) {
    LAppView.prototype.initializeSprite = function initializeSprite() {
      if (this._programId == null) {
        this._programId = this._subdelegate.createShader()
      }
    }
    LAppView.prototype.__appLive2dPatched = true
  }

  // 修补 LAppSubdelegate：自定义 WebGL 渲染流程，支持 context lost 检测和透明背景
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
      // 透明背景 + 深度测试 + 混合
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

  // 修补 LAppLive2DManager：点击时播放"招右手"动作 (Tap 组索引 0) + 随机表情
  if (!LAppLive2DManager.prototype.__appLive2dTapPatched) {
    LAppLive2DManager.prototype.onTap = function onTap() {
      const model = this._models[0]
      if (model) {
        const motionCount = model._modelSetting.getMotionCount('Tap')
        console.log('[Live2D Tap] Motion count for Tap group:', motionCount)
        const handle = model.startMotion('Tap', 0, 3)
        console.log('[Live2D Tap] startMotion result:', handle)
        model.setRandomExpression()
      }
    }
    LAppLive2DManager.prototype.__appLive2dTapPatched = true
  }

  // 设置模型目录为 'Yachiyo'
  live2dDefine.ModelDir.splice(0, live2dDefine.ModelDir.length, 'Yachiyo')

  // 缓存 SDK 对象
  live2dSdk = {
    CubismFramework,
    Option,
    LAppPal,
    live2dDefine,
    LAppSubdelegate
  }

  return live2dSdk
}

/**
 * 解绑 Live2D canvas 上的所有指针事件。
 */
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

/**
 * 绑定 Live2D canvas 的指针事件（点击、移动、释放、取消）。
 * 将鼠标/触摸坐标转换为 Live2D 视图坐标，驱动模型的视线跟随和点击交互。
 * @param {Object} subdelegate - LAppSubdelegate 实例
 * @param {HTMLCanvasElement} canvas - Live2D 渲染画布
 */
const attachLive2dPointerEvents = (subdelegate, canvas) => {
  /**
   * 更新 Live2D 模型的光标位置（视线跟随效果）。
   * @param {PointerEvent} event - 指针事件
   */
  const updateLive2dCursor = (event) => {
    const view = subdelegate._view
    if (!view) return

    // 将视口坐标映射到 canvas 坐标，再转换为 Live2D 视图坐标
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

  // passive: true 提升滚动性能，不阻止默认行为
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

/**
 * 销毁 Live2D 子委托实例：停止渲染循环、解绑事件、释放 WebGL 资源。
 */
const destroyLive2dInstance = () => {
  stopLive2dRenderLoop()
  detachLive2dPointerEvents()

  if (live2dSubdelegate) {
    try {
      live2dSubdelegate.release()
    } catch (e) {
      // WebGL 上下文可能已被浏览器清理，忽略异常
    }
    live2dSubdelegate = null
  }
}

/** 停止 Live2D 的 requestAnimationFrame 渲染循环 */
const stopLive2dRenderLoop = () => {
  if (live2dAnimationFrame) {
    cancelAnimationFrame(live2dAnimationFrame)
    live2dAnimationFrame = 0
  }
}

/**
 * 启动 Live2D 渲染循环。
 * 每帧调用 LAppPal.updateTime() 更新时间戳，subdelegate.update() 渲染一帧。
 * 当页面离开游戏角或 subdelegate 被替换时自动停止。
 * @param {Object} subdelegate - LAppSubdelegate 实例
 */
const startLive2dRenderLoop = (subdelegate) => {
  if (!subdelegate || live2dAnimationFrame || !live2dSdk?.LAppPal) {
    return
  }

  const render = () => {
    // 如果 subdelegate 已更换或不在游戏页面，停止渲染
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

/**
 * 彻底销毁 Live2D（实例 + 框架），释放所有资源。
 * 在离开游戏页面或组件卸载时调用。
 */
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

/**
 * 挂载并初始化 Live2D 模型。
 * 仅在游戏页面时执行。流程：等待 DOM → 加载 SDK → 初始化框架 → 创建实例 → 绑定事件 → 开始渲染。
 * 如果已在游戏页面上的 subdelegate 存在则先销毁再重建。
 */
const mountLive2d = async () => {
  if (currentPage.value !== 'games') {
    live2dLoading.value = false
    return
  }

  // 等待 canvas 元素渲染
  await nextTick()

  if (!live2dCanvas.value) {
    live2dLoading.value = true
    return
  }

  // 销毁已有实例（如果存在）
  if (live2dSubdelegate) {
    destroyLive2dInstance()
  }

  live2dLoading.value = true
  live2dError.value = ''

  try {
    const { CubismFramework, Option, LAppPal, live2dDefine, LAppSubdelegate } = await loadLive2dSdk()
    // 框架全局只初始化一次
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



// ==================== 侧边栏 / 导航 / 访客统计 / 画廊加载 ====================

/** 切换侧边栏展开/收起 */
function toggleSidebar() {
  isSidebarOpen.value = !isSidebarOpen.value
}

/** 关闭侧边栏 */
function closeSidebar() {
  isSidebarOpen.value = false
}

/** 打开侧边栏 */
function openSidebar() {
  isSidebarOpen.value = true
}

/**
 * 根据页面名称导航到指定页面。
 * 包含权限校验：管理员页面需 isAdmin，游戏页面需已登录（否则跳转到 BBS 登录）。
 * @param {string} pageName - 目标页面名称，必须在 validPageNames 集合中
 */
function showPage(pageName) {
  if (!validPageNames.has(pageName)) {
    return
  }
  // 仅管理员可访问管理员页面
  if (pageName === 'admin' && !isAdmin.value) {
    return
  }
  // 未登录用户尝试访问游戏页 → 跳转到 BBS 页面进行登录
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

/**
 * 内部导航函数：更新 currentPage 并通过 Vue Router 执行实际路由跳转。
 * 忽略 NavigationDuplicated 错误（重复导航到同一页面不报警告）。
 * @param {string} pageName - 目标页面名称
 */
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

/**
 * 打开认证页面并设置模式（登录/注册）。
 * @param {string} mode - 'login' 或 'register'
 */
function openAuthPage(mode) {
  authMode.value = mode
  showPage('bbs')
}

/** 切换认证弹窗的展开/关闭状态 */
function toggleAuthMenu() {
  isAuthMenuOpen.value = !isAuthMenuOpen.value
}

/**
 * 记录访客并获取累计访客总数。
 * 失败时回退为默认值 168，不阻塞页面加载。
 */
async function recordAndGetVisitor() {
  try {
    await recordVisitor()
    const result = await getTotalVisitors()
    visitorCount.value = result.totalVisitors
  } catch (error) {
    console.error('访客统计失败', error)
    // 失败时显示默认数值
    visitorCount.value = 168
  }
}

/**
 * 从后端加载画廊图片列表。
 * 失败时保持现有 mock 数据，静默处理。
 */
async function loadGallery() {
  try {
    const result = await getImages()
    galleryImages.value = result
  } catch (error) {
    console.error('获取画廊数据失败', error)
  }
}

/**
 * 从当前路由的 name 同步 currentPage 状态。
 * 确保页面刷新或直接访问 URL 时导航状态正确。
 * @param {Object} route - Vue Router 当前路由对象
 */
function syncCurrentPageFromRoute(route) {
  const pageName = typeof route.name === 'string' ? route.name : 'home'
  if (validPageNames.has(pageName)) {
    currentPage.value = pageName
  }
}

// ==================== 导出：全局状态对象 ====================

/**
 * 博客应用的全局状态对象。
 * 包含所有响应式状态和操作方法，通过 useBlogApp() 提供给各页面组件使用。
 * 注意：live2dCanvas 和 gameMessageListRef 是模板引用（ref），由 Vue 自动绑定。
 */
const blogAppState = {
  // --- 核心页面状态 ---
  currentPage,
  isSidebarOpen,
  visitorCount,
  galleryImages,

  // --- 认证状态 ---
  currentUser,
  authMode,
  isAuthMenuOpen,
  authForm,
  authSubmitting,
  isLoggedIn,
  isAdmin,
  canDeleteWithoutKey,

  // --- 管理员表单 ---
  adminMusicForm,
  adminImageForm,
  adminUploading,
  adminSubmitting,
  adminDeleting,

  // --- Live2D ---
  live2dCanvas,
  live2dError,
  live2dLoading,

  // --- 游戏聊天 ---
  gameActivePanel,
  gameMessageListRef,
  gameSending,
  gameInput,
  gameMessages,
  gameStreamingMessageId,
  startupErrors,
  gameConfigItems,

  // --- 认证操作 ---
  handleClap,
  submitAuth,
  logout,

  // --- BBS 帖子 ---
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

  // --- 音乐播放 ---
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

  // --- 游戏聊天操作 ---
  clearGameConversationAndMemory,
  sendGameMessage,

  // --- 导航 / 侧边栏 ---
  toggleSidebar,
  closeSidebar,
  openSidebar,
  showPage,
  openAuthPage,
  toggleAuthMenu,

  // --- 数据加载 ---
  recordAndGetVisitor,
  loadGallery
}

// ==================== 导出 composable ====================

/**
 * 获取博客应用的全局状态和操作方法。
 * 在任一 Vue 组件的 setup 中调用，即可获得所有共享状态。
 * 首次调用时会注册路由同步 watcher（全局只注册一次）。
 * @returns {Object} blogAppState 对象
 */
export function useBlogApp() {
  const router = useRouter()
  const route = useRoute()

  // 缓存 router 实例供 navigateToPage 使用
  appRouter = router

  // 全局只注册一次路由同步
  if (!routeSyncStarted) {
    routeSyncStarted = true
    syncCurrentPageFromRoute(route)
    watch(() => route.name, () => syncCurrentPageFromRoute(route))
  }

  return blogAppState
}

/**
 * 管理博客应用的生命周期钩子。
 * 需要在 DefaultLayout 中调用，负责：
 * - onBeforeUnmount: 清理 SSE、Live2D、音频资源
 * - onMounted: 按顺序加载聊天历史、访客统计、画廊、帖子、音乐列表
 * - watcher: 页面切换到游戏页时挂载 Live2D，离开时销毁
 * - watcher: canvas 元素就绪时重新挂载 Live2D
 * - watcher: 用户切换时重新加载聊天历史
 */
export function useBlogAppLifecycle() {
  // 组件卸载前清理所有外部资源
  onBeforeUnmount(() => {
    closeGameEventSource()
    destroyLive2d()
    releaseAudio()
  })

  // 组件挂载后按顺序加载初始数据
  onMounted(async () => {
    resetAdminForms()

    await runStartupTask('loadGameChatHistory', async () => {
      loadGameChatHistory()
    })
    await runStartupTask('recordAndGetVisitor', recordAndGetVisitor)
    await runStartupTask('loadGallery', loadGallery)
    await runStartupTask('loadPosts', loadPosts)
    await runStartupTask('loadMusicList', loadMusicList)

    // 如果初始页面就是游戏页，立即挂载 Live2D
    if (currentPage.value === 'games') {
      await runStartupTask('mountLive2d', mountLive2d)
    }
  })

  // 页面切换：进入游戏页 → 挂载 Live2D；离开游戏页 → 销毁实例（保留框架）
  watch(currentPage, async (pageName) => {
    if (pageName === 'games') {
      await nextTick()
      await runStartupTask('mountLive2d', mountLive2d)
      return
    }

    destroyLive2dInstance()
  })

  // canvas 元素就绪时重新挂载 Live2D（处理 DOM 延迟渲染的情况）
  watch(live2dCanvas, async (canvas) => {
    if (!canvas || currentPage.value !== 'games') {
      return
    }
    await runStartupTask('mountLive2d', mountLive2d)
  })

  // 用户切换时重新加载聊天历史（切换账号后显示新用户的对话记录）
  watch(currentUser, () => {
    loadGameChatHistory()
  })
}
