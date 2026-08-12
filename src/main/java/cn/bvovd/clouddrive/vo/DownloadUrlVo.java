package cn.bvovd.clouddrive.vo;

import lombok.Data;

@Data
public class DownloadUrlVo {
    private String downloadUrl;   // COS 预签名 URL
    private String fileName;      // 原始文件名（用于前端下载时命名）
    private Long fileSize;        // 文件大小
    private Long expireTime;      // URL 过期时间戳（秒）
}