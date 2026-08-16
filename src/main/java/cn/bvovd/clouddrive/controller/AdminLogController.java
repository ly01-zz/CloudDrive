package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.entity.AdminLog;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/log")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    /**
     * 操作日志列表（按时间倒序）
     */
    @GetMapping("/list")
    public Result<List<AdminLog>> list(@RequestParam(defaultValue = "200") Integer limit) {
        UserContext.requireAdmin();
        return Result.success("获取成功", adminLogService.listLatest(limit));
    }
}
