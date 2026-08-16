package cn.bvovd.clouddrive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcements")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private Integer status;        // 0-发布中，1-已下架
    private Long createdBy;        // 发布管理员ID

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
