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
    /** 删除文件-逻辑删除 **/
    @DeleteMapping("/delect")
    public Result delete(){

        return Result.success("删除成功,文件已经移至回收站，15天后删除");
    }
    /** 删除文件-永久删除 **/
    @DeleteMapping("/")
    public Result Pdelete(){
        return Result.success("文件删除成功");
    }
}
