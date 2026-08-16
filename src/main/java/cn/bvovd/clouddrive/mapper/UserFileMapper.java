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

    /**
     * 统计引用同一 COS 存储路径的有效文件记录数（秒传共享保护）
     * 被删除的记录 deleted_at 非空，天然不参与计数
     */
    @Select("SELECT COUNT(*) FROM files WHERE storage_path = #{storagePath} " +
            "AND upload_status = 1 AND deleted_at IS NULL")
    Long countActiveByStoragePath(@Param("storagePath") String storagePath);

    /**
     * 管理端查询单个文件（含已逻辑删除，无用户约束）
     */
    @Select("SELECT * FROM files WHERE id = #{id}")
    UserFile selectAdminById(@Param("id") Long id);

    /**
     * 管理端分页查询文件（含已逻辑删除）
     * @param deleted 0-仅正常，1-仅回收站，null-全部
     */
    @Select("<script>" +
            "SELECT * FROM files WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='userId != null'> AND user_id = #{userId}</if>" +
            "<if test='isFolder != null'> AND is_folder = #{isFolder}</if>" +
            "<if test='deleted != null and deleted == 0'> AND deleted_at IS NULL</if>" +
            "<if test='deleted != null and deleted == 1'> AND deleted_at IS NOT NULL</if>" +
            " ORDER BY deleted_at IS NOT NULL, created_at DESC LIMIT #{offset}, #{size}" +
            "</script>")
    List<UserFile> selectAdminList(@Param("keyword") String keyword, @Param("userId") Long userId,
                                   @Param("isFolder") Boolean isFolder, @Param("deleted") Integer deleted,
                                   @Param("offset") int offset, @Param("size") int size);

    /**
     * 管理端统计文件数量（条件与 selectAdminList 一致）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM files WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='userId != null'> AND user_id = #{userId}</if>" +
            "<if test='isFolder != null'> AND is_folder = #{isFolder}</if>" +
            "<if test='deleted != null and deleted == 0'> AND deleted_at IS NULL</if>" +
            "<if test='deleted != null and deleted == 1'> AND deleted_at IS NOT NULL</if>" +
            "</script>")
    Long selectAdminCount(@Param("keyword") String keyword, @Param("userId") Long userId,
                          @Param("isFolder") Boolean isFolder, @Param("deleted") Integer deleted);
}
