package cn.bvovd.clouddrive.service;

import cn.bvovd.clouddrive.dto.LoginRequest;
import cn.bvovd.clouddrive.dto.UpdatePasswordRequest;
import cn.bvovd.clouddrive.vo.LoginVo;
import cn.bvovd.clouddrive.dto.RegisterRequest;
import cn.bvovd.clouddrive.dto.UpdateProfileRequest;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.vo.UserProfileVo;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;


public interface UserService extends IService<User> {
    LoginVo login(LoginRequest login, HttpServletRequest request);

    User register(RegisterRequest request);

    UserProfileVo update(Long currentId, UpdateProfileRequest update);

    void updatePassword(Long userId, UpdatePasswordRequest passwordRequest);

    void disableUser(Long userId, Long currentAdminId);

    void enableUser(Long userId, Long currentAdminId);

    /** 获取所有用户列表（管理员） */
    java.util.List<User> getAllUsers();

    /** 获取当前登录用户的完整信息（脱敏），用于前端刷新空间/流量等数据 */
    User getUserInfo(Long userId);
}