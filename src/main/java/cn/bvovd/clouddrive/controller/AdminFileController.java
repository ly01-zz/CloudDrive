package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.service.AdminFileService;
import cn.bvovd.clouddrive.vo.AdminFileVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/file")
@RequiredArgsConstructor
public class AdminFileController {

    private final AdminFileService adminFileService;

    /**
     * 全局文件治理列表（含已逻辑删除，按文件名/手机号/类型/回收站筛选）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Boolean isFolder,
            @RequestParam(required = false) Integer deleted,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        UserContext.requireAdmin();

        List<AdminFileVo> list = adminFileService.listFiles(keyword, phone, isFolder, deleted, page, size);
        long total = adminFileService.countFiles(keyword, phone, isFolder, deleted);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return Result.success("获取成功", result);
    }

    /**
     * 物理删除文件（级联子孙 + 秒传共享对象保护）
     */
    @DeleteMapping("/{fileId}/purge")
    public Result<String> purge(@PathVariable Long fileId) {
        UserContext.requireAdmin();
        adminFileService.deleteFile(fileId);
        return Result.success("已永久删除");
    }

    /**
     * 恢复回收站文件（级联子孙）
     */
    @PutMapping("/{fileId}/restore")
    public Result<String> restore(@PathVariable Long fileId) {
        UserContext.requireAdmin();
        adminFileService.restoreFile(fileId);
        return Result.success("已恢复");
    }
}
