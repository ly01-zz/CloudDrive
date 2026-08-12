package cn.bvovd.clouddrive.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_config")
public class SystemConfig {
    @TableId  // 主键是 config_key，不是自增
//    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    private String description;
}