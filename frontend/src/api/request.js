import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 请求拦截器：自动附加 Token
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回格式：{ code, msg, data }
    if (res.code !== 200 && res.code !== 10001) {
      // 月度流量额度不足：用弹窗友好提示（toast 容易被忽略，且无法完整展示剩余额度）
      // 后端相关报错：UserFileServiceImpl「本月下载流量已用完…」、ShareServiceImpl「您的月度下载流量不足…」
      if (res.msg && res.msg.includes('月') && res.msg.includes('流量')) {
        ElMessageBox.alert(
          `${res.msg}。本月额度用完后将无法继续下载，如需更多流量请联系管理员`,
          '本月流量额度已达上限',
          { type: 'warning', confirmButtonText: '知道了' }
        )
      } else {
        ElMessage.error(res.msg || '请求失败')
      }
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  (error) => {
    const { response } = error
    if (response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
    } else {
      // 优先展示后端返回的业务消息，避免 "Request failed with status code 500" 这类技术文案
      const msg = response?.data?.msg || error.message || '网络错误'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default request
