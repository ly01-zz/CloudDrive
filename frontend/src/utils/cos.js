import COS from 'cos-js-sdk-v5'
import { checkSHA, getUploadCredential, confirmUpload } from '@/api/file'
import { ElMessage } from 'element-plus'

// 腾讯云 COS 存储桶配置（与后端 application.yml 保持一致）
const COS_CONFIG = {
  bucket: import.meta.env.VITE_COS_BUCKET || 'mycloud-1250000000',
  region: import.meta.env.VITE_COS_REGION || 'ap-guangzhou',
}

/**
 * 计算文件 SHA-256（用于秒传校验）
 * 注意：一次性读取文件到内存计算，受单文件大小限制约束（默认 100MB）
 * 若环境不支持 Web Crypto（非 https），返回 null，调用方将跳过秒传直接上传
 * @param {File} file
 * @returns {Promise<string|null>}
 */
async function calcFileSha256(file) {
  try {
    if (!crypto?.subtle) return null
    const buffer = await file.arrayBuffer()
    const hash = await crypto.subtle.digest('SHA-256', buffer)
    return Array.from(new Uint8Array(hash))
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('')
  } catch (err) {
    console.error('计算文件 SHA-256 失败：', err)
    return null
  }
}

/**
 * 使用后端颁发的 STS 临时密钥初始化 COS 客户端
 * @param {Object} credential 后端返回的临时密钥信息
 * @returns {COS} COS 客户端实例
 */
function createCosClient(credential) {
  return new COS({
    getAuthorization: (options, callback) => {
      callback({
        TmpSecretId: credential.tmpSecretId,
        TmpSecretKey: credential.tmpSecretKey,
        SecurityToken: credential.sessionToken,
        ExpiredTime: credential.expiredTime,
      })
    },
  })
}

/**
 * 上传单个文件到 COS（含秒传：先计算 SHA-256 检查后端缓存，命中则直接完成）
 * @param {File} file 待上传的文件
 * @param {Number} parentId 父文件夹 ID
 * @param {Function} onProgress 进度回调 (percent 0-100)
 * @returns {Promise<{uploadId: Number|null, quick: boolean}>} uploadId 用于失败清理；quick=true 表示秒传成功
 */
export async function uploadFileToCos(file, parentId, onProgress) {
  // 0. 计算 SHA-256 并尝试秒传
  const sha = await calcFileSha256(file)
  if (sha) {
    const quickRes = await checkSHA({
      sha, // 注意：Jackson 从 getSHA() 派生的属性名是小写 sha，传大写 SHA 无法反序列化
      fileSize: file.size,
      parentId: parentId || 0,
      fileName: file.name,
      mimeType: file.type || undefined,
    })
    if (quickRes.data?.quickUpload) {
      onProgress?.(100)
      return { uploadId: null, quick: true }
    }
  }

  // 1. 从后端获取上传凭证（STS 临时密钥 + cosKey）
  const res = await getUploadCredential({
    fileName: file.name,
    fileSize: file.size,
    parentId: parentId || 0,
    sha, // 上传完成后后端写入秒传缓存
  })
  const credential = res.data

  // 2. 初始化 COS 客户端并直传
  const cos = createCosClient(credential)
  const uploadId = Number(credential.uploadId)

  // 请求开始时先给一个初始进度，提供即时视觉反馈
  onProgress?.(1)

  await new Promise((resolve, reject) => {
    cos.putObject(
      {
        Bucket: COS_CONFIG.bucket,
        Region: COS_CONFIG.region,
        Key: credential.cosKey,
        Body: file,
        onProgress: (progressData) => {
          // progressData.percent 范围 0~1；钳制到 99，最后 1% 留给"上传完成确认"
          const percent = Math.min(99, Math.round((progressData.percent || 0) * 100))
          onProgress?.(percent)
        },
      },
      (err) => {
        if (err) {
          // 附带 uploadId，便于调用方在上传失败后清理后端遗留记录
          const e = new Error(err.message || '上传失败')
          e.uploadId = uploadId
          reject(e)
        } else {
          resolve()
        }
      }
    )
  })

  // 3. 通知后端上传完成
  await confirmUpload(uploadId)
  return { uploadId, quick: false }
}
