package cn.bvovd.clouddrive.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminFileVo {
    private Long id;
    private Long userId;           // 所属用户ID
    private String phone;          // 所属用户手机号
    private String nickname;       // 所属用户昵称
    private Long parentId;
    private String name;
    private Boolean isFolder;
    private Long fileSize;
    private String fileSizeDesc;
    private String storagePath;
    private Integer uploadStatus;  // 0-上传中，1-已完成
    private Integer downloadCount;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt; // null-正常，非空-回收站
}
