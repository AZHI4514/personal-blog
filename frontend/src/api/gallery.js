import request from './request'

// 查询所有画作（不分页）
export const getImages = () => {
  return request({
    url: '/images',
    method: 'get'
  })
}
