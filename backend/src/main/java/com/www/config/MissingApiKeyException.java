package com.www.config;

/** 기동에 필요한 Claude API 키가 없을 때. {@link MissingApiKeyFailureAnalyzer}가 안내문으로 바꿔준다. */
public class MissingApiKeyException extends RuntimeException {

    public MissingApiKeyException(String envKey) {
        super(envKey + " 환경변수가 설정되지 않았습니다.");
    }
}
