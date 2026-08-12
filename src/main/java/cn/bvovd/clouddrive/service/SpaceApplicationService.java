package cn.bvovd.clouddrive.service;

import cn.bvovd.clouddrive.dto.SpaceApplyRequest;
import cn.bvovd.clouddrive.dto.SpaceApproveRequest;
import cn.bvovd.clouddrive.entity.SpaceApplication;
import cn.bvovd.clouddrive.vo.AdminSpaceApplicationVo;
import cn.bvovd.clouddrive.vo.SpaceApplicationVo;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface SpaceApplicationService {
    void applySpace(Long userId, SpaceApplyRequest request);
    IPage<SpaceApplicationVo> queryMyApplications(IPage<SpaceApplication> pageParam, Integer status);
    IPage<AdminSpaceApplicationVo> queryAllApplications(IPage<SpaceApplication> page, Integer status);
    void approveApplication(Long applicationId, SpaceApproveRequest request, Long adminId);
}
