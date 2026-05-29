import request from './request'

// 查询所有音乐（不分页）
export const getMusics = () => {
  return request({
    url: '/musics',
    method: 'get'
  })
}