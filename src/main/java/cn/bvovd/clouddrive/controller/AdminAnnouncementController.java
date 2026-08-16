package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.entity.Announcement;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.service.AdminLogService;
import cn.bvovd.clouddrive.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/announcement")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;
    private final AdminLogService adminLogService;

    /**
     * 发布公告（广播给全部用户）
     */
    @PostMapping
    public Result<String> publish(@RequestBody Announcement announcement) {
        UserContext.requireAdmin();
        Long adminId = UserContext.getUserId();
        announcementService.publish(adminId, announcement.getTitle(), announcement.getContent());
        adminLogService.record(adminId, "PUBLISH_ANNOUNCEMENT", "announcement", null,
                "标题:" + announcement.getTitle());
        return Result.success("公告已发布");
    }

    /**
     * 公告列表（按时间倒序）
     */
    @GetMapping("/list")
    public Result<List<Announcement>> listAll() {
        UserContext.requireAdmin();
        return Result.success("获取成功", announcementService.listAll());
    }

    /**
     * 下架公告
     */
    @PutMapping("/{id}/offline")
    public Result<String> offline(@PathVariable Long id) {
        UserContext.requireAdmin();
        announcementService.offline(id);
        return Result.success("已下架");
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        UserContext.requireAdmin();
        announcementService.delete(id);
        return Result.success("已删除");
    }
}
