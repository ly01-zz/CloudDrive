package cn.bvovd.clouddrive.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminSpaceApplicationVo {
    private Long id;
    private Long userId;           // 申请人ID
    private String phone;          // 手机号
    private String nickname;       // 昵称
    private Long applySize;
    private String applySizeDesc;  // 格式化大小
    private Long originalTotal;
    private String originalTotalDesc;
    private String reason;
    private Integer status;
    private String statusDesc;     // 待审批/已通过/已拒绝
    private String approveRemark;
    private Long adminId;          // 审批人ID（可能为空）
    private LocalDateTime applyTime;
    private LocalDateTime approveTime;
}