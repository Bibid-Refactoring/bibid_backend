package bibid.common;

import bibid.dto.AuctionImageDto;
import bibid.dto.ProfileImageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

// 파일 업로드와 관련된 공통 로직을 담은 유틸리티 클래스
@Component
public class FileUtils {

    // application.properties 또는 application-dev.properties에서 주입받는 S3 버킷 이름
    @Value("${cloud.aws.s3.bucket.name}")
    private String bucket;

    // AWS SDK v2의 S3 클라이언트
    private final S3Client s3;

    // 생성자 주입 방식으로 S3Client 주입
    public FileUtils(S3Client s3Client) {
        this.s3 = s3Client;
    }

    /**
     * 프로필 이미지 업로드 처리 메서드
     * S3에 업로드 후 ProfileImageDto를 생성하여 반환한다.
     *
     * @param multipartFile 업로드할 파일
     * @param directory     저장 경로 (예: "profile/")
     * @return ProfileImageDto
     */
    public ProfileImageDto parserFileInfo(MultipartFile multipartFile, String directory) {
        // 파일 업로드 (S3에 저장하고 고유 파일명 반환)
        String fileName = uploadFile(multipartFile, directory);

        // 업로드된 파일 정보를 기반으로 DTO 생성
        ProfileImageDto dto = new ProfileImageDto();
        dto.setNewfilename(fileName);
        dto.setOriginalname(multipartFile.getOriginalFilename());
        dto.setFilesize(multipartFile.getSize());
        dto.setFilepath(directory);
        dto.setFiletype(
                multipartFile.getContentType() != null && multipartFile.getContentType().startsWith("image")
                        ? "image"
                        : "etc"
        );
        return dto;
    }

    /**
     * 경매 이미지 업로드 처리 메서드
     * S3에 업로드 후 AuctionImageDto를 생성하여 반환한다.
     *
     * @param multipartFile 업로드할 파일
     * @param directory     저장 경로 (예: "auction/")
     * @return AuctionImageDto
     */
    public AuctionImageDto auctionImageParserFileInfo(MultipartFile multipartFile, String directory) {
        // 파일 업로드 (S3에 저장하고 고유 파일명 반환)
        String fileName = uploadFile(multipartFile, directory);

        // 업로드된 파일 정보를 기반으로 DTO 생성
        AuctionImageDto dto = new AuctionImageDto();
        dto.setFilename(fileName);
        dto.setFileoriginname(multipartFile.getOriginalFilename());
        dto.setFilepath(directory);
        dto.setFilesize(multipartFile.getSize());
        dto.setFiletype(
                multipartFile.getContentType() != null && multipartFile.getContentType().startsWith("image")
                        ? "image"
                        : "etc"
        );
        return dto;
    }

    /**
     * 파일을 S3(R2 포함) 버킷에 업로드하고 고유 파일명을 반환한다.
     *
     * @param multipartFile 업로드할 파일
     * @param directory     저장 디렉토리 (예: "profile/", "auction/")
     * @return S3에 저장된 고유 파일명
     */
    private String uploadFile(MultipartFile multipartFile, String directory) {
        // 현재 시간 + UUID + 원본 파일명으로 고유 파일명 생성
        String now = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String uuid = UUID.randomUUID().toString();
        String originalName = multipartFile.getOriginalFilename();
        String fileName = uuid + "_" + now + "_" + originalName;
        String key = directory + fileName;

        // S3에 파일 업로드
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)                                 // 업로드 대상 버킷 이름
                    .key(key)                                       // 객체 키 (S3 내 저장될 경로+파일명)
                    .contentType(multipartFile.getContentType())    // MIME 타입 (예: image/jpeg)
                    .acl("public-read")                             // 접근 권한: 공개 읽기
                    .build();

            s3.putObject(putRequest, RequestBody.fromBytes(multipartFile.getBytes()));
        } catch (IOException e) {
            System.out.println("파일 업로드 실패: " + e.getMessage());
        }

        return fileName;
    }
}
