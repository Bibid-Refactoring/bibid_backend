package bibid.repository.banner;

import bibid.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    /**
     * publicId가 중복인지 확인하고 싶다면
     */
    boolean existsByPublicId(String publicId);
}
