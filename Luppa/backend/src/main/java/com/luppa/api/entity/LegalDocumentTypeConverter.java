package com.luppa.api.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LegalDocumentTypeConverter implements AttributeConverter<LegalDocumentType, String> {
    @Override
    public String convertToDatabaseColumn(LegalDocumentType attribute) {
        return attribute == null ? null : attribute.databaseValue();
    }

    @Override
    public LegalDocumentType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LegalDocumentType.fromDatabaseValue(dbData);
    }
}
