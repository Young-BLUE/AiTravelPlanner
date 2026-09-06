package com.wayfarer.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * React SPA 라우팅 지원.
 * /result 같은 클라이언트 라우트로 직접 접속하거나 새로고침해도 index.html을 돌려준다.
 * 확장자가 있는 요청(정적 파일)과 /api, /assets 로 시작하는 요청은 제외한다.
 */
@Controller
public class SpaForwardingConfig {

    private static final String NOT_API = "^(?!api$|assets$)[^.]*";

    @GetMapping(value = {"/{path:" + NOT_API + "}", "/{path:" + NOT_API + "}/{sub:[^.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
