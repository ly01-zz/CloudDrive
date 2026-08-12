package cn.bvovd.clouddrive.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("space_applications")
public class SpaceApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("apply_size")
    private Long applySize; // 申请增加的空间（字节）

    @TableField("original_total")
    private Long originalTotal; // 申请前的总空间

    @TableField("reason")
    private String reason;

    @TableField("status")
    private Integer status; // 0-待审批，1-已通过，2-已拒绝

    @TableField("admin_id")
    private Long adminId; // 审批的管理员ID

    @TableField("approve_remark")
    private String approveRemark;

    // ★ 申请时间：插入时自动填充（由 MyMetaObjectHandler 处理）
    @TableField(value = "apply_time", fill = FieldFill.INSERT)
    private LocalDateTime applyTime;

    // ★ 审批时间：手动设置（管理员审批时由代码赋值），不需要自动填充
    @TableField("approve_time")
    private LocalDateTime approveTime;
}