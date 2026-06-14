<script setup>
import { ref } from 'vue'
import { useBlogApp } from '@/composables/useBlogApp'
import LifeSimulator from '@/components/LifeSimulator.vue'
import LlmConfigPanel from '@/components/LlmConfigPanel.vue'
import { readJson, writeJson } from '@/utils/storage'

const {
  live2dCanvas,
  live2dError,
  live2dLoading,
  gameActivePanel,
  gameMessageListRef,
  gameSending,
  gameInput,
  gameMessages,
  gameConfigItems,
  clearGameConversationAndMemory,
  sendGameMessage
} = useBlogApp()

// ==================== 设备 ID ====================

function getDeviceId() {
  const stored = readJson('lifeSimDeviceId', null)
  if (stored) return stored
  const id = 'device-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10)
  writeJson('lifeSimDeviceId', id)
  return id
}

const deviceId = ref(getDeviceId())

// ==================== 人生模拟器子面板 ====================
// 'config' → 配置面板（默认入口）
// 'game'   → 游戏界面

const lifeSimSubPanel = ref('config')
const initialGameData = ref(null)

function onGameStarted(data) {
  initialGameData.value = data
  lifeSimSubPanel.value = 'game'
}

function onBackToConfig() {
  initialGameData.value = null
  lifeSimSubPanel.value = 'config'
}
</script>

<template>
  <!-- 游戏角结构 -->
  <div class="game-container">
    <div class="game-toolbar">
      <div class="game-toolbar-left">
        <button type="button" class="game-tab-btn" :class="{ active: gameActivePanel === 'chat' }" @click="gameActivePanel = 'chat'">对话</button>
        <button type="button" class="game-tab-btn" :class="{ active: gameActivePanel === 'config' }" @click="gameActivePanel = 'config'">
          配置信息
        </button>
        <button type="button" class="game-tab-btn" :class="{ active: gameActivePanel === 'life-sim' }" @click="gameActivePanel = 'life-sim'">
          🎮 人生模拟器
        </button>
      </div>
    </div>

    <div class="game-layout">
      <div class="game-main-column">
        <div class="live2d-stage">
          <canvas ref="live2dCanvas" class="live2d-canvas"></canvas>
          <div v-if="live2dLoading && !live2dError" class="live2d-loading">模型加载中...</div>
          <div v-if="live2dError" class="live2d-error">{{ live2dError }}</div>
        </div>
      </div>

      <div class="game-side-column">
        <!-- 聊天面板 -->
        <div v-if="gameActivePanel === 'chat'" class="game-chat-panel">
          <div ref="gameMessageListRef" class="game-message-list">
            <div v-for="message in gameMessages" :key="message.id" class="game-message" :class="`role-${message.role}`">
              <div class="game-message-role">{{ message.role === 'user' ? '你' : message.role === 'assistant' ? 'Agent' : 'Yachiyo' }}</div>
              <div class="game-message-body">
                <p>{{ message.content }}</p>
              </div>
            </div>
          </div>

          <div class="game-chat-form">
            <textarea
              v-model="gameInput"
              rows="4"
              class="game-chat-input"
              placeholder="在这里输入你想聊的内容。"
              @keydown.enter.exact.prevent="sendGameMessage"
            ></textarea>
            <div class="game-chat-actions">
              <button type="button" class="game-link-btn" @click="clearGameConversationAndMemory">清空对话/记忆</button>
              <button type="button" class="game-send-btn" :disabled="gameSending" @click="sendGameMessage">
                {{ gameSending ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 配置面板 -->
        <div v-else-if="gameActivePanel === 'config'" class="game-chat-panel">
          <div class="game-config-panel">
            <h3 class="game-config-title">当前配置信息</h3>
            <div class="game-config-list">
              <p v-for="item in gameConfigItems" :key="item.label" class="game-config-item">
                <span class="game-config-label">{{ item.label }}：</span>
                <span>{{ item.value }}</span>
              </p>
            </div>
          </div>
        </div>

        <!-- 人生模拟器面板 -->
        <div v-else-if="gameActivePanel === 'life-sim'" class="life-sim-panel">
          <LlmConfigPanel
            v-if="lifeSimSubPanel === 'config'"
            :device-id="deviceId"
            @game-started="onGameStarted"
          />
          <LifeSimulator
            v-else
            :device-id="deviceId"
            :initial-game-data="initialGameData"
            @exit="onBackToConfig"
            @no-character="onBackToConfig"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.life-sim-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 复用游戏面板的背景样式 */
.life-sim-panel :deep(.game-chat-panel) {
  background: #fffef7;
  border: 1px solid #b9a982;
  border-radius: 4px;
  padding: 12px;
}

.life-sim-panel :deep(.game-config-title) {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #503e2c;
}

.life-sim-panel :deep(.game-send-btn) {
  padding: 8px 20px;
  border: 2px outset #800000;
  background: #800000;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  font-family: inherit;
}

.life-sim-panel :deep(.game-send-btn:hover:not(:disabled)) {
  background: #a00000;
}

.life-sim-panel :deep(.game-send-btn:disabled) {
  opacity: 0.5;
  cursor: not-allowed;
}

.life-sim-panel :deep(.game-link-btn) {
  background: none;
  border: none;
  color: #800000;
  font-size: 12px;
  cursor: pointer;
  font-family: inherit;
  text-decoration: underline;
}

.life-sim-panel :deep(.game-tab-btn) {
  padding: 5px 12px;
  border: 2px outset #fff;
  background: #d2c29e;
  color: #333;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  border-radius: 4px;
}
</style>
