package org.example.smashhub.file.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;


@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum FileFolder {
    BRAND("brands"),
    CATEGORY("categories"),
    PRODUCT("products"),
    REVIEW("reviews"),
    AVATAR("avatars"),
    // Ảnh không thuộc loại nào ở trên (banner, ảnh tạm...)
    OTHER("others");


    String path;

    /** Ghép thư mục gốc + thư mục con, vd: ("smashhub", PRODUCT) -> "smashhub/products". */
    public String resolve(String rootFolder) {
        if (rootFolder == null || rootFolder.isBlank()) {
            return path;
        }
        String root = rootFolder.endsWith("/")
                ? rootFolder.substring(0, rootFolder.length() - 1)
                : rootFolder;
        return root + "/" + path;
    }
}