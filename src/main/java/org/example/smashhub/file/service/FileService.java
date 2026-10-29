package org.example.smashhub.file.service;

import org.example.smashhub.file.dto.FileUploadResponse;
import org.example.smashhub.file.enums.FileFolder;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * Upload file lên Cloudinary vào đúng thư mục tương ứng với {@code folder}.
     * Không dùng trong transaction DB — không có cơ chế rollback tự động.
     * Dùng {@link #uploadInTransaction} khi đang trong một transaction.
     */
    FileUploadResponse upload(MultipartFile file, FileFolder folder);

    /**
     * Xóa file trên Cloudinary ngay lập tức theo {@code publicId}.
     * Chỉ gọi trực tiếp từ các endpoint admin không có transaction DB kèm theo.
     * Trong transaction, dùng {@link #deleteAfterCommit}.
     */
    void delete(String publicId);

    /**
     * Upload file trong phạm vi một transaction DB đang hoạt động.
     * Nếu transaction rollback, ảnh vừa upload trên Cloudinary sẽ được dọn tự động.
     * Nếu không có transaction, hoạt động giống {@link #upload}.
     */
    FileUploadResponse uploadInTransaction(MultipartFile file, FileFolder folder);

    /**
     * Đăng ký xóa file trên Cloudinary SAU KHI transaction commit thành công.
     * Nếu không có transaction đang hoạt động, xóa ngay (fallback an toàn).
     * Lỗi xóa ảnh chỉ được log, không throw để tránh làm hỏng dữ liệu DB đã commit.
     */
    void deleteAfterCommit(String publicId);

    /** Kiểm tra file có hợp lệ (non-null, non-empty) để upload không. */
    default boolean isPresent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }
}
