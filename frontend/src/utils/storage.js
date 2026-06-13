/**
 * 安全获取 localStorage 引用。
 * 在 SSR 环境或隐私模式下可能不可用，此时返回 null 避免抛出异常。
 * @returns {Storage|null} localStorage 对象，不可用时返回 null
 */
function getStorage() {
  try {
    if (typeof window === 'undefined' || !window.localStorage) {
      return null
    }
    return window.localStorage
  } catch (error) {
    return null
  }
}

/**
 * 从 localStorage 读取并解析 JSON 数据。
 * 读取失败、key 不存在或解析异常时返回 fallback 默认值。
 * @param {string} key - 存储的键名
 * @param {*} [fallback=null] - 读取失败时的默认返回值
 * @returns {*} 解析后的 JSON 值
 */
export function readJson(key, fallback = null) {
  const storage = getStorage()
  if (!storage) return fallback

  try {
    const raw = storage.getItem(key)
    if (raw == null) return fallback
    const value = JSON.parse(raw)
    return value == null ? fallback : value
  } catch (error) {
    return fallback
  }
}

/**
 * 从 localStorage 读取纯文本数据（不做 JSON 解析）。
 * 适用于存储非 JSON 格式的简单字符串。
 * @param {string} key - 存储的键名
 * @param {string} [fallback=''] - 读取失败时的默认返回值
 * @returns {string} 读取到的文本内容
 */
export function readText(key, fallback = '') {
  const storage = getStorage()
  if (!storage) return fallback

  try {
    const value = storage.getItem(key)
    return value == null ? fallback : value
  } catch (error) {
    return fallback
  }
}

/**
 * 将数据序列化为 JSON 后写入 localStorage。
 * @param {string} key - 存储的键名
 * @param {*} value - 要存储的值（会被 JSON.stringify 序列化）
 * @returns {boolean} 写入成功返回 true，失败返回 false
 */
export function writeJson(key, value) {
  const storage = getStorage()
  if (!storage) return false

  try {
    storage.setItem(key, JSON.stringify(value))
    return true
  } catch (error) {
    return false
  }
}

/**
 * 从 localStorage 中移除指定 key 的数据。
 * @param {string} key - 要删除的键名
 * @returns {boolean} 删除成功返回 true，失败返回 false
 */
export function removeItem(key) {
  const storage = getStorage()
  if (!storage) return false

  try {
    storage.removeItem(key)
    return true
  } catch (error) {
    return false
  }
}
