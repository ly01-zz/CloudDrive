package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.dto.SpaceApproveRequest;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.entity.SpaceApplication;
import cn.bvovd.clouddrive.exception.BusinessException;
import cn.bvovd.clouddrive.service.SpaceApplicationService;
import cn.bvovd.clouddrive.vo.AdminSpaceApplicationVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/space")
@RequiredArgsConstructor
public class AdminSpaceController {
    private final SpaceApplicationService spaceApplicationService;
    @GetMapping("/application")
    public Result<IPage<AdminSpaceApplicationVo>> listApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {

        // 权限校验：当前用户必须是管理员（role=1）
        Integer role = UserContext.getRole();
        if (role == null || role != 1) {
            throw new BusinessException("无权限访问，仅管理员可查看");
        }

        Page<SpaceApplication> pageParam = new Page<>(page, size);
        IPage<AdminSpaceApplicationVo> result = spaceApplicationService.queryAllApplications(pageParam, status);
        return Result.success("查询成功", result);
    }
    /**
     * 审批扩容申请
     * @param applicationId 申请ID（路径参数）
     * @param request 审批参数
     * @return Result
     */
    @PutMapping("/approve/{applicationId}")
    public Result<String> approveApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody SpaceApproveRequest request) {

        // 权限校验：当前用户必须是管理员
        Integer role = UserContext.getRole();
        if (role == null || role != 1) {
            throw new BusinessException("无权限操作，仅管理员可审批");
        }

        Long adminId = UserContext.getUserId();
        spaceApplicationService.approveApplication(applicationId, request, adminId);
        return Result.success("审批完成");
    }
}
