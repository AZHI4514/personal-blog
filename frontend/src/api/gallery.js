import request from './request'

/**
 * 查询所有画廊画作（不分页，返回完整图片数组）。
 * 每张图片包含 path（图片地址）和 author（作者名）。
 * @returns {Promise<Array>} 画作列表
 */
export const getImages = () => {
  return request({
    url: '/images',
    method: 'get'
  })
}

/**
 * 添加一张画作到画廊。
 * 需要管理员权限。path 字段应已通过上传接口获得。
 * @param {Object} data - { path: string, author: string }
 * @returns {Promise<Object>} 创建成功的图片对象
 */
export const createImage = (data) => {
  return request({
    url: '/images',
    method: 'post',
    data
  })
}

/**
 * 从画廊中删除指定画作。
 * 需要管理员权限。
 * @param {number|string} imageId - 图片 ID
 * @returns {Promise<void>}
 */
export const deleteImage = (imageId) => {
  return request({
    url: `/images/${imageId}`,
    method: 'delete'
  })
}
