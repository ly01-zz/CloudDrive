package cn.bvovd.clouddrive.service.impl;

import cn.bvovd.clouddrive.entity.SystemConfig;
import cn.bvovd.clouddrive.exception.BusinessException;
import cn.bvovd.clouddrive.mapper.SystemConfigMapper;
import cn.bvovd.clouddrive.service.SystemConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    @Override
    public SystemConfig getByKey(String key) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = this.getOne(wrapper);
        if (config == null) {
            throw new BusinessException("系统配置项不存在: " + key);
        }
        return config;
    }
}