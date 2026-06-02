import { nextTick, ref } from 'vue';
import { defaultKnowledgeEntries } from '../constants/knowledgeEntries';
import { readJson, writeJson } from '../services/agentStorage';

function uid() {
  return `msg-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function compactText(value, limit = 240) {
  return String(value || '').replace(/\s+/g, ' ').trim().slice(0, limit);
}

function readKnowledgeContext(message) {
  const settings = readJson('roomKnowledgeSettings', null);
  if (settings?.enabled === false) return '';
  const defaults = defaultKnowledgeEntries();
  const custom = Array.isArray(settings?.entries) ? settings.entries : [];
  const knownIds = new Set(defaults.map((item) => item.id));
  const entries = [
    ...defaults,
    ...custom.filter((item) => item?.id && !knownIds.has(item.id))
  ];
  const query = String(message || '').toLowerCase();
  const tokens = query.split(/[\s,，。！？、；:：()[\]]+/).filter((item) => item.length >= 2).slice(0, 12);
  const picked = entries
    .filter((item) => item && item.enabled !== false)
    .map((item, index) => {
      const haystack = `${item.title || ''} ${item.tags || ''} ${item.content || ''}`.toLowerCase();
      const score = tokens.reduce((sum, token) => sum + (haystack.includes(token) ? 1 : 0), 0) + (index < 5 ? 2 : 0);
      return { ...item, score, index };
    })
    .sort((a, b) => (b.score - a.score) || (a.index - b.index))
    .slice(0, 8);

  if (!picked.length) return '';
  return [
    '角色知识库：',
    ...picked.map((item, index) => `${index + 1}. ${compactText(`${item.title}：${item.content}`, 260)}`)
  ].join('\n');
}

async function fetchRelevantMemories(message) {
  const memorySettings = readJson('roomMemorySettings', { enabled: true });
  if (memorySettings.enabled === false) return [];
  const params = new URLSearchParams({ q: String(message || '').trim(), limit: '5' });
  const response = await fetch(`/api/room-agent/memory?${params}`, {
    credentials: 'include',
    cache: 'no-store'
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result.success) return [];
  return Array.isArray(result.data) ? result.data : [];
}

function memoryContext(memories) {
  if (!memories.length) return '';
  return [
    '与当前用户相关的长期记忆：',
    ...memories.map((item, index) => `${index + 1}. [${item.type || 'memory'}] ${compactText(item.summary || item.content || '', 200)}`)
  ].join('\n');
}

async function callMcpTool(name, args = {}) {
  const settings = readJson('roomMCPSettings', {});
  if (!settings.enabled) return '';
  const response = await fetch('/api/room-agent/mcp/call', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ name, args })
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result.success) throw new Error(result.message || `MCP ${response.status}`);
  return String(result.data?.text || '');
}

function shouldUseWebSearch(message) {
  return /(搜索|查一下|最新|官网|新闻|search|web)/i.test(String(message || ''));
}

async function buildRoomContext(message, image, llmSettings) {
  const context = [readKnowledgeContext(message)];
  const memories = await fetchRelevantMemories(message).catch(() => []);
  const memoryText = memoryContext(memories);
  if (memoryText) context.push(memoryText);

  if (image && (llmSettings.visionMode === 'mcp' || llmSettings.visionMode === 'auto')) {
    const imageText = await callMcpTool('understand_image', {
      imageData: image.dataUrl,
      prompt: message || '请描述这张图片，并指出和当前对话相关的内容。'
    }).catch(() => '');
    if (imageText) context.push(`MCP 图片理解结果：\n${imageText}`);
  }

  if (!image && shouldUseWebSearch(message)) {
    const searchText = await callMcpTool('web_search', { query: message }).catch(() => '');
    if (searchText) context.push(`MCP 搜索结果：\n${searchText}`);
  }

  return context.filter(Boolean).join('\n\n');
}

function roomSystemPrompt() {
  return [
    '你是八千代，虚拟房间中的管理者、引导者和陪伴者。',
    '你要保持稳定人格、温柔语气和明确边界。',
    '面对情绪议题先看见感受；面对项目和技术问题可清晰拆解，但不要变成命令式口吻。',
    '只返回 JSON：{"reply":"...", "live2d":{"emotion":"happy","expression":"smile","motion":"none","intensity":0.6,"durationMs":5000}}'
  ].join('\n');
}

function fallbackReply(message, image) {
  if (image) return '我收到图片了。如果当前模型还不能直接看图，我会先结合图片理解结果继续陪你聊。';
  return message ? `我听见了：${message}` : '我在这里。';
}

function extractReply(data) {
  return data?.reply
    || data?.output_text
    || data?.choices?.[0]?.message?.content
    || '';
}

async function remember(userMessage, assistantReply) {
  const memorySettings = readJson('roomMemorySettings', { enabled: true });
  if (memorySettings.enabled === false) return;
  await fetch('/api/room-agent/memory', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ userMessage, assistantReply })
  });
}

export function useAgentRoomChat() {
  const messages = ref([]);
  const input = ref('');
  const sending = ref(false);
  const imageAttachment = ref(null);
  const messageListRef = ref(null);

  function addMessage(role, content, options = {}) {
    messages.value.push({
      id: uid(),
      role,
      content: String(content || ''),
      image: options.image || null,
      createdAt: Date.now()
    });
    nextTick(() => {
      if (messageListRef.value) messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    });
  }

  async function attachImage(file) {
    if (!file || !/^image\//.test(file.type)) return;
    const reader = new FileReader();
    imageAttachment.value = await new Promise((resolve, reject) => {
      reader.onload = () => resolve({
        name: file.name || 'image',
        type: file.type,
        size: file.size,
        dataUrl: String(reader.result || '')
      });
      reader.onerror = () => reject(reader.error || new Error('image read failed'));
      reader.readAsDataURL(file);
    });
  }

  function clearImage() {
    imageAttachment.value = null;
  }

  async function send() {
    const message = input.value.trim();
    const image = imageAttachment.value;
    if (!message && !image) return;
    addMessage('user', message || '请看这张图片。', { image });
    input.value = '';
    imageAttachment.value = null;
    sending.value = true;

    try {
      const settings = readJson('roomLLMSettings', {});
      const history = readJson('roomChatHistory', []).slice(-12);
      const roomContext = await buildRoomContext(message, image, settings);
      const systemPrompt = [roomSystemPrompt(), roomContext].filter(Boolean).join('\n\n');

      let result;
      if (settings.apiUrl && settings.apiKey) {
        const response = await fetch(settings.useProxy ? '/api/chat' : settings.apiUrl, {
          method: 'POST',
          headers: settings.useProxy
            ? { 'Content-Type': 'application/json' }
            : { 'Content-Type': 'application/json', Authorization: `Bearer ${settings.apiKey}` },
          body: JSON.stringify(settings.useProxy
            ? {
                message,
                image,
                conversation: history,
                apiKey: settings.apiKey,
                apiUrl: settings.apiUrl,
                model: settings.model,
                systemPrompt
              }
            : {
                model: settings.model || 'gpt-4o-mini',
                messages: [
                  { role: 'system', content: systemPrompt },
                  ...history.map((item) => ({ role: item.role, content: item.content })),
                  { role: 'user', content: image ? [{ type: 'text', text: message || '请描述这张图片。' }, { type: 'image_url', image_url: { url: image.dataUrl } }] : message }
                ]
              })
        });
        if (!response.ok) throw new Error(`LLM ${response.status}`);
        result = await response.json();
      } else {
        result = { reply: fallbackReply(message, image) };
      }

      const reply = extractReply(result) || fallbackReply(message, image);
      addMessage('assistant', reply);
      writeJson('roomChatHistory', [
        ...history,
        { role: 'user', content: message || '[image]' },
        { role: 'assistant', content: reply }
      ].slice(-24));
      remember(message || '[image]', reply).catch(() => {});
    } catch (error) {
      addMessage('system', `发送失败：${error.message}`);
    } finally {
      sending.value = false;
    }
  }

  return {
    messages,
    input,
    sending,
    imageAttachment,
    messageListRef,
    addMessage,
    attachImage,
    clearImage,
    send
  };
}

