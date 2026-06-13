import request from './request'

/**
 * 注册新用户。
 * 发送 POST /users/register，请求体包含 username、password、email。
 * 注册成功后后端可能同时完成登录（返回用户对象）。
 * @param {Object} data - { username, password, email }
 * @returns {Promise<Object>} 注册成功后返回的用户对象
 */
export const registerUser = (data) => {
  return request({
    url: '/users/register',
    method: 'post',
    data
  })
}

/**
 * 用户登录。
 * 发送 POST /users/login，请求体包含 username、password。
 * 后端通过 session/cookie 保持登录状态（withCredentials）。
 * @param {Object} data - { username, password }
 * @returns {Promise<Object>} 登录成功返回的用户对象
 */
export const loginUser = (data) => {
  return request({
    url: '/users/login',
    method: 'post',
    data
  })
}

/**
 * 退出登录。
 * 发送 POST /users/logout，后端清除当前 session。
 * @returns {Promise<void>}
 */
export const logoutUser = () => {
  return request({
    url: '/users/logout',
    method: 'post'
  })
}
