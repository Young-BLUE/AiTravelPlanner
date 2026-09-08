package com.www.plan;

import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.www.config.DemoModeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /** 입력 검증 실패 - 프론트에 그대로 보여줄 수 있는 메시지. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("입력값을 확인해 주세요.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    /** Claude API 호출량 초과 - 재시도 안내. */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(RateLimitException e) {
        log.warn("Claude API 호출량 초과", e);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("message", "요청이 몰리고 있어요. 잠시 후 다시 시도해 주세요."));
    }

    /**
     * 데모 모드에서 새로 생성해야 하는 요청이 온 경우.
     * 서버 잘못이 아니라 기능이 꺼져 있는 것이므로 무엇이 가능한지 알려준다.
     */
    @ExceptionHandler(DemoModeException.class)
    public ResponseEntity<Map<String, String>> handleDemoMode(DemoModeException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message",
                        "데모 모드예요. 미리 만들어 둔 일정만 볼 수 있어요. "
                                + "도쿄, 오사카, 후쿠오카, 다낭, 방콕, 타이베이를 3박 4일로 검색해 보세요."));
    }

    /**
     * 인증 실패 - 키가 폐기·만료됐거나 잘못된 경우.
     * 기동 시 키 존재는 검사하므로, 여기까지 왔다면 키 자체가 무효라는 뜻이다.
     * 사용자가 재시도해도 절대 낫지 않으니 502가 아닌 500으로 구분하고 로그로 알린다.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException e) {
        log.error("[설정 오류] Claude API 인증 실패 - ANTHROPIC_API_KEY 가 무효합니다. "
                + "재시도로는 해결되지 않으니 키를 확인하세요.", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "서버 설정에 문제가 있어요. 잠시 후 다시 시도해 주세요."));
    }

    /**
     * 400 계열 - 크레딧 소진("credit balance is too low")이 여기로 온다.
     * 운영 중 가장 흔한 중단 사유라 원문 메시지를 로그에 남긴다.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException e) {
        log.error("[설정 오류] Claude API 요청 거부 - 크레딧 잔액이나 요청 형식을 확인하세요: {}",
                e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "일정을 만들 수 없는 상태예요. 잠시 후 다시 시도해 주세요."));
    }

    /** 그 외 Claude API 오류 - 내부 사정은 감추고 재시도만 안내. */
    @ExceptionHandler(AnthropicServiceException.class)
    public ResponseEntity<Map<String, String>> handleAnthropic(AnthropicServiceException e) {
        log.error("Claude API 오류", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "일정을 만드는 중 문제가 생겼어요. 다시 시도해 주세요."));
    }
}
