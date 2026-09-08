package com.www;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 컨텍스트가 뜨는지만 본다.
 *
 * - api-key: AnthropicConfig 가 키 없으면 기동을 막는다. 실제 호출은 하지 않으므로
 *   유효한 키일 필요는 없지만, 없으면 키가 없는 CI 에서 build 가 실패한다.
 * - datasource: 운영 H2 파일을 열면 서버가 떠 있는 동안 테스트가 잠금 충돌로 깨진다.
 */
@SpringBootTest(properties = {
		"www.claude.api-key=test-key-not-used",
		"spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
