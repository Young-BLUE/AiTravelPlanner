package com.wayfarer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 컨텍스트가 뜨는지만 본다.
 *
 * AnthropicConfig 가 키 없으면 기동을 막으므로 더미 키를 넣어준다.
 * 이 테스트는 실제 API를 호출하지 않으니 유효한 키일 필요가 없고,
 * 이게 없으면 키가 없는 CI에서 ./gradlew build 가 실패한다.
 */
@SpringBootTest(properties = "wayfarer.claude.api-key=test-key-not-used")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
