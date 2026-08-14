package cn.bvovd.clouddrive.service;

import cn.bvovd.clouddrive.vo.DashboardStatsVo;

public interface AdminStatsService {

    /**
     * 仪表盘统计数据（用户/空间/流量/分享 + 近7天下载趋势）
     */
    DashboardStatsVo getDashboardStats();
}
