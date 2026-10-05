package org.example.smashhub.file.controller;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.common.response.ApiResponse;
import org.example.smashhub.file.dto.FileUploadResponse;
import org.example.smashhub.file.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileController {
    FileService fileService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.<FileUploadResponse>builder()
                .result(fileService.upload(file))
                .message("File uploaded successfully")
                .build();
    }

    /**
     * Dung @RequestParam thay vi @PathVariable vi public_id cua Cloudinary
     * thuong co dang "smashhub/ten-file" (chua dau '/'), neu de path variable
     * Spring se hieu nham la 2 segment khac nhau.
     */
    @DeleteMapping
    ApiResponse<Void> delete(@RequestParam("publicId") String publicId) {
        fileService.delete(publicId);
        return ApiResponse.<Void>builder()
                .message("File deleted successfully")
                .build();
    }
}
