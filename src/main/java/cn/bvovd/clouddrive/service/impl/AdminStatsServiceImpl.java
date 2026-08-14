package cn.bvovd.clouddrive.service.impl;

import cn.bvovd.clouddrive.entity.ShareLink;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.mapper.DownloadLogMapper;
import cn.bvovd.clouddrive.mapper.ShareLinkMapper;
import cn.bvovd.clouddrive.mapper.UserMapper;
import cn.bvovd.clouddrive.service.AdminStatsService;
import cn.bvovd.clouddrive.vo.DashboardStatsVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final UserMapper userMapper;
    private final DownloadLogMapper downloadLogMapper;
    private final ShareLinkMapper shareLinkMapper;

    @Override
    public DashboardStatsVo getDashboardStats() {
        DashboardStatsVo vo = new DashboardStatsVo();

        // 1. 用户统计（MP 逻辑删除自动过滤 deleted_at IS NULL）
        vo.setTotalUsers(userMapper.selectCount(null));
        vo.setFrozenUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1)));

        // 2. 空间统计（自定义 SQL，需手动过滤逻辑删除）
        Map<String, Object> space = userMapper.selectSpaceStats();
        vo.setTotalSpace(((Number) space.get("total_space")).longValue());
        vo.setUsedSpace(((Number) space.get("used_space")).longValue());

        // 3. 本月总下载流量
        LocalDateTime monthStart = LocalDateTime.now()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        Long monthTraffic = downloadLogMapper.selectTotalSizeSince(monthStart);
        vo.setMonthDownloadTraffic(monthTraffic != null ? monthTraffic : 0L);

        // 4. 当前有效分享数（status=0）
        vo.setActiveShares(shareLinkMapper.selectCount(
                new LambdaQueryWrapper<ShareLink>().eq(ShareLink::getStatus, 0)));

        // 5. 近 7 天下载趋势（无记录的天补齐为 0）
        List<Map<String, Object>> rows = downloadLogMapper.selectTrendSince(
                LocalDateTime.now().minusDays(6).withHour(0).withMinute(0).withSecond(0));
        Map<String, DashboardStatsVo.DailyDownloadVo> byDate = rows.stream()
                .map(row -> new DashboardStatsVo.DailyDownloadVo(
                        (String) row.get("date"),
                        ((Number) row.get("download_count")).intValue(),
                        ((Number) row.get("download_size")).longValue()))
                .collect(Collectors.toMap(DashboardStatsVo.DailyDownloadVo::getDate, Function.identity()));

        LocalDate today = LocalDate.now();
        List<DashboardStatsVo.DailyDownloadVo> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            trend.add(byDate.getOrDefault(date, new DashboardStatsVo.DailyDownloadVo(date, 0, 0L)));
        }
        vo.setDownloadTrend(trend);

        return vo;
    }
}
