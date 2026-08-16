package cn.bvovd.clouddrive.dto;

import lombok.Data;

@Data
public class UpdateQuotaRequest {
    private Long totalSpace;            // 调整后的总空间（字节，可选）
    private Long monthlyDownloadLimit;  // 调整后的月度下载流量（字节，可选）
    private String reason;              // 调整原因（可选）
}
