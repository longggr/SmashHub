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
    PASSWORD_ATTEMPT_EXCEEDED(1011,"Incorrect password entered too many times. Your account has been locked for %d seconds.", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_NOT_EXISTED(1012, "Please provide an valid email!", HttpStatus.NOT_FOUND),
    PASSWORD_CONFIRM_NOT_MATCH(1013,"Password confirm not match",HttpStatus.BAD_REQUEST),
    PASSWORD_SAME_AS_OLD(1014, "New password must be different from old password", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(1015, "Invalid or expired token", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTED(1016, "This email is already registered!", HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_EXISTS(1017, "Phone number already exists!", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1018, "Role has not been seeded!", HttpStatus.NOT_FOUND),
    USERNAME_EXISTED(1024, "Username existed", HttpStatus.BAD_REQUEST),
    OTP_INVALID(1019, "Invalid or expired OTP code", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_VERIFIED(1020, "This account has already been verified", HttpStatus.BAD_REQUEST),
    OTP_RESEND_TOO_SOON(1021, "Please wait a while before requesting a new OTP", HttpStatus.TOO_MANY_REQUESTS),
    MAIL_SEND_FAILED(1022, "Failed to send verification email", HttpStatus.INTERNAL_SERVER_ERROR),
    ACCOUNT_TEMPORARILY_LOCKED(1023, "Account temporarily locked due to too many failed login attempts. Please try again in %d seconds.", HttpStatus.TOO_MANY_REQUESTS),

    // Catalog module
    BRAND_NOT_FOUND(2001, "Brand not found", HttpStatus.NOT_FOUND),
    BRAND_SLUG_EXISTED(2002, "Brand slug already exists", HttpStatus.BAD_REQUEST),
    BRAND_NAME_INVALID(2003, "Brand name must not be blank", HttpStatus.BAD_REQUEST),
    BRAND_SLUG_INVALID(2004, "Brand slug must not be blank", HttpStatus.BAD_REQUEST),

    CATEGORY_NOT_FOUND(2010, "Category not found", HttpStatus.NOT_FOUND),
    CATEGORY_SLUG_EXISTED(2011, "Category slug already exists", HttpStatus.BAD_REQUEST),
    CATEGORY_PARENT_NOT_FOUND(2012, "Parent category not found", HttpStatus.NOT_FOUND),
    CATEGORY_SELF_PARENT(2013, "A category cannot be its own parent", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_CHILDREN(2014, "Cannot delete a category that still has sub-categories", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_INVALID(2015, "Category name must not be blank", HttpStatus.BAD_REQUEST),
    CATEGORY_SLUG_INVALID(2016, "Category slug must not be blank", HttpStatus.BAD_REQUEST),

    PRODUCT_NOT_FOUND(2020, "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_SLUG_EXISTED(2021, "Product slug already exists", HttpStatus.BAD_REQUEST),
    PRODUCT_NAME_INVALID(2022, "Product name must not be blank", HttpStatus.BAD_REQUEST),
    PRODUCT_SLUG_INVALID(2023, "Product slug must not be blank", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_INVALID(2024, "Product price must be greater than 0", HttpStatus.BAD_REQUEST),
    PRODUCT_TYPE_INVALID(2025, "Product type must not be null", HttpStatus.BAD_REQUEST),
    PRODUCT_CATEGORY_INVALID(2026, "categoryId must not be null", HttpStatus.BAD_REQUEST),
    PRODUCT_BRAND_INVALID(2027, "brandId must not be null", HttpStatus.BAD_REQUEST),
    PRODUCT_STATUS_INVALID(2028, "Product status must not be null", HttpStatus.BAD_REQUEST),
    PRODUCT_DESCRIPTION_INVALID(2029, "Product description must not be blank", HttpStatus.BAD_REQUEST),

    PRODUCT_COLOR_NOT_FOUND(2030, "Product color not found", HttpStatus.NOT_FOUND),
    PRODUCT_COLOR_NAME_INVALID(2031, "Color name must not be blank", HttpStatus.BAD_REQUEST),

    PRODUCT_VARIANT_NOT_FOUND(2040, "Product variant not found", HttpStatus.NOT_FOUND),
    PRODUCT_VARIANT_SKU_EXISTED(2041, "SKU already exists", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_SKU_INVALID(2042, "SKU must not be blank", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_STOCK_INVALID(2043, "Stock must not be negative", HttpStatus.BAD_REQUEST),

    PRODUCT_IMAGE_NOT_FOUND(2050, "Product image not found", HttpStatus.NOT_FOUND),
    PRODUCT_IMAGE_URL_INVALID(2051, "Image url must not be blank", HttpStatus.BAD_REQUEST),

    PRODUCT_ATTRIBUTE_NOT_FOUND(2060, "Product attribute not found", HttpStatus.NOT_FOUND),
    PRODUCT_ATTR_NAME_INVALID(2061, "Attribute name must not be blank", HttpStatus.BAD_REQUEST),
    PRODUCT_ATTR_VALUE_INVALID(2062, "Attribute value must not be blank", HttpStatus.BAD_REQUEST),

    // File module
    FILE_EMPTY(2100, "Uploaded file must not be empty", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_SUPPORTED(2101, "Only image files (jpg, jpeg, png, webp) are supported", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(2102, "File size must not exceed 5MB", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(2103, "Failed to upload file to Cloudinary", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED(2104, "Failed to delete file on Cloudinary", HttpStatus.INTERNAL_SERVER_ERROR)
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