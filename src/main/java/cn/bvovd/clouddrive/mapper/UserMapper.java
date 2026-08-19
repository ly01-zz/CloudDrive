package cn.bvovd.clouddrive.mapper;

import cn.bvovd.clouddrive.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 带行锁查询用户（防止并发扣减空间时数据不一致）
     */
    @Select("SELECT * FROM users WHERE id = #{id} FOR UPDATE")
    User selectByIdForUpdate(Long id);

    /**
     * 统计用户总空间/已用空间（自定义 SQL，需手动过滤逻辑删除）
     */
    @Select("SELECT COALESCE(SUM(total_space), 0) AS total_space, COALESCE(SUM(used_space), 0) AS used_space " +
            "FROM users WHERE deleted_at IS NULL")
    Map<String, Object> selectSpaceStats();

    /**
     * 月度流量批量清零（每月 1 号定时任务调用）
     * 只更新"流量月份锚点不在本月"的用户，避免重复执行时做无意义的更新；
     * traffic_reset_time 同步为当月 1 号，保证与下载时的"懒重置"逻辑一致
     *
     * @return 受影响（即被清零）的用户数
     */
    @Update("UPDATE users SET used_download_traffic = 0, " +
            "traffic_reset_time = DATE_FORMAT(NOW(), '%Y-%m-01 00:00:00') " +
            "WHERE deleted_at IS NULL " +
            "AND (traffic_reset_time IS NULL " +
            "     OR DATE_FORMAT(traffic_reset_time, '%Y-%m') != DATE_FORMAT(NOW(), '%Y-%m'))")
    int resetMonthlyTraffic();
}