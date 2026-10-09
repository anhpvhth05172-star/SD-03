package com.example.datn.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public class ImageValidator {

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

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
                    "Dung lượng hình ảnh (" + sizeMb + "MB) vượt quá giới hạn tối đa cho phép là 5MB/ảnh!");
        }

        if (!isValidMagicBytes(imageBytes)) {
            throw new IllegalArgumentException(
                    "Tệp tải lên không phải là hình ảnh hợp lệ (chỉ chấp nhận các định dạng thực tế: JPG, JPEG, PNG, WEBP)!");
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null && !isWebP(imageBytes)) {
                throw new IllegalArgumentException(
                        "Tệp hình ảnh bị hỏng hoặc chứa cấu trúc mã thực thi không an toàn!");
            }
        } catch (Exception e) {
            if (!isWebP(imageBytes)) {
                throw new IllegalArgumentException("Không thể đọc tệp hình ảnh: " + e.getMessage());
            }
        }
    }

    public static boolean isValidMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return false;
        }

        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return true;
        }

        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 &&
                bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return true;
        }

        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38) {
            return true;
        }

        return isWebP(bytes);
    }

    private static boolean isWebP(byte[] bytes) {
        if (bytes == null || bytes.length < 12)
            return false;
        return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }
}
