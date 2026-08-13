package cn.bvovd.clouddrive.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShareInfoVo {
    private Long id;                 // 分享ID（用于取消分享）
    private String shareCode;
    private String extractCode;      // 提取码（仅"我的分享列表"返回，公开查看接口不返回）
    private String fileName;
    private Long fileSize;
    private String fileSizeDesc;
    private Integer shareType;          // 0-公开，1-私密
    private Boolean needExtract;        // 是否需要提取码
    private LocalDateTime expireTime;
    private Integer totalVisits;
    private Integer maxVisits;
    private Integer totalDownloads;
    private Integer maxDownloads;
    private Long maxDownloadSize;
    private String maxDownloadSizeDesc;
    private Integer status;         // 0-正常，1-已过期，2-已取消
    private String statusDesc;      // 正常/已过期/已取消
}