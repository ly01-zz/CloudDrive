package cn.bvovd.clouddrive.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserProfileVo {
    private Long id;
    private String phone;
    private String nickname;
    private String email;          // 新增，用于展示
    private String avatarUrl;
    private Long totalSpace;
    private Long usedSpace;
    private LocalDateTime updatedAt; // 让前端知道最近修改时间
}