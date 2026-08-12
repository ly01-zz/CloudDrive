package cn.bvovd.clouddrive.service.impl;

import cn.bvovd.clouddrive.dto.LoginRequest;
import cn.bvovd.clouddrive.dto.UpdatePasswordRequest;
import cn.bvovd.clouddrive.vo.LoginVo;
import cn.bvovd.clouddrive.dto.RegisterRequest;
import cn.bvovd.clouddrive.dto.UpdateProfileRequest;
import cn.bvovd.clouddrive.entity.SystemConfig;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.entity.UserFile;
import cn.bvovd.clouddrive.exception.BusinessException;
import cn.bvovd.clouddrive.mapper.UserFileMapper;
import cn.bvovd.clouddrive.mapper.UserMapper;
import cn.bvovd.clouddrive.service.SystemConfigService;
import cn.bvovd.clouddrive.service.UserService;
import cn.bvovd.clouddrive.utils.JwtUtil;
import cn.bvovd.clouddrive.vo.UserProfileVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final UserFileMapper userFileMapper;
    private final JwtUtil jwtUtil;
    private final SystemConfigService systemConfigService;  // 下面会定义
    private final BCryptPasswordEncoder passwordEncoder;




    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId, Long adminId) {
        // 1. 不能禁用自己
        if (userId.equals(adminId)) {
            throw new BusinessException("不能禁用当前登录的管理员账号");
        }

        // 2. 查询用户（加锁，防止并发状态变更）
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 1) {
            // 已经是禁用状态，幂等处理，不报错但可提示
            throw new BusinessException("该用户已被禁用");
            // 或者直接返回成功，按业务需求决定
        }

        // 3. 更新状态为冻结
        user.setStatus(1);
        // 可选：同时清空登录失败计数（因为冻结了，解锁后重置更合理）
        user.setLoginFailedCount(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);

        // 可选：记录操作日志（扩展）
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId, Long adminId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("该用户已是正常状态");
        }

        // 更新为正常
        user.setStatus(0);
        // 重置失败计数和锁定时间
        user.setLoginFailedCount(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);
    }
    @Override
    public void updatePassword(Long userId, UpdatePasswordRequest passwordRequest) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(passwordRequest.getOpassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        if (!Objects.equals(passwordRequest.getPassword(), passwordRequest.getPasswordTwo())) {
            throw new BusinessException("两次密码不一致，请重新输入！");
        }
        String password = passwordEncoder.encode(passwordRequest.getPassword());
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("id",userId);
        wrapper.set("password_hash",password);
        boolean updated = this.update(wrapper);
        if (!updated){
            throw new BusinessException("修改失败，请重试");
        }

    }

    @Override
    public UserProfileVo update(Long userId, UpdateProfileRequest update) {

        User user = this.getById(userId);
        if (user==null){
            throw new BusinessException("用户不存在");
        }
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("id",userId);
        if (update.getNickname()!=null){
            wrapper.set("nickname",update.getNickname());
        }
        if (update.getEmail() != null) {
            wrapper.set("email", update.getEmail());
        }
        if (update.getAvatarUrl() != null) {
            wrapper.set("avatar_url", update.getAvatarUrl());
        }
        // 2. 执行更新（updated_at 字段由 MyMetaObjectHandler 自动填充）
        boolean updated = this.update(wrapper);
        if (!updated) {
            throw new BusinessException("更新失败，请重试");
        }
        // 3. ★ 重新查询最新数据（确保 updated_at 也拿到最新的）
        User updatedUser = this.getById(userId);

        // 4. 组装 VO 返回
        UserProfileVo vo = new UserProfileVo();
        vo.setId(updatedUser.getId());
        vo.setPhone(updatedUser.getPhone());
        vo.setNickname(updatedUser.getNickname());
        vo.setEmail(updatedUser.getEmail());
        vo.setAvatarUrl(updatedUser.getAvatarUrl());
        vo.setTotalSpace(updatedUser.getTotalSpace());
        vo.setUsedSpace(updatedUser.getUsedSpace());
        vo.setUpdatedAt(updatedUser.getUpdatedAt());

        return vo;
    }

    @Override
    public LoginVo login(LoginRequest login, HttpServletRequest request) {
        // 1. 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, login.getPhone());
        User user = userMapper.selectOne(wrapper);

        // 2. 用户不存在
        if (user == null) {
            throw new BusinessException("该账号不存在");
        }
        if (user.getStatus()==1){
            throw new BusinessException("该账号已被封禁，请联系管理员解封");
        }
        // 3. ★ 检查账户是否被锁定
        if (user.getLockedUntil() != null &&
                user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockedUntil());
            throw new BusinessException("账户已被锁定，请 " + (minutes + 1) + " 分钟后重试");
        }

        // 4. 校验密码
        if (!passwordEncoder.matches(login.getPassword(), user.getPasswordHash())) {
            // ★ 密码错误：失败次数 +1，达到阈值则锁定
            int failedCount = (user.getLoginFailedCount() == null ? 0 : user.getLoginFailedCount()) + 1;
            user.setLoginFailedCount(failedCount);

            if (failedCount >= 5) {
                // 锁定 15 分钟
                user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            }
            userMapper.updateById(user);
            throw new BusinessException("密码错误");
        }

        // 5. ★ 登录成功：重置失败计数、解锁、更新登录信息
        user.setLoginFailedCount(0);
        user.setLockedUntil(null);
        user.setLastLoginIp(request.getRemoteAddr());
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 6. 组装返回 VO
        LoginVo userVo = new LoginVo();
        userVo.setId(user.getId());
        userVo.setPhone(user.getPhone());
        userVo.setNickname(user.getNickname());
        userVo.setAvatarUrl(user.getAvatarUrl());
        userVo.setTotalSpace(user.getTotalSpace());
        userVo.setUsedSpace(user.getUsedSpace());
        userVo.setMonthlyDownloadLimit(user.getMonthlyDownloadLimit());
        userVo.setUsedDownloadTraffic(user.getUsedDownloadTraffic());

        // TODO: 后续加上 JWT Token
         userVo.setToken(jwtUtil.generateToken(user.getId(), user.getRole(),user.getPhone()));

        return userVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(RegisterRequest request) {
        // 1. 检查是否已满员
        SystemConfig maxLimitConfig = systemConfigService.getByKey("max_user_limit");
        long limit = Long.parseLong(maxLimitConfig.getConfigValue());

        // ★ MP 查询：统计未删除的用户数（逻辑删除自动过滤 deleted_at IS NULL）
        long currentCount = this.count();
        if (currentCount >= limit) {
            throw new BusinessException("系统注册名额已满，请联系管理员");
        }

        // 2. 检查手机号是否已注册（使用 LambdaQueryWrapper）
        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getPhone, request.getPhone());
        if (this.getOne(phoneWrapper) != null) {
            throw new BusinessException("该手机号已被注册");
        }

        // 3. 校验邮箱唯一性
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(User::getEmail, request.getEmail());
            if (this.getOne(emailWrapper) != null) {
                throw new BusinessException("该邮箱已被使用");
            }
        }

        // 4. 读取默认配置（空间、流量）
        long defaultSpace = Long.parseLong(
                systemConfigService.getByKey("default_space").getConfigValue()
        );
        long monthlyTraffic = Long.parseLong(
                systemConfigService.getByKey("monthly_traffic_limit").getConfigValue()
        );

        // 5. 构建用户对象
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + request.getPhone().substring(7));
        user.setEmail(request.getEmail());
        user.setRole(0);
        user.setTotalSpace(defaultSpace);
        user.setUsedSpace(0L);
        user.setMonthlyDownloadLimit(monthlyTraffic);
        user.setUsedDownloadTraffic(0L);
        user.setTrafficResetTime(LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
        user.setStatus(0);
        user.setLoginFailedCount(0);
        user.setLockedUntil(null);

        // ★ 6. 保存用户（MP 的 save 方法）
        this.save(user);

        // 7. 初始化根目录
        UserFile rootFolder = new UserFile();
        rootFolder.setUserId(user.getId());
        rootFolder.setParentId(0L);
        rootFolder.setName("/");
        rootFolder.setIsFolder(true);
        rootFolder.setFileSize(0L);
        rootFolder.setStoragePath(null);
        rootFolder.setFileMd5(null);
        rootFolder.setMimeType(null);
        rootFolder.setDownloadCount(0);
        userFileMapper.insert(rootFolder);  // 也可以用 userFileService.save(rootFolder)

        return user;
    }
}