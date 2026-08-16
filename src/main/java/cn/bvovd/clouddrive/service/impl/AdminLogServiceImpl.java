package cn.bvovd.clouddrive.service.impl;

import cn.bvovd.clouddrive.entity.AdminLog;
import cn.bvovd.clouddrive.mapper.AdminLogMapper;
import cn.bvovd.clouddrive.service.AdminLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

    private final AdminLogMapper adminLogMapper;

    @Override
    public void record(Long adminId, String action, String targetType, String targetId, String reason) {
        try {
            AdminLog adminLog = new AdminLog();
            adminLog.setAdminId(adminId);
            adminLog.setAction(action);
            adminLog.setTargetType(targetType);
            adminLog.setTargetId(targetId);
            adminLog.setReason(reason);
            adminLog.setIpAddress(getClientIp());
            adminLogMapper.insert(adminLog);
        } catch (Exception e) {
            // 日志记录失败不影响主流程，仅记录错误日志
            log.warn("记录管理员操作日志失败，action：{}，原因：{}", action, e.getMessage());
        }
    }

    @Override
    public List<AdminLog> listLatest(int limit) {
        LambdaQueryWrapper<AdminLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AdminLog::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500)));
        return adminLogMapper.selectList(wrapper);
    }

    /**
     * 从当前请求中获取客户端 IP（代理场景取 X-Forwarded-For 第一个 IP）
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }
}
