package cn.bvovd.clouddrive.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")  // 指定表名
public class User {

    @TableId(type = IdType.AUTO)  // 自增主键
    private Long id;

    @TableField(updateStrategy = FieldStrategy.NEVER)  // 手机号一旦设置不可修改
    private String phone;

    private String nickname;
    private String email;

    @TableField(updateStrategy = FieldStrategy.NEVER)  // 密码一般不直接更新，用专门方法
    private String passwordHash;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer role; // 0-用户，1-管理员

    @TableField("total_space")
    private Long totalSpace;

    @TableField("used_space")
    private Long usedSpace;

    @TableField("monthly_download_limit")
    private Long monthlyDownloadLimit;

    @TableField("used_download_traffic")
    private Long usedDownloadTraffic;

    @TableField("traffic_reset_time")
    private LocalDateTime trafficResetTime;

    private Integer status; // 0-正常，1-冻结

    @TableField("login_failed_count")
    private Integer loginFailedCount;

    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    // ★ 自动填充：插入时自动填充
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ★ 自动填充：插入和更新时自动填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ★ 逻辑删除字段（配合 MP 的逻辑删除功能）
    @TableLogic(value = "NULL", delval = "NOW()")
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime deletedAt;
}