package com.luppa.api.service.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luppa.api.config.CloudinaryProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class CloudinaryImageStorage implements ImageStorage {
    private final CloudinaryProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public CloudinaryImageStorage(CloudinaryProperties properties, ObjectMapper objectMapper) {
        this(properties, HttpClient.newHttpClient(), objectMapper, Clock.systemUTC());
    }

    CloudinaryImageStorage(
            CloudinaryProperties properties,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public StoredImage upload(MultipartFile file, ImageUploadTarget target) {
        requireConfigured();
        ImageFileValidator.ValidatedImage image = ImageFileValidator.validate(file);
        String publicId = properties.normalizedRootFolder() + "/" + target.folder() + "/" + UUID.randomUUID();

        Map<String, String> fields = new TreeMap<>();
        fields.put("api_key", properties.apiKey());
        fields.put("public_id", publicId);
        fields.put("timestamp", timestamp());
        fields.put("signature", signature(Map.of(
                "public_id", publicId,
                "timestamp", fields.get("timestamp")
        )));

        JsonNode response = send("upload", multipart(fields, fileName(file, publicId), image));
        String returnedPublicId = requiredText(response, "public_id");
        String secureUrl = requiredText(response, "secure_url");
        long bytes = response.path("bytes").asLong(image.fileSize());
        Integer width = response.hasNonNull("width") ? response.path("width").asInt() : image.width();
        Integer height = response.hasNonNull("height") ? response.path("height").asInt() : image.height();

        return new StoredImage(returnedPublicId, secureUrl, image.contentType(), bytes, width, height);
    }

    @Override
    public void delete(String reference) {
        if (!isManagedReference(reference)) {
            return;
        }
        requireConfigured();

        String timestamp = timestamp();
        Map<String, String> fields = new TreeMap<>();
        fields.put("api_key", properties.apiKey());
        fields.put("public_id", reference);
        fields.put("timestamp", timestamp);
        fields.put("signature", signature(Map.of(
                "public_id", reference,
                "timestamp", timestamp
        )));

        send("destroy", multipart(fields, null, null));
    }

    @Override
    public boolean isManagedReference(String reference) {
        return properties.isManagedPublicId(reference);
    }

    private JsonNode send(String action, MultipartBody body) {
        HttpRequest request = HttpRequest.newBuilder(endpoint(action))
                .header("Content-Type", "multipart/form-data; boundary=" + body.boundary())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.bytes()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ImageStorageException("Cloudinary request failed");
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new ImageStorageException("Cloudinary request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ImageStorageException("Cloudinary request interrupted", exception);
        }
    }

    private MultipartBody multipart(
            Map<String, String> fields,
            String fileName,
            ImageFileValidator.ValidatedImage image
    ) {
        String boundary = "----luppa-cloudinary-" + UUID.randomUUID();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        fields.forEach((name, value) -> writeField(output, boundary, name, value));
        if (image != null) {
            writeFile(output, boundary, fileName, image);
        }
        write(output, "--" + boundary + "--\r\n");
        return new MultipartBody(boundary, output.toByteArray());
    }

    private void writeField(ByteArrayOutputStream output, String boundary, String name, String value) {
        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        write(output, value + "\r\n");
    }

    private void writeFile(
            ByteArrayOutputStream output,
            String boundary,
            String fileName,
            ImageFileValidator.ValidatedImage image
    ) {
        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"file\"; filename=\"" + sanitizeFileName(fileName) + "\"\r\n");
        write(output, "Content-Type: " + image.contentType() + "\r\n\r\n");
        try {
            output.write(image.bytes());
        } catch (IOException exception) {
            throw new ImageStorageException("Could not prepare image upload", exception);
        }
        write(output, "\r\n");
    }

    private void write(ByteArrayOutputStream output, String value) {
        try {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new ImageStorageException("Could not prepare Cloudinary request", exception);
        }
    }

    private String signature(Map<String, String> params) {
        StringBuilder payload = new StringBuilder();
        new TreeMap<>(params).forEach((key, value) -> {
            if (!payload.isEmpty()) {
                payload.append('&');
            }
            payload.append(key).append('=').append(value);
        });
        payload.append(properties.apiSecret());
        return sha1(payload.toString());
    }

    private String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new ImageStorageException("SHA-1 is not available", exception);
        }
    }

    private URI endpoint(String action) {
        return URI.create("https://api.cloudinary.com/v1_1/" + properties.cloudName() + "/image/" + action);
    }

    private String timestamp() {
        return String.valueOf(clock.instant().getEpochSecond());
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("");
        if (value.isBlank()) {
            throw new ImageStorageException("Cloudinary response did not include " + field);
        }
        return value;
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new ImageStorageException("Cloudinary is not configured");
        }
    }

    private String fileName(MultipartFile file, String publicId) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return publicId.substring(publicId.lastIndexOf('/') + 1);
        }
        return original;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replace("\\", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }

    private record MultipartBody(String boundary, byte[] bytes) {
    }
}
