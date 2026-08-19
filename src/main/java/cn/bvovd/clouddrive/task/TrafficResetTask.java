package cn.bvovd.clouddrive.task;

import cn.bvovd.clouddrive.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 月度下载流量清零定时任务
 *
 * 背景：系统有两套流量重置机制，互为补充——
 * 1. 【懒重置】用户下载时检查跨月（UserFileServiceImpl / ShareServiceImpl），
 *    跨月则当场清零。优点：零成本；缺点：跨月后用户不下载，数据库里还是旧值。
 * 2. 【本任务】每月 1 号 00:00 批量清零，保证数据库与"本月"始终一致，
 *    让侧边栏流量显示、管理后台统计跨月后立即准确。
 *
 * cron 表达式说明（6 位）：秒 分 时 日 月 周
 *   0 0 0 1 * ?  = 每月 1 号的 00:00:00 执行一次（? 表示"不指定周几"）
 *   zone = "Asia/Shanghai" 显式指定时区，与 Docker 容器 TZ 保持一致，
 *   避免服务器时区不同导致清零时间偏移。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrafficResetTask {

    private final UserMapper userMapper;

    /**
     * 每月 1 号凌晨批量清零所有用户的本月已用流量
     */
    @Scheduled(cron = "0 0 0 1 * ?", zone = "Asia/Shanghai")
    public void resetMonthlyTraffic() {
        // SQL 内部只更新"流量月份锚点不在本月"的用户（幂等，重复执行无副作用）
        int count = userMapper.resetMonthlyTraffic();
        log.info("【定时任务】月度流量清零完成，影响用户数：{}", count);
    }
}
