import request from './request'

// 查询所有画作（不分页）
export const getImages = () => {
  return request({
    url: '/images',
    method: 'get'
  })
}

export const createImage = (data) => {
  return request({
    url: '/images',
    method: 'post',
    data
  })
}

export const deleteImage = (imageId) => {
  return request({
    url: `/images/${imageId}`,
    method: 'delete'
  })
}
