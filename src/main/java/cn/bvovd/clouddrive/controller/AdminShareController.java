package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.service.ShareService;
import cn.bvovd.clouddrive.vo.ShareInfoVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/share")
@RequiredArgsConstructor
public class AdminShareController {

    private final ShareService shareService;

    /**
     * 所有分享列表（含创建者信息，最新在前）
     */
    @GetMapping("/list")
    public Result<List<ShareInfoVo>> listAllShares() {
        UserContext.requireAdmin();
        List<ShareInfoVo> voList = shareService.listAllShares();
        return Result.success("获取成功", voList);
    }

    /**
     * 强制取消分享（置为已取消）
     */
    @PutMapping("/{shareId}/cancel")
    public Result<String> forceCancelShare(@PathVariable Long shareId) {
        UserContext.requireAdmin();
        shareService.forceCancelShare(shareId);
        return Result.success("已强制取消");
    }
}
