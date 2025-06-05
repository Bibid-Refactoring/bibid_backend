package bibid.service.banner;

import bibid.dto.BannerDto;

import java.util.List;
import java.util.Optional;

/**
 * BannerService 인터페이스
 */
public interface BannerService {

    /**
     * 페이징 없이 전체 배너 조회 (displayOrder 오름차순)
     *
     * @return BannerDto 리스트
     */
    List<BannerDto> findAllNoPaging();

    /**
     * 단일 배너 생성
     *
     * @param dto 생성할 배너 정보가 담긴 DTO
     * @return 생성된 배너의 DTO
     */
    BannerDto create(BannerDto dto);

    /**
     * 단일 배너 조회
     *
     * @param id 조회할 배너의 ID
     * @return 조회된 배너가 존재하면 BannerDto, 없으면 Optional.empty()
     */
    Optional<BannerDto> findById(Long id);

    /**
     * 단일 배너 수정
     *
     * @param id  수정할 배너의 ID
     * @param dto 수정할 필드를 담은 DTO
     * @return 수정된 배너의 DTO
     * @throws RuntimeException 배너가 존재하지 않으면 예외 발생
     */
    BannerDto update(Long id, BannerDto dto);

    /**
     * 단일 배너 삭제
     *
     * @param id 삭제할 배너의 ID
     * @throws RuntimeException 배너가 존재하지 않으면 예외 발생
     */
    void delete(Long id);
}