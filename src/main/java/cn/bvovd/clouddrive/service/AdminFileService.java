package cn.bvovd.clouddrive.service;

import cn.bvovd.clouddrive.vo.AdminFileVo;

import java.util.List;

public interface AdminFileService {

    /**
     * 管理端分页查询文件（含已逻辑删除）
     *
     * @param keyword 文件名关键字（模糊）
     * @param phone   所属用户手机号（精确）
     * @param isFolder 是否文件夹
     * @param deleted 0-仅正常，1-仅回收站，null-全部
     */
    List<AdminFileVo> listFiles(String keyword, String phone, Boolean isFolder, Integer deleted, int page, int size);

    /**
     * 管理端统计文件数量（条件与 listFiles 一致）
     */
    long countFiles(String keyword, String phone, Boolean isFolder, Integer deleted);

    /**
     * 管理端物理删除文件（级联子孙 + COS 共享对象保护）
     */
    void deleteFile(Long fileId);

    /**
     * 管理端恢复回收站文件（级联子孙）
     */
    void restoreFile(Long fileId);
}
