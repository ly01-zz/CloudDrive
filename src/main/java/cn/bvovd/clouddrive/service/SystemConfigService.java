package cn.bvovd.clouddrive.service;

import cn.bvovd.clouddrive.entity.SystemConfig;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SystemConfigService extends IService<SystemConfig> {
    SystemConfig getByKey(String key);
}