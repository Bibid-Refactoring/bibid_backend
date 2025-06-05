package bibid.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Banner 엔티티 (Member 엔티티 스타일 기준으로 적용)
 */
@Entity
@SequenceGenerator(
        name = "bannerSeqGenerator",
        sequenceName = "BANNER_SEQ",
        initialValue = 1,
        allocationSize = 1
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "bannerSeqGenerator"
    )
    private Long id;

    /**
     * Cloudinary/R2에 업로드한 이미지의 public_id
     */
    @NotBlank(message = "publicId는 필수 입력 항목입니다.")
    @Size(max = 255, message = "publicId는 최대 255자까지 가능합니다.")
    @Column(name = "public_id", nullable = false, unique = true, length = 255)
    private String publicId;

    /**
     * 배너 제목 (선택)
     */
    @Size(max = 255, message = "title은 최대 255자까지 가능합니다.")
    private String title;

    /**
     * 클릭 시 이동할 URL (선택)
     */
    @Size(max = 500, message = "linkUrl은 최대 500자까지 가능합니다.")
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    /**
     * 화면에 노출할 순서 (낮은 숫자부터 상단에 표시)
     */
    @Min(value = 0, message = "displayOrder는 0 이상의 숫자여야 합니다.")
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * 배너 노출 시작일 (선택)
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * 배너 노출 종료일 (선택)
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * 생성 시각 (자동 설정)
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private Timestamp createTime;

    /**
     * 최종 수정 시각 (자동 설정)
     */
    @Column(name = "update_time")
    private Timestamp updateTime;

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}