package cn.bvovd.clouddrive.dto;

import lombok.Data;

@Data
public class ResetTrafficRequest {
    private String reason; // 重置原因（可选，建议填写）
}
