package com.example.pg.common.config;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.common.exception.dto.ErrorResponse;
import com.example.pg.common.exception.dto.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리.
 * REST 요청(JSON): ErrorResponse 반환.
 * MVC 요청(HTML): 에러 페이지 렌더링 또는 리다이렉트.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionAdvice {

    private static final String ERROR_VIEW = "error/error";

    /** ErrorCode 기반 비즈니스 예외 */
    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException e, HttpServletRequest request) {
        ErrorCode ec = e.getErrorCode();
        String message = ec.formatMessage(e.getArgs());
        log.warn("BusinessException: {} - {}", ec.getCode(), message);

        if (isApiRequest(request)) {
            // 결제 실패: 가맹점 프론트에서 "승인 실패" 페이지·재시도 버튼 분기용
            if (ec == ErrorCode.PAYMENT_CREATION_FAILED) {
                return ResponseEntity
                        .status(ec.getStatus())
                        .body(ErrorResponse.ofPaymentFailure(ec.getCode(), message, true, "RETRY_PAYMENT", null));
            }
            if (ec == ErrorCode.AUTHORIZATION_START_FAILED) {
                String paymentId = (e.getArgs() != null && e.getArgs().length > 0) ? String.valueOf(e.getArgs()[0]) : null;
                return ResponseEntity
                        .status(ec.getStatus())
                        .body(ErrorResponse.ofPaymentFailure(ec.getCode(), message, true, "RETRY_AUTHORIZE", paymentId));
            }
            return ResponseEntity
                    .status(ec.getStatus())
                    .body(ErrorResponse.of(ec.getCode(), message));
        }
        return toErrorView(ec.getStatus().value(), ec.getCode(), message);
    }

    /** 400 Bad Request - 잘못된 인자 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Bad request: {}", e.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("BAD_REQUEST", e.getMessage()));
        }
        return toErrorView(400, "BAD_REQUEST", e.getMessage());
    }

    /** 409 Conflict - 상태 불일치 */
    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        log.warn("Conflict: {}", e.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of("CONFLICT", e.getMessage()));
        }
        return toErrorView(409, "CONFLICT", e.getMessage());
    }

    /** 409 Conflict - 낙관적 락 실패 (동시 수정) */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public Object handleOptimisticLock(ObjectOptimisticLockingFailureException e, HttpServletRequest request) {
        String message = ErrorCode.CONCURRENT_MODIFICATION.formatMessage();
        log.warn("Optimistic lock failure: {}", e.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of(ErrorCode.CONCURRENT_MODIFICATION.getCode(), message));
        }
        return toErrorView(409, ErrorCode.CONCURRENT_MODIFICATION.getCode(), message);
    }

    /** 409 Conflict - unique 제약 위반 (사업자번호 중복 등) */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        String message = "이미 저장된 데이터입니다.";
        log.warn("DataIntegrityViolation: {}", e.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of(ErrorCode.DUPLICATE_BUSINESS_NUMBER.getCode(), message));
        }
        return toErrorView(409, ErrorCode.DUPLICATE_BUSINESS_NUMBER.getCode(), message);
    }

    /** 400 Bad Request - @Valid 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<FieldErrorDetail> errors = e.getBindingResult().getFieldErrors().stream()
                .map(err -> new FieldErrorDetail(
                        err.getField(),
                        err.getDefaultMessage() != null ? err.getDefaultMessage() : "invalid"
                ))
                .collect(Collectors.toList());
        log.warn("Validation failed: {}", errors);
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("VALIDATION_FAILED", "입력값 검증에 실패했습니다.", errors));
        }
        return toErrorView(400, "VALIDATION_FAILED", "입력값 검증에 실패했습니다.");
    }

    /** 404 - 핸들러 없음 (잘못된 URL) */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNoHandlerFound(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("No handler found: {}", e.getRequestURL());
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of("NOT_FOUND", "요청하신 경로를 찾을 수 없습니다."));
        }
        return toErrorView(404, "NOT_FOUND", "요청하신 경로를 찾을 수 없습니다.");
    }

    /** 404 - 정적 리소스 없음 (Spring 6.1+, 경로가 리소스로 해석됐으나 없을 때) */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("No resource found: {}", e.getResourcePath());
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of("NOT_FOUND", "요청하신 경로를 찾을 수 없습니다."));
        }
        return toErrorView(404, "NOT_FOUND", "요청하신 경로를 찾을 수 없습니다.");
    }

    /** 500 - 그 외 예외 */
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception", e);
        String code = ErrorCode.INTERNAL.getCode();
        String message = ErrorCode.INTERNAL.formatMessage();
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.of(code, message));
        }
        return toErrorView(500, code, message);
    }

    /**
     * JSON 응답을 할 요청인지 판단.
     * Accept에 text/html이 있으면 브라우저 요청으로 보고 HTML(에러 뷰) 반환.
     * 명시적으로 application/json만 요청한 경우에만 JSON 반환.
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept == null) {
            return false;
        }
        if (accept.contains("text/html")) {
            return false;
        }
        return accept.contains("application/json");
    }

    private ModelAndView toErrorView(int statusCode, String code, String message) {
        ModelAndView mav = new ModelAndView(ERROR_VIEW);
        mav.setStatus(HttpStatus.valueOf(statusCode));
        mav.addObject("statusCode", statusCode);
        mav.addObject("code", code);
        mav.addObject("message", message);
        return mav;
    }
}
