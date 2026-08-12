package cn.bvovd.clouddrive.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SpaceApplicationVo {
    private Long id;
    private Long applySize;          // 申请大小（字节）
    private String applySizeDesc;    // 格式化显示（如 "2.5 GB"）
    private Long originalTotal;      // 申请前总空间
    private String originalTotalDesc;
    private String reason;
    private Integer status;          // 0-待审批 1-通过 2-拒绝
    private String statusDesc;       // 状态文字描述
    private String approveRemark;    // 审批意见
    private LocalDateTime applyTime;
    private LocalDateTime approveTime;
}