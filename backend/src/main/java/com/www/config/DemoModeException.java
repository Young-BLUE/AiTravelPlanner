package com.www.config;

/** 데모 모드에서 새로 생성해야 하는 요청이 들어왔을 때. */
public class DemoModeException extends RuntimeException {

    public DemoModeException() {
        super("데모 모드에서는 미리 만들어 둔 일정만 볼 수 있습니다.");
    }
}
