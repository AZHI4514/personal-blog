<script setup>
import { useBlogApp } from '@/composables/useBlogApp'

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
</script>

<template>
  <div class="game-container">
          <div class="game-toolbar">
            <div class="game-toolbar-left">
              <button type="button" class="game-tab-btn" :class="{ active: gameActivePanel === 'chat' }" @click="gameActivePanel = 'chat'">对话</button>
              <button type="button" class="game-tab-btn" :class="{ active: gameActivePanel === 'config' }" @click="gameActivePanel = 'config'">
                配置信息
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
  
              <div v-else class="game-chat-panel">
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
            </div>
          </div>
        </div>
</template>
