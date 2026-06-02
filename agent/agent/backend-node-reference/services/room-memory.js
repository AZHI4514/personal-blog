const crypto = require('crypto');

const VECTOR_SIZE = 96;
const store = new Map();

function hashString(value) {
  let hash = 2166136261;
  for (let i = 0; i < value.length; i += 1) {
    hash ^= value.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

function tokenize(text) {
  const value = String(text || '').toLowerCase();
  const words = value.match(/[a-z0-9_]+|[\u4e00-\u9fff]/g) || [];
  const grams = [];
  for (let i = 0; i < words.length - 1; i += 1) grams.push(`${words[i]}${words[i + 1]}`);
  return words.concat(grams);
}

function createEmbedding(text) {
  const vector = Array(VECTOR_SIZE).fill(0);
  tokenize(text).forEach((token) => {
    const hash = hashString(token);
    const slot = hash % VECTOR_SIZE;
    vector[slot] += (hash & 1) ? 1 : -1;
  });
  const norm = Math.sqrt(vector.reduce((sum, value) => sum + value * value, 0)) || 1;
  return vector.map((value) => Number((value / norm).toFixed(6)));
}

function similarity(a, b) {
  if (!Array.isArray(a) || !Array.isArray(b)) return 0;
  let score = 0;
  for (let i = 0; i < Math.min(a.length, b.length); i += 1) score += Number(a[i] || 0) * Number(b[i] || 0);
  return score;
}

function inferMemoryType(text) {
  const source = String(text || '');
  if (/(我叫|叫我|我的名字|我是)/.test(source)) return 'profile';
  if (/(喜欢|偏好|希望|不要|风格|主题)/.test(source)) return 'preference';
  if (/(项目|网站|功能|计划|开发|部署)/.test(source)) return 'project';
  if (/(上次|刚才|昨天|今天|继续|完成|报错)/.test(source)) return 'episodic';
  return 'conversation';
}

function estimateImportance(text) {
  const source = String(text || '');
  let score = 0.42;
  if (/(记住|以后|喜欢|偏好|名字|重要|不要)/.test(source)) score += 0.28;
  if (source.length > 120) score += 0.08;
  return Math.min(1, Number(score.toFixed(2)));
}

function userBucket(userId) {
  if (!store.has(userId)) store.set(userId, []);
  return store.get(userId);
}

function buildMemoryCandidate(payload = {}) {
  const content = String(payload.content || `用户：${payload.userMessage || ''}\n八千代：${payload.assistantReply || ''}`).trim();
  if (content.length < 12) return null;
  const summary = String(payload.summary || content).slice(0, 500);
  const type = String(payload.type || inferMemoryType(content));
  return {
    id: crypto.randomUUID(),
    type,
    summary,
    content: content.slice(0, 4000),
    importance: estimateImportance(content),
    embedding: createEmbedding(`${summary}\n${content}`),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
}

async function recordMemory(userId, payload = {}) {
  const candidate = buildMemoryCandidate(payload);
  if (!candidate) return null;
  userBucket(userId).unshift(candidate);
  return { action: 'created', memory: candidate };
}

function searchMemories(userId, query, limit = 5) {
  const bucket = userBucket(userId);
  const vector = createEmbedding(query);
  return bucket
    .map((item) => ({
      ...item,
      score: similarity(vector, item.embedding) + Number(item.importance || 0) * 0.2
    }))
    .sort((a, b) => b.score - a.score)
    .slice(0, Math.max(1, Math.min(20, Number(limit) || 5)));
}

function listMemories(userId, { limit = 50, type = '' } = {}) {
  const bucket = userBucket(userId);
  const normalizedType = String(type || '').trim();
  return bucket
    .filter((item) => !normalizedType || item.type === normalizedType)
    .slice(0, Math.max(1, Math.min(200, Number(limit) || 50)));
}

function updateMemory(userId, id, payload = {}) {
  const bucket = userBucket(userId);
  const index = bucket.findIndex((item) => item.id === id);
  if (index < 0) return null;
  const current = bucket[index];
  const next = {
    ...current,
    type: String(payload.type || current.type),
    summary: String(payload.summary || current.summary),
    content: String(payload.content || current.content),
    updatedAt: new Date().toISOString()
  };
  next.embedding = createEmbedding(`${next.summary}\n${next.content}`);
  bucket[index] = next;
  return next;
}

function deleteMemory(userId, id) {
  const bucket = userBucket(userId);
  const before = bucket.length;
  store.set(userId, bucket.filter((item) => item.id !== id));
  return before - store.get(userId).length;
}

module.exports = {
  createEmbedding,
  similarity,
  recordMemory,
  searchMemories,
  listMemories,
  updateMemory,
  deleteMemory
};

