package cn.bvovd.clouddrive.controller;

import cn.bvovd.clouddrive.context.UserContext;
import cn.bvovd.clouddrive.entity.Result;
import cn.bvovd.clouddrive.entity.SystemConfig;
import cn.bvovd.clouddrive.exception.BusinessException;
import cn.bvovd.clouddrive.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 配置项列表
     */
    @GetMapping("/list")
    public Result<List<SystemConfig>> listAll() {
        UserContext.requireAdmin();
        return Result.success("获取成功", systemConfigService.list());
    }

    /**
     * 修改配置项（实时生效：业务逻辑均为每次请求时读取，无启动缓存）
     */
    @PutMapping("/{configKey}")
    public Result<String> update(@PathVariable String configKey, @RequestBody SystemConfig config) {
        UserContext.requireAdmin();
        SystemConfig existing = systemConfigService.getById(configKey);
        if (existing == null) {
            throw new BusinessException("配置项不存在");
        }
        if (!StringUtils.hasText(config.getConfigValue())) {
            throw new BusinessException("配置值不能为空");
        }
        existing.setConfigValue(config.getConfigValue());
        existing.setDescription(config.getDescription());
        systemConfigService.updateById(existing);
        return Result.success("修改成功");
    }

    /**
     * 新增配置项（注意：需在代码中接入该配置键后才真正生效）
     */
    @PostMapping
    public Result<String> add(@RequestBody SystemConfig config) {
        UserContext.requireAdmin();
        if (!StringUtils.hasText(config.getConfigKey())) {
            throw new BusinessException("配置键不能为空");
        }
        if (systemConfigService.getById(config.getConfigKey()) != null) {
            throw new BusinessException("配置项已存在");
        }
        systemConfigService.save(config);
        return Result.success("新增成功");
    }

    /**
     * 删除配置项
     */
    @DeleteMapping("/{configKey}")
    public Result<String> delete(@PathVariable String configKey) {
        UserContext.requireAdmin();
        systemConfigService.removeById(configKey);
        return Result.success("删除成功");
    }
}
