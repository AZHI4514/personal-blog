import request from './request'

/**
 * 记录一次访客访问（每次进入首页时调用）。
 * 后端根据 IP / session 去重，避免重复计数。
 * @returns {Promise<void>}
 */
export const recordVisitor = () => {
  return request({
    url: '/visitor-stats/record',
    method: 'post'
  })
}

/**
 * 获取累计访客总数，用于首页计数器展示。
 * @returns {Promise<{ totalVisitors: number }>} 访客统计对象
 */
export const getTotalVisitors = () => {
  return request({
    url: '/visitor-stats/total',
    method: 'get'
  })
}