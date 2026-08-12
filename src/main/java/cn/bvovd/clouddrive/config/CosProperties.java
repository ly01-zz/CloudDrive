package cn.bvovd.clouddrive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tencent.cos")
public class CosProperties {
    private String secretId;
    private String secretKey;
    private String region;
    private String bucketName;
    private Integer durationSeconds = 1800;
    private Long maxFileSize = 104857600L; // 默认 100MB
}