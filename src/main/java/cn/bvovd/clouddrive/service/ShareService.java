package cn.bvovd.clouddrive.service;


import cn.bvovd.clouddrive.dto.CreateShareRequest;
import cn.bvovd.clouddrive.entity.ShareLink;
import cn.bvovd.clouddrive.vo.DownloadUrlVo;
import cn.bvovd.clouddrive.vo.ShareInfoVo;

import java.util.List;

public interface ShareService  {
    /**
     * 创建分享链接
     */
    ShareLink createShare(CreateShareRequest request);

    /**
     * 获取分享信息（不校验登录）
     */
    ShareInfoVo getShareInfo(String shareCode);

    /**
     * 下载分享文件（需登录，扣用户流量）
     * @param shareCode 分享码
     * @param extractCode 提取码（私密分享必填，公开可不填）
     * @param clientIp 客户端IP
     * @param userAgent 用户代理
     */
    DownloadUrlVo downloadSharedFile(String shareCode, String extractCode, String clientIp, String userAgent);

    /**
     * 查询当前用户创建的分享列表（最新在前）
     */
    List<ShareInfoVo> listMyShares(Long userId);

    /**
     * 取消分享（创建者）
     */
    void cancelShare(Long shareId);

    /**
     * 校验分享有效性（内部使用）
     */
    void validateShare(ShareLink link);
}
