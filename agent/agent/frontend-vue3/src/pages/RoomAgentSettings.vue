<script setup>
import { reactive } from 'vue';
import { cloneKnowledgeEntry, defaultKnowledgeEntries } from '../constants/knowledgeEntries';
import { readJson, writeJson } from '../services/agentStorage';

const llm = reactive({ apiUrl: '', apiKey: '', model: '', useProxy: false, visionMode: 'auto' });
const tts = reactive({ enabled: false, provider: 'openai', apiUrl: '', apiKey: '', model: 'tts-1', voice: 'alloy' });
const memory = reactive({ enabled: true });
const mcp = reactive({ enabled: false, endpoint: '', toolAllowlist: 'understand_image,web_search' });
const knowledge = reactive({
  enabled: true,
  entries: [],
  editingId: null,
  draft: { title: '', content: '', tags: '', enabled: true }
});

loadSettings();

function loadSettings() {
  Object.assign(llm, readJson('roomLLMSettings', {}));
  Object.assign(tts, readJson('roomTTSSettings', {}));
  Object.assign(memory, { enabled: true, ...readJson('roomMemorySettings', {}) });
  Object.assign(mcp, { enabled: false, endpoint: '', toolAllowlist: 'understand_image,web_search', ...readJson('roomMCPSettings', {}) });
  const storedKnowledge = readJson('roomKnowledgeSettings', {});
  knowledge.enabled = storedKnowledge.enabled !== false;
  knowledge.entries = Array.isArray(storedKnowledge.entries) && storedKnowledge.entries.length
    ? storedKnowledge.entries.map(cloneKnowledgeEntry)
    : defaultKnowledgeEntries();
  resetKnowledgeDraft();
}

function saveLLM() {
  writeJson('roomLLMSettings', { ...llm });
}

function saveTTS() {
  writeJson('roomTTSSettings', { ...tts });
}

function saveMemory() {
  writeJson('roomMemorySettings', { enabled: Boolean(memory.enabled) });
}

function saveMCP() {
  writeJson('roomMCPSettings', { ...mcp });
}

function saveKnowledge() {
  knowledge.entries = knowledge.entries.map(cloneKnowledgeEntry).filter((item) => item.title || item.content);
  writeJson('roomKnowledgeSettings', {
    enabled: Boolean(knowledge.enabled),
    entries: knowledge.entries
  });
}

function resetKnowledgeDraft() {
  knowledge.editingId = null;
  knowledge.draft = { title: '', content: '', tags: '', enabled: true };
}

function editKnowledgeEntry(item) {
  knowledge.editingId = item.id;
  knowledge.draft = cloneKnowledgeEntry(item);
}

function saveKnowledgeEntry() {
  const entry = cloneKnowledgeEntry(knowledge.draft);
  if (!entry.title || !entry.content) return;
  const index = knowledge.entries.findIndex((item) => item.id === knowledge.editingId);
  if (index >= 0) knowledge.entries[index] = { ...entry, id: knowledge.editingId };
  else knowledge.entries.unshift(entry);
  resetKnowledgeDraft();
  saveKnowledge();
}

function deleteKnowledgeEntry(item) {
  knowledge.entries = knowledge.entries.filter((entry) => entry.id !== item.id);
  if (knowledge.editingId === item.id) resetKnowledgeDraft();
  saveKnowledge();
}

function resetKnowledgeDefaults() {
  knowledge.enabled = true;
  knowledge.entries = defaultKnowledgeEntries();
  resetKnowledgeDraft();
  saveKnowledge();
}
</script>

<template>
  <section class="room-agent-settings">
    <h1>Room Agent 设置</h1>

    <article class="card">
      <h2>LLM</h2>
      <label>API URL <input v-model="llm.apiUrl" type="text"></label>
      <label>API Key <input v-model="llm.apiKey" type="password"></label>
      <label>Model <input v-model="llm.model" type="text"></label>
      <label>Vision Mode
        <select v-model="llm.visionMode">
          <option value="auto">auto</option>
          <option value="llm">llm</option>
          <option value="mcp">mcp</option>
        </select>
      </label>
      <label><input v-model="llm.useProxy" type="checkbox"> 使用服务端代理</label>
      <button type="button" @click="saveLLM">保存 LLM</button>
    </article>

    <article class="card">
      <h2>TTS</h2>
      <label><input v-model="tts.enabled" type="checkbox"> 启用 TTS</label>
      <label>Provider <input v-model="tts.provider" type="text"></label>
      <label>API URL <input v-model="tts.apiUrl" type="text"></label>
      <label>API Key <input v-model="tts.apiKey" type="password"></label>
      <label>Model <input v-model="tts.model" type="text"></label>
      <label>Voice <input v-model="tts.voice" type="text"></label>
      <button type="button" @click="saveTTS">保存 TTS</button>
    </article>

    <article class="card">
      <h2>长期记忆</h2>
      <label><input v-model="memory.enabled" type="checkbox"> 启用长期记忆</label>
      <button type="button" @click="saveMemory">保存记忆设置</button>
    </article>

    <article class="card">
      <h2>MCP</h2>
      <label><input v-model="mcp.enabled" type="checkbox"> 启用 MCP</label>
      <label>Endpoint <input v-model="mcp.endpoint" type="text"></label>
      <label>Allowlist <input v-model="mcp.toolAllowlist" type="text"></label>
      <button type="button" @click="saveMCP">保存 MCP</button>
    </article>

    <article class="card">
      <h2>角色知识库</h2>
      <label><input v-model="knowledge.enabled" type="checkbox"> 启用知识库注入</label>
      <form @submit.prevent="saveKnowledgeEntry">
        <label>标题 <input v-model="knowledge.draft.title" type="text"></label>
        <label>内容 <textarea v-model="knowledge.draft.content"></textarea></label>
        <label>标签 <input v-model="knowledge.draft.tags" type="text"></label>
        <label><input v-model="knowledge.draft.enabled" type="checkbox"> 启用该条</label>
        <button type="submit">{{ knowledge.editingId ? '保存条目' : '添加条目' }}</button>
        <button type="button" @click="resetKnowledgeDraft">清空表单</button>
        <button type="button" @click="saveKnowledge">保存知识库</button>
        <button type="button" @click="resetKnowledgeDefaults">恢复默认</button>
      </form>

      <div class="knowledge-list">
        <article v-for="item in knowledge.entries" :key="item.id" class="knowledge-item">
          <strong>{{ item.title }}</strong>
          <p>{{ item.content }}</p>
          <small>{{ item.tags }}</small>
          <div>
            <button type="button" @click="editKnowledgeEntry(item)">编辑</button>
            <button type="button" @click="deleteKnowledgeEntry(item)">删除</button>
          </div>
        </article>
      </div>
    </article>
  </section>
</template>

