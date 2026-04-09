/**
 * 装备操作 AHP API 封装
 */
import request from '@/utils/request'

export function getEquipmentAhpMeta(type) {
  return request({
    url: '/equipment/ahp/meta',
    method: 'get',
    params: { type }
  })
}

export function calculateEquipmentAhp(payload) {
  return request({
    url: '/equipment/ahp/calculate',
    method: 'post',
    data: payload
  })
}

export function getEquipmentAhpScores(expertId) {
  return request({
    url: '/equipment/ahp/scores',
    method: 'get',
    params: { expertId }
  })
}

export function saveEquipmentAhpScores(payload) {
  return request({
    url: '/equipment/ahp/scores',
    method: 'post',
    data: payload
  })
}

export function simulateEquipmentAhpScores(payload) {
  return request({
    url: '/equipment/ahp/scores/simulate',
    method: 'post',
    data: payload
  })
}
