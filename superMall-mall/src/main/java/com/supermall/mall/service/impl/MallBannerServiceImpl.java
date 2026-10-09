package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallBannerDTO;
import com.supermall.mall.entity.MallBanner;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallBannerMapper;
import com.supermall.mall.service.MallBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MallBannerServiceImpl implements MallBannerService {

    private static final int ONLINE = 1;
    private static final int OFFLINE = 0;

    private final MallBannerMapper mallBannerMapper;

    @Override
    public List<MallBanner> list(Integer position, Integer status) {
        return mallBannerMapper.list(position, status);
    }

    @Override
    public List<MallBanner> listEffective(Integer position) {
        return mallBannerMapper.listEffective(position);
    }

    @Override
    public MallBanner getById(Long id) {
        MallBanner banner = mallBannerMapper.getById(id);
        if (banner == null) {
            throw new MallBusinessException("推荐位不存在");
        }
        return banner;
    }

    @Override
    @Transactional
    public Long save(MallBannerDTO dto) {
        validateTimeWindow(dto);

        LocalDateTime now = LocalDateTime.now();
        if (dto.getId() == null) {
            MallBanner banner = MallBanner.builder()
                    .title(dto.getTitle().trim())
                    .image(dto.getImage().trim())
                    .position(dto.getPosition() == null ? 1 : dto.getPosition())
                    .linkType(dto.getLinkType() == null ? 1 : dto.getLinkType())
                    .linkValue(dto.getLinkValue())
                    .sort(dto.getSort() == null ? 0 : dto.getSort())
                    .status(dto.getStatus() == null ? ONLINE : dto.getStatus())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            mallBannerMapper.insert(banner);
            return banner.getId();
        }

        getById(dto.getId());
        mallBannerMapper.update(MallBanner.builder()
                .id(dto.getId())
                .title(dto.getTitle().trim())
                .image(dto.getImage().trim())
                .position(dto.getPosition())
                .linkType(dto.getLinkType())
                .linkValue(dto.getLinkValue())
                .sort(dto.getSort())
                .status(dto.getStatus())
                // 时间窗允许置空（表示不限），但 XML 用的是 <if != null>，
                // 所以这里传空串表达"要清空"是做不到的 —— 明确提示前端用 null 之外的方式。
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .updateTime(now)
                .build());
        return dto.getId();
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        getById(id);
        if (status == null || (status != ONLINE && status != OFFLINE)) {
            throw new MallBusinessException("推荐位状态只能是 1（上线）或 0（下线）");
        }
        mallBannerMapper.updateStatus(id, status);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getById(id);
        mallBannerMapper.deleteById(id);
    }

    private void validateTimeWindow(MallBannerDTO dto) {
        if (dto.getStartTime() != null && dto.getEndTime() != null
                && dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new MallBusinessException("失效时间不能早于生效时间");
        }
    }
}
