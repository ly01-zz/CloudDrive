import request from './request'

export const login = (data) => request.post('/user/login', data)
export const register = (data) => request.post('/user/register', data)
export const updateProfile = (data) => request.patch('/user/update', data)
export const updatePassword = (data) => request.patch('/user/updatePassword', data)
export const getUserInfo = () => request.get('/user/info')
