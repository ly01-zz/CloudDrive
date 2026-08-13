package cn.bvovd.clouddrive.service.impl;

import cn.bvovd.clouddrive.config.CosProperties;
import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.dto.CreateShareRequest;
import cn.bvovd.clouddrive.entity.DownloadLog;
import cn.bvovd.clouddrive.entity.ShareLink;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.entity.UserFile;
import cn.bvovd.clouddrive.exception.BusinessException;
import cn.bvovd.clouddrive.mapper.DownloadLogMapper;
import cn.bvovd.clouddrive.mapper.ShareLinkMapper;
import cn.bvovd.clouddrive.mapper.UserFileMapper;
import cn.bvovd.clouddrive.mapper.UserMapper;
import cn.bvovd.clouddrive.service.ShareService;
import cn.bvovd.clouddrive.vo.DownloadUrlVo;
import cn.bvovd.clouddrive.vo.ShareInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareServiceImpl extends ServiceImpl<ShareLinkMapper, ShareLink> implements ShareService {

    private final ShareLinkMapper shareLinkMapper;
    private final UserFileMapper userFileMapper;
    private final UserMapper userMapper;
    private final DownloadLogMapper downloadLogMapper;
    private final COSClient cosClient;
    private final CosProperties cosProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareLink createShare(CreateShareRequest request) {
        Long userId = UserContext.getUserId();

        // 1. 校验文件是否存在且属于当前用户
        UserFile file = userFileMapper.selectById(request.getFileId());
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException("文件不存在或无权限");
        }
        // 文件夹分享暂不支持（可扩展）
        if (file.getIsFolder()) {
            throw new BusinessException("暂不支持分享文件夹");
        }

        // 2. 生成唯一分享码（8位字母数字混合）
        String shareCode = generateUniqueCode();

        // 3. 处理提取码
        String extractCode = request.getExtractCode();
        if (Integer.valueOf(1).equals(request.getShareType())) {
            // 私密分享：若未提供提取码则自动生成4位数字
            if (!StringUtils.hasText(extractCode)) {
                extractCode = String.format("%04d", new Random().nextInt(10000));
            } else if (extractCode.length() < 4 || extractCode.length() > 6) {
                throw new BusinessException("提取码长度需为4~6位");
            }
        } else {
            // 公开分享无需提取码
            extractCode = null;
        }

        // 4. 有效期计算
        LocalDateTime expireTime = null;
        if (request.getExpireDays() != null && request.getExpireDays() > 0) {
            expireTime = LocalDateTime.now().plusDays(request.getExpireDays());
        }

        // 5. 构建实体
        ShareLink link = new ShareLink();
        link.setUserId(userId);
        link.setFileId(request.getFileId());
        link.setShareCode(shareCode);
        link.setExtractCode(extractCode);
        link.setShareType(request.getShareType());
        link.setAccessMode(0); // 仅下载
        link.setTotalVisits(0);
        link.setMaxVisits(request.getMaxVisits());
        link.setTotalDownloads(0);
        link.setMaxDownloads(request.getMaxDownloads());
        link.setTotalDownloadSize(0L);
        link.setMaxDownloadSize(request.getMaxDownloadSize());
        link.setExpireTime(expireTime);
        link.setStatus(0);
        shareLinkMapper.insert(link);
        log.info("创建分享成功，用户：{}，分享码：{}，文件：{}", userId, shareCode, request.getFileId());

        return link;
    }

    @Override
    public ShareInfoVo getShareInfo(String shareCode) {
        ShareLink link = shareLinkMapper.selectByShareCode(shareCode);
        if (link == null) {
            throw new BusinessException("分享链接不存在");
        }
        // 校验有效性（不抛出异常，仅标记状态）
        validateShare(link);

        // 查询文件信息
        UserFile file = userFileMapper.selectById(link.getFileId());
        if (file == null) {
            throw new BusinessException("分享的文件已被删除");
        }

        // 增加访问计数
        link.setTotalVisits(link.getTotalVisits() + 1);
        shareLinkMapper.updateById(link);

        ShareInfoVo vo = new ShareInfoVo();
        vo.setShareCode(link.getShareCode());
        vo.setFileName(file.getName());
        vo.setFileSize(file.getFileSize());
        vo.setFileSizeDesc(formatFileSize(file.getFileSize()));
        vo.setShareType(link.getShareType());
        vo.setNeedExtract(StringUtils.hasText(link.getExtractCode()));
        vo.setExpireTime(link.getExpireTime());
        vo.setTotalVisits(link.getTotalVisits());
        vo.setMaxVisits(link.getMaxVisits());
        vo.setTotalDownloads(link.getTotalDownloads());
        vo.setMaxDownloads(link.getMaxDownloads());
        vo.setMaxDownloadSize(link.getMaxDownloadSize());
        vo.setMaxDownloadSizeDesc(link.getMaxDownloadSize() != null ? formatFileSize(link.getMaxDownloadSize()) : "无限制");
        vo.setStatusDesc(getStatusDesc(link.getStatus()));
        return vo;
    }

    @Override
    public List<ShareInfoVo> listMyShares(Long userId) {
        // 1. 查询当前用户的分享记录（最新在前）
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getUserId, userId)
                .orderByDesc(ShareLink::getCreatedAt);
        List<ShareLink> links = shareLinkMapper.selectList(wrapper);
        if (links.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量查询文件名（被删除的文件显示为"文件已删除"）
        List<Long> fileIds = links.stream()
                .map(ShareLink::getFileId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserFile> fileMap = userFileMapper.selectBatchIds(fileIds).stream()
                .collect(Collectors.toMap(UserFile::getId, Function.identity()));

        // 3. 组装 VO
        return links.stream().map(link -> {
            ShareInfoVo vo = new ShareInfoVo();
            vo.setId(link.getId());
            vo.setShareCode(link.getShareCode());
            vo.setExtractCode(link.getExtractCode());
            UserFile file = fileMap.get(link.getFileId());
            vo.setFileName(file != null ? file.getName() : "文件已删除");
            vo.setFileSize(file != null ? file.getFileSize() : 0L);
            vo.setFileSizeDesc(file != null ? formatFileSize(file.getFileSize()) : "-");
            vo.setShareType(link.getShareType());
            vo.setNeedExtract(StringUtils.hasText(link.getExtractCode()));
            vo.setExpireTime(link.getExpireTime());
            vo.setTotalVisits(link.getTotalVisits());
            vo.setMaxVisits(link.getMaxVisits());
            vo.setTotalDownloads(link.getTotalDownloads());
            vo.setMaxDownloads(link.getMaxDownloads());
            vo.setMaxDownloadSize(link.getMaxDownloadSize());
            vo.setMaxDownloadSizeDesc(link.getMaxDownloadSize() != null ? formatFileSize(link.getMaxDownloadSize()) : "无限制");
            vo.setStatus(link.getStatus());
            vo.setStatusDesc(getStatusDesc(link.getStatus()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DownloadUrlVo downloadSharedFile(String shareCode, String extractCode, String clientIp, String userAgent) {
        // 1. 获取当前登录用户
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }

        // 2. 查询分享记录（加锁）
        ShareLink link = shareLinkMapper.selectByShareCodeForUpdate(shareCode);
        if (link == null) {
            throw new BusinessException("分享链接不存在");
        }
        // 校验状态、过期等
        validateShare(link);

        // 3. 私密分享验证提取码
        if (Integer.valueOf(1).equals(link.getShareType())) {
            if (!StringUtils.hasText(extractCode) || !extractCode.equals(link.getExtractCode())) {
                throw new BusinessException("提取码错误");
            }
        }

        // 4. 获取文件
        UserFile file = userFileMapper.selectById(link.getFileId());
        if (file == null) {
            throw new BusinessException("文件已被删除");
        }
        // 校验文件是否在回收站
        if (file.getDeletedAt() != null) {
            throw new BusinessException("文件已被删除或移入回收站");
        }
        // 校验是否是文件夹
        if (file.getIsFolder()) {
            throw new BusinessException("文件夹不支持下载");
        }
        String cosKey = file.getStoragePath();
        if (cosKey == null || cosKey.isEmpty()) {
            throw new BusinessException("文件存储路径异常，请联系管理员");
        }
        long fileSize = file.getFileSize();

        // 5. 检查分享自身限制（次数/流量）
        if (link.getMaxDownloads() != null && link.getTotalDownloads() >= link.getMaxDownloads()) {
            throw new BusinessException("分享下载次数已达上限");
        }
        if (link.getMaxDownloadSize() != null) {
            if (link.getTotalDownloadSize() + fileSize > link.getMaxDownloadSize()) {
                throw new BusinessException("分享下载流量已达上限");
            }
            // 预扣分享流量
            link.setTotalDownloadSize(link.getTotalDownloadSize() + fileSize);
        }
        link.setTotalDownloads(link.getTotalDownloads() + 1);
        shareLinkMapper.updateById(link);

        // 6. 检查用户月度流量限额（行锁）
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 检查流量是否跨月，若跨月则重置
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resetTime = user.getTrafficResetTime();
        if (resetTime == null || resetTime.getMonthValue() != now.getMonthValue()
                || resetTime.getYear() != now.getYear()) {
            // 重置流量
            user.setUsedDownloadTraffic(0L);
            user.setTrafficResetTime(now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
        }
        long remaining = user.getMonthlyDownloadLimit() - user.getUsedDownloadTraffic();
        if (remaining < fileSize) {
            throw new BusinessException("您的月度下载流量不足，当前剩余 " + formatFileSize(remaining));
        }
        // 扣减用户流量
        user.setUsedDownloadTraffic(user.getUsedDownloadTraffic() + fileSize);
        userMapper.updateById(user);

        // 7. 记录下载日志（包含 share_code）
        DownloadLog downloadLog = new DownloadLog();
        downloadLog.setUserId(userId);
        downloadLog.setFileId(file.getId());
        downloadLog.setDownloadSize(fileSize);
        downloadLog.setIpAddress(clientIp);
        downloadLog.setUserAgent(userAgent);
        downloadLog.setShareCode(shareCode);
        downloadLogMapper.insert(downloadLog);

        // 8. 生成COS预签名URL
        String presignedUrl = generatePresignedUrl(cosKey);

        DownloadUrlVo vo = new DownloadUrlVo();
        vo.setDownloadUrl(presignedUrl);
        vo.setFileName(file.getName());
        log.info("分享文件下载成功，用户：{}，分享码：{}，文件：{}，大小：{}字节",
                userId, shareCode, file.getId(), fileSize);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelShare(Long shareId) {
        Long userId = UserContext.getUserId();
        ShareLink link = shareLinkMapper.selectById(shareId);
        if (link == null) {
            throw new BusinessException("分享不存在");
        }
        if (!link.getUserId().equals(userId)) {
            throw new BusinessException("无权取消此分享");
        }
        if (link.getStatus() != 0) {
            throw new BusinessException("该分享已失效");
        }
        link.setStatus(2); // 已取消
        shareLinkMapper.updateById(link);
        log.info("取消分享成功，用户：{}，分享ID：{}", userId, shareId);
    }

    @Override
    public void validateShare(ShareLink link) {
        if (link == null) {
            throw new BusinessException("分享不存在");
        }
        if (link.getStatus() == 1) {
            throw new BusinessException("分享已过期");
        }
        if (link.getStatus() == 2) {
            throw new BusinessException("分享已被创建者取消");
        }
        if (link.getExpireTime() != null && link.getExpireTime().isBefore(LocalDateTime.now())) {
            // 更新状态为过期
            link.setStatus(1);
            shareLinkMapper.updateById(link);
            throw new BusinessException("分享已过期");
        }
        // 访问次数限制（可选）
        if (link.getMaxVisits() != null && link.getTotalVisits() >= link.getMaxVisits()) {
            throw new BusinessException("分享访问次数已达上限");
        }
    }

    // ---------- 辅助方法 ----------
    private String generateUniqueCode() {
        // 生成8位随机字母数字组合，确保唯一
        String code;
        int attempts = 0;
        do {
            // 使用UUID生成短码
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toLowerCase();
            attempts++;
            if (attempts > 10) {
                throw new BusinessException("生成分享码失败，请重试");
            }
        } while (shareLinkMapper.selectByShareCode(code) != null);
        return code;
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

    // 文件大小格式化（与 SpaceApplicationServiceImpl 保持一致）
    private String formatFileSize(Long size) {
        if (size == null || size == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // 状态描述
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "正常";
            case 1: return "已过期";
            case 2: return "已取消";
            default: return "未知";
        }
    }
}
