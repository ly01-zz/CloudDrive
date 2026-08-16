package cn.bvovd.clouddrive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_log")
public class AdminLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;           // 操作管理员ID
    private String action;          // 操作类型（如 DISABLE_USER / RESET_TRAFFIC / UPDATE_CONFIG）
    private String targetType;      // 操作对象类型（user / share / config / application / file）
    private String targetId;        // 操作对象ID
    private String reason;          // 操作原因/备注
    private String ipAddress;       // 操作IP

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
