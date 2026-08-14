package cn.bvovd.clouddrive.mapper;

import cn.bvovd.clouddrive.entity.DownloadLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface DownloadLogMapper extends BaseMapper<DownloadLog> {

    /**
     * 统计指定时间之后的下载流量总和（字节）
     */
    @Select("SELECT COALESCE(SUM(download_size), 0) FROM download_logs WHERE created_at >= #{start}")
    Long selectTotalSizeSince(@Param("start") LocalDateTime start);

    /**
     * 按天统计下载次数与流量（用于趋势图）
     */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, " +
            "COUNT(*) AS download_count, COALESCE(SUM(download_size), 0) AS download_size " +
            "FROM download_logs WHERE created_at >= #{start} " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> selectTrendSince(@Param("start") LocalDateTime start);
}
