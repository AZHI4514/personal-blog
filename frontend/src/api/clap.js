import request from './request'

/**
 * 发送"拍手"应援（Web 拍手功能）。
 * 用户点击首页拍手按钮时调用，向后端发送一条应援消息。
 * 无需登录，匿名用户也可以使用。
 * @returns {Promise<void>}
 */
export function sendClap() {
  return request({
    url: '/clap',
    method: 'post'
  })
}