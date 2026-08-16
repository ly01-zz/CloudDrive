package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.dto.ResetTrafficRequest;
import cn.bvovd.clouddrive.dto.UpdateQuotaRequest;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.service.AdminLogService;
import cn.bvovd.clouddrive.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;
    private final AdminLogService adminLogService;

    /**
     * 禁用用户（冻结账号）
     */
    @PutMapping("/{userId}/disable")
    public Result<String> disableUser(@PathVariable Long userId) {
        // 权限校验：必须是管理员
        UserContext.requireAdmin();

        Long adminId = UserContext.getUserId();
        userService.disableUser(userId, adminId);
        adminLogService.record(adminId, "DISABLE_USER", "user", String.valueOf(userId), null);
        return Result.success("用户已禁用");
    }

    /**
     * 启用用户（解冻账号）
     */
    @PutMapping("/{userId}/enable")
    public Result<String> enableUser(@PathVariable Long userId) {
        UserContext.requireAdmin();

        Long adminId = UserContext.getUserId();
        userService.enableUser(userId, adminId);
        adminLogService.record(adminId, "ENABLE_USER", "user", String.valueOf(userId), null);
        return Result.success("用户已启用");
    }

    /**
     * 重置用户本月下载流量为 0
     */
    @PutMapping("/{userId}/reset-traffic")
    public Result<String> resetTraffic(@PathVariable Long userId, @RequestBody(required = false) ResetTrafficRequest request) {
        UserContext.requireAdmin();

        Long adminId = UserContext.getUserId();
        userService.resetTraffic(userId, request != null ? request.getReason() : null);
        adminLogService.record(adminId, "RESET_TRAFFIC", "user", String.valueOf(userId),
                request != null ? request.getReason() : null);
        return Result.success("流量已重置");
    }

    /**
     * 调整用户空间/月度流量配额
     */
    @PutMapping("/{userId}/quota")
    public Result<String> updateQuota(@PathVariable Long userId, @RequestBody UpdateQuotaRequest request) {
        UserContext.requireAdmin();

        Long adminId = UserContext.getUserId();
        userService.updateQuota(userId, request.getTotalSpace(), request.getMonthlyDownloadLimit());
        adminLogService.record(adminId, "UPDATE_QUOTA", "user", String.valueOf(userId), request.getReason());
        return Result.success("配额已更新");
    }

    // 获取所有用户信息（仅管理员）
    @GetMapping("/all")
    public Result<java.util.List<User>> all() {
        UserContext.requireAdmin();
        return Result.success("获取成功", userService.getAllUsers());
    }
}
