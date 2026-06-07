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
