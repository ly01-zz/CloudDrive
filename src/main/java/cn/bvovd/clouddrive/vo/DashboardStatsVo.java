package cn.bvovd.clouddrive.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsVo {
    private Long totalUsers;            // 总用户数
    private Long frozenUsers;           // 冻结用户数
    private Long totalSpace;            // 总存储空间（字节）
    private Long usedSpace;             // 已用存储空间（字节）
    private Long monthDownloadTraffic;  // 本月总下载流量（字节）
    private Long activeShares;          // 当前有效分享数（status=0）
    private List<DailyDownloadVo> downloadTrend; // 近 7 天下载趋势

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyDownloadVo {
        private String date;            // 日期 yyyy-MM-dd
        private Integer downloadCount;  // 下载次数
        private Long downloadSize;      // 下载流量（字节）
    }
}
