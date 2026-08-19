package cn.bvovd.clouddrive.task;

import cn.bvovd.clouddrive.entity.UserFile;
import cn.bvovd.clouddrive.mapper.UserFileMapper;
import cn.bvovd.clouddrive.utils.CosCleanupUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 回收站过期文件自动清理定时任务
 *
 * 背景：用户删除文件后进入回收站（逻辑删除，deleted_at 有值），
 * 前端提示"15 天后自动清理"。本任务每天凌晨 1 点执行一次：
 * 找到回收站中超过 15 天未恢复的文件，物理删除数据库记录 + 删除 COS 对象，
 * 实现真正的空间释放。
 *
 * 处理逻辑：
 * 1. 查出所有超期的"根记录"（含文件夹）
 * 2. 对每个根记录递归收集其全部子孙（文件夹级联，含已逻辑删除的）
 * 3. 合并去重后统一物理删除——记录删除 + COS 对象删除（秒传共享保护）
 *
 * cron 表达式：0 0 1 * * ?  = 每天 01:00:00 执行一次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecycleCleanupTask {

    /** 回收站保留天数（与前端提示一致） */
    private static final int KEEP_DAYS = 15;

    private final UserFileMapper userFileMapper;
    private final CosCleanupUtil cosCleanupUtil;

    /**
     * 每天凌晨清理超过 15 天的回收站文件
     */
    @Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Shanghai")
    public void cleanupExpiredRecycle() {
        // 1. 查出回收站中超过 15 天的文件记录（只查根记录，子孙随后递归收集）
        List<Long> expiredRootIds = userFileMapper.selectExpiredRecycleIds(KEEP_DAYS);
        if (expiredRootIds.isEmpty()) {
            return; // 没有过期记录，直接结束
        }

        // 2. 对每个根记录收集自身 + 全部子孙 ID（文件夹级联），合并去重
        //    LinkedHashSet 保持插入顺序，避免重复删除同一记录
        Set<Long> allIds = new LinkedHashSet<>(expiredRootIds);
        for (Long rootId : expiredRootIds) {
            // 需要文件所属的 user_id 才能递归查询其子孙（递归 SQL 按用户隔离）
            UserFile file = userFileMapper.selectAdminById(rootId);
            if (file != null) {
                allIds.addAll(userFileMapper.selectDescendantIdsIncludingDeleted(rootId, file.getUserId()));
            }
        }
        List<Long> idsToDelete = new ArrayList<>(allIds);

        // 3. 先删 COS 对象（带秒传共享保护：被其他有效记录引用的对象不删）
        cosCleanupUtil.deleteObjectsIfUnshared(idsToDelete);

        // 4. 物理删除数据库记录（绕过逻辑删除拦截器）
        userFileMapper.physicalDeleteByIds(idsToDelete);

        log.info("【定时任务】回收站过期清理完成，根记录：{} 条，共删除：{} 条", expiredRootIds.size(), idsToDelete.size());
    }
}
