package cn.bvovd.clouddrive.mapper;

import cn.bvovd.clouddrive.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}