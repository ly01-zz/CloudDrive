package cn.bvovd.clouddrive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 开启定时任务（月度流量清零、回收站过期清理，见 task 包）
public class CloudDriveApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudDriveApplication.class, args);
    }

}
