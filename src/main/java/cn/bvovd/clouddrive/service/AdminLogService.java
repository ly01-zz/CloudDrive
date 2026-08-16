package cn.bvovd.clouddrive.service;

import cn.bvovd.clouddrive.entity.AdminLog;

import java.util.List;

public interface AdminLogService {

    /**
     * 记录管理员操作日志
     *
     * @param adminId    操作管理员ID
     * @param action     操作类型
     * @param targetType 操作对象类型（user/share/config/application/file）
     * @param targetId   操作对象ID
     * @param reason     操作原因/备注
     */
    void record(Long adminId, String action, String targetType, String targetId, String reason);

    /**
     * 查询操作日志（按时间倒序）
     *
     * @param limit 条数
     * @return 日志列表
     */
    List<AdminLog> listLatest(int limit);
}
