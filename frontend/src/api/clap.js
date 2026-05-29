import request from './request'

export function sendClap() {
  return request({
    url: '/clap',
    method: 'post'
  })
}