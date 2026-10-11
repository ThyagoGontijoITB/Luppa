package com.luppa.api.entity;

public enum LegalDocumentType {
    TERMS_OF_USE("TERMOS_USO"),
    PRIVACY_POLICY("POLITICA_PRIVACIDADE");

    private final String databaseValue;

    LegalDocumentType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static LegalDocumentType fromDatabaseValue(String value) {
        for (LegalDocumentType type : values()) {
            if (type.databaseValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported legal document type: " + value);
    }
}
