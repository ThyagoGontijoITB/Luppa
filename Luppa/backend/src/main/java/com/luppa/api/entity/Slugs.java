package com.luppa.api.entity;

import java.text.Normalizer;
import java.util.Locale;

final class Slugs {
    private Slugs() {
    }

    static String from(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
