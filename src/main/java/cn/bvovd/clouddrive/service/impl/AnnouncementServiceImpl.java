package cn.bvovd.clouddrive.service.impl;

import cn.bvovd.clouddrive.entity.Announcement;
import cn.bvovd.clouddrive.exception.BusinessException;
import cn.bvovd.clouddrive.mapper.AnnouncementMapper;
import cn.bvovd.clouddrive.service.AnnouncementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Override
    public void publish(Long adminId, String title, String content) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new BusinessException("公告标题和内容不能为空");
        }
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setStatus(0); // 发布中
        announcement.setCreatedBy(adminId);
        announcementMapper.insert(announcement);
        log.info("发布公告，ID：{}，标题：{}，操作人：{}", announcement.getId(), title, adminId);
    }

    @Override
    public List<Announcement> listAll() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getCreatedAt);
        return announcementMapper.selectList(wrapper);
    }

    @Override
    public void offline(Long id) {
        Announcement announcement = getById(id);
        announcement.setStatus(1); // 已下架
        announcementMapper.updateById(announcement);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        announcementMapper.deleteById(id);
    }

    @Override
    public Announcement latest() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 0)
                .orderByDesc(Announcement::getCreatedAt)
                .last("LIMIT 1");
        return announcementMapper.selectOne(wrapper);
    }

    private Announcement getById(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        return announcement;
    }
}
