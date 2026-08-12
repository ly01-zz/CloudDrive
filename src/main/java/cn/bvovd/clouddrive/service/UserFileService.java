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
}
