import request from './request'

export const getAllUsers = () => request.get('/admin/user/all')
export const disableUser = (userId) => request.put(`/admin/user/${userId}/disable`)
export const enableUser = (userId) => request.put(`/admin/user/${userId}/enable`)
export const resetUserTraffic = (userId, data) =>
  request.put(`/admin/user/${userId}/reset-traffic`, data)
export const updateUserQuota = (userId, data) =>
  request.put(`/admin/user/${userId}/quota`, data)

export const getAllApplications = (params) =>
  request.get('/admin/space/application', { params })
export const approveApplication = (applicationId, data) =>
  request.put(`/admin/space/approve/${applicationId}`, data)

// 分享管理
export const listAllShares = () => request.get('/admin/share/list')
export const forceCancelShare = (shareId) =>
  request.put(`/admin/share/${shareId}/cancel`)

// 数据统计
export const getDashboardStats = () => request.get('/admin/stats/dashboard')

// 系统配置管理
export const listAllConfigs = () => request.get('/admin/config/list')
export const updateConfig = (configKey, data) =>
  request.put(`/admin/config/${configKey}`, data)
export const addConfig = (data) => request.post('/admin/config', data)
export const deleteConfig = (configKey) =>
  request.delete(`/admin/config/${configKey}`)

// 全局文件治理
export const listAdminFiles = (params) =>
  request.get('/admin/file/list', { params })
export const purgeAdminFile = (fileId) =>
  request.delete(`/admin/file/${fileId}/purge`)
export const restoreAdminFile = (fileId) =>
  request.put(`/admin/file/${fileId}/restore`)

// 操作日志
export const listAdminLogs = (limit = 200) =>
  request.get('/admin/log/list', { params: { limit } })

// 公告管理
export const publishAnnouncement = (data) => request.post('/admin/announcement', data)
export const listAnnouncements = () => request.get('/admin/announcement/list')
export const offlineAnnouncement = (id) =>
  request.put(`/admin/announcement/${id}/offline`)
export const deleteAnnouncement = (id) =>
  request.delete(`/admin/announcement/${id}`)

// 用户端：最新公告
export const getLatestAnnouncement = () => request.get('/announcement/latest')
