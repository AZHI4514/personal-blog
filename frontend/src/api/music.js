import request from './request'

/**
 * 查询所有音乐曲目（不分页，返回完整音乐数组）。
 * 每首音乐包含 title、artist、filePath（音频地址）、coverPath（封面地址）。
 * @returns {Promise<Array>} 音乐列表
 */
export const getMusics = () => {
  return request({
    url: '/musics',
    method: 'get'
  })
}

/**
 * 添加一首音乐到曲目列表。
 * 需要管理员权限。filePath 和 coverPath 应已通过上传接口获得。
 * @param {Object} data - { title, artist, filePath, coverPath }
 * @returns {Promise<Object>} 创建成功的音乐对象
 */
export const createMusic = (data) => {
  return request({
    url: '/musics',
    method: 'post',
    data
  })
}

/**
 * 从曲目列表中删除指定音乐。
 * 需要管理员权限。
 * @param {number|string} musicId - 音乐 ID
 * @returns {Promise<void>}
 */
export const deleteMusic = (musicId) => {
  return request({
    url: `/musics/${musicId}`,
    method: 'delete'
  })
}
