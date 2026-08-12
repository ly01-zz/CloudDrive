package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.dto.SpaceApplyRequest;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.entity.SpaceApplication;
import cn.bvovd.clouddrive.service.SpaceApplicationService;
import cn.bvovd.clouddrive.vo.SpaceApplicationVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/space")
@RequiredArgsConstructor
public class SpaceApplicationController {
    private final SpaceApplicationService service;
    @PostMapping("/apply")
    public Result apply(@Valid @RequestBody SpaceApplyRequest request){
        Long userId = UserContext.getUserId();
        service.applySpace(userId,request);
        return Result.success("已申请");
    }
    @GetMapping("/my-applications")
    public Result<IPage<SpaceApplicationVo>> getMyApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {

        // 构建分页对象（MyBatis-Plus 的 Page）
        Page<SpaceApplication> pageParam = new Page<>(page, size);
        IPage<SpaceApplicationVo> result = service.queryMyApplications(pageParam, status);
        return Result.success("查询成功", result);
    }
}
