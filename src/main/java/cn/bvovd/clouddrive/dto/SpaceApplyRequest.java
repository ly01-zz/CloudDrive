package cn.bvovd.clouddrive.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class SpaceApplyRequest {
    @NotNull(message = "申请空间大小不能为空")
    @Min(value = 1048576, message = "申请空间至少为1MB") // 最小1MB，防止无效申请
    private Long applySize; // 单位：字节

    @Size(max = 500, message = "申请原因不超过500字")
    private String reason;
}