package bibid.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 설정 클래스
 * JPAQueryFactory를 스프링 빈으로 등록하여 DI를 통해 어디서든 주입받아 사용할 수 있게 함
 */
@Configuration
public class QueryDslConfiguration {

    /**
     * JPA의 EntityManager를 주입받기 위한 어노테이션
     * PersistenceContext: Spring에서 EntityManager를 관리해줌
     * EntityManager: JPA의 핵심 객체로, DB와의 모든 상호작용을 담당
     */
    @PersistenceContext
    private EntityManager entityManager;

    // JPAQueryFactory: QueryDSL에서 JPQL 대신 타입 세이프한 쿼리를 작성할 수 있게 해주는 클래스
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
