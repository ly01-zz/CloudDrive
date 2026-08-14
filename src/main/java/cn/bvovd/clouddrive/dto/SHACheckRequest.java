package cn.bvovd.clouddrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SHACheckRequest {
    @NotBlank(message = "文件SHA-256不能为空")
    private String SHA;
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;
    @NotNull(message = "父文件夹ID不能为空")
    private Long parentId;
    @NotBlank(message = "文件名不能为空")
    private String fileName;
    private String mimeType;
}
