package cn.bvovd.clouddrive.service.impl;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.dto.SpaceApplyRequest;
import cn.bvovd.clouddrive.dto.SpaceApproveRequest;
import cn.bvovd.clouddrive.entity.SpaceApplication;
import cn.bvovd.clouddrive.entity.User;
import cn.bvovd.clouddrive.exception.BusinessException;
import cn.bvovd.clouddrive.mapper.SpaceApplicationMapper;
import cn.bvovd.clouddrive.mapper.UserMapper;
import cn.bvovd.clouddrive.service.SpaceApplicationService;
import cn.bvovd.clouddrive.service.UserService;
import cn.bvovd.clouddrive.vo.AdminSpaceApplicationVo;
import cn.bvovd.clouddrive.vo.SpaceApplicationVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceApplicationMapper, SpaceApplication> implements SpaceApplicationService {
    private final UserService userService;
    private final SpaceApplicationMapper spaceApplicationMapper;
    private final UserMapper userMapper;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveApplication(Long applicationId, SpaceApproveRequest request, Long adminId) {
        // 1. 加行锁查询申请记录（防止并发重复审批）
        SpaceApplication app = spaceApplicationMapper.selectByIdForUpdate(applicationId);
        if (app == null) {
            throw new BusinessException("申请记录不存在");
        }
        if (app.getStatus() != 0) {
            throw new BusinessException("该申请已被处理，请勿重复操作");
        }

        // 2. 更新申请记录
        app.setStatus(request.getStatus());
        app.setApproveRemark(request.getApproveRemark());
        app.setAdminId(adminId);
        app.setApproveTime(LocalDateTime.now());
        spaceApplicationMapper.updateById(app);

        // 3. 如果审批通过 → 增加用户总空间
        if (request.getStatus() == 1) {
            // 锁定用户行，防止并发更新导致空间超额
            User user = userMapper.selectByIdForUpdate(app.getUserId());
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            // 更新总空间（直接累加）
            user.setTotalSpace(user.getTotalSpace() + app.getApplySize());
            userMapper.updateById(user);

            // 可选：记录操作日志（可扩展）
            // log.info("管理员 {} 批准用户 {} 扩容 {} 字节", adminId, user.getId(), app.getApplySize());
        }

        // 如果是拒绝，无需修改用户空间，仅记录状态
    }

    @Override
    public IPage<AdminSpaceApplicationVo> queryAllApplications(IPage<SpaceApplication> page, Integer status) {
        // 1. 构建查询条件
        LambdaQueryWrapper<SpaceApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SpaceApplication::getStatus, status);
        }
        wrapper.orderByDesc(SpaceApplication::getApplyTime);

        // 2. 分页查询申请记录
        IPage<SpaceApplication> entityPage = this.page(page, wrapper);

        if (entityPage.getRecords().isEmpty()) {
            IPage<AdminSpaceApplicationVo> emptyPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 3. 提取所有 userId，批量查询用户信息
        List<Long> userIds = entityPage.getRecords().stream()
                .map(SpaceApplication::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));

        // 4. 转换为 VO
        List<AdminSpaceApplicationVo> voList = entityPage.getRecords().stream()
                .map(app -> {
                    AdminSpaceApplicationVo vo = new AdminSpaceApplicationVo();
                    vo.setId(app.getId());
                    vo.setUserId(app.getUserId());
                    User user = userMap.get(app.getUserId());
                    if (user != null) {
                        vo.setPhone(user.getPhone());
                        vo.setNickname(user.getNickname());
                    }
                    vo.setApplySize(app.getApplySize());
                    vo.setApplySizeDesc(formatFileSize(app.getApplySize()));
                    vo.setOriginalTotal(app.getOriginalTotal());
                    vo.setOriginalTotalDesc(formatFileSize(app.getOriginalTotal()));
                    vo.setReason(app.getReason());
                    vo.setStatus(app.getStatus());
                    vo.setStatusDesc(getStatusDesc(app.getStatus()));
                    vo.setApproveRemark(app.getApproveRemark());
                    vo.setAdminId(app.getAdminId());
                    vo.setApplyTime(app.getApplyTime());
                    vo.setApproveTime(app.getApproveTime());
                    return vo;
                })
                .collect(Collectors.toList());

        IPage<AdminSpaceApplicationVo> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }



    @Override
    public IPage<SpaceApplicationVo> queryMyApplications(IPage<SpaceApplication> page, Integer status) {
        Long userId = UserContext.getUserId();

        // 1. 构建查询条件
        LambdaQueryWrapper<SpaceApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpaceApplication::getUserId, userId)
                .orderByDesc(SpaceApplication::getApplyTime); // 最新申请在前

        if (status != null) {
            wrapper.eq(SpaceApplication::getStatus, status);
        }

        // 2. 执行分页查询（这里 page 是 MyBatis-Plus 的 Page 对象）
        IPage<SpaceApplication> entityPage = this.page(page, wrapper);

        // 3. 转换为 VO 并添加描述字段
        IPage<SpaceApplicationVo> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(
                entityPage.getRecords().stream()
                        .map(this::convertToVo)
                        .collect(Collectors.toList())
        );
        return voPage;
    }

    // 私有方法：将实体转为 VO，并填充描述字段
    private SpaceApplicationVo convertToVo(SpaceApplication entity) {
        SpaceApplicationVo vo = new SpaceApplicationVo();
        vo.setId(entity.getId());
        vo.setApplySize(entity.getApplySize());
        vo.setApplySizeDesc(formatFileSize(entity.getApplySize()));
        vo.setOriginalTotal(entity.getOriginalTotal());
        vo.setOriginalTotalDesc(formatFileSize(entity.getOriginalTotal()));
        vo.setReason(entity.getReason());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(getStatusDesc(entity.getStatus()));
        vo.setApproveRemark(entity.getApproveRemark());
        vo.setApplyTime(entity.getApplyTime());
        vo.setApproveTime(entity.getApproveTime());
        return vo;
    }

    // 文件大小格式化（简单版，可自行优化）
    private String formatFileSize(Long size) {
        if (size == null || size == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // 状态描述
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待审批";
            case 1: return "已通过";
            case 2: return "已拒绝";
            default: return "未知状态";
        }
    }

    @Override
    public void applySpace(Long userId, SpaceApplyRequest request) {
        // 1. 检查用户状态
        User user = userService.getById(userId);
        if (user.getStatus() == 1) throw new BusinessException("账户已被冻结，无法申请");

        // 2. 检查是否有待审批的申请
        LambdaQueryWrapper<SpaceApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpaceApplication::getUserId, userId)
                .eq(SpaceApplication::getStatus, 0); // 0-待审批
        if (spaceApplicationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("您已有待审批的申请，请勿重复提交");
        }

        // 3. 插入记录
        SpaceApplication app = new SpaceApplication();
        app.setUserId(userId);
        app.setApplySize(request.getApplySize());
        app.setOriginalTotal(user.getTotalSpace());
        app.setReason(request.getReason());
        app.setStatus(0); // 待审批
        app.setApplyTime(LocalDateTime.now());
        spaceApplicationMapper.insert(app);
    }
}
