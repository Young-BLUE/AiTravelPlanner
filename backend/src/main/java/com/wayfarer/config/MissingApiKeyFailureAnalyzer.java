package com.wayfarer.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * 키가 없을 때 스택 트레이스 대신 해결 방법을 로그 맨 아래에 띄운다.
 * 스택 트레이스는 이 오류에 아무 정보도 주지 못하고 안내문만 가린다.
 */
public class MissingApiKeyFailureAnalyzer extends AbstractFailureAnalyzer<MissingApiKeyException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MissingApiKeyException cause) {
        return new FailureAnalysis(
                cause.getMessage()
                        + " 키가 없으면 일정 생성 요청이 전부 401로 실패하므로 기동을 중단했습니다.",
                """
                        키를 환경변수로 넣고 다시 실행하세요.

                          export ANTHROPIC_API_KEY=sk-ant-...
                          ./gradlew bootRun -PskipFrontend

                        한 줄로 실행할 수도 있습니다.

                          ANTHROPIC_API_KEY=sk-ant-... ./gradlew bootRun -PskipFrontend

                        키는 https://platform.claude.com/settings/keys 에서 발급합니다.""",
                cause);
    }
}
