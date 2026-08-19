import request from './request'

export const createShare = (data) =>
  request.post('/share/create', data)

export const getShareInfo = (shareCode) =>
  request.get(`/share/info/${shareCode}`)

export const downloadShare = (shareCode, extractCode) =>
  request.get(`/share/download/${shareCode}`, { params: { extractCode } })

export const cancelShare = (shareId) =>
  request.delete(`/share/${shareId}`)

export const listMyShares = () =>
  request.get('/share/list')
