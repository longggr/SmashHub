package org.example.smashhub.file.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.example.smashhub.file.dto.FileUploadResponse;
import org.example.smashhub.file.enums.FileFolder;
import org.example.smashhub.file.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileServiceImpl implements FileService {

    // ---- Các MIME type client khai báo được chấp nhận ----
    static Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    // ---- Magic bytes để xác minh định dạng thực sự của file (không tin hoàn toàn MIME) ----
    // JPEG: FF D8 FF
    static byte[] MAGIC_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    // PNG:  89 50 4E 47
    static byte[] MAGIC_PNG  = {(byte) 0x89, 0x50, 0x4E, 0x47};
    // WEBP: RIFF....WEBP (byte 0-3: RIFF, byte 8-11: WEBP)
    static byte[] MAGIC_RIFF = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
    static byte[] MAGIC_WEBP = {0x57, 0x45, 0x42, 0x50}; // "WEBP"

    static long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    Cloudinary cloudinary;

    @NonFinal
    @Value("${cloudinary.folder}")
    String rootFolder;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    @Override
    public FileUploadResponse upload(MultipartFile file, FileFolder folder) {
        validate(file);
        String targetFolder = folder.resolve(rootFolder);
        return doUpload(file, targetFolder);
    }

    @Override
    public FileUploadResponse uploadInTransaction(MultipartFile file, FileFolder folder) {
        FileUploadResponse uploaded = upload(file, folder);
        String publicId = uploaded.getPublicId();
        // Nếu đang trong transaction: rollback → dọn ảnh; commit → giữ nguyên.
        runAfterCompletion(status -> {
            if (status != TransactionSynchronization.STATUS_COMMITTED) {
                log.info("Transaction rolled back — deleting uploaded Cloudinary asset: {}", publicId);
                deleteQuietly(publicId);
            }
        });
        return uploaded;
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            Map<?, ?> result = cloudinary.uploader()
                    .destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
            // Cloudinary trả "not found" nếu ảnh không tồn tại — log cảnh báo nhưng không throw.
            Object resultCode = result.get("result");
            if (!"ok".equals(resultCode)) {
                log.warn("Cloudinary destroy returned '{}' for publicId={}", resultCode, publicId);
            }
        } catch (IOException e) {
            log.error("Delete file {} on Cloudinary failed", publicId, e);
            throw new AppException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    @Override
    public void deleteAfterCommit(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        runAfterCommit(() -> deleteQuietly(publicId));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private FileUploadResponse doUpload(MultipartFile file, String folder) {
        try {
            // Tên file ngẫu nhiên để tránh ghi đè ảnh khác trong cùng thư mục.
            String publicId = folder + "/" + UUID.randomUUID();
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image",
                            // overwrite=false vì đã dùng UUID nên không bao giờ trùng;
                            // giữ false để tránh vô tình ghi đè nếu logic UUID bị lỗi.
                            "overwrite", false
                    )
            );
            return FileUploadResponse.builder()
                    .url((String) result.get("secure_url"))
                    .publicId((String) result.get("public_id"))
                    .format((String) result.get("format"))
                    .bytes(result.get("bytes") == null ? null
                            : Long.valueOf(result.get("bytes").toString()))
                    .width(result.get("width") == null ? null
                            : Integer.valueOf(result.get("width").toString()))
                    .height(result.get("height") == null ? null
                            : Integer.valueOf(result.get("height").toString()))
                    .build();
        } catch (IOException e) {
            log.error("Upload file to Cloudinary failed (folder={})", folder, e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * Kiểm tra toàn diện file trước khi upload:
     * 1. Non-null, non-empty.
     * 2. Kích thước ≤ MAX_FILE_SIZE.
     * 3. MIME type client khai báo nằm trong whitelist.
     * 4. Magic bytes thực sự khớp với định dạng ảnh (không tin MIME type mù quáng).
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
        if (!hasValidMagicBytes(file)) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
    }

    /**
     * Đọc tối đa 12 byte đầu để xác định định dạng thực của file.
     * Tránh trường hợp client gửi file HTML/script nhưng khai báo Content-Type là image/jpeg.
     */
    private boolean hasValidMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 3) return false;

            // JPEG
            if (startsWith(header, MAGIC_JPEG)) return true;
            // PNG
            if (read >= 4 && startsWith(header, MAGIC_PNG)) return true;
            // WEBP: bytes 0-3 = RIFF, bytes 8-11 = WEBP
            if (read >= 12 && startsWith(header, MAGIC_RIFF)) {
                byte[] webpMark = new byte[4];
                System.arraycopy(header, 8, webpMark, 0, 4);
                if (startsWith(webpMark, MAGIC_WEBP)) return true;
            }
            return false;
        } catch (IOException e) {
            log.warn("Cannot read magic bytes of uploaded file", e);
            return false;
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    /** Xóa ảnh mà không throw — dùng trong afterCompletion / afterCommit hooks. */
    private void deleteQuietly(String publicId) {
        try {
            delete(publicId);
        } catch (Exception e) {
            log.warn("Could not delete Cloudinary asset '{}' (will be orphaned): {}", publicId, e.getMessage());
        }
    }

    /**
     * Đăng ký action chạy sau khi transaction kết thúc (commit hoặc rollback).
     * Nếu không có transaction đang hoạt động thì không làm gì (caller tự quyết định fallback).
     */
    private void runAfterCompletion(IntConsumer action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                action.accept(status);
            }
        });
    }

    /**
     * Chạy action SAU KHI transaction commit. Nếu không có transaction, chạy ngay (fallback).
     * Đây là hành vi mong muốn cho "xóa ảnh cũ": nếu không có transaction (ví dụ: endpoint admin
     * xóa trực tiếp), xóa ngay; nếu đang trong transaction, chờ commit mới xóa.
     */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}