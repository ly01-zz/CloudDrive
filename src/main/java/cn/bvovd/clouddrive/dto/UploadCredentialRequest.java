package cn.bvovd.clouddrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UploadCredentialRequest {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    private Long fileSize;

    private Long parentId = 0L; // 父文件夹ID，默认根目录

    private String sha; // 文件 SHA-256（可选，前端计算后传入，用于秒传缓存）
}