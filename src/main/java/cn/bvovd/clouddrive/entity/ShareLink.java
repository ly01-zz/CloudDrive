package cn.bvovd.clouddrive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("share_links")
public class ShareLink {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long fileId;
    private String shareCode;
    private String extractCode;          // 提取码（私有分享）
    private Integer shareType;           // 0-公开（需登录），1-私密（需登录+提取码）
    private Integer accessMode;          // 0-仅下载，预留扩展
    private Integer totalVisits;         // 总访问次数
    private Integer maxVisits;           // 最大访问次数，null表示无限制
    private Integer totalDownloads;      // 累计下载次数
    private Integer maxDownloads;        // 最大下载次数，null表示无限制
    private Long totalDownloadSize;      // 已消耗的下载流量（字节）
    private Long maxDownloadSize;        // 最大下载流量限制（字节），null表示无限制
    private LocalDateTime expireTime;    // 过期时间，null表示永久
    private Integer status;              // 0-正常，1-已过期，2-已取消

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}