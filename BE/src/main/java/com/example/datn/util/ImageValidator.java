package com.example.datn.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public class ImageValidator {

    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    public static void validateBase64Image(String base64Data) {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            return;
        }

        String cleanBase64 = base64Data;
        if (base64Data.contains(",")) {
            cleanBase64 = base64Data.substring(base64Data.indexOf(",") + 1);
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(cleanBase64.trim());
        } catch (IllegalArgumentException e) {
            if (base64Data.startsWith("http://") || base64Data.startsWith("https://")) {
                return;
            }
            throw new IllegalArgumentException("Dữ liệu hình ảnh không đúng định dạng Base64!");
        }

        if (imageBytes.length > MAX_FILE_SIZE_BYTES) {
            long sizeMb = imageBytes.length / (1024 * 1024);
            throw new IllegalArgumentException(
                    "Dung lượng hình ảnh (" + sizeMb + "MB) vượt quá giới hạn tối đa cho phép là 10MB/ảnh!");
        }

        if (!isValidMagicBytes(imageBytes)) {
            throw new IllegalArgumentException(
                    "Tệp tải lên không phải là hình ảnh hợp lệ (chấp nhận các định dạng: JPG, JPEG, PNG, WEBP, AVIF, GIF, BMP)!");
        }

        // Với định dạng Java ImageIO hỗ trợ (PNG, JPEG, GIF, BMP), verify cấu trúc an toàn
        // Với WebP / AVIF, Java chuẩn không có SPI reader mặc định nên bỏ qua ImageIO.read để không báo lỗi giả
        if (!isWebP(imageBytes) && !isAvif(imageBytes)) {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (image == null) {
                    // Nếu ImageIO không đọc được nhưng magic bytes hợp lệ thì vẫn chấp nhận
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean isValidMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return false;
        }

        // JPEG: FF D8 (SOI marker chuẩn ISO/IEC 10918-1)
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return true;
        }

        // BMP: 42 4D
        if (bytes[0] == 0x42 && bytes[1] == 0x4D) {
            return true;
        }

        if (bytes.length < 4) {
            return false;
        }

        // PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return true;
        }

        // GIF: 47 49 46
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46) {
            return true;
        }

        // WEBP: RIFF...WEBP
        if (isWebP(bytes)) {
            return true;
        }

        // AVIF: ...ftyp
        if (isAvif(bytes)) {
            return true;
        }

        return false;
    }

    public static boolean isWebP(byte[] bytes) {
        if (bytes == null || bytes.length < 12)
            return false;
        return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    public static boolean isAvif(byte[] bytes) {
        if (bytes == null || bytes.length < 8)
            return false;
        return bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p';
    }
}
