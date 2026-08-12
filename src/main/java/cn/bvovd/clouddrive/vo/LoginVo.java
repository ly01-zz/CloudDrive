package cn.bvovd.clouddrive.vo;

import lombok.Data;

@Data
public class LoginVo {
    private Long id;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private Integer role; // 0-用户，1-管理员（前端用于判断管理后台权限）
    private Long totalSpace;
    private Long usedSpace;
    private Long monthlyDownloadLimit;
    private Long usedDownloadTraffic;
    private String token;
}
