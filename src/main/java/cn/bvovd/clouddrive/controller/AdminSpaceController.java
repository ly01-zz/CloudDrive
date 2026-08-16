package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.dto.SpaceApproveRequest;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.entity.SpaceApplication;
import cn.bvovd.clouddrive.service.AdminLogService;
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
    private final AdminLogService adminLogService;
    @GetMapping("/application")
    public Result<IPage<AdminSpaceApplicationVo>> listApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {

        // 权限校验：当前用户必须是管理员（role=1）
        UserContext.requireAdmin();

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
        UserContext.requireAdmin();

        Long adminId = UserContext.getUserId();
        spaceApplicationService.approveApplication(applicationId, request, adminId);
        adminLogService.record(adminId, "APPROVE_APPLICATION", "application", String.valueOf(applicationId),
                "结果:" + (request.getStatus() == 1 ? "通过" : "拒绝") + " " + request.getApproveRemark());
        return Result.success("审批完成");
    }
}
