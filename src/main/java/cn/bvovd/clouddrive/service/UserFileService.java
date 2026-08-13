package cn.bvovd.clouddrive.service;

import cn.bvovd.clouddrive.dto.CreateFolderRequest;
import cn.bvovd.clouddrive.dto.UploadCredentialRequest;
import cn.bvovd.clouddrive.entity.UserFile;
import cn.bvovd.clouddrive.vo.DownloadUrlVo;
import cn.bvovd.clouddrive.vo.UploadCredentialVo;

import java.util.List;

public interface UserFileService {

    void createFolder(Long userId, CreateFolderRequest request);

    List<UserFile> list(Long userId, Long parentId);

    /** 获取上传凭证（临时密钥 + COS 路径） */
    UploadCredentialVo getUploadCredential(Long userId, UploadCredentialRequest request);

    /** 上传完成回调：前端直传 COS 成功后调用，更新文件状态 */
    void confirmUpload(Long userId, Long fileId);

    DownloadUrlVo getDownloadUrl(Long userId, Long fileId);

    /** 逻辑删除（移入回收站），文件夹会级联删除其所有子孙 */
    void deleteToRecycle(Long userId, Long fileId);

    /** 永久删除（仅回收站中的文件生效），文件夹会级联删除其所有子孙 */
    void deletePermanently(Long userId, Long fileId);

    /** 回收站列表（含已逻辑删除的文件），按删除时间倒序 */
    List<UserFile> listRecycle(Long userId);

    /** 恢复文件（移出回收站），文件夹会级联恢复其所有子孙 */
    void restoreFile(Long userId, Long fileId);

    /** 取消上传：清理未完成上传的文件记录并回滚预扣空间（前端直传失败时调用） */
    void cancelUpload(Long userId, Long fileId);
}
