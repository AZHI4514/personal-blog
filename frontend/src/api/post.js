import request from './request'

// 发布帖子（顶级帖子或回复）
export const createPost = (data) => {
  return request({
    url: '/posts',
    method: 'post',
    data
  })
}

// 查询所有帖子
export const getPosts = () => {
  return request({
    url: '/posts',
    method: 'get'
  })
}

// 删除帖子
export const deletePost = (postId, deleteKey = null) => {
  const data = deleteKey ? { deleteKey } : {}
  return request({
    url: `/posts/${postId}`,
    method: 'delete',
    data
  })
}

// 更新帖子（需要验证删除密钥）
export const updatePost = (postId, data) => {
  return request({
    url: `/posts/${postId}`,
    method: 'put',
    data
  })
}