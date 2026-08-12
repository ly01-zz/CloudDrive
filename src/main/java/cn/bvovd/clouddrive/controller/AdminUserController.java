package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.exception.BusinessException;
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
        Integer role = UserContext.getRole();
        if (role == null || role != 1) {
            throw new BusinessException("无权限操作，仅管理员可执行");
        }

        Long currentAdminId = UserContext.getUserId();
        userService.disableUser(userId, currentAdminId);
        return Result.success("用户已禁用");
    }

    /**
     * 启用用户（解冻账号）
     */
    @PutMapping("/{userId}/enable")
    public Result<String> enableUser(@PathVariable Long userId) {
        Integer role = UserContext.getRole();
        if (role == null || role != 1) {
            throw new BusinessException("无权限操作，仅管理员可执行");
        }

        Long currentAdminId = UserContext.getUserId();
        userService.enableUser(userId, currentAdminId);
        return Result.success("用户已启用");
    }
    //获取所有用户信息
    @GetMapping("/all")
    public Result<User> all(){
        Integer role = UserContext.getRole();
        if (role==0){
            throw new BusinessException("该用户没有权限！");
        }

        return Result.success("");
    }
}
