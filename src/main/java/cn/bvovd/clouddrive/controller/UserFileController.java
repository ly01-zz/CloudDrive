package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.dto.CreateFolderRequest;
import cn.bvovd.clouddrive.dto.UploadCredentialRequest;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.entity.UserFile;
import cn.bvovd.clouddrive.service.UserFileService;
import cn.bvovd.clouddrive.vo.DownloadUrlVo;
import cn.bvovd.clouddrive.vo.UploadCredentialVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class UserFileController {

    private final UserFileService userFileService;

    /** 创建文件夹 */
    @PostMapping("/folder")
    public Result createFolder(@Valid @RequestBody CreateFolderRequest request) {
        Long userId = UserContext.getUserId();
        userFileService.createFolder(userId, request);
        return Result.success("创建成功");
    }

    /** 列出文件 */
    @GetMapping("/list")
    public Result listFiles(@RequestParam(required = false, defaultValue = "0") Long parentId) {
        Long userId = UserContext.getUserId();
        List<UserFile> fileList = userFileService.list(userId, parentId);
        return Result.success("获取成功", fileList);
    }

    /** 获取上传凭证（STS 临时密钥） */
    @PostMapping("/upload/credential")
    public Result getUploadCredential(@Valid @RequestBody UploadCredentialRequest request) {
        Long userId = UserContext.getUserId();
        UploadCredentialVo vo = userFileService.getUploadCredential(userId, request);
        return Result.success("获取上传凭证成功", vo);
    }

    /** 上传完成回调（前端直传 COS 成功后调用） */
    @PostMapping("/upload/callback")
    public Result confirmUpload(@RequestParam Long fileId) {
        Long userId = UserContext.getUserId();
        userFileService.confirmUpload(userId, fileId);
        return Result.success("上传完成");
    }
    /**下载文件 **/
    @GetMapping("/download/{fileId}")
    public Result getDownloadUrl(@PathVariable Long fileId) {
        Long userId = UserContext.getUserId();
        DownloadUrlVo vo = userFileService.getDownloadUrl(userId, fileId);
        return Result.success("获取下载链接成功", vo);
    }
    /** 逻辑删除（移入回收站），文件夹级联删除子孙 */
    @DeleteMapping("/recycle/{fileId}")
    public Result deleteToRecycle(@PathVariable Long fileId) {
        Long userId = UserContext.getUserId();
        userFileService.deleteToRecycle(userId, fileId);
        return Result.success("删除成功，文件已移至回收站，15天后自动清理");
    }

    /** 永久删除（仅回收站中的文件生效），文件夹级联删除子孙 */
    @DeleteMapping("/purge/{fileId}")
    public Result deletePermanently(@PathVariable Long fileId) {
        Long userId = UserContext.getUserId();
        userFileService.deletePermanently(userId, fileId);
        return Result.success("文件已永久删除");
    }

    /** 回收站列表 */
    @GetMapping("/recycle")
    public Result listRecycle() {
        Long userId = UserContext.getUserId();
        List<UserFile> recycleList = userFileService.listRecycle(userId);
        return Result.success("获取成功", recycleList);
    }

    /** 恢复文件（移出回收站），文件夹级联恢复子孙 */
    @PutMapping("/restore/{fileId}")
    public Result restoreFile(@PathVariable Long fileId) {
        Long userId = UserContext.getUserId();
        userFileService.restoreFile(userId, fileId);
        return Result.success("恢复成功");
    }

    /** 取消上传：清理未完成上传的记录并回滚空间（前端直传 COS 失败时调用） */
    @DeleteMapping("/upload/pending/{fileId}")
    public Result cancelUpload(@PathVariable Long fileId) {
        Long userId = UserContext.getUserId();
        userFileService.cancelUpload(userId, fileId);
        return Result.success("已取消上传");
    }
}
