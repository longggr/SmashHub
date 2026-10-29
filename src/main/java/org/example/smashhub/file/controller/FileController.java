package org.example.smashhub.file.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.response.ApiResponse;
import org.example.smashhub.file.dto.FileUploadResponse;
import org.example.smashhub.file.enums.FileFolder;
import org.example.smashhub.file.service.FileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint upload/delete ảnh trực tiếp lên Cloudinary — chỉ dành cho ADMIN/STAFF.
 *
 * <p>Upload qua đây KHÔNG nằm trong transaction DB: nếu cần transaction an toàn,
 * dùng endpoint chuyên biệt của từng module (BrandController, ProductController…).
 *
 * <p>Xóa file: chỉ nhận {@code publicId} — không nhận URL để tránh client tự ý
 * xóa ảnh bất kỳ trên Cloudinary bằng cách giả mạo URL.
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class FileController {

    FileService fileService;

    /**
     * Upload ảnh vào thư mục Cloudinary tương ứng với {@code folder}.
     * Client PHẢI chỉ định folder bằng enum — không nhận đường dẫn tự do.
     *
     * <p>Ví dụ: {@code POST /files/upload?folder=PRODUCT}
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") FileFolder folder) {
        return ApiResponse.<FileUploadResponse>builder()
                .result(fileService.upload(file, folder))
                .message("File uploaded successfully")
                .build();
    }

    /**
     * Xóa ảnh khỏi Cloudinary theo {@code publicId}.
     *
     * <p>Lưu ý: endpoint này xóa ngay lập tức, không kiểm tra xem publicId còn được
     * bản ghi DB nào tham chiếu hay không. Chỉ dùng khi chắc chắn ảnh không còn dùng.
     *
     * <p>Dùng {@code @RequestParam} thay vì {@code @PathVariable} vì public_id của
     * Cloudinary thường chứa dấu '/' (vd: "smashhub/products/uuid"), nếu để path
     * variable Spring sẽ hiểu nhầm thành nhiều segment.
     */
    @DeleteMapping
    public ApiResponse<Void> delete(@RequestParam("publicId") String publicId) {
        fileService.delete(publicId);
        return ApiResponse.<Void>builder()
                .message("File deleted successfully")
                .build();
    }
}