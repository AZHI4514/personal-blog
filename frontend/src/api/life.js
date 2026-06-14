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

/** 获取角色状态 */
export function getLifeCharacterState(characterId) {
  return request.get('/api/life/state', { params: { characterId } })
}

/** 获取事件历史 */
export function getLifeEvents(characterId, page = 1, size = 20) {
  return request.get('/api/life/events', { params: { characterId, page, size } })
}

/** 重新开局 */
export function resetLifeGame(characterId) {
  return request.post('/api/life/reset', { characterId })
}

/** 删除角色及事件 */
export function deleteLifeCharacter(characterId) {
  return request.delete('/api/life/character', { params: { characterId } })
}

/** 删除用户全部数据 */
export function deleteLifeUserData(deviceId) {
  return request.delete('/api/life/user/data', { params: { deviceId } })
}
