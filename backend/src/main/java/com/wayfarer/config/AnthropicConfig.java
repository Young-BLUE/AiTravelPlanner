package com.wayfarer.config;

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
    @Value("${wayfarer.claude.api-key:}")
    private String apiKey;

    /**
     * API 키는 서버에만 두고 프론트로 절대 내려보내지 않는다.
     *
     * SDK는 자격증명을 첫 요청 시점에야 해석하므로, 키가 없어도 앱은 멀쩡히 뜨고
     * 사용자가 버튼을 누를 때마다 401 -> 502만 반복된다. 재시도해도 절대 낫지 않는
     * 실패라서, 차라리 기동 단계에서 멈추고 원인을 알려주는 편이 낫다.
     */
    @Bean
    public AnthropicClient anthropicClient() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new MissingApiKeyException(ENV_KEY);
        }

        log.info("Claude 클라이언트 초기화 완료 (키 길이 {})", apiKey.length());
        return AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }
}
