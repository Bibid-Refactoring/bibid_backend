package bibid.controller.banner;

import bibid.dto.BannerDto;
import bibid.dto.ResponseDto;
import bibid.service.banner.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
@Slf4j
public class BannerController {

    private final BannerService bannerService;

    /**
     * 1) 페이징 없이 전체 배너 조회
     *    GET /api/banners/all
     */
    @GetMapping({"/all"})
    public ResponseEntity<?> getAllBannersNoPaging() {
    	
    	log.info("[Backend] GET /api/banners/all 호출됨");
        ResponseDto<BannerDto> responseDto = new ResponseDto<>();
        
        try {
            List<BannerDto> list = bannerService.findAllNoPaging();
            log.info("[Backend] findAllNoPaging 결과 개수: {}", list.size()); // 반환된 리스트 길이 로그
            
            responseDto.setItems(list);
            responseDto.setStatusCode(HttpStatus.OK.value());
            responseDto.setStatusMessage("ok");
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("getAllBannersNoPaging error: {}", e.getMessage());
            responseDto.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDto.setStatusMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(responseDto);
        }
    }

    /**
     * 2) 단일 배너 생성
     *    POST /api/banners
     *    Request Body → JSON 형태의 BannerDto (id 제외)
     */
    @PostMapping
    public ResponseEntity<?> createBanner(@Valid @RequestBody BannerDto bannerDto) {
    	
        ResponseDto<BannerDto> responseDto = new ResponseDto<>();
        
        try {
            log.info("createBanner request: {}", bannerDto);
            BannerDto savedDto = bannerService.create(bannerDto);

            responseDto.setItem(savedDto);
            responseDto.setStatusCode(HttpStatus.CREATED.value());
            responseDto.setStatusMessage("created");
            return ResponseEntity
                    .created(new URI("/api/banners/" + savedDto.getId()))
                    .body(responseDto);
        } catch (Exception e) {
            log.error("createBanner error: {}", e.getMessage());
            responseDto.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDto.setStatusMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(responseDto);
        }
    }

    /**
     * 3) 단일 배너 수정
     *    PUT /api/banners/{id}
     *    Request Body → JSON 형태의 BannerDto (id는 @PathVariable로)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBanner(
            @PathVariable Long id,
            @Valid @RequestBody BannerDto bannerDto
    ) {
        ResponseDto<BannerDto> responseDto = new ResponseDto<>();
        try {
            log.info("updateBanner id={}, dto={}", id, bannerDto);

            bannerDto.setId(id);
            BannerDto updatedDto = bannerService.update(id, bannerDto);

            responseDto.setItem(updatedDto);
            responseDto.setStatusCode(HttpStatus.OK.value());
            responseDto.setStatusMessage("updated");
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException re) {
            log.error("updateBanner not found: {}", re.getMessage());
            responseDto.setStatusCode(HttpStatus.NOT_FOUND.value());
            responseDto.setStatusMessage(re.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDto);
        } catch (Exception e) {
            log.error("updateBanner error: {}", e.getMessage());
            responseDto.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDto.setStatusMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(responseDto);
        }
    }

    /**
     * 4) 단일 배너 삭제
     *    DELETE /api/banners/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBanner(@PathVariable Long id) {
        ResponseDto<BannerDto> responseDto = new ResponseDto<>();
        try {
            log.info("deleteBanner id={}", id);
            bannerService.delete(id);

            responseDto.setStatusCode(HttpStatus.NO_CONTENT.value());
            responseDto.setStatusMessage("deleted");
            // 본문이 필요 없으므로 .noContent().build() 사용해도 무방
            return ResponseEntity.noContent().build();
        } catch (RuntimeException re) {
            log.error("deleteBanner not found: {}", re.getMessage());
            responseDto.setStatusCode(HttpStatus.NOT_FOUND.value());
            responseDto.setStatusMessage(re.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDto);
        } catch (Exception e) {
            log.error("deleteBanner error: {}", e.getMessage());
            responseDto.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDto.setStatusMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(responseDto);
        }
    }

    /**
     * 5) 단일 배너 조회
     *    GET /api/banners/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBannerById(@PathVariable Long id) {
        ResponseDto<BannerDto> responseDto = new ResponseDto<>();
        try {
        	log.info("getBannerById id={}", id);
            BannerDto dto = bannerService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Banner not found. id=" + id));

            responseDto.setItem(dto);
            responseDto.setStatusCode(HttpStatus.OK.value());
            responseDto.setStatusMessage("ok");
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException re) {
            log.error("getBannerById not found: {}", re.getMessage());
            responseDto.setStatusCode(HttpStatus.NOT_FOUND.value());
            responseDto.setStatusMessage(re.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDto);
        } catch (Exception e) {
            log.error("getBannerById error: {}", e.getMessage());
            responseDto.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDto.setStatusMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(responseDto);
        }
    }
}