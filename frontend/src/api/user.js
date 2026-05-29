import request from './request'

export const registerUser = (data) => {
  return request({
    url: '/users/register',
    method: 'post',
    data
  })
}

export const loginUser = (data) => {
  return request({
    url: '/users/login',
    method: 'post',
    data
  })
}

export const logoutUser = () => {
  return request({
    url: '/users/logout',
    method: 'post'
  })
}
