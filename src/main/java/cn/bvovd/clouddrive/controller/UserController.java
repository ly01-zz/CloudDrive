package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.dto.LoginRequest;
import cn.bvovd.clouddrive.dto.UpdatePasswordRequest;
import cn.bvovd.clouddrive.vo.LoginVo;
import cn.bvovd.clouddrive.dto.RegisterRequest;
import cn.bvovd.clouddrive.dto.UpdateProfileRequest;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.service.UserService;
import cn.bvovd.clouddrive.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterRequest request) {
        User newUser = userService.register(request);
        return Result.success("注册成功", newUser.getId());
    }
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest request,
                        HttpServletRequest httpRequest) {
        LoginVo vo = userService.login(request, httpRequest);
        return Result.success("登录成功", vo);
    }
    @PatchMapping("/update")
    public Result update(@Valid @RequestBody UpdateProfileRequest update,HttpServletRequest request){
        Long currentUserId = UserContext.getUserId();
        cn.bvovd.clouddrive.vo.UserProfileVo updated = userService.update(currentUserId,update);
        return Result.success("更新成功", updated);
    }
    @PatchMapping("/updatePassword")
    public Result updatePassword(@Valid @RequestBody UpdatePasswordRequest passwordRequest){
        Long userId = UserContext.getUserId();
        userService.updatePassword(userId,passwordRequest);
        //需要在前端进行判断，收到返回码10001后就跳转到登录页面
        return Result.success(10001,"密码修改成功，请重新登录",null);
    }

}
