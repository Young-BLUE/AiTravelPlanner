package com.www.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AnthropicConfig {

    private static final String ENV_KEY = "ANTHROPIC_API_KEY";

    /** 기본값은 ANTHROPIC_API_KEY 환경변수. 테스트에서는 프로퍼티로 덮어쓴다. */
    @Value("${www.claude.api-key:}")
    private String apiKey;

    /**
     * API 키는 서버에만 두고 프론트로 절대 내려보내지 않는다.
     *
     * 키가 없으면 빈을 만들지 않고 데모 모드로 뜬다. 미리 생성해 둔 일정은 그대로
     * 조회되고, 새로 만들어야 하는 요청만 막힌다. 클론해서 바로 돌려볼 수 있게 하려는 것이다.
     *
     * 예전에는 키가 없으면 기동을 중단시켰다. 그건 SDK 가 자격증명을 첫 요청에야 확인해서
     * "서버는 멀쩡히 뜨는데 모든 요청이 502" 가 되는 것을 막으려던 조치였다.
     * 데모 모드는 그 조용한 실패와 다르다 - 기동 로그와 화면 배너, 요청별 응답에서
     * 축소 모드임을 계속 알린다.
     */
    @Bean
    public AnthropicClient anthropicClient() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("""

                    ============================================================
                     {} 가 없어 데모 모드로 시작합니다.

                     · 미리 생성된 일정 조회  → 정상 동작
                     · 새 일정/추천 생성      → 차단 (안내 메시지 반환)

                     전체 기능을 쓰려면:
                       export {}=sk-ant-...
                       키 발급 https://platform.claude.com/settings/keys
                    ============================================================
                    """, ENV_KEY, ENV_KEY);
            return null;
        }

        log.info("Claude 클라이언트 초기화 완료 (키 길이 {})", apiKey.length());
        return AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }
}
