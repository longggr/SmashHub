package org.example.smashhub.file.service;

import org.example.smashhub.file.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadResponse upload(MultipartFile file);
    void delete(String publicId);

}
