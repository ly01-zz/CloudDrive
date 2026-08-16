package cn.bvovd.clouddrive.service;

import cn.bvovd.clouddrive.entity.Announcement;

import java.util.List;

public interface AnnouncementService {

    /**
     * 发布公告（广播给全部用户，无需登录即可感知）
     */
    void publish(Long adminId, String title, String content);

    /**
     * 公告列表（按时间倒序，管理端）
     */
    List<Announcement> listAll();

    /**
     * 下架公告
     */
    void offline(Long id);

    /**
     * 删除公告
     */
    void delete(Long id);

    /**
     * 最新一条发布中的公告（用户端拉取，广播不记录已读）
     */
    Announcement latest();
}
