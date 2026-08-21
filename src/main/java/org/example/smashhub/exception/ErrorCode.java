package org.example.smashhub.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    ACCOUNT_LOCKED(1009,"Your account has been locked.Please contact the Admin.", HttpStatus.FORBIDDEN),
    INCORRECT_PASSWORD(1010,"Password incorrect", HttpStatus.NOT_FOUND),
    PASSWORD_ATTEMPT_EXCEEDED(1011,"incorrect password has exceeded the allowed number of attempts", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_NOT_EXISTED(1012, "Please provide an valid email!", HttpStatus.NOT_FOUND),
    PASSWORD_CONFIRM_NOT_MATCH(1013,"Password confirm not match",HttpStatus.BAD_REQUEST),
    PASSWORD_SAME_AS_OLD(1014, "New password must be different from old password", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(1015, "Invalid or expired token", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTED(1016, "This email is already registered!", HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_EXISTS(1017, "Phone number already exists!", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1018, "Role has not been seeded!", HttpStatus.NOT_FOUND),
    USERNAME_EXISTED(1009, "Username existed", HttpStatus.BAD_REQUEST)
    ;

    private int code = 1000;
    private String message;
    private HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
