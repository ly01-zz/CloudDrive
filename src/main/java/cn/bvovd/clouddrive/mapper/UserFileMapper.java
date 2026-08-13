package cn.bvovd.clouddrive.mapper;

import cn.bvovd.clouddrive.entity.UserFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserFileMapper extends BaseMapper<UserFile> {

    /**
     * 物理删除文件记录（绕过逻辑删除拦截器，用于回收站永久删除）
     */
    @Delete("<script>" +
            "DELETE FROM files WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int physicalDeleteByIds(@Param("ids") List<Long> ids);

    /**
     * 查询文件记录（含回收站中已逻辑删除的，getById 会自动过滤已删除记录）
     */
    @Select("SELECT * FROM files WHERE id = #{id} AND user_id = #{userId}")
    UserFile selectIncludingDeleted(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 递归查询某文件（含已逻辑删除的）的所有子孙 ID（含自身）
     */
    @Select("""
            WITH RECURSIVE descendants AS (
                SELECT id FROM files WHERE id = #{rootId} AND user_id = #{userId}
                UNION ALL
                SELECT f.id FROM files f
                INNER JOIN descendants d ON f.parent_id = d.id
                WHERE f.user_id = #{userId}
            )
            SELECT id FROM descendants
            """)
    List<Long> selectDescendantIdsIncludingDeleted(@Param("rootId") Long rootId, @Param("userId") Long userId);

    /**
     * 恢复文件：将逻辑删除标记置回 NULL（绕过逻辑删除拦截器，用于回收站恢复）
     */
    @Update("<script>" +
            "UPDATE files SET deleted_at = NULL WHERE user_id = #{userId} AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int restoreByIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /**
     * 修改文件父目录（用于父文件夹被永久删除时恢复到根目录）
     */
    @Update("UPDATE files SET parent_id = #{newParentId} WHERE id = #{id} AND user_id = #{userId}")
    int updateParentId(@Param("id") Long id, @Param("userId") Long userId, @Param("newParentId") Long newParentId);

    /**
     * 查询未完成上传的文件记录（upload_status = 0）
     */
    @Select("SELECT * FROM files WHERE id = #{id} AND user_id = #{userId} AND upload_status = 0")
    UserFile selectPendingUpload(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 回收站列表（自定义 SQL 绕开 MP 逻辑删除拦截器，否则会自动追加 deleted_at IS NULL）
     */
    @Select("SELECT * FROM files WHERE user_id = #{userId} AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    List<UserFile> selectRecycleList(@Param("userId") Long userId);

    /**
     * 按 ID 批量查询 COS 存储路径（含已逻辑删除的记录，用于永久删除时清理 COS 对象）
     */
    @Select("<script>" +
            "SELECT storage_path FROM files WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<String> selectStoragePathsByIds(@Param("ids") List<Long> ids);
}
