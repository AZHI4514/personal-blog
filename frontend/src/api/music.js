import request from './request'

// 查询所有音乐（不分页）
export const getMusics = () => {
  return request({
    url: '/musics',
    method: 'get'
  })
}

export const createMusic = (data) => {
  return request({
    url: '/musics',
    method: 'post',
    data
  })
}

export const deleteMusic = (musicId) => {
  return request({
    url: `/musics/${musicId}`,
    method: 'delete'
  })
}
