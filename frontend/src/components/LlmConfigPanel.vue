<script setup>
import { ref, onMounted } from 'vue'
import { saveLlmConfig, getLlmConfig, testLlmConnection, startLifeGame, getLifeCharacterState, deleteLifeUserData } from '@/api/life'
import { readJson, writeJson } from '@/utils/storage'

const props = defineProps({
  deviceId: { type: String, required: true },
  username: { type: String, default: '' }
})

const emit = defineEmits(['gameStarted'])

// ==================== 表单字段 ====================

const baseUrl = ref('')
const apiKey = ref('')
const modelName = ref('gpt-3.5-turbo')
const customPrompt = ref('')
const showApiKey = ref(false)

// ==================== 状态 ====================

const testing = ref(false)
const saving = ref(false)
const starting = ref(false)
const testResult = ref('')
const error = ref('')
const hasConfig = ref(false)
const hasSavedGame = ref(false)

// ==================== 生命周期 ====================

onMounted(async () => {
  // 优先从 localStorage 恢复完整 apiKey（明文），后端只存加密后的
  const localApiKey = readJson(getStorageKey('apiKey'), '')
  if (localApiKey) apiKey.value = localApiKey

  // 恢复 LLM 配置（baseUrl、modelName、customPrompt 从后端取）
  try {
    const config = await getLlmConfig(props.deviceId)
    if (config && config.configured) {
      hasConfig.value = true
      baseUrl.value = config.baseUrl || ''
      modelName.value = config.modelName || 'gpt-3.5-turbo'
      customPrompt.value = config.customPrompt || ''
      if (!localApiKey) {
        apiKey.value = config.apiKey || ''
      }
    }
  } catch {
    // 未配置
  }

  // 检查是否有可继续的存档
  const savedCharId = readJson(getStorageKey('characterId'), null)
  if (savedCharId) {
    try {
      const char = await getLifeCharacterState(savedCharId)
      if (char && char.isAlive) {
        hasSavedGame.value = true
      }
    } catch {
      // 存档失效
    }
  }
})

// ==================== 方法 ====================

function getStorageKey(key) {
  return `lifeSim:${props.deviceId}:${key}`
}

async function handleTest() {
  if (!baseUrl.value.trim()) { error.value = '请输入 Base URL'; return }
  if (!apiKey.value.trim()) { error.value = '请输入 API Key'; return }
  testing.value = true; error.value = ''; testResult.value = ''
  try {
    testResult.value = await testLlmConnection({
      deviceId: props.deviceId,
      baseUrl: baseUrl.value.trim(),
      apiKey: apiKey.value.trim(),
      modelName: modelName.value.trim() || 'gpt-3.5-turbo'
    })
  } catch (e) {
    error.value = e.message || '连接测试失败'
  } finally { testing.value = false }
}

