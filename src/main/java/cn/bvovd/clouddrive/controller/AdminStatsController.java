package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.service.AdminStatsService;
import cn.bvovd.clouddrive.vo.DashboardStatsVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    /**
     * 仪表盘统计数据
     */
    @GetMapping("/dashboard")
    public Result<DashboardStatsVo> dashboard() {
        UserContext.requireAdmin();
        return Result.success("获取成功", adminStatsService.getDashboardStats());
    }
}
