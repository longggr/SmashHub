package org.example.smashhub.exception;

public class AppException extends RuntimeException{
    private ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * Dung khi can nhet gia tri dong (VD: so giay con lai) vao message,
     * thay vi dung nguyen message tinh trong ErrorCode. code/status van
     * lay tu errorCode nhu binh thuong.
     */
    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
