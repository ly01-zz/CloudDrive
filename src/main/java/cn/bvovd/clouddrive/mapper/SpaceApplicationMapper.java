package cn.bvovd.clouddrive.mapper;

import cn.bvovd.clouddrive.entity.SpaceApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SpaceApplicationMapper extends BaseMapper<SpaceApplication> {
    @Select("SELECT * FROM space_applications WHERE id = #{id} FOR UPDATE")
    SpaceApplication selectByIdForUpdate(@Param("id") Long id);
}
