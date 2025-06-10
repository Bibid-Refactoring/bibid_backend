package bibid.oauth2;

import bibid.dto.MemberDto;
import bibid.dto.ResponseDto;
import bibid.entity.Account;
import bibid.entity.CustomUserDetails;
import bibid.entity.Member;
import bibid.entity.SellerInfo;
import bibid.jwt.JwtProvider;
import bibid.repository.member.MemberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor

public class GoogleServiceImpl {

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    // ㅁ [1번] 코드로 구글에서 토큰 받기
    public String saveUserAndGetToken(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken); // Bearer 토큰 형식으로 설정

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        GoogleProfileDto googleProfileDto;
        try {
            googleProfileDto = objectMapper.readValue(response.getBody(), GoogleProfileDto.class);

            Member googleMember = saveOrUpdateMember(googleProfileDto);

            String jwtToken = jwtProvider.createOauthJwt(googleMember);

            System.out.println("jwtToken service:" + jwtToken);
            return jwtToken;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public Member saveOrUpdateMember(GoogleProfileDto googleProfileDto) {
        Member existing = memberRepository.findByEmail(googleProfileDto.getEmail());

        // ✅ 기존 회원이 존재할 경우
        if (existing != null) {
            if (!"Google".equalsIgnoreCase(existing.getOauthType())) {
                // ❌ 로컬 가입자인데 구글 로그인 시도 → 차단 or 연동 안내
                throw new IllegalStateException("이메일이 이미 일반 가입 계정으로 등록되어 있습니다.");
            }
            // ✅ 기존 Google 계정이면 그대로 로그인
            return existing;
        }

        // ✅ 신규 회원 생성
        Member googleMember = Member.builder()
                .memberId(googleProfileDto.getName())
                .email(googleProfileDto.getEmail())
                .name(googleProfileDto.getFamily_name())
                .nickname(googleProfileDto.getName())
                .role("ROLE_USER")
                .oauthType("Google")
                .build();

        // Account, SellerInfo 연동
        googleMember.setAccount(Account.builder().member(googleMember).userMoney("1000000").build());
        googleMember.setSellerInfo(SellerInfo.builder().member(googleMember).build());

        return memberRepository.save(googleMember);
    }

    public MemberDto getMember(String jwtTokenValue) {
        try {
            // JWT에서 memberId 추출
            String memberId = jwtProvider.validateAndGetSubject(jwtTokenValue);

            // Member 조회
            Optional<Member> optionalMember = memberRepository.findByMemberId(memberId);

            if (optionalMember.isPresent()) {
                Member member = optionalMember.get();

                // 비밀번호 필드는 toDto()에서 포함되지 않으므로 안전
                return member.toDto();

            } else {
                throw new RuntimeException("회원 정보를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException("토큰에서 회원 정보를 추출하는 데 실패했습니다.", e);
        }
    }
}


// ㅁ 프로필 이미지 base 64 변환 (수정중)
//    public String convertImageToBase64(String imageUrl) throws Exception {
//        URL url = new URL(imageUrl);
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setDoInput(true);
//        connection.connect();
//
//        InputStream inputStream = connection.getInputStream();
//        byte[] imageBytes = inputStream.readAllBytes();
//        inputStream.close();
//
//        return Base64.getEncoder().encodeToString(imageBytes);
//    }
//







