package cn.bvovd.clouddrive.service.impl;

import cn.bvovd.clouddrive.config.CosProperties;
import cn.bvovd.clouddrive.dto.CreateFolderRequest;
import cn.bvovd.clouddrive.dto.UploadCredentialRequest;
import cn.bvovd.clouddrive.entity.DownloadLog;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.entity.UserFile;
import cn.bvovd.clouddrive.exception.BusinessException;
import cn.bvovd.clouddrive.mapper.DownloadLogMapper;
import cn.bvovd.clouddrive.mapper.UserFileMapper;
import cn.bvovd.clouddrive.mapper.UserMapper;
import cn.bvovd.clouddrive.service.UserFileService;
import cn.bvovd.clouddrive.vo.DownloadUrlVo;
import cn.bvovd.clouddrive.vo.UploadCredentialVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.tencent.cloud.CosStsClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFileServiceImpl extends ServiceImpl<UserFileMapper,UserFile> implements UserFileService {

    private final UserFileMapper userFileMapper;
    private final UserMapper userMapper;
    private final CosProperties cosProperties;
    private final DownloadLogMapper downloadLogMapper;
    private final COSClient cosClient;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadCredentialVo getUploadCredential(Long userId, UploadCredentialRequest request) {
        String fileName = request.getFileName();
        Long fileSize = request.getFileSize();
        Long parentId = request.getParentId();

        // ========== 1. 校验文件大小是否超过系统限制 ==========
        if (fileSize > cosProperties.getMaxFileSize()) {
            throw new BusinessException("文件过大，最大支持 " + cosProperties.getMaxFileSize() / 1024 / 1024 + "MB");
        }

        // ========== 2. 检查用户空间是否充足（带行锁） ==========
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        long remainingSpace = user.getTotalSpace() - user.getUsedSpace();
        if (remainingSpace < fileSize) {
            throw new BusinessException("云盘空间不足，剩余 " + remainingSpace / 1024 / 1024 + "MB，需要 " + fileSize / 1024 / 1024 + "MB");
        }

        // ========== 3. 校验父文件夹是否存在 ==========
        if (parentId != null && parentId != 0) {
            UserFile parent = this.getById(parentId);
            if (parent == null) {
                throw new BusinessException("父文件夹不存在");
            }
            if (!parent.getUserId().equals(userId)) {
                throw new BusinessException("无权操作此文件夹");
            }
            if (!parent.getIsFolder()) {
                throw new BusinessException("目标路径不是文件夹");
            }
        }

        // ========== 4. 检查同名文件（防止覆盖） ==========
        LambdaQueryWrapper<UserFile> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(UserFile::getUserId, userId)
                .eq(UserFile::getParentId, parentId)
                .eq(UserFile::getName, fileName)
                .isNull(UserFile::getDeletedAt);
        if (this.count(nameWrapper) > 0) {
            throw new BusinessException("该目录下已存在同名文件，请重命名后再上传");
        }

        // ========== 5. 预扣空间（立即扣减） ==========
        user.setUsedSpace(user.getUsedSpace() + fileSize);
        userMapper.updateById(user);

        // ========== 6. 生成 COS 存储路径 ==========
        // 格式：user-files/{userId}/{timestamp}_{uuid}_{原始文件名}
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String cosKey = String.format("user-files/%d/%s_%s_%s", userId, timestamp, uuid, fileName);

        // ========== 7. 创建文件记录（状态：上传中） ==========
        UserFile tempFile = new UserFile();
        tempFile.setUserId(userId);
        tempFile.setParentId(parentId);
        tempFile.setName(fileName);
        tempFile.setFileSize(fileSize);
        tempFile.setIsFolder(false);
        tempFile.setUploadStatus(0); // 0-上传中
        tempFile.setStoragePath(cosKey);
        this.save(tempFile);

        // ========== 8. 调用腾讯云 STS SDK 生成临时密钥 ==========
        TreeMap<String, Object> config = buildStsConfig(cosKey, fileSize);
        JSONObject response;
        try {
            response = CosStsClient.getCredential(config);
            log.info("临时密钥生成成功，用户：{}，文件：{}，cosKey：{}", userId, fileName, cosKey);
        } catch (Exception e) {
            log.error("生成临时密钥失败，bucket：{}，region：{}，cosKey：{}",
                    cosProperties.getBucketName(), cosProperties.getRegion(), cosKey, e);
            // 提取腾讯云返回的错误信息（如 InvalidParameter.GrantOtherResource），便于前端/日志排查
            String detail = extractCosError(e);
            if (detail != null) {
                throw new BusinessException("获取上传凭证失败：" + detail);
            }
            throw new BusinessException("获取上传凭证失败，请检查腾讯云 COS 配置");
        }

        // ========== 9. 组装返回 VO ==========
        JSONObject credentials = response.getJSONObject("credentials");
        UploadCredentialVo vo = new UploadCredentialVo();
        vo.setCosKey(cosKey);
        vo.setUploadId(String.valueOf(tempFile.getId()));
        vo.setTmpSecretId(credentials.getString("tmpSecretId"));
        vo.setTmpSecretKey(credentials.getString("tmpSecretKey"));
        vo.setSessionToken(credentials.getString("sessionToken"));
        vo.setStartTime(response.getLong("startTime"));
        vo.setExpiredTime(response.getLong("expiredTime"));

        return vo;
    }

    /**
     * 生成下载预签名 URL，并扣减流量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DownloadUrlVo getDownloadUrl(Long userId, Long fileId) {
        // 1. 查询文件信息
        UserFile file = this.getById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        // 校验文件是否属于当前用户
        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("无权下载此文件");
        }
        // 校验文件是否在回收站
        if (file.getDeletedAt() != null) {
            throw new BusinessException("文件已被删除或移入回收站");
        }
        // 校验是否是文件夹
        if (file.getIsFolder()) {
            throw new BusinessException("文件夹不支持下载");
        }

        Long fileSize = file.getFileSize();
        String cosKey = file.getStoragePath();
        if (cosKey == null || cosKey.isEmpty()) {
            throw new BusinessException("文件存储路径异常，请联系管理员");
        }

        // 2. 查询用户信息（加行锁，防止并发流量超限）
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 3. 检查流量是否跨月，若跨月则重置
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resetTime = user.getTrafficResetTime();
        if (resetTime == null || resetTime.getMonthValue() != now.getMonthValue()
                || resetTime.getYear() != now.getYear()) {
            // 重置流量
            user.setUsedDownloadTraffic(0L);
            user.setTrafficResetTime(now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
        }

        // 4. 检查月度流量是否充足
        long monthlyLimit = user.getMonthlyDownloadLimit();
        long usedTraffic = user.getUsedDownloadTraffic();
        if (usedTraffic + fileSize > monthlyLimit) {
            throw new BusinessException("本月下载流量已用完，剩余 "
                    + (monthlyLimit - usedTraffic) / 1024 / 1024 + "MB，本次需要 "
                    + fileSize / 1024 / 1024 + "MB");
        }

        // 5. 扣减流量
        user.setUsedDownloadTraffic(usedTraffic + fileSize);
        userMapper.updateById(user);

        // 6. 记录下载日志
        DownloadLog log = new DownloadLog();
        log.setUserId(userId);
        log.setFileId(fileId);
        log.setDownloadSize(fileSize);
        log.setIpAddress(getClientIp()); // 获取客户端IP（需在Service中传入或从RequestContextHolder获取）
        log.setUserAgent(getUserAgent());
        downloadLogMapper.insert(log);

        // 7. 增加文件下载次数（非必须，可做统计）
        file.setDownloadCount(file.getDownloadCount() + 1);
        this.updateById(file);

        // 8. 生成 COS 预签名 URL
        String downloadUrl = generatePresignedUrl(cosKey);

        // 9. 组装返回 VO
        DownloadUrlVo vo = new DownloadUrlVo();
        vo.setDownloadUrl(downloadUrl);
        vo.setFileName(file.getName());
        vo.setFileSize(fileSize);
        vo.setExpireTime(System.currentTimeMillis() / 1000 + 600); // 10分钟后过期

        return vo;
    }

    /**
     * 从异常中提取腾讯云 COS/STS 返回的错误信息
     * 错误格式形如：result = {"Response":{"Error":{"Code":"xxx","Message":"yyy"},"RequestId":"zzz"}}
     */
    private String extractCosError(Exception e) {
        try {
            String msg = e.getMessage();
            if (msg == null) return null;
            int idx = msg.indexOf('{');
            if (idx < 0) return null;
            JSONObject resp = new JSONObject(msg.substring(idx));
            JSONObject error = resp.optJSONObject("Response").optJSONObject("Error");
            if (error == null) return null;
            String code = error.optString("Code");
            String message = error.optString("Message");
            return code + ": " + message;
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 生成 COS 预签名 URL（有效期10分钟）
     */
    private String generatePresignedUrl(String cosKey) {
        Date expirationDate = new Date(System.currentTimeMillis() + 600 * 1000); // 10分钟
        URL url = cosClient.generatePresignedUrl(
                cosProperties.getBucketName(),
                cosKey,
                expirationDate,
                HttpMethodName.GET
        );
        return url.toString();
    }
    /**
     * 从当前请求中获取客户端真实 IP
     * 优先从代理头 X-Forwarded-For / Proxy-Client-IP 等获取，兼容反向代理场景
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "127.0.0.1";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个 IP（最接近客户端的）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        // 统一 IPv6 本地回环地址
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    /**
     * 从当前请求中获取 User-Agent 头
     */
    private String getUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "Unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }


    private TreeMap<String, Object> buildStsConfig(String cosKey, Long fileSize) {
        TreeMap<String, Object> config = new TreeMap<>();

        // 永久密钥
        config.put("secretId", cosProperties.getSecretId());
        config.put("secretKey", cosProperties.getSecretKey());

        // 临时密钥有效期（秒）
        config.put("durationSeconds", cosProperties.getDurationSeconds());

        // 存储桶所在地域
        config.put("region", cosProperties.getRegion());

        // ========== 权限策略（Policy）—— 核心安全控制 ==========
        // 从存储桶名中提取 APPID（格式：bucketName-appid，如 mycloud-1250000000）
        String bucketName = cosProperties.getBucketName();
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new BusinessException("COS 存储桶名称未配置（tencent.cos.bucket-name）");
        }
        String[] bucketParts = bucketName.split("-");
        if (bucketParts.length < 2) {
            throw new BusinessException("COS 存储桶名称格式错误，应为「存储桶名-APPID」，如 mycloud-1250000000");
        }
        String appId = bucketParts[bucketParts.length - 1];
        if (!appId.matches("\\d+")) {
            throw new BusinessException("COS 存储桶名称格式错误，APPID 必须为纯数字，当前配置：" + bucketName);
        }

        // 构建资源路径：qcs::cos:{region}:uid/{appId}:{bucketName}/{cosKey}
        String resource = String.format(
                "qcs::cos:%s:uid/%s:%s/%s",
                cosProperties.getRegion(),
                appId,
                cosProperties.getBucketName(),
                cosKey
        );

        // 构建 Policy JSON
        // 限制：只允许 PutObject 及相关分片上传操作，且只允许上传到指定路径，且文件大小不超过限制
        String policy = String.format(
                "{" +
                        "  \"version\": \"2.0\"," +
                        "  \"statement\": [" +
                        "    {" +
                        "      \"effect\": \"allow\"," +
                        "      \"action\": [" +
                        "        \"cos:PutObject\"," +
                        "        \"cos:PostObject\"," +
                        "        \"cos:InitiateMultipartUpload\"," +
                        "        \"cos:ListMultipartUploads\"," +
                        "        \"cos:UploadPart\"," +
                        "        \"cos:CompleteMultipartUpload\"" +
                        "      ]," +
                        "      \"resource\": [\"%s\"]," +
                        "      \"condition\": {" +
                        "        \"numeric_less_than_equal\": {" +
                        "          \"cos:content-length\": %d" +
                        "        }" +
                        "      }" +
                        "    }" +
                        "  ]" +
                        "}",
                resource,
                fileSize  // 限制上传文件大小不超过本次申请的大小
        );

        config.put("policy", policy);
        return config;
    }

    @Override
    public List<UserFile> list(Long userId, Long parentId) {

        LambdaQueryWrapper<UserFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFile::getUserId, userId)
                .eq(UserFile::getParentId, parentId)
                .isNull(UserFile::getDeletedAt)      // 不显示回收站内容
                .orderByDesc(UserFile::getIsFolder)  // 文件夹排在前面
                .orderByAsc(UserFile::getName);      // 按名称排序
        List<UserFile> fileList = this.list(wrapper);
        return fileList;
    }

    @Override
    public void confirmUpload(Long userId, Long fileId) {
        UserFile file = this.getById(fileId);
        if (file == null) {
            throw new BusinessException("文件记录不存在");
        }
        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此文件");
        }
        if (file.getIsFolder()) {
            throw new BusinessException("文件夹无需确认上传");
        }
        // 将上传状态改为已完成
        file.setUploadStatus(1);
        this.updateById(file);
        log.info("上传确认完成，文件ID：{}，用户ID：{}", fileId, userId);
    }

    @Override
    public void createFolder(Long userId, CreateFolderRequest request) {
        String folderName = request.getName();
        Long parentId = request.getParentId();

        // 1. 校验文件夹名称（禁止包含路径分隔符）
        if (folderName.contains("/") || folderName.contains("\\")) {
            throw new BusinessException("文件夹名称不能包含 / 或 \\ 字符");
        }
        // 2. 如果父目录不是根目录(0)，校验父目录是否存在且属于该用户
        if (parentId != null && parentId != 0) {
            UserFile parentFolder = this.getById(parentId);
            if (parentFolder == null) {
                throw new BusinessException("父文件夹不存在");
            }
            // 检查父文件夹是否属于当前用户
            if (!parentFolder.getUserId().equals(userId)) {
                throw new BusinessException("无权操作此文件夹");
            }
            // 检查父文件夹是否真的是文件夹（防止用户在某文件下创建文件夹）
            if (!parentFolder.getIsFolder()) {
                throw new BusinessException("目标路径不是文件夹");
            }
        }
        // 3. 检查同一父目录下是否已存在同名文件夹（防止重名）
        LambdaQueryWrapper<UserFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFile::getUserId, userId)
                .eq(UserFile::getParentId, parentId)
                .eq(UserFile::getName, folderName)
                .eq(UserFile::getIsFolder, true)
                .isNull(UserFile::getDeletedAt); // 只查未被删除的
        long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException("该目录下已存在同名文件夹，请重新命名");
        }
        // 4. 构建文件夹实体并插入
        UserFile folder = new UserFile();
        folder.setUserId(userId);
        folder.setParentId(parentId);
        folder.setName(folderName);
        folder.setIsFolder(true);
        folder.setFileSize(0L);
        folder.setStoragePath(null);      // 文件夹没有物理路径
        folder.setFileMd5(null);
        folder.setMimeType(null);
        folder.setDownloadCount(0);

        this.save(folder);
    }

    /**
     * 逻辑删除（移入回收站）
     * 文件夹会级联删除其所有子孙文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteToRecycle(Long userId, Long fileId) {
        // getById 自动过滤已逻辑删除的记录（回收站中的无法再次删除）
        UserFile file = this.getById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在或已在回收站中");
        }
        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此文件");
        }

        // 收集自身 + 所有未删除的子孙文件 ID（文件夹级联）
        List<Long> ids = new ArrayList<>();
        ids.add(fileId);
        collectDescendants(userId, fileId, ids);

        // MP 逻辑删除：自动转换为 UPDATE files SET deleted_at = NOW() WHERE id IN (...)
        this.removeByIds(ids);
        log.info("逻辑删除成功，用户：{}，文件ID：{}，级联数量：{}", userId, fileId, ids.size());
    }

    /**
     * 永久删除（物理删除）
     * 仅回收站中的文件可永久删除；文件夹会级联删除其所有子孙
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanently(Long userId, Long fileId) {
        // 含已删除记录一起查询（getById 会过滤回收站数据，故用自定义 SQL）
        UserFile file = userFileMapper.selectIncludingDeleted(fileId, userId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        if (file.getDeletedAt() == null) {
            throw new BusinessException("仅回收站中的文件可永久删除，请先删除至回收站");
        }

        // 递归收集自身 + 所有子孙 ID（含已逻辑删除的，用 WITH RECURSIVE 一次查询）
        List<Long> ids = userFileMapper.selectDescendantIdsIncludingDeleted(fileId, userId);

        // 1. 先删除 COS 存储对象（文件夹无 storagePath 自动跳过）
        deleteCosObjects(ids);

        // 2. 物理删除数据库记录（自定义 SQL，绕过逻辑删除拦截器）
        userFileMapper.physicalDeleteByIds(ids);
        log.info("永久删除成功，用户：{}，文件ID：{}，级联数量：{}", userId, fileId, ids.size());
    }

    /**
     * 批量删除 COS 存储对象
     * 尽力而为：单个对象删除失败仅记录日志，不阻断数据库删除（避免删除操作永远失败）
     */
    private void deleteCosObjects(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<String> paths = userFileMapper.selectStoragePathsByIds(ids);
        for (String path : paths) {
            if (path == null || path.trim().isEmpty()) continue; // 文件夹没有物理路径
            try {
                cosClient.deleteObject(cosProperties.getBucketName(), path);
                log.info("COS 对象已删除：{}", path);
            } catch (Exception e) {
                log.warn("COS 对象删除失败：{}，原因：{}", path, e.getMessage());
            }
        }
    }

    /**
     * 回收站列表（含已逻辑删除的文件），按删除时间倒序
     */
    @Override
    public List<UserFile> listRecycle(Long userId) {
        // 自定义 SQL：wrapper 查询会被 MP 逻辑删除拦截器追加 deleted_at IS NULL 导致查不到
        return userFileMapper.selectRecycleList(userId);
    }

    /**
     * 恢复文件（移出回收站）
     * - 文件夹级联恢复其所有子孙
     * - 父文件夹若也在回收站则递归恢复祖先链（防止恢复后成为孤儿）
     * - 父文件夹若已被永久删除，则恢复到根目录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreFile(Long userId, Long fileId) {
        // 含已删除记录一起查询（getById 会过滤回收站数据）
        UserFile file = userFileMapper.selectIncludingDeleted(fileId, userId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        if (file.getDeletedAt() == null) {
            throw new BusinessException("文件不在回收站中");
        }

        // 1. 处理父目录：若在回收站则递归恢复祖先链；若已被永久删除则改挂根目录
        Long parentId = file.getParentId();
        if (parentId != null && parentId != 0) {
            UserFile parent = userFileMapper.selectIncludingDeleted(parentId, userId);
            if (parent == null) {
                // 父文件夹已被永久删除 → 恢复到根目录，避免成为孤儿数据
                log.info("父文件夹已被永久删除，文件恢复到根目录：fileId={}", fileId);
                userFileMapper.updateParentId(fileId, userId, 0L);
            } else if (parent.getDeletedAt() != null) {
                // 父文件夹在回收站 → 递归恢复祖先链（仅恢复文件夹本身，不影响其他子孙）
                restoreAncestors(userId, parent);
            }
        }

        // 2. 检查目标目录下同名冲突（防止覆盖已有文件）
        LambdaQueryWrapper<UserFile> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(UserFile::getUserId, userId)
                .eq(UserFile::getParentId, parentId)
                .eq(UserFile::getName, file.getName())
                .eq(UserFile::getIsFolder, file.getIsFolder())
                .isNull(UserFile::getDeletedAt)
                .ne(UserFile::getId, fileId);
        if (this.count(nameWrapper) > 0) {
            throw new BusinessException("目标目录已存在同名文件/文件夹，请先重命名后再恢复");
        }

        // 3. 恢复自身 + 所有子孙（含已逻辑删除的）
        List<Long> ids = userFileMapper.selectDescendantIdsIncludingDeleted(fileId, userId);
        userFileMapper.restoreByIds(ids, userId);
        log.info("恢复成功，用户：{}，文件ID：{}，级联数量：{}", userId, fileId, ids.size());
    }

    /**
     * 取消上传：清理未完成上传的文件记录并回滚预扣空间（前端直传 COS 失败时调用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelUpload(Long userId, Long fileId) {
        // 只允许取消 upload_status=0（上传中）的记录
        UserFile pending = userFileMapper.selectPendingUpload(fileId, userId);
        if (pending == null) {
            log.warn("取消上传：记录不存在或已确认，userId={}，fileId={}", userId, fileId);
            return; // 幂等处理
        }

        // 1. 清理可能已上传到 COS 的对象（上传失败时对象通常不存在，deleteObject 对不存在的 key 不报错）
        if (pending.getStoragePath() != null && !pending.getStoragePath().isEmpty()) {
            try {
                cosClient.deleteObject(cosProperties.getBucketName(), pending.getStoragePath());
                log.info("取消上传：COS 对象已清理：{}", pending.getStoragePath());
            } catch (Exception e) {
                log.warn("取消上传：COS 对象清理失败：{}，原因：{}", pending.getStoragePath(), e.getMessage());
            }
        }

        // 2. 物理删除文件记录（未确认的记录直接删除，不走回收站）
        userFileMapper.physicalDeleteByIds(List.of(fileId));

        // 3. 回滚预扣的空间
        User user = userMapper.selectByIdForUpdate(userId);
        if (user != null) {
            user.setUsedSpace(Math.max(0, user.getUsedSpace() - pending.getFileSize()));
            userMapper.updateById(user);
        }

        log.info("取消上传成功，userId：{}，fileId：{}，回滚空间：{}字节",
                userId, fileId, pending.getFileSize());
    }

    /**
     * 递归恢复回收站中的祖先文件夹链（只恢复文件夹本身，防止恢复后的文件成为孤儿）
     */
    private void restoreAncestors(Long userId, UserFile folder) {
        userFileMapper.restoreByIds(List.of(folder.getId()), userId);
        Long parentId = folder.getParentId();
        if (parentId != null && parentId != 0) {
            UserFile parent = userFileMapper.selectIncludingDeleted(parentId, userId);
            if (parent != null && parent.getDeletedAt() != null) {
                restoreAncestors(userId, parent);
            }
        }
    }

    /**
     * 递归收集某文件夹下所有未删除子孙文件的 ID（用于移入回收站）
     */
    private void collectDescendants(Long userId, Long parentId, List<Long> collector) {
        LambdaQueryWrapper<UserFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFile::getUserId, userId)
                .eq(UserFile::getParentId, parentId); // MP 自动追加 deleted_at IS NULL
        List<UserFile> children = this.list(wrapper);
        for (UserFile child : children) {
            collector.add(child.getId());
            if (Boolean.TRUE.equals(child.getIsFolder())) {
                collectDescendants(userId, child.getId(), collector);
            }
        }
    }
}
