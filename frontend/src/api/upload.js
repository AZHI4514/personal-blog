import request from './request'

// 上传图片（用于帖子附件、画廊等）
export const uploadImageFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/uploads/images',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const uploadMusicFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/uploads/musics',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
