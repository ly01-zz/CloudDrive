package cn.bvovd.clouddrive.vo;

import lombok.Data;

@Data
public class UploadCredentialVo {
    private String cosKey;          // COS 存储路径
    private String uploadId;        // 本次上传的唯一标识（文件记录ID）
    private String tmpSecretId;     // 临时密钥 ID[reference:4]
    private String tmpSecretKey;    // 临时密钥 Key[reference:5]
    private String sessionToken;    // 安全令牌[reference:6]
    private Long startTime;         // 密钥生效时间（秒级时间戳）
    private Long expiredTime;       // 密钥过期时间（秒级时间戳）
}