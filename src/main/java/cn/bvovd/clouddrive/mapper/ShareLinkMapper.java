package cn.bvovd.clouddrive.mapper;

import cn.bvovd.clouddrive.entity.ShareLink;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShareLinkMapper extends BaseMapper<ShareLink> {
    /**
     * 根据分享码查询（无锁）
     */
    @Select("SELECT * FROM share_links WHERE share_code = #{shareCode}")
    ShareLink selectByShareCode(@Param("shareCode") String shareCode);

    /**
     * 根据分享码查询（加行锁，用于更新）
     */
    @Select("SELECT * FROM share_links WHERE share_code = #{shareCode} FOR UPDATE")
    ShareLink selectByShareCodeForUpdate(@Param("shareCode") String shareCode);
}
