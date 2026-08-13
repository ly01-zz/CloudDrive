package cn.bvovd.clouddrive.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class CreateShareRequest {

    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    private String extractCode;              // 自定义提取码（可选，4-6位数字/字母）

    @Range(min = 0, max = 1, message = "分享类型必须为0(公开)或1(私密)")
    private Integer shareType = 0;           // 默认公开

    private Integer maxVisits;               // 最大访问次数（可选）
    private Integer maxDownloads;            // 最大下载次数（可选）
    private Long maxDownloadSize;            // 最大下载流量（字节，可选）

    @Range(min = 1, max = 365, message = "有效期天数在1~365之间")
    private Integer expireDays = 7;          // 默认7天
}