package com.luppa.api.service.image;

import com.luppa.api.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageFileValidatorTest {
    @Test
    void acceptsWebpSignatureWhenImageIoCannotDecodeWebp() {
        byte[] webp = new byte[]{
                0x52, 0x49, 0x46, 0x46,
                0x04, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50
        };

        ImageFileValidator.ValidatedImage image = ImageFileValidator.validate(
                new MockMultipartFile("file", "image.webp", "image/webp", webp)
        );

        assertThat(image.contentType()).isEqualTo("image/webp");
    }

    @Test
    void rejectsContentThatDoesNotMatchDeclaredImageType() {
        assertThatThrownBy(() -> ImageFileValidator.validate(
                new MockMultipartFile("file", "image.png", "image/png", new byte[]{1, 2, 3})
        )).isInstanceOf(InvalidRequestException.class);
    }
}
