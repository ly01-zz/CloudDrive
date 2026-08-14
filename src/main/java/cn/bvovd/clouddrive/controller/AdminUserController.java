package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    /**
     * 禁用用户（冻结账号）
     */
    @PutMapping("/{userId}/disable")
    public Result<String> disableUser(@PathVariable Long userId) {
        // 权限校验：必须是管理员
        UserContext.requireAdmin();

        Long currentAdminId = UserContext.getUserId();
        userService.disableUser(userId, currentAdminId);
        return Result.success("用户已禁用");
    }

    /**
     * 启用用户（解冻账号）
     */
    @PutMapping("/{userId}/enable")
    public Result<String> enableUser(@PathVariable Long userId) {
        UserContext.requireAdmin();

        Long currentAdminId = UserContext.getUserId();
        userService.enableUser(userId, currentAdminId);
        return Result.success("用户已启用");
    }
    // 获取所有用户信息（仅管理员）
    @GetMapping("/all")
    public Result<java.util.List<User>> all() {
        UserContext.requireAdmin();
        return Result.success("获取成功", userService.getAllUsers());
    }
}
