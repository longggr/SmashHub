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
import org.example.smashhub.file.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileServiceImpl implements FileService {
    static Set<String> ALLOWED_CONTENT_TYPES =Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    static long MAX_FILE_SIZE = 5 * 1024 * 1024;

    Cloudinary cloudinary;
    @NonFinal
    @Value("${cloudinary.folder}")
    String folder;

    @Override
    public FileUploadResponse upload(MultipartFile file) {
        validate(file);
        try {
            Map<?,?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",folder,
                            "resource_type", "image",
                            "overwrite", true
                    )
            );
            return FileUploadResponse.builder()
                    .url((String) result.get("secure_url"))
                    .publicId((String) result.get("public_id"))
                    .format((String) result.get("format"))
                    .bytes(result.get("bytes") == null ? null : Long.valueOf(result.get("bytes").toString()))
                    .width(result.get("width") == null ? null : Integer.valueOf(result.get("width").toString()))
                    .height(result.get("height") == null ? null : Integer.valueOf(result.get("height").toString()))
                    .build();

        } catch (IOException e) {
            log.error("upload file to Cloudinary failed", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }


    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            // Gọi API destroy của Cloudinary
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException e) {
            log.error("Delete file {} on Cloudinary failed", publicId, e);
            throw new AppException(ErrorCode.FILE_DELETE_FAILED);
        }
    }
    private void validate(MultipartFile file) {
        // Kiểm tra file có tồn tại hay không
        if (file == null || file.isEmpty())
            throw new AppException(ErrorCode.FILE_EMPTY);
        // Kiểm tra dung lượng
        if (file.getSize() > MAX_FILE_SIZE)
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        // Kiểm tra định dạng (MIME Type)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase()))
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
    }

}
