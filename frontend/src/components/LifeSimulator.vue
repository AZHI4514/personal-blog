<script setup>
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { makeLifeChoice, getLifeCharacterState, getLifeEvents, resetLifeGame, deleteLifeCharacter } from '@/api/life'
import { readJson, writeJson } from '@/utils/storage'

const props = defineProps({
  deviceId: { type: String, required: true },
  initialGameData: { type: Object, default: null }
})

const emit = defineEmits(['exit', 'noCharacter'])

// ==================== 响应式状态 ====================

const character = reactive({
  id: null, name: '', age: 0, money: 100, health: 80,
  happiness: 60, morality: 50, knowledge: 30, isAlive: true, generation: 1
})

const loading = ref(false)
const error = ref('')
const storyText = ref('')
const options = ref([])
const isGameOver = ref(false)
const eventLog = ref([])
const showEventLog = ref(false)
const storyContainerRef = ref(null)

// ==================== 工具函数 ====================

function statColor(value) {
  if (value >= 70) return '#4caf50'
  if (value >= 40) return '#ff9800'
  return '#f44336'
}

function statPercent(value) {
  return Math.max(0, Math.min(100, value))
}

function getStorageKey(key) {
  return `lifeSim:${props.deviceId}:${key}`
}

// ==================== 生命周期 ====================

onMounted(async () => {
  if (props.initialGameData) {
    // 从配置面板传入的初始数据，直接使用
    applyGameData(props.initialGameData)
    return
  }
  // 页面刷新 / 重新进入 → 从后端恢复
  await restoreProgress()
})

// ==================== 核心方法 ====================

function applyGameData(data) {
  const { character: char, story } = data
  Object.assign(character, char)
  storyText.value = story.description || ''
  options.value = story.options || []
  isGameOver.value = !!story.isGameOver
  writeJson(getStorageKey('characterId'), char.id)
  // 拉取事件历史
  fetchEvents(char.id)
}

async function restoreProgress() {
  const saved = readJson(getStorageKey('characterId'), null)
  if (!saved) {
    emit('noCharacter')
    return
  }
  try {
    const data = await getLifeCharacterState(saved)
    if (data && data.isAlive) {
      Object.assign(character, data)
      const events = await getLifeEvents(saved, 1, 5)
      eventLog.value = events?.list || []
      // 从最新事件恢复剧情
      if (eventLog.value.length > 0) {
        const lastEvent = eventLog.value[0]
        try {
          const story = JSON.parse(lastEvent.effects || '{}')
          storyText.value = story.description || lastEvent.description || ''
          options.value = story.options || []
          isGameOver.value = !!story.isGameOver
        } catch {
          storyText.value = lastEvent.description || ''
        }
      }
      return
    }
  } catch {
    // 进度失效
  }
  emit('noCharacter')
}

async function fetchEvents(characterId) {
  try {
    const data = await getLifeEvents(characterId, 1, 100)
    if (data?.list) eventLog.value = data.list
  } catch { /* ignore */ }
}

// 切换事件面板时刷新
watch(showEventLog, async (visible) => {
  if (visible && character.id) await fetchEvents(character.id)
})

async function handleChoice(index) {
  if (loading.value || isGameOver.value) return
  loading.value = true; error.value = ''

  try {
    const result = await makeLifeChoice({ characterId: character.id, choiceIndex: index })
    const { character: char, story } = result

    Object.assign(character, char)
    storyText.value = story.description || ''

    if (story.isGameOver) {
      isGameOver.value = true; options.value = []
    } else {
      options.value = story.options || []; isGameOver.value = false
    }

    await fetchEvents(character.id)
  } catch (e) {
    error.value = e.message || '操作失败，请重试'
  } finally { loading.value = false }

  scrollToBottom()
}

async function handleReset() {
  if (!confirm('确定要重新开局吗？当前角色的部分成就将被继承。')) return
  loading.value = true; error.value = ''

  try {
    const result = await resetLifeGame(character.id)
    const { character: char, story } = result
    Object.assign(character, char)
    storyText.value = story.description || ''
    options.value = story.options || []
    isGameOver.value = false
    writeJson(getStorageKey('characterId'), char.id)
    await fetchEvents(char.id)
  } catch (e) {
    error.value = e.message || '重新开局失败'
  } finally { loading.value = false }

  scrollToBottom()
}

async function handleDeleteCharacter() {
  if (!confirm('确定要删除当前角色的存档吗？\n\n这将删除该角色的所有事件记录。此操作不可撤销！')) return
  loading.value = true; error.value = ''
  try {
    await deleteLifeCharacter(character.id)
    writeJson(getStorageKey('characterId'), null)
    emit('noCharacter')
  } catch (e) {
    error.value = e.message || '删除失败'
  } finally { loading.value = false }
}

function scrollToBottom() {
  nextTick(() => {
    if (storyContainerRef.value) {
      storyContainerRef.value.scrollTop = storyContainerRef.value.scrollHeight
    }
  })
}
</script>

