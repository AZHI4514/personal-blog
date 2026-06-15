import request from './request'

/**
 * 人生模拟器 API 模块。
 * request 实例已配置响应拦截器，自动解包 { code, data, message } → data。
 */

/** 保存/更新 LLM 配置 */
export function saveLlmConfig({ deviceId, baseUrl, apiKey, modelName, customPrompt }) {
  return request.post('/api/life/llm/config', { deviceId, baseUrl, apiKey, modelName, customPrompt })
}

/** 获取 LLM 配置（脱敏） */
export function getLlmConfig(deviceId) {
  return request.get('/api/life/llm/config', { params: { deviceId } })
}

/** 测试 LLM 连接 */
export function testLlmConnection({ deviceId, baseUrl, apiKey, modelName }) {
  return request.post('/api/life/llm/test', { deviceId, baseUrl, apiKey, modelName })
}

/** 初始化新角色 */
export function startLifeGame({ deviceId, name }) {
  return request.post('/api/life/start', { deviceId, name })
}

/** 提交用户选择 */
export function makeLifeChoice({ characterId, choiceIndex }) {
  return request.post('/api/life/action', { characterId, choiceIndex })
}

/**
 * 流式开始游戏：AI 剧情通过 SSE 实时推送。
 * @param {Object} params - { deviceId, name }
 * @param {Function} onText - 收到文本块 (chunk: string) => void
 * @returns {Promise<Object>} 完成后的结构化结果 { character, story }
 */
export async function startLifeGameStream({ deviceId, name }, onText) {
  return streamRequest('/api/life/start/stream', { deviceId, name }, onText)
}

/**
 * 流式提交选择：AI 剧情通过 SSE 实时推送。
 * @param {Object} params - { characterId, choiceIndex }
 * @param {Function} onText - 收到文本块 (chunk: string) => void
 * @returns {Promise<Object>} 完成后的结构化结果 { character, story, lastChoice, lastEffects }
 */
export async function makeLifeChoiceStream({ characterId, choiceIndex }, onText) {
  return streamRequest('/api/life/action/stream', { characterId, choiceIndex }, onText)
}

/**
 * 通用 SSE 流式请求。
 * @param {string} url - 端点路径
 * @param {Object} body - JSON 请求体
 * @param {Function} onText - 收到文本块回调
 * @returns {Promise<Object>} 完成后的 result 数据
 */
async function streamRequest(url, body, onText) {
  const baseURL = (import.meta.env.VITE_API_BASE_URL || '/').replace(/\/+$/, '')
  const response = await fetch(`${baseURL}${url}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    credentials: 'include'
  })

  if (!response.ok) {
    let message = `HTTP ${response.status}`
    try {
      const err = await response.json()
      message = err.message || message
    } catch { /* ignore */ }
    throw new Error(message)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let result = null

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      // 刷新 decoder 中残留的字节，然后处理 buffer 中的残余事件
      buffer += decoder.decode()
      if (buffer.trim()) processSSEBuffer(buffer)
      break
    }

    buffer += decoder.decode(value, { stream: true })
    // 以 \n\n 分割完整事件，保留不完整部分
    const idx = buffer.lastIndexOf('\n\n')
    if (idx >= 0) {
      const complete = buffer.substring(0, idx + 2)
      buffer = buffer.substring(idx + 2)
      processSSEBuffer(complete)
    }
  }

  if (!result) throw new Error('未收到有效响应')
  return result

  /** 解析一个或多个完整的 SSE 事件文本 */
  function processSSEBuffer(text) {
    // 统一换行符，按 \n\n 分割事件
    const normalized = text.replace(/\r\n/g, '\n')
    const events = normalized.split('\n\n')
    for (const block of events) {
      if (!block.trim()) continue
      const lines = block.split('\n')
      let eventType = ''
      for (let line of lines) {
        line = line.trim()
        if (line.startsWith('event:')) {
          // 兼容 "event:text" 和 "event: text"
          eventType = line.substring(6).trim()
        } else if (line.startsWith('data:')) {
          // 兼容 "data:{...}" 和 "data: {...}"
          const jsonStr = line.substring(5).trim()
          if (!jsonStr) continue
          try {
            const data = JSON.parse(jsonStr)
            if (eventType === 'text' && onText) {
              onText(data.text || '')
            } else if (eventType === 'result') {
              result = data
            } else if (eventType === 'error') {
              throw new Error(data.message || '生成失败')
            }
          } catch (e) {
            // 业务错误重新抛出；JSON 解析失败则跳过（流式文本碎片）
            if (e.message && !e.message.includes('JSON') && !e.message.includes('Unexpected token') && e.message !== '生成失败') throw e
          }
          eventType = ''
        }
      }
    }
  }
}

/** 获取角色状态 */
export function getLifeCharacterState(characterId) {
  return request.get('/api/life/state', { params: { characterId } })
}

/** 获取事件历史 */
export function getLifeEvents(characterId, page = 1, size = 20) {
  return request.get('/api/life/events', { params: { characterId, page, size } })
}

/** 删除角色及事件 */
export function deleteLifeCharacter(characterId) {
  return request.delete('/api/life/character', { params: { characterId } })
}

/** 删除用户全部数据 */
export function deleteLifeUserData(deviceId) {
  return request.delete('/api/life/user/data', { params: { deviceId } })
}
