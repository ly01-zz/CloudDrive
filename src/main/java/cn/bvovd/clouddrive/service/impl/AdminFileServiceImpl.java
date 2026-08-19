package cn.bvovd.clouddrive.service.impl;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.entity.UserFile;
import cn.bvovd.clouddrive.exception.BusinessException;
import cn.bvovd.clouddrive.mapper.UserFileMapper;
import cn.bvovd.clouddrive.mapper.UserMapper;
import cn.bvovd.clouddrive.service.AdminFileService;
import cn.bvovd.clouddrive.service.AdminLogService;
import cn.bvovd.clouddrive.utils.CosCleanupUtil;
import cn.bvovd.clouddrive.vo.AdminFileVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFileServiceImpl implements AdminFileService {

    private final UserFileMapper userFileMapper;
    private final UserMapper userMapper;
    private final AdminLogService adminLogService;
    private final CosCleanupUtil cosCleanupUtil;

    @Override
    public List<AdminFileVo> listFiles(String keyword, String phone, Boolean isFolder, Integer deleted,
                                       int page, int size) {
        // 1. 按手机号反查 userId（可选）
        Long userId = null;
        if (StringUtils.hasText(phone)) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, phone);
            User user = userMapper.selectOne(wrapper);
            if (user == null) {
                return Collections.emptyList();
            }
            userId = user.getId();
        }

        // 2. 查询文件（含已逻辑删除）
        int offset = Math.max(0, (page - 1) * size);
        List<UserFile> files = userFileMapper.selectAdminList(keyword, userId, isFolder, deleted, offset, size);
        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 批量查询所属用户信息
        List<Long> userIds = files.stream()
                .map(UserFile::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 4. 组装 VO
        return files.stream().map(file -> {
            AdminFileVo vo = new AdminFileVo();
            vo.setId(file.getId());
            vo.setUserId(file.getUserId());
            User owner = userMap.get(file.getUserId());
            vo.setPhone(owner != null ? owner.getPhone() : null);
            vo.setNickname(owner != null ? owner.getNickname() : null);
            vo.setParentId(file.getParentId());
            vo.setName(file.getName());
            vo.setIsFolder(file.getIsFolder());
            vo.setFileSize(file.getFileSize());
            vo.setFileSizeDesc(file.getIsFolder() ? "-" : formatFileSize(file.getFileSize()));
            vo.setStoragePath(file.getStoragePath());
            vo.setUploadStatus(file.getUploadStatus());
            vo.setDownloadCount(file.getDownloadCount());
            vo.setCreatedAt(file.getCreatedAt());
            vo.setDeletedAt(file.getDeletedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public long countFiles(String keyword, String phone, Boolean isFolder, Integer deleted) {
        Long userId = null;
        if (StringUtils.hasText(phone)) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, phone);
            User user = userMapper.selectOne(wrapper);
            if (user == null) {
                return 0;
            }
            userId = user.getId();
        }
        Long count = userFileMapper.selectAdminCount(keyword, userId, isFolder, deleted);
        return count != null ? count : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId) {
        // 含已删除记录一起查询
        UserFile file = userFileMapper.selectAdminById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 递归收集自身 + 所有子孙 ID（含已逻辑删除）
        List<Long> ids = userFileMapper.selectDescendantIdsIncludingDeleted(fileId, file.getUserId());

        // 1. 删除 COS 存储对象（带秒传共享对象保护）
        cosCleanupUtil.deleteObjectsIfUnshared(ids);

        // 2. 物理删除数据库记录
        userFileMapper.physicalDeleteByIds(ids);

        adminLogService.record(UserContext.getUserId(), "DELETE_FILE", "file", String.valueOf(fileId),
                "文件:" + file.getName());
        log.info("管理员物理删除文件，文件ID：{}，级联数量：{}，操作人：{}", fileId, ids.size(), UserContext.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreFile(Long fileId) {
        UserFile file = userFileMapper.selectAdminById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        if (file.getDeletedAt() == null) {
            throw new BusinessException("文件不在回收站中");
        }

        List<Long> ids = userFileMapper.selectDescendantIdsIncludingDeleted(fileId, file.getUserId());
        userFileMapper.restoreByIds(ids, file.getUserId());

        adminLogService.record(UserContext.getUserId(), "RESTORE_FILE", "file", String.valueOf(fileId),
                "文件:" + file.getName());
        log.info("管理员恢复文件，文件ID：{}，级联数量：{}，操作人：{}", fileId, ids.size(), UserContext.getUserId());
    }

    private String formatFileSize(Long size) {
        if (size == null || size == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
