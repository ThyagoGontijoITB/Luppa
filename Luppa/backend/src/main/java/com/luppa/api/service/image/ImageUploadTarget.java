package com.luppa.api.service.image;

public enum ImageUploadTarget {
    EVENT_COVER("events"),
    PROFILE("profiles");

    private final String folder;

    ImageUploadTarget(String folder) {
        this.folder = folder;
    }

    public String folder() {
        return folder;
    }
}
