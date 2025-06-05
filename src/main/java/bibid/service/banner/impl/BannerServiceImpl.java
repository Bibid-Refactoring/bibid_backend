package bibid.service.banner.impl;

import bibid.dto.BannerDto;
import bibid.entity.Banner;
import bibid.repository.banner.BannerRepository;
import bibid.service.auction.AuctionService;
import bibid.service.banner.BannerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    @Autowired
    public BannerServiceImpl(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    /**
     * 페이징 없이 전체 배너 조회 (displayOrder 오름차순)
     */
    public List<BannerDto> findAllNoPaging() {
        List<Banner> entities = bannerRepository.findAll(Sort.by("displayOrder").ascending());
        return entities.stream()
                       .map(BannerDto::fromEntity)
                       .toList();
    }

    /**
     * 단일 생성 (DTO → Entity 변환 후 저장)
     */
    public BannerDto create(BannerDto dto) {
        Banner entity = Banner.builder()
                .publicId(dto.getPublicId())
                .title(dto.getTitle())
                .linkUrl(dto.getLinkUrl())
                .displayOrder(dto.getDisplayOrder())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
        Banner saved = bannerRepository.save(entity);
        return BannerDto.fromEntity(saved);
    }

    /**
     * 단일 조회
     */
    public Optional<BannerDto> findById(Long id) {
        return bannerRepository.findById(id)
                .map(BannerDto::fromEntity);
    }

    /**
     * 단일 수정
     */
    public BannerDto update(Long id, BannerDto dto) {
        Banner existing = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found. id=" + id));

        existing.setPublicId(dto.getPublicId());
        existing.setTitle(dto.getTitle());
        existing.setLinkUrl(dto.getLinkUrl());
        existing.setDisplayOrder(dto.getDisplayOrder());
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());

        Banner updated = bannerRepository.save(existing);
        return BannerDto.fromEntity(updated);
    }

    /**
     * 단일 삭제
     */
    public void delete(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new RuntimeException("Banner not found. id=" + id);
        }
        bannerRepository.deleteById(id);
    }
}