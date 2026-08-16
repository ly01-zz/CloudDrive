package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.entity.Announcement;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 最新公告（登录后广播展示，不记录已读；无公告时 data 为 null）
     */
    @GetMapping("/latest")
    public Result<Announcement> latest() {
        return Result.success("获取成功", announcementService.latest());
    }
}
