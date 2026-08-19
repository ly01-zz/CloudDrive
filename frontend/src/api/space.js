import request from './request'

export const applySpace = (data) => request.post('/space/apply', data)
export const getMyApplications = (params) => request.get('/space/my-applications', { params })
