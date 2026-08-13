package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.dto.CreateShareRequest;
import cn.bvovd.clouddrive.entity.ShareLink;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.service.ShareService;
import cn.bvovd.clouddrive.vo.DownloadUrlVo;
import cn.bvovd.clouddrive.vo.ShareInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    /**
     * 创建分享链接
     */
    @PostMapping("/create")
    public Result<ShareLink> createShare(@Valid @RequestBody CreateShareRequest request) {
        ShareLink link = shareService.createShare(request);
        return Result.success("分享创建成功", link);
    }

    /**
     * 获取分享信息（无需登录，WebConfig 中已放行 /share/info/**）
     */
    @GetMapping("/info/{shareCode}")
    public Result<ShareInfoVo> getShareInfo(@PathVariable String shareCode) {
        ShareInfoVo vo = shareService.getShareInfo(shareCode);
        return Result.success("获取成功", vo);
    }

    /**
     * 我的分享列表（需登录）
     */
    @GetMapping("/list")
    public Result<List<ShareInfoVo>> listMyShares() {
        Long userId = UserContext.getUserId();
        List<ShareInfoVo> voList = shareService.listMyShares(userId);
        return Result.success("获取成功", voList);
    }

    /**
     * 下载分享文件（需登录）
     */
    @GetMapping("/download/{shareCode}")
    public Result<DownloadUrlVo> downloadSharedFile(
            @PathVariable String shareCode,
            @RequestParam(required = false) String extractCode,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        DownloadUrlVo vo = shareService.downloadSharedFile(shareCode, extractCode, clientIp, userAgent);
        return Result.success("下载链接生成成功", vo);
    }

    /**
     * 取消分享
     */
    @DeleteMapping("/{shareId}")
    public Result<String> cancelShare(@PathVariable Long shareId) {
        shareService.cancelShare(shareId);
        return Result.success("分享已取消");
    }
}