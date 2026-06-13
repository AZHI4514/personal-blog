import request from './request'

/**
 * 发布帖子（顶级帖子或回复）。
 * 发送 POST /posts，请求体包含 username、email、title、content、parentId、imagePath、deleteKey。
 * @param {Object} data - 帖子数据对象
 * @returns {Promise<Object>} 创建成功的帖子对象
 */
export const createPost = (data) => {
  return request({
    url: '/posts',
    method: 'post',
    data
  })
}

/**
 * 查询所有帖子（不分页，返回完整帖子数组）。
 * 每个帖子内含 replies 子数组。
 * @returns {Promise<Array>} 帖子列表
 */
export const getPosts = () => {
  return request({
    url: '/posts',
    method: 'get'
  })
}

/**
 * 删除帖子。
 * 管理员可直接删除；普通用户需提供发帖时设置的 deleteKey（删除钥匙）。
 * @param {number|string} postId - 帖子 ID
 * @param {string|null} [deleteKey=null] - 删除钥匙，可选
 * @returns {Promise<void>}
 */
export const deletePost = (postId, deleteKey = null) => {
  // 仅在提供了删除钥匙时才携带到请求体
  const data = deleteKey ? { deleteKey } : {}
  return request({
    url: `/posts/${postId}`,
    method: 'delete',
    data
  })
}

/**
 * 更新帖子（需要验证删除钥匙）。
 * 发送 PUT /posts/:postId，请求体必须包含正确的 deleteKey。
 * @param {number|string} postId - 帖子 ID
 * @param {Object} data - 更新数据（deleteKey, title, content, imagePath）
 * @returns {Promise<Object>} 更新后的帖子对象
 */
export const updatePost = (postId, data) => {
  return request({
    url: `/posts/${postId}`,
    method: 'put',
    data
  })
}
