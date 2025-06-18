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

    // properties 파일에 정의된 Cloudflare R2 버킷 이름 값을 주입 받음
    @Value("${cloud.aws.s3.bucket.name}")
    private String bucket;

    // AWS SDK v2의 S3Client를 사용해 R2와 통신
    private final S3Client s3;

    public FileUtils(S3Client s3Client) {
        this.s3 = s3Client;
    }

    // 프로필 이미지 업로드용
    public ProfileImageDto parserFileInfo(MultipartFile multipartFile, String directory) {
        // 파일명 생성
        // 현재 시간 + UUID + 파일 고유명으로 파일명을 재생성 -> 경로명과 합쳐서 key로 전송
        String now = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String uuid = UUID.randomUUID().toString();
        String originalName = multipartFile.getOriginalFilename();
        String fileName = uuid + "_" + now + "_" + originalName;
        String key = directory + fileName;

        // S3 버킷에 파일을 업로드하는 요청 객체 생성
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)                                 // 업로드 대상 버킷 이름
                    .key(key)                                       // 객체 키(버킷 내 저장될 경로와 파일 이름)
                    .contentType(multipartFile.getContentType())    // MIME 타입(예: image/jpeg)
                    .acl("public-read")                          // 접근 권한: 공개 읽기
                    .build();
            s3.putObject(putRequest, RequestBody.fromBytes(multipartFile.getBytes()));
        } catch (IOException e) {
            System.out.println("파일 업로드 실패: " + e.getMessage());
        }

        // 업로드된 파일의 정보(이름, 크기, 타입 등)를 담은 DTO를 생성해 반환
        ProfileImageDto dto = new ProfileImageDto();
        dto.setNewfilename(fileName);
        dto.setOriginalname(originalName);
        dto.setFilesize(multipartFile.getSize());
        dto.setFilepath(directory);
        dto.setFiletype(
                multipartFile.getContentType() != null && multipartFile.getContentType().startsWith("image")
                        ? "image"
                        : "etc"
        );
        return dto;
    }

    // 경매 이미지 업로드용, 위의 메소드와 반환 타입 빼곤 방식이 동일함
    public AuctionImageDto auctionImageParserFileInfo(MultipartFile multipartFile, String directory) {
        String now = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String uuid = UUID.randomUUID().toString();
        String originalName = multipartFile.getOriginalFilename();
        String fileName = uuid + "_" + now + "_" + originalName;
        String key = directory + fileName;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(multipartFile.getContentType())
                    .acl("public-read")
                    .build();

            s3.putObject(putRequest, RequestBody.fromBytes(multipartFile.getBytes()));

        } catch (IOException e) {
            System.out.println("파일 업로드 실패: " + e.getMessage());
        }

        AuctionImageDto dto = new AuctionImageDto();
        dto.setFilename(fileName);
        dto.setFileoriginname(originalName);
        dto.setFilepath(directory);
        dto.setFilesize(multipartFile.getSize());
        dto.setFiletype(
                multipartFile.getContentType() != null && multipartFile.getContentType().startsWith("image")
                        ? "image"
                        : "etc"
        );
        return dto;
    }
}
