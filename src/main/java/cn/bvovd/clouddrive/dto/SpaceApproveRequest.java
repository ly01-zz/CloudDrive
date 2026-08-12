package cn.bvovd.clouddrive.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class SpaceApproveRequest {

    @NotNull(message = "审批结果不能为空")
    @Range(min = 1, max = 2, message = "审批结果必须为1(通过)或2(拒绝)")
    private Integer status;

    private String approveRemark; // 审批意见，可为空
}