<template>
  <div class="life-sim-container">
    <div class="life-sim-game">
      <!-- 顶栏 -->
      <div class="life-topbar">
        <span class="life-topbar-name">{{ character.name }}</span>
        <span class="life-topbar-meta">🎂 {{ character.age }}岁</span>
        <span class="life-topbar-meta">🔄 第{{ character.generation }}代</span>
        <span v-if="!character.isAlive" class="life-topbar-dead">💀 已死亡</span>
        <span class="life-topbar-spacer"></span>
        <button type="button" class="life-exit-btn" @click="emit('exit')">⚙️ 返回配置</button>
      </div>

      <div class="life-layout">
        <!-- 属性面板 -->
        <div class="life-stats">
          <div class="life-stat-item">
            <span class="life-stat-icon">💰</span>
            <span class="life-stat-label">金钱</span>
            <div class="life-stat-bar"><div class="life-stat-fill" :style="{ width: statPercent(character.money) + '%', background: statColor(character.money) }"></div></div>
            <span class="life-stat-value">{{ character.money }}</span>
          </div>
          <div class="life-stat-item">
            <span class="life-stat-icon">❤️</span>
            <span class="life-stat-label">健康</span>
            <div class="life-stat-bar"><div class="life-stat-fill" :style="{ width: statPercent(character.health) + '%', background: statColor(character.health) }"></div></div>
            <span class="life-stat-value">{{ character.health }}</span>
          </div>
          <div class="life-stat-item">
            <span class="life-stat-icon">😊</span>
            <span class="life-stat-label">快乐</span>
            <div class="life-stat-bar"><div class="life-stat-fill" :style="{ width: statPercent(character.happiness) + '%', background: statColor(character.happiness) }"></div></div>
            <span class="life-stat-value">{{ character.happiness }}</span>
          </div>
          <div class="life-stat-item">
            <span class="life-stat-icon">⚖️</span>
            <span class="life-stat-label">道德</span>
            <div class="life-stat-bar"><div class="life-stat-fill" :style="{ width: statPercent(character.morality) + '%', background: statColor(character.morality) }"></div></div>
            <span class="life-stat-value">{{ character.morality }}</span>
          </div>
          <div class="life-stat-item">
            <span class="life-stat-icon">📚</span>
            <span class="life-stat-label">知识</span>
            <div class="life-stat-bar"><div class="life-stat-fill" :style="{ width: statPercent(character.knowledge) + '%', background: statColor(character.knowledge) }"></div></div>
            <span class="life-stat-value">{{ character.knowledge }}</span>
          </div>

          <button type="button" class="game-tab-btn life-reset-btn" :disabled="loading" @click="handleReset">
            🔄 重新开局
          </button>
          <button type="button" class="game-link-btn life-log-btn" @click="showEventLog = !showEventLog">
            {{ showEventLog ? '📜 隐藏记录' : '📜 事件记录' }}
          </button>
          <button type="button" class="life-delete-btn" :disabled="loading" @click="handleDeleteCharacter">
            🗑️ 删除存档
          </button>
        </div>

        <!-- 剧情区域 -->
        <div class="life-story-column">
          <div ref="storyContainerRef" class="life-story-area">
            <div class="life-story-text">{{ storyText }}</div>
            <div v-if="loading" class="life-loading">
              <span class="life-loading-dot">⏳</span> AI 正在生成剧情...
            </div>
            <div v-if="error && !loading" class="llm-config-result error" style="margin-top: 12px">{{ error }}</div>
          </div>

          <!-- 选项 -->
          <div v-if="!isGameOver && options.length > 0" class="life-options">
            <button v-for="(option, index) in options" :key="index" type="button"
              class="life-option-btn" :disabled="loading" @click="handleChoice(index)">
              {{ String.fromCharCode(65 + index) }}. {{ option.text }}
            </button>
          </div>

          <!-- 游戏结束 -->
          <div v-if="isGameOver" class="life-gameover">
            <p>💀 人生结束</p>
            <button type="button" class="game-send-btn" :disabled="loading" @click="handleReset">
              🔄 重新开局（继承成就）
            </button>
          </div>
        </div>
      </div>

      <!-- 事件记录 -->
      <div v-if="showEventLog" class="life-event-log">
        <h4 class="game-config-title">📜 事件记录</h4>
        <div v-if="eventLog.length === 0" class="life-event-empty">还没有事件记录。</div>
        <div v-for="event in eventLog" :key="event.id || event.age" class="life-event-item">
          <span class="life-event-age">🎂 {{ event.age }}岁</span>
          <span class="life-event-desc">{{ event.description }}</span>
          <span v-if="event.choiceMade" class="life-event-choice">→ {{ event.choiceMade }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.life-sim-container { height: 100%; display: flex; flex-direction: column; }

/* 顶栏 */
.life-topbar { display: flex; align-items: center; gap: 12px; padding: 8px 12px; background: #fdf8e8; border: 1px solid #e0d5b7; border-radius: 4px; margin-bottom: 10px; flex-wrap: wrap; }
.life-topbar-name { font-weight: bold; font-size: 15px; color: #503e2c; }
.life-topbar-meta { font-size: 12px; color: #6b5e3e; }
.life-topbar-dead { font-size: 12px; color: #c62828; font-weight: bold; }
.life-topbar-spacer { flex: 1; }
.life-exit-btn { padding: 3px 10px; border: 1px solid #b9a982; border-radius: 4px; background: #fffef7; color: #6b5e3e; font-size: 12px; font-family: inherit; cursor: pointer; white-space: nowrap; }
.life-exit-btn:hover { background: #f0e8d0; border-color: #800000; color: #800000; }

/* 布局 */
.life-layout { display: flex; gap: 12px; flex: 1; min-height: 0; }

/* 属性面板 */
.life-stats { width: 160px; flex-shrink: 0; display: flex; flex-direction: column; gap: 8px; padding: 10px; background: #fffef7; border: 1px solid #e0d5b7; border-radius: 4px; }
.life-stat-item { display: flex; align-items: center; gap: 4px; font-size: 12px; }
.life-stat-icon { width: 20px; text-align: center; }
.life-stat-label { width: 30px; color: #503e2c; font-weight: bold; font-size: 11px; }
.life-stat-bar { flex: 1; height: 8px; background: #e0d5b7; border-radius: 4px; overflow: hidden; min-width: 30px; }
.life-stat-fill { height: 100%; border-radius: 4px; transition: width 0.5s ease, background 0.5s ease; }
.life-stat-value { width: 24px; text-align: right; font-weight: bold; font-size: 11px; color: #333; }
.life-reset-btn { margin-top: 8px; width: 100%; text-align: center; }
.life-log-btn { width: 100%; text-align: center; }
.life-delete-btn { width: 100%; margin-top: 4px; padding: 4px 8px; border: 1px solid #e0c0c0; border-radius: 4px; background: #fff5f5; color: #c62828; font-size: 11px; font-family: inherit; cursor: pointer; text-align: center; }
.life-delete-btn:hover:not(:disabled) { background: #fce4ec; border-color: #c62828; }
.life-delete-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* 剧情区域 */
.life-story-column { flex: 1; display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.life-story-area { flex: 1; min-height: 200px; max-height: 320px; overflow-y: auto; padding: 14px; background: #fffef7; border: 1px solid #e0d5b7; border-radius: 4px; line-height: 1.8; }
.life-story-text { font-size: 14px; color: #333; white-space: pre-wrap; word-break: break-word; }
.life-loading { margin-top: 12px; font-size: 13px; color: #999; display: flex; align-items: center; gap: 6px; }
.life-loading-dot { animation: pulse 1s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

/* 选项 */
.life-options { display: flex; flex-direction: column; gap: 8px; }
.life-option-btn { display: block; width: 100%; padding: 10px 14px; min-height: 44px; border: 2px outset #e0d5b7; border-radius: 4px; background: #fdf8e8; color: #503e2c; font-size: 13px; font-family: inherit; cursor: pointer; text-align: left; transition: background 0.15s; }
.life-option-btn:hover:not(:disabled) { background: #fff6cc; border-color: #b9a982; }
.life-option-btn:active:not(:disabled) { border-style: inset; }
.life-option-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* 游戏结束 */
.life-gameover { text-align: center; padding: 20px; background: #fce4ec; border: 1px solid #ef9a9a; border-radius: 4px; }
.life-gameover p { font-size: 18px; color: #c62828; margin: 0 0 12px 0; font-weight: bold; }

/* 事件记录 */
.life-event-log { margin-top: 10px; padding: 12px; background: #fffef7; border: 1px solid #e0d5b7; border-radius: 4px; max-height: 200px; overflow-y: auto; }
.life-event-empty { font-size: 12px; color: #999; text-align: center; padding: 12px; }
.life-event-item { display: flex; align-items: baseline; gap: 8px; padding: 6px 0; border-bottom: 1px dashed #e0d5b7; font-size: 12px; }
.life-event-item:last-child { border-bottom: none; }
.life-event-age { color: #800000; font-weight: bold; white-space: nowrap; }
.life-event-desc { flex: 1; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.life-event-choice { color: #6b5e3e; white-space: nowrap; }

/* 响应式 */
@media (max-width: 768px) {
  .life-layout { flex-direction: column; }
  .life-stats { width: 100%; flex-direction: row; flex-wrap: wrap; gap: 6px; }
  .life-stat-item { flex: 1 1 auto; min-width: 100px; }
  .life-stat-bar { min-width: 20px; }
  .life-story-area { max-height: 220px; }
  .life-reset-btn, .life-log-btn { width: auto; flex: 0 0 auto; }
}
</style>
