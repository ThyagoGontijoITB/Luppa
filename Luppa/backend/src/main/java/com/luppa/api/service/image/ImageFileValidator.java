package com.luppa.api.service.image;

import com.luppa.api.exception.InvalidRequestException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

public final class ImageFileValidator {
    private static final long MAX_SIZE = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private ImageFileValidator() {
    }

    public static ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Image file is required");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new InvalidRequestException("Image must be at most 2 MB");
        }
        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidRequestException("Only JPEG, PNG and WebP images are allowed");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new InvalidRequestException("Could not read image");
        }

        if (!matchesSignature(bytes, contentType)) {
            throw new InvalidRequestException("Invalid image file");
        }

        Integer width = null;
        Integer height = null;
        if (!"image/webp".equals(contentType)) {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image == null) {
                    throw new InvalidRequestException("Invalid image file");
                }
                width = image.getWidth();
                height = image.getHeight();
            } catch (IOException exception) {
                throw new InvalidRequestException("Invalid image file");
            }
        }
        return new ValidatedImage(bytes, contentType, file.getSize(), width, height);
    }

    private static boolean matchesSignature(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0D
                    && bytes[5] == 0x0A
                    && bytes[6] == 0x1A
                    && bytes[7] == 0x0A;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 0x52
                    && bytes[1] == 0x49
                    && bytes[2] == 0x46
                    && bytes[3] == 0x46
                    && bytes[8] == 0x57
                    && bytes[9] == 0x45
                    && bytes[10] == 0x42
                    && bytes[11] == 0x50;
            default -> false;
        };
    }

    public record ValidatedImage(
            byte[] bytes,
            String contentType,
            long fileSize,
            Integer width,
            Integer height
    ) {
    }
}
