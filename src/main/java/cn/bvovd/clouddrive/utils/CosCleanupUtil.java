package cn.bvovd.clouddrive.utils;

import cn.bvovd.clouddrive.config.CosProperties;
import cn.bvovd.clouddrive.mapper.UserFileMapper;
import com.qcloud.cos.COSClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * COS 存储对象清理工具（供用户侧删除、管理端删除、定时任务三处复用）
 *
 * 核心逻辑：删除前先检查该存储路径是否被其他有效文件记录引用。
 * 原因：秒传/内容寻址后，同一份 COS 对象可能被多个用户的文件记录共享
 * （storage_path 相同）。如果直接删除对象，其他用户的文件就会损坏，
 * 因此被共享的对象只删数据库记录、不删物理文件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CosCleanupUtil {

    private final COSClient cosClient;
    private final CosProperties cosProperties;
    private final UserFileMapper userFileMapper;

    /**
     * 按文件记录 ID 批量删除 COS 对象（带秒传共享保护）
     *
     * @param fileIds 文件记录 ID 列表（文件夹没有 storage_path 会自动跳过）
     */
    public void deleteObjectsIfUnshared(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        // 1. 批量查出这些记录对应的 COS 存储路径（含已逻辑删除的记录）
        List<String> paths = userFileMapper.selectStoragePathsByIds(fileIds);
        for (String path : paths) {
            // 文件夹没有物理路径，跳过
            if (path == null || path.trim().isEmpty()) {
                continue;
            }
            // 2. 秒传共享保护：统计还有多少条"有效记录"（upload_status=1 且未删除）引用该对象
            Long refCount = userFileMapper.countActiveByStoragePath(path);
            if (refCount != null && refCount > 0) {
                // 还有别的用户文件在用这份数据，只能删记录不能删对象
                log.info("存储对象被其他记录共享，跳过删除：{}", path);
                continue;
            }
            // 3. 确认无人引用后才物理删除（尽力而为：失败只记日志，不阻断数据库删除）
            try {
                cosClient.deleteObject(cosProperties.getBucketName(), path);
                log.info("COS 对象已删除：{}", path);
            } catch (Exception e) {
                log.warn("COS 对象删除失败：{}，原因：{}", path, e.getMessage());
            }
        }
    }
}
