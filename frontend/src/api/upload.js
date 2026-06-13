import request from './request'

/**
 * 上传图片文件（用于帖子附件、画廊插图等）。
 * 使用 FormData 以 multipart/form-data 格式发送。
 * @param {File} file - 用户选择的图片文件
 * @returns {Promise<{ filePath: string }>} 返回上传后的访问路径
 */
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

/**
 * 上传音乐文件（MP3/WAV/OGG/FLAC/M4A 等格式）。
 * 使用 FormData 以 multipart/form-data 格式发送。
 * @param {File} file - 用户选择的音乐文件
 * @returns {Promise<{ filePath: string }>} 返回上传后的访问路径
 */
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
