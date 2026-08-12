package cn.bvovd.clouddrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFolderRequest {

    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 255, message = "文件夹名称长度不能超过255个字符")
    private String name;

    @NotNull(message = "父文件夹ID不能为空")
    private Long parentId; // 0 表示在根目录下创建
}