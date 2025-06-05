package bibid.dto;

import bibid.entity.Banner;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BannerDto
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerDto {

    private Long id;

    @NotBlank(message = "publicId는 필수 입력 항목입니다.")
    @Size(max = 255, message = "publicId는 최대 255자까지 가능합니다.")
    private String publicId;

    @Size(max = 255, message = "title은 최대 255자까지 가능합니다.")
    private String title;

    @Size(max = 500, message = "linkUrl은 최대 500자까지 가능합니다.")
    private String linkUrl;

    @Min(value = 0, message = "displayOrder는 0 이상의 숫자여야 합니다.")
    private Integer displayOrder;

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static BannerDto fromEntity(Banner banner) {
        return BannerDto.builder()
                .id(banner.getId())
                .publicId(banner.getPublicId())
                .title(banner.getTitle())
                .linkUrl(banner.getLinkUrl())
                .displayOrder(banner.getDisplayOrder())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .createdAt(banner.getCreateTime().toLocalDateTime())
                .updatedAt(banner.getUpdateTime().toLocalDateTime())
                .build();
    }

    /**
     * DTO → Entity 변환
     */
    public Banner toEntity() {
        return Banner.builder()
                .publicId(this.publicId)
                .title(this.title)
                .linkUrl(this.linkUrl)
                .displayOrder(this.displayOrder)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .build();
    }
}