async function handleSave() {
  if (!baseUrl.value.trim()) { error.value = '请输入 Base URL'; return }
  if (!apiKey.value.trim()) { error.value = '请输入 API Key'; return }

  saving.value = true; error.value = ''; testResult.value = ''
  try {
    await saveLlmConfig({
      deviceId: props.deviceId,
      baseUrl: baseUrl.value.trim(),
      apiKey: apiKey.value.trim(),
      modelName: modelName.value.trim() || 'gpt-3.5-turbo',
      customPrompt: customPrompt.value.trim()
    })
    hasConfig.value = true
    // 明文保存 apiKey 到 localStorage，下次回来可直接测试连接
    writeJson(getStorageKey('apiKey'), apiKey.value.trim())
    testResult.value = '✅ 配置已保存'
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally { saving.value = false }
}

async function handleStartGame() {
  if (!props.username) { error.value = '请先登录'; return }

  // 如果还没保存配置，先自动保存
  if (!hasConfig.value && baseUrl.value.trim() && apiKey.value.trim()) {
    await handleSave()
    if (!hasConfig.value) return
  }

  if (!hasConfig.value) { error.value = '请先保存 LLM 配置'; return }

  starting.value = true; error.value = ''
  try {
    const result = await startLifeGame({
      deviceId: props.deviceId,
      name: props.username
    })
    emit('gameStarted', result)
  } catch (e) {
    error.value = e.message || '游戏启动失败'
  } finally { starting.value = false }
}

function handleContinueGame() {
  emit('gameStarted', null) // null = 从后端恢复存档，不创建新游戏
}

async function handleDeleteAll() {
  if (!confirm('确定要删除所有数据吗？\n\n这将清除 LLM 配置、所有角色存档和事件记录。\n此操作不可撤销！')) return
  if (!confirm('再次确认：真的要删除所有数据吗？')) return
  try {
    await deleteLifeUserData(props.deviceId)
    hasConfig.value = false
    hasSavedGame.value = false
    baseUrl.value = ''; apiKey.value = ''; modelName.value = 'gpt-3.5-turbo'
    customPrompt.value = ''
    writeJson(getStorageKey('apiKey'), null)
    writeJson(getStorageKey('characterId'), null)
    error.value = ''
    alert('所有数据已删除')
  } catch (e) {
    error.value = e.message || '删除失败'
  }
}
</script>

<template>
  <div class="game-chat-panel">
    <div class="llm-config-panel">
      <h3 class="game-config-title">⚙️ 人生模拟器 — 配置</h3>
      <p class="llm-config-hint">配置大模型接口 + 设置玩家信息，然后开始你的人生旅程。</p>

      <div class="llm-config-form">
        <!-- ========== LLM 配置区 ========== -->
        <div class="llm-config-section-title">🔌 大模型连接</div>

        <div class="llm-config-field">
          <label class="llm-config-label">Base URL</label>
          <input v-model="baseUrl" type="text" class="llm-config-input"
            placeholder="https://api.openai.com/v1" />
          <span class="llm-config-example">需包含 /v1 路径，如 https://api.openai.com/v1</span>
        </div>

        <div class="llm-config-field">
          <label class="llm-config-label">API Key</label>
          <div class="llm-config-input-wrap">
            <input v-model="apiKey" :type="showApiKey ? 'text' : 'password'"
              class="llm-config-input" placeholder="sk-..." />
            <button type="button" class="llm-config-toggle" @click="showApiKey = !showApiKey">
              {{ showApiKey ? '🙈' : '👁️' }}
            </button>
          </div>
        </div>

        <div class="llm-config-field">
          <label class="llm-config-label">模型名称</label>
          <input v-model="modelName" type="text" class="llm-config-input"
            placeholder="gpt-3.5-turbo" />
        </div>

        <!-- ========== 玩家设置区 ========== -->
        <div class="llm-config-section-title">🎮 玩家设置</div>

        <div class="llm-config-field">
          <label class="llm-config-label">玩家名字</label>
          <div class="llm-config-readonly">{{ username || '（未登录）' }}</div>
          <span class="llm-config-example">使用登录账号名，AI 以此称呼你</span>
        </div>

        <div class="llm-config-field">
          <label class="llm-config-label">🎨 自定义剧情风格（可选）</label>
          <textarea v-model="customPrompt" rows="4"
            class="llm-config-input llm-config-textarea"
            placeholder="示例：&#10;剧情风格：中国古代仙侠世界，充满江湖恩怨&#10;地名：青云山、落日峰、长安城&#10;角色：剑客、道士、商人、官员&#10;禁用词：手机、电脑、汽车"></textarea>
          <span class="llm-config-example">设定世界观、角色类型、禁用词等，AI 生成剧情时会遵循这些规则</span>
        </div>

        <!-- ========== 消息区 ========== -->
        <div v-if="testResult" class="llm-config-result success">{{ testResult }}</div>
        <div v-if="error" class="llm-config-result error">{{ error }}</div>

        <!-- ========== 操作按钮 ========== -->
        <div class="llm-config-actions">
          <button type="button" class="game-tab-btn" :disabled="testing" @click="handleTest">
            {{ testing ? '测试中...' : '🔍 测试连接' }}
          </button>
          <button type="button" class="game-tab-btn" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : '💾 保存配置' }}
          </button>
          <button v-if="hasSavedGame" type="button" class="game-send-btn" @click="handleContinueGame">
            ▶️ 继续游戏
          </button>
          <button type="button" class="game-send-btn" :disabled="starting" @click="handleStartGame">
            {{ starting ? '启动中...' : '🚀 开始游戏' }}
          </button>
        </div>

        <!-- ========== 删除数据 ========== -->
        <div v-if="hasConfig" style="margin-top: 16px; padding-top: 12px; border-top: 1px dashed #e0d5b7;">
          <button type="button" class="llm-config-delete-btn" @click="handleDeleteAll">
            🗑️ 删除所有数据
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.llm-config-panel { padding: 8px 0; }
.llm-config-hint { font-size: 13px; color: #6b5e3e; margin-bottom: 16px; }
.llm-config-form { display: flex; flex-direction: column; gap: 14px; }
.llm-config-section-title {
  font-size: 13px; font-weight: bold; color: #800000;
  padding-bottom: 4px; border-bottom: 1px solid #e0d5b7;
}
.llm-config-field { display: flex; flex-direction: column; gap: 4px; }
.llm-config-label { font-size: 13px; font-weight: bold; color: #503e2c; }
.llm-config-input {
  width: 100%; padding: 8px 10px; border: 1px solid #b9a982; border-radius: 4px;
  font-size: 13px; font-family: inherit; background: #fffef7; color: #333; box-sizing: border-box;
}
.llm-config-input:focus { outline: none; border-color: #800000; box-shadow: 0 0 0 2px rgba(128,0,0,0.1); }
.llm-config-readonly {
  width: 100%; padding: 8px 10px; border: 1px solid #e0d5b7; border-radius: 4px;
  font-size: 14px; font-weight: bold; color: #800000; background: #fdf8e8; box-sizing: border-box;
}
.llm-config-textarea { resize: vertical; min-height: 80px; line-height: 1.6; font-size: 12px; }
.llm-config-input-wrap { display: flex; gap: 0; }
.llm-config-input-wrap .llm-config-input { flex: 1; border-radius: 4px 0 0 4px; }
.llm-config-toggle {
  padding: 8px 10px; border: 1px solid #b9a982; border-left: none;
  border-radius: 0 4px 4px 0; background: #f5f0e0; cursor: pointer; font-size: 14px; line-height: 1;
}
.llm-config-example { font-size: 11px; color: #999; }
.llm-config-actions { display: flex; gap: 10px; margin-top: 4px; flex-wrap: wrap; }
.llm-config-result { padding: 8px 12px; border-radius: 4px; font-size: 13px; }
.llm-config-result.success { background: #e8f5e9; color: #2e7d32; border: 1px solid #a5d6a7; }
.llm-config-result.error { background: #fce4ec; color: #c62828; border: 1px solid #ef9a9a; }
.llm-config-delete-btn {
  padding: 5px 14px; border: 1px solid #e0c0c0; border-radius: 4px;
  background: #fff5f5; color: #c62828; font-size: 12px; font-family: inherit; cursor: pointer;
}
.llm-config-delete-btn:hover { background: #fce4ec; border-color: #c62828; }
</style>
