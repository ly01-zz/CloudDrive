import request from './request'

export const listFiles = (parentId = 0) =>
  request.get('/file/list', { params: { parentId } })

/** 秒传检查：后端命中相同 SHA 时直接完成上传（quickUpload=true） */
export const checkSHA = (data) =>
  request.post('/file/check', data)

export const createFolder = (data) =>
  request.post('/file/folder', data)

export const getUploadCredential = (data) =>
  request.post('/file/upload/credential', data)

export const confirmUpload = (fileId) =>
  request.post('/file/upload/callback', null, { params: { fileId } })

export const getDownloadUrl = (fileId) =>
  request.get(`/file/download/${fileId}`)

export const deleteToRecycle = (fileId) =>
  request.delete(`/file/recycle/${fileId}`)

export const deletePermanently = (fileId) =>
  request.delete(`/file/purge/${fileId}`)

export const listRecycle = () =>
  request.get('/file/recycle')

export const restoreFile = (fileId) =>
  request.put(`/file/restore/${fileId}`)

export const cancelUpload = (fileId) =>
  request.delete(`/file/upload/pending/${fileId}`)
