import request from './request'

// 记录一次访客（每次访问首页时调用）
export const recordVisitor = () => {
  return request({
    url: '/visitor-stats/record',
    method: 'post'
  })
}

// 获取访客总数
export const getTotalVisitors = () => {
  return request({
    url: '/visitor-stats/total',
    method: 'get'
  })
}