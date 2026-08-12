package cn.bvovd.clouddrive.vo;

import lombok.Data;

@Data
public class LoginVo {
    private Long id;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private Long totalSpace;
    private Long usedSpace;
    private Long monthlyDownloadLimit;
    private Long usedDownloadTraffic;
    private String token;
}
