package org.example.smashhub.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Lỗi không lường trước -> 500, chỉ log chi tiết ở server, không lộ ra client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handlingRuntimeException(Exception exception) {
        log.error("Exception: ", exception);
        return build(ErrorCode.UNCATEGORIZED_EXCEPTION, null);
    }

    /** Lỗi nghiệp vụ do mình chủ động ném. */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handlingAppException(AppException exception) {
        return build(exception.getErrorCode(), exception.getMessage());
    }

    /**
     * Bị từ chối quyền (@PreAuthorize) -> 403.
     * Trước đây bị handler Exception.class nuốt và trả 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handlingAccessDenied(AccessDeniedException exception) {
        return build(ErrorCode.UNAUTHORIZED, null);
    }

    /**
     * Lỗi validation của @Valid.
     * message trong DTO có thể là:
     *  - tên một ErrorCode (vd "PRODUCT_NAME_INVALID") -> dùng code/status/message của ErrorCode đó
     *  - một câu bất kỳ (vd "email must not be blank")  -> trả INVALID_REQUEST kèm đúng câu đó
     * Trước đây ErrorCode.valueOf() ném IllegalArgumentException cho trường hợp thứ hai (và cho key bị thiếu).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {
        String key = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        ErrorCode errorCode = resolveErrorCode(key);
        if (errorCode != null) {
            return build(errorCode, null);
        }
        return build(ErrorCode.INVALID_REQUEST, key);
    }

    /** Validation trên @RequestParam / @PathVariable (khi dùng @Validated). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException exception) {
        String key = exception.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        ErrorCode errorCode = resolveErrorCode(key);
        return errorCode != null ? build(errorCode, null) : build(ErrorCode.INVALID_REQUEST, key);
    }

    /** Body JSON sai cú pháp / enum không hợp lệ -> 400 thay vì 500. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadable(HttpMessageNotReadableException exception) {
        log.debug("Unreadable request body", exception);
        return build(ErrorCode.INVALID_REQUEST, "Malformed request body");
    }

    /** Sai kiểu tham số (vd ?type=ABC với enum, ?id=abc với Long) -> 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return build(ErrorCode.INVALID_REQUEST, "Invalid value for parameter '" + exception.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(MissingServletRequestParameterException exception) {
        return build(ErrorCode.INVALID_REQUEST, "Missing parameter '" + exception.getParameterName() + "'");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException exception) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, null);
    }

    // ----------------------------------------------------------------------------------------

    private ErrorCode resolveErrorCode(String key) {
        if (key == null) return null;
        try {
            return ErrorCode.valueOf(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Gom việc dựng response về một chỗ: customMessage == null thì dùng message mặc định của ErrorCode. */
    private ResponseEntity<ApiResponse<Object>> build(ErrorCode errorCode, String customMessage) {
        ApiResponse<Object> response = new ApiResponse<>();
        response.setCode(errorCode.getCode());
        response.setMessage(customMessage != null ? customMessage : errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getStatusCode() != null ? errorCode.